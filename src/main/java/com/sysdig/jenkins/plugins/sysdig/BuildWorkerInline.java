package com.sysdig.jenkins.plugins.sysdig;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.netty.NettyDockerCmdExecFactory;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningException;
import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningSubmission;
import com.sysdig.jenkins.plugins.sysdig.client.SysdigSecureClient;
import com.sysdig.jenkins.plugins.sysdig.client.SysdigSecureClientImpl;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.antlr.v4.runtime.misc.NotNull;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.VFS;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * A helper class to ensure concurrent jobs don't step on each other's toes. Sysdig Secure plugin instantiates a new instance of this class
 * for each individual job i.e. invocation of perform(). Global and project configuration at the time of execution is loaded into
 * worker instance via its constructor. That specific worker instance is responsible for the bulk of the plugin operations for a given
 * job.
 */
public class BuildWorkerInline extends BuildWorker {
  private static final String INLINE_SCAN_IMAGE = "docker.io/anchore/inline-scan:v0.6.1";
  private static final String INLINE_CONTAINER_NAME = "inline-scan";


  public BuildWorkerInline(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, BuildConfig config) throws AbortException {
    super(build, workspace, launcher, listener, config);
  }

  @Override
  public @NotNull
  ArrayList<ImageScanningSubmission> scanImages(Map<String, String> imagesAndDockerfiles) throws AbortException, InterruptedException {

    SysdigSecureClient sysdigSecureClient = config.getEngineverify() ?
      SysdigSecureClientImpl.newClient(config.getSysdigToken(), config.getEngineurl()) :
      SysdigSecureClientImpl.newInsecureClient(config.getSysdigToken(), config.getEngineurl());

    DockerClient dockerClient = DockerClientBuilder
      .getInstance()
      .withDockerCmdExecFactory(new NettyDockerCmdExecFactory())
      .build();

    logger.logInfo(String.format("Pulling scanning image %s", INLINE_SCAN_IMAGE));
    dockerClient.pullImageCmd(INLINE_SCAN_IMAGE).start().awaitCompletion();

    ArrayList<ImageScanningSubmission> imageScanningSubmissions = new ArrayList<>();

    try {
      for (Map.Entry<String, String> entry : imagesAndDockerfiles.entrySet()) {
        ImageScanningSubmission submission = scanImage(sysdigSecureClient, dockerClient, entry.getKey(), entry.getValue());
        imageScanningSubmissions.add(submission);
      }
    } catch (Exception e) {
      throw new AbortException(e.toString());
    }

    return imageScanningSubmissions;
  }

  private ImageScanningSubmission scanImage(SysdigSecureClient sysdigSecureClient, DockerClient dockerClient, String imageName, String dockerFileContents) throws ImageScanningException, IOException, InterruptedException {
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

    logger.logInfo(String.format("%s image ID to scan: %s", imageName, imageID));


    List<String> args = Arrays.asList(
      "docker-entrypoint.sh",
      "analyze",
      "-i",
      imageID,
      "-d",
      imageDigest,
      "-u",
      sysdigSecureClient.getScanningAccount(),
      imageName);

    cleanUpContainers(dockerClient);

    logger.logInfo(String.format("Creating container for scanning with image: %s", INLINE_SCAN_IMAGE));
    String scanningContainerID = createScanningContainer(dockerClient);
    logger.logInfo(String.format("Created container for scanning: %s", scanningContainerID));

    logger.logInfo(String.format("Launching container for scanning: %s", scanningContainerID));
    dockerClient.startContainerCmd(scanningContainerID).exec();

    logger.logInfo(String.format("Copying image %s to container %s", imageName, scanningContainerID));
    copyImageToContainer(dockerClient, imageName, scanningContainerID);

    logger.logInfo(String.format("Executing Inline Scanning"));
    String scanOutput = performScanInContainer(dockerClient, args, scanningContainerID);
    logger.logInfo(scanOutput);

    logger.logInfo(String.format("Extracting results from scanning container %s", scanningContainerID));
    File resultsFromContainer = extractScanResultsFromContainer(dockerClient, scanningContainerID);

    logger.logInfo("Sending results to Sysdig Secure");
    ImageScanningSubmission submission = sysdigSecureClient.submitImageForScanning(imageID, imageName, imageDigest, resultsFromContainer);

    cleanUpContainers(dockerClient);

    return submission;

  }

