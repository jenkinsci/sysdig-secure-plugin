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
package com.sysdig.jenkins.plugins.sysdig.scanner;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.netty.NettyDockerCmdExecFactory;
import com.sysdig.jenkins.plugins.sysdig.BuildConfig;
import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningException;
import com.sysdig.jenkins.plugins.sysdig.log.ConsoleLog;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.FilePath;
import hudson.model.TaskListener;
import net.sf.json.JSONObject;
import hudson.remoting.Callable;
import org.jenkinsci.remoting.RoleChecker;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class InlineScannerRemoteExecutor implements Callable<JSONObject, Exception>, Serializable {
  private static final String INLINE_SCAN_IMAGE = "quay.io/sysdig/secure-inline-scan:2";

  private final String imageName;
  private final FilePath dockerFile;
  private final BuildConfig config;
  private final SysdigLogger logger;

  public InlineScannerRemoteExecutor(String imageName, FilePath dockerFile, TaskListener listener, BuildConfig config) {
    this.imageName = imageName;
    this.dockerFile = dockerFile;
    this.config = config;
    this.logger = new ConsoleLog(this.getClass().getSimpleName(), listener.getLogger(), false);
  }

  @Override
  public JSONObject call() throws Exception {
    DockerClient dockerClient = DockerClientBuilder
      .getInstance()
      .withDockerCmdExecFactory(new NettyDockerCmdExecFactory())
      .build();
    return scanImage(dockerClient);
  }

  @Override
  public void checkRoles(RoleChecker checker) throws SecurityException {

  }

  public JSONObject scanImage(DockerClient dockerClient) throws InterruptedException, ImageScanningException {
    //TODO(airadier): dockerFileContents
    logger.logInfo(String.format("Pulling inline-scan image %s", INLINE_SCAN_IMAGE));
    dockerClient.pullImageCmd(INLINE_SCAN_IMAGE).start().awaitCompletion();

    logger.logInfo(String.format("Creating container for scanning with image: %s", INLINE_SCAN_IMAGE));
    List<String> args = Arrays.asList("--storage-type=docker-daemon", "--format=JSON", imageName);
    String scanningContainerID = this.createScanningContainer(dockerClient, args);

    logger.logInfo("Executing Inline Scanning...");
    String scanRawOutput = this.performScanInContainer(dockerClient, scanningContainerID);

    JSONObject scanOutput = JSONObject.fromObject(scanRawOutput);

    logger.logInfo("Inline Scanning output:\n" + scanOutput.getString("log"));
    if (scanOutput.has("error")) {
      throw new ImageScanningException(scanOutput.getString("error"));
    }

    return scanOutput;
  }

  private String performScanInContainer(DockerClient dockerClient, String scanningContainerID) throws InterruptedException {
    ResultCallbackTemplate<?, Frame> logCallback = new ResultCallback.Adapter<Frame>() {
      final StringBuilder builder = new StringBuilder();

      @Override
      public void onNext(Frame item) {
        builder.append(new String(item.getPayload(), StandardCharsets.UTF_8).replaceAll("\\s+$", ""));
        super.onNext(item);
      }

      @Override
      public String toString() {
        return builder.toString();
      }
    };

    dockerClient.startContainerCmd(scanningContainerID)
      .exec();

    dockerClient.logContainerCmd(scanningContainerID)
      .withStdOut(true)
      .withStdErr(true)
      .withFollowStream(true)
      .withTailAll()
      .exec(logCallback)
      .awaitCompletion();

    return logCallback.toString();
  }

  /**
   * Creates a container with the Inline Scan image
   * @param dockerClient Docker client
   * @param args args to the inline-scan command
   * @return The created container ID.
   */
  private String createScanningContainer(DockerClient dockerClient, List<String> args) {
    HostConfig hostConfig = HostConfig.newHostConfig()
      .withAutoRemove(true)
      .withBinds(
        Bind.parse("/var/run/docker.sock:/var/run/docker.sock"));

    CreateContainerResponse createdScanningContainer = dockerClient.createContainerCmd(INLINE_SCAN_IMAGE)
      .withCmd(args.toArray(new String[0]))
      .withEnv("SYSDIG_API_TOKEN="+ this.config.getSysdigToken())
      .withHostConfig(hostConfig)
      .exec();

    return createdScanningContainer.getId();
  }

}
