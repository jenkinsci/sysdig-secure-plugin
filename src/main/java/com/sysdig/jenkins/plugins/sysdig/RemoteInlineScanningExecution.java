/*
Copyright (C) 2016-2020 Sysdig

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.sysdig.jenkins.plugins.sysdig;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.netty.NettyDockerCmdExecFactory;
import com.google.common.collect.Iterables;
import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningException;
import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningSubmission;
import com.sysdig.jenkins.plugins.sysdig.client.SysdigSecureClient;
import com.sysdig.jenkins.plugins.sysdig.log.ConsoleLog;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.AbortException;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.VFS;
import org.jenkinsci.remoting.RoleChecker;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RemoteInlineScanningExecution implements Callable<ImageScanningSubmission, Exception>, Serializable {
  private static final String anchoreVersion = "0.6.1"; // TODO do NOT hardcode this, retrieve it from /api/scanning/v1/anchore/status
  private static final String INLINE_SCAN_IMAGE = "docker.io/anchore/anchore-engine:v" + anchoreVersion;
  public static final String RESULTS_PATH_INSIDE_SCANNING_CONTAINER = "/tmp/image-analysis-archive.tgz";

  private final String imageName;
  private final String dockerfileContents;
  private final TaskListener listener;
  private SysdigSecureClient sysdigSecureClient;


  public RemoteInlineScanningExecution(String imageName, String dockerfileContents, TaskListener listener, SysdigSecureClient sysdigSecureClient) {
    this.imageName = imageName;
    this.dockerfileContents = dockerfileContents;
    this.listener = listener;
    this.sysdigSecureClient = sysdigSecureClient;
  }

  @Override
  public ImageScanningSubmission call() throws Exception {
    DockerClient dockerClient = DockerClientBuilder
      .getInstance()
      .withDockerCmdExecFactory(new NettyDockerCmdExecFactory())
      .build();
    return scanImage(sysdigSecureClient, dockerClient, imageName, dockerfileContents);
  }

  @Override
  public void checkRoles(RoleChecker checker) throws SecurityException {

  }


  private ImageScanningSubmission scanImage(SysdigSecureClient sysdigSecureClient, DockerClient dockerClient, String imageName, String dockerFileContents) throws ImageScanningException, IOException, InterruptedException {
    SysdigLogger logger = new ConsoleLog("InlineScanner", this.listener.getLogger(), false);
    logger.logInfo(String.format("Pulling scanning image %s", INLINE_SCAN_IMAGE));
    dockerClient.pullImageCmd(INLINE_SCAN_IMAGE).start().awaitCompletion();
    logger.logInfo(String.format("Checking if the image %s to scan exists", imageName));
    if (dockerClient.listImagesCmd().withImageNameFilter(imageName).exec().isEmpty()) {
      throw new AbortException(String.format("Image %s not found", imageName));
    }

    logger.logInfo(String.format("Retrieving ID and Digest from image %s", imageName));
    String imageID = dockerClient.inspectImageCmd(imageName).exec().getId();
    if (imageID == null) {
      throw new ImageScanningException("Unable to retrieve the ID from image");
    }
    imageID = Iterables.getLast(Arrays.asList(imageID.split(":")));
    String imageDigest = getDigestIDFromImage(dockerClient, imageName);
    if (imageDigest == null || imageDigest.trim().isEmpty()) {
      throw new ImageScanningException("Unable to retrieve digest from the image, can't continue the scanning.");
    }

    logger.logInfo(String.format("%s image ID to scan: %s", imageName, imageID));



    logger.logInfo(String.format("Creating container for scanning with image: %s", INLINE_SCAN_IMAGE));
    String scanningContainerID = createScanningContainer(dockerClient);
    logger.logInfo(String.format("Created container for scanning: %s", scanningContainerID));

    logger.logInfo(String.format("Launching container for scanning: %s", scanningContainerID));
    dockerClient.startContainerCmd(scanningContainerID).exec();

    logger.logInfo(String.format("Copying image %s to scanning container %s", imageName, scanningContainerID));
    String remoteImagePath = copyImageToContainer(dockerClient, imageName, scanningContainerID);

    logger.logInfo(String.format("Executing Inline Scanning"));
    List<String> args = Arrays.asList(
      "anchore-manager",
      "analyzers",
      "exec",
      remoteImagePath,
      RESULTS_PATH_INSIDE_SCANNING_CONTAINER,
      "--image-id",
      imageID,
      "--digest",
      imageDigest,
      "--account-id",
      sysdigSecureClient.getScanningAccount(),
      "--tag",
      imageName);
    String scanOutput = performScanInContainer(dockerClient, args, scanningContainerID);
    logger.logInfo(scanOutput);

    logger.logInfo(String.format("Extracting results from scanning container: %s", scanningContainerID));
    File resultsFromContainer = extractScanResultsFromContainer(dockerClient, scanningContainerID);

    logger.logInfo(String.format("Removing scanning container: %s", scanningContainerID));
    dockerClient.removeContainerCmd(scanningContainerID).withRemoveVolumes(true).withForce(true).exec();

    logger.logInfo("Sending results to Sysdig Secure");
    ImageScanningSubmission submission = sysdigSecureClient.submitImageForScanning(imageID, imageName, imageDigest, resultsFromContainer);

    return submission;

  }

  private String getDigestIDFromImage(DockerClient dockerClient, String imageName) throws InterruptedException {
    String imageToRetrieveDigest = "sysdiglabs/digest-id:latest";
    dockerClient.pullImageCmd(imageToRetrieveDigest).start().awaitCompletion();

    Bind dockerSocket = Bind.parse("/var/run/docker.sock:/var/run/docker.sock");
    CreateContainerResponse containerCreated = dockerClient.createContainerCmd(imageToRetrieveDigest)
      .withHostConfig(HostConfig.newHostConfig().withBinds(dockerSocket).withSecurityOpts(Collections.singletonList("label:disable")))
      .withCmd("-c", "sleep 60") // 1 minute to retrieve the digest should be enough
      .withEntrypoint("/bin/sh")
      .withAttachStdout(true)
      .withAttachStderr(true)
      .withTty(true)
      .exec();

    dockerClient.startContainerCmd(containerCreated.getId()).exec();

    ResultCallbackTemplate resultCallback = new ResultCallback.Adapter<Frame>() {
      private StringBuffer logbuffer = new StringBuffer();

      @Override
      public void onNext(Frame item) {
        logbuffer.append(new String(item.getPayload(), StandardCharsets.UTF_8));
        super.onNext(item);
      }

      @Override
      public String toString() {
        return logbuffer.toString();
      }
    };

    String execID = dockerClient.execCreateCmd(containerCreated.getId())
      .withCmd("docker-entrypoint.sh", imageName)
      .withAttachStderr(true)
      .withAttachStdout(true)
      .withTty(true)
      .exec()
      .getId();

    dockerClient.execStartCmd(execID)
      .withTty(true)
      .exec(resultCallback)
      .awaitCompletion();

    dockerClient.removeContainerCmd(containerCreated.getId()).withForce(true).exec();

    return resultCallback.toString();
  }

  private static File extractScanResultsFromContainer(DockerClient dockerClient, String scanningContainerID) throws IOException {
    Path resultFile = Paths.get(String.format("/tmp/image-analysis-archive-%s.tgz", scanningContainerID.substring(5)));

    // Copy file from the container
    String tempTarFile = String.format("/tmp/temp-image-analysis-archive%s.tar", scanningContainerID.substring(5));
    Path tempTarPath = Paths.get(tempTarFile);

    InputStream copyToHostStream = dockerClient
      .copyArchiveFromContainerCmd(scanningContainerID, RESULTS_PATH_INSIDE_SCANNING_CONTAINER)
      .exec();
    OutputStream tempTarFileStream = Files.newOutputStream(tempTarPath);
    IOUtils.copy(copyToHostStream, tempTarFileStream);
    copyToHostStream.close();

    // The container will extract it as .tar file with the real file inside, so we have to extract it
    FileObject fileObject = VFS.getManager().resolveFile(String.format("tar:%s!/image-analysis-archive.tgz", tempTarFile));

    // Copy it from the tar to disk
    InputStream inputStream = fileObject.getContent().getInputStream();
    OutputStream outputStream = Files.newOutputStream(resultFile);
    IOUtils.copy(inputStream, outputStream);
    fileObject.close();

    Files.deleteIfExists(tempTarPath); // Remove temporary file

    return resultFile.toFile();
  }

  private static String performScanInContainer(DockerClient dockerClient, List<String> args, String scanningContainerID) throws InterruptedException {
    ResultCallbackTemplate resultCallback = new ResultCallback.Adapter<Frame>() {
      private StringBuffer logbuffer = new StringBuffer();

      @Override
      public void onNext(Frame item) {
        logbuffer.append(new String(item.getPayload(), StandardCharsets.UTF_8));
        super.onNext(item);
      }

      @Override
      public String toString() {
        return logbuffer.toString();
      }
    };

    String execID = dockerClient.execCreateCmd(scanningContainerID)
      .withCmd(args.toArray(new String[args.size()]))
      .withAttachStderr(true)
      .withAttachStdout(true)
      .withTty(true)
      .exec()
      .getId();

    dockerClient.execStartCmd(execID)
      .withTty(true)
      .exec(resultCallback)
      .awaitCompletion();

    return resultCallback.toString();
  }

  /**
   * Creates a container with the Inline Scan image, but forces it to sleep
   * for 1h, automatically removing the container after this time.
   * The scanning will be executed with an exec into this container.
   * The Sleep process is very lightweight, and can be left running if the
   * scanning fails for some reason, without impacting the performance.
   * @param dockerClient
   * @return The created container ID.
   */
  private static String createScanningContainer(DockerClient dockerClient) {
    CreateContainerResponse createdScanningContainer = dockerClient.createContainerCmd(INLINE_SCAN_IMAGE)
      .withEntrypoint("/bin/sh")
      .withCmd("-c", "sleep 3600") // 1 hour to scan the image should be enough
      .withAttachStdout(true)
      .withAttachStderr(true)
      .withTty(true)
      .withHostConfig(HostConfig.newHostConfig().withAutoRemove(true))
      .withEnv("ANCHORE_DB_HOST=useless", "ANCHORE_DB_USER=useless", "ANCHORE_DB_PASSWORD=useless")
      .exec();

    return createdScanningContainer.getId();
  }

  private static String copyImageToContainer(DockerClient dockerClient, String imageName, String scanningContainerID) throws ImageScanningException {
    try (InputStream imageToScan = dockerClient.saveImageCmd(imageName).exec()) {
      String imageBaseName = Iterables.getLast(Arrays.asList(imageName.split("/")), imageName).replaceAll("/|:|\\.", "_");

      String imageTarFile = String.format("/tmp/%s.tar", imageBaseName);
      Path imageTarPath = Paths.get(imageTarFile);
      OutputStream imageTarFileOS = Files.newOutputStream(imageTarPath);
      IOUtils.copy(imageToScan, imageTarFileOS);

      dockerClient.copyArchiveToContainerCmd(scanningContainerID)
        .withHostResource(imageTarFile)
        .withRemotePath("/tmp")
        .exec();

      Files.deleteIfExists(imageTarPath);
      return imageTarFile;
    } catch (Exception e) {
      throw new ImageScanningException(e);
    }
  }
}