  private String getDigestIDFromImage(DockerClient dockerClient, String imageName) throws InterruptedException {
    String imageToRetrieveDigest = "sysdiglabs/digest-id:latest";
    dockerClient.pullImageCmd(imageToRetrieveDigest).start().awaitCompletion();

    Bind dockerSocket = Bind.parse("/var/run/docker.sock:/var/run/docker.sock");
    CreateContainerResponse containerCreated = dockerClient.createContainerCmd(imageToRetrieveDigest)
      .withHostConfig(HostConfig.newHostConfig().withBinds(dockerSocket))
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
    Path resultFile = Paths.get("/tmp/image-analysis-archive.tgz");

    // Copy file from the container
    String tempTarFile = "/tmp/temp-image-analysis-archive.tar";
    InputStream copyToHostStream = dockerClient
      .copyArchiveFromContainerCmd(scanningContainerID, "/anchore-engine/image-analysis-archive.tgz")
      .exec();
    OutputStream tempTarFileStream = Files.newOutputStream(Paths.get(tempTarFile));
    IOUtils.copy(copyToHostStream, tempTarFileStream);
    copyToHostStream.close();

    // The container will extract it as .tar file with the real file inside, so we have to extract it
    FileObject fileObject = VFS.getManager().resolveFile(String.format("tar:%s!/image-analysis-archive.tgz", tempTarFile));

    // Copy it from the tar to disk
    InputStream inputStream = fileObject.getContent().getInputStream();
    OutputStream outputStream = Files.newOutputStream(resultFile);
    IOUtils.copy(inputStream, outputStream);
    fileObject.close();

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

  private static String createScanningContainer(DockerClient dockerClient) {
    CreateContainerResponse createdScanningContainer = dockerClient.createContainerCmd(INLINE_SCAN_IMAGE)
      .withName(INLINE_CONTAINER_NAME)
      .withEntrypoint("/bin/sh")
      .withCmd("-c", "sleep 3600") // 1 hour to scan the image should be enough
      .withAttachStdout(true)
      .withAttachStderr(true)
      .withTty(true)
      .exec();

    return createdScanningContainer.getId();
  }

  private static void copyImageToContainer(DockerClient dockerClient, String imageName, String scanningContainerID) throws ImageScanningException {
    try (InputStream imageToScan = dockerClient.saveImageCmd(imageName).exec()) {
      String imageBaseName = Iterables.getLast(Arrays.asList(imageName.split("/")), imageName.replaceAll("/", "_"));

      String imageTarFile = String.format("/tmp/%s.tar", imageBaseName);
      OutputStream imageTarFileOS = Files.newOutputStream(Paths.get(imageTarFile));
      IOUtils.copy(imageToScan, imageTarFileOS);

      dockerClient.copyArchiveToContainerCmd(scanningContainerID)
        .withHostResource(imageTarFile)
        .withRemotePath("/anchore-engine")
        .exec();

    } catch (Exception e) {
      throw new ImageScanningException(e);
    }
  }

  private void cleanUpContainers(DockerClient dockerClient) {
    List<Container> containerList = dockerClient.listContainersCmd()
      .withShowAll(true)
      .withNameFilter(Collections.singletonList(INLINE_CONTAINER_NAME))
      .exec();

    for (Container container : containerList) {
      logger.logInfo(String.format("Removing existing container %s: %s", container.getNames()[0], container.getId()));
      dockerClient.removeContainerCmd(container.getId()).withForce(true).exec();
    }
  }
}
