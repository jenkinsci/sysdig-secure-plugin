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
import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningResult;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.AbortException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import hudson.remoting.Callable;
import org.jenkinsci.remoting.RoleChecker;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class InlineScannerRemoteExecutor implements Callable<ImageScanningResult, Exception>, Serializable {
  private static final String anchoreVersion = "0.6.1"; // TODO do NOT hardcode this, retrieve it from /api/scanning/v1/anchore/status
  private static final String INLINE_SCAN_IMAGE = "quay.io/sysdig/secure-inline-scan:2";

  private final String imageName;
  private final String dockerfileContents;
  private final BuildConfig config;
  private final SysdigLogger logger;

  public InlineScannerRemoteExecutor(String imageName, String dockerfileContents, SysdigLogger logger, BuildConfig config) {
    this.imageName = imageName;
    this.dockerfileContents = dockerfileContents;
    this.config = config;
    this.logger = logger;
  }

  @Override
  public ImageScanningResult call() throws Exception {
    DockerClient dockerClient = DockerClientBuilder
      .getInstance()
      .withDockerCmdExecFactory(new NettyDockerCmdExecFactory())
      .build();
    return scanImage(dockerClient, imageName, dockerfileContents);
  }

  @Override
  public void checkRoles(RoleChecker checker) throws SecurityException {

  }

  private ImageScanningResult scanImage(DockerClient dockerClient, String imageName, String dockerFileContents) throws InterruptedException, ImageScanningException, AbortException {
    //TODO: dockerFileContents
    logger.logInfo(String.format("Pulling inline-scan image %s", INLINE_SCAN_IMAGE));
    dockerClient.pullImageCmd(INLINE_SCAN_IMAGE).start().awaitCompletion();

    logger.logInfo(String.format("Creating container for scanning with image: %s", INLINE_SCAN_IMAGE));
    List<String> args = Arrays.asList("-D", "-x", "-j", "/dev/stdout", imageName);
    String scanningContainerID = this.createScanningContainer(dockerClient, args);

    logger.logInfo("Executing Inline Scanning...");
    JSONObject scanOutput = JSONObject.fromObject(this.performScanInContainer(dockerClient, scanningContainerID));
    if (scanOutput.has("error")) {
      throw new ImageScanningException(scanOutput.getString("error"));
    }
    logger.logInfo("Inline Scanning output:\n" + scanOutput.getString("log"));

    JSONObject scanReport = scanOutput.getJSONArray("scanReport").getJSONObject(0);
    return buildImageScanningResult(scanReport, scanOutput.getString("digest"), scanOutput.getString("tag"));
  }

  private ImageScanningResult buildImageScanningResult(JSONObject scanReport, String imageDigest, String tag) throws AbortException {
    /*TODO(airadier): dup code with SysdigSecureClientImpl. Get this code out of the client, which should \
    just return the raw JSON, and process the JSON in the Scanner? */

    JSONObject tagEvalObj = scanReport.getJSONObject(imageDigest);
    JSONArray tagEvals = null;
    for (Object key : tagEvalObj.keySet()) {
      tagEvals = tagEvalObj.getJSONArray((String) key);
      break;
    }
    if (tagEvals == null) {
      throw new AbortException(String.format("Failed to analyze %s due to missing tag eval records in sysdig-secure-engine policy evaluation response", tag));
    }
    if (tagEvals.size() < 1) {
      return null;
    }

    String evalStatus = tagEvals.getJSONObject(0).getString("status");
    JSONObject gateResult = tagEvals.getJSONObject(0).getJSONObject("detail").getJSONObject("result").getJSONObject("result");

    return new ImageScanningResult(tag, imageDigest, evalStatus, gateResult);
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
   * @param dockerClient
   * @param args
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
