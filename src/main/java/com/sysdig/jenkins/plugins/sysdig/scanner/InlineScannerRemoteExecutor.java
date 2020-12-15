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

import com.sysdig.jenkins.plugins.sysdig.BuildConfig;
import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningException;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.ContainerRunner;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.DockerClientRunner;
import com.sysdig.jenkins.plugins.sysdig.log.ConsoleLog;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.FilePath;
import hudson.model.TaskListener;
import net.sf.json.JSONObject;
import hudson.remoting.Callable;
import org.jenkinsci.remoting.RoleChecker;

import java.io.*;
import java.util.Arrays;
import java.util.List;

public class InlineScannerRemoteExecutor implements Callable<JSONObject, Exception>, Serializable {
  private static final String INLINE_SCAN_IMAGE = "quay.io/sysdig/secure-inline-scan:2";

  private final String imageName;
  private final FilePath dockerFile;
  private final BuildConfig config;
  private final TaskListener listener;

  public InlineScannerRemoteExecutor(String imageName, FilePath dockerFile, TaskListener listener, BuildConfig config) {
    this.imageName = imageName;
    this.dockerFile = dockerFile;
    this.listener = listener;
    this.config = config;
  }

  @Override
  public JSONObject call() throws Exception {
    ContainerRunner runner = new DockerClientRunner(listener, config.getDebug());

    SysdigLogger logger = new ConsoleLog(
      this.getClass().getSimpleName(),
      listener.getLogger(),
      config.getDebug());

    return scanImage(runner, logger);
  }

  @Override
  public void checkRoles(RoleChecker checker) throws SecurityException {

  }

  public JSONObject scanImage(ContainerRunner containerRunner, SysdigLogger logger) throws InterruptedException, ImageScanningException {

    //TODO(airadier): dockerFileContents
    List<String> args = Arrays.asList("--storage-type=docker-daemon", "--format=JSON", imageName);
    List<String> envVars = Arrays.asList("SYSDIG_API_TOKEN="+ this.config.getSysdigToken());

    String scanRawOutput = containerRunner.runContainer(INLINE_SCAN_IMAGE, args, envVars);
    JSONObject scanOutput = JSONObject.fromObject(scanRawOutput);

    if (scanOutput.has("log")) {
      logger.logInfo("Inline Scanning output:\n" + scanOutput.getString("log"));
    }

    if (scanOutput.has("error")) {
      throw new ImageScanningException(scanOutput.getString("error"));
    }

    return scanOutput;
  }

}
