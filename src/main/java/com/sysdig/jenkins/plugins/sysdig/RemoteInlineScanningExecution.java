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
import com.github.dockerjava.api.model.WaitResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.netty.NettyDockerCmdExecFactory;
import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningException;
import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningSubmission;
import com.sysdig.jenkins.plugins.sysdig.log.ConsoleLog;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.AbortException;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import org.jenkinsci.remoting.RoleChecker;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class RemoteInlineScanningExecution implements Callable<ImageScanningSubmission, Exception>, Serializable {
  private static final String anchoreVersion = "0.6.1"; // TODO do NOT hardcode this, retrieve it from /api/scanning/v1/anchore/status
  private static final String INLINE_SCAN_IMAGE = "docker.io/sysdiglabs/sysdig-inline-scan:latest"; // TODO: Get matching AIP version

  private final String imageName;
  private final String dockerfileContents;
  private final TaskListener listener;
  private BuildConfig config;
  private FilePath workspace;
  private SysdigLogger logger;


  public RemoteInlineScanningExecution(String imageName, String dockerfileContents, TaskListener listener, BuildConfig config, FilePath workspace) throws AbortException {
    this.imageName = imageName;
    this.dockerfileContents = dockerfileContents;
    this.listener = listener;
    this.config = config;
    this.workspace = workspace;
    this.logger = new ConsoleLog("InlineScanner", this.listener.getLogger(), false);
  }

  @Override
  public ImageScanningSubmission call() throws Exception {
    DockerClient dockerClient = DockerClientBuilder
      .getInstance()
      .withDockerCmdExecFactory(new NettyDockerCmdExecFactory())
      .build();
    return scanImage(config, dockerClient, imageName, dockerfileContents);
  }

  @Override
  public void checkRoles(RoleChecker checker) throws SecurityException {

  }

  private ImageScanningSubmission scanImage(BuildConfig config, DockerClient dockerClient, String imageName, String dockerFileContents) throws ImageScanningException, IOException, InterruptedException {
    logger.logInfo(String.format("Pulling inline-scan image %s", INLINE_SCAN_IMAGE));
    dockerClient.pullImageCmd(INLINE_SCAN_IMAGE).start().awaitCompletion();


    logger.logInfo(String.format("Creating container for scanning with image: %s", INLINE_SCAN_IMAGE));
    List<String> args = Arrays.asList("-D", "-j", "/out/output.json", imageName);
    String scanningContainerID = this.createScanningContainer(dockerClient, args);

    logger.logInfo(String.format("Executing Inline Scanning"));
    String scanOutput = this.performScanInContainer(dockerClient, scanningContainerID);
    //TODO: output is just the result code. How to stream container output?
    //TODO: This is apparently the container exit code, not the internal one? from docker? or what? always 1
    logger.logInfo("Exit code:" + scanOutput);

    //TODO: Get JSON from $workspace/jsonoutput/output.json

    logger.logInfo("Sending results to Sysdig Secure");
    //TODO: Get tag and digest from json output.
    //TODO: We could directly get the report, and skip querying the backend!
    String tag = "/localbuild/sysdigcicd/cronagent:sysdig-line-scan";
    String digest = "sha256:9be6c870235fa9d4843889d7e70f7e50a3d177df12c86e37740f313b926a49ef";
    return new ImageScanningSubmission(tag, digest);
  }

  private String performScanInContainer(DockerClient dockerClient, String scanningContainerID) throws InterruptedException {
    ResultCallbackTemplate logCallback = new ResultCallback.Adapter<Frame>() {

      @Override
      public void onNext(Frame item) {
        logger.logInfo(new String(item.getPayload(), StandardCharsets.UTF_8).replaceAll("\\s+$", ""));
        super.onNext(item);
      }
    };

    ResultCallbackTemplate resultCallback = new ResultCallback.Adapter<WaitResponse>() {
      private int statusCode;

      @Override
      public void onNext(WaitResponse item) {
        this.statusCode = item.getStatusCode();
        super.onNext(item);
      }

      @Override
      public String toString() {
        return Integer.toString(statusCode);
      }
    };    

    dockerClient.startContainerCmd(scanningContainerID)
      .exec();

    dockerClient.attachContainerCmd(scanningContainerID)
      .withStdOut(true)
      .withStdErr(true)
      .withFollowStream(true)
      .withLogs(true)
      .withLogs(true)
      .exec(logCallback)
      .awaitCompletion();

    dockerClient.waitContainerCmd(scanningContainerID)
      .exec(resultCallback)
      .awaitCompletion();

    return resultCallback.toString();
  }

  /**
   * Creates a container with the Inline Scan image
   * @param dockerClient
   * @return The created container ID.
   */
  private String createScanningContainer(DockerClient dockerClient, List<String> args) throws IOException, InterruptedException {
    Volume dockerSocket = new Volume("/var/run/docker.sock");
    File outputdir = new File(this.workspace.toString(), "jsonoutput");
    outputdir.mkdirs();
    outputdir.setWritable(true);
    outputdir.setExecutable(true);
    Volume output = new Volume(outputdir.toString());

    CreateContainerResponse createdScanningContainer = dockerClient.createContainerCmd(INLINE_SCAN_IMAGE)
      .withCmd(args.toArray(new String[args.size()]))
      .withEnv("SYSDIG_API_TOKEN="+ this.config.getSysdigToken())
      .withHostConfig(HostConfig.newHostConfig().withAutoRemove(true))
      .withBinds(
        Bind.parse("/var/run/docker.sock:/var/run/docker.sock"),
        Bind.parse(outputdir.toString() + ":/out"))
      .exec();

    return createdScanningContainer.getId();
  }
}