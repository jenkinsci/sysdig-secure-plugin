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

import com.google.common.base.Strings;
import com.sysdig.jenkins.plugins.sysdig.BuildConfig;
import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningException;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.Container;
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
import java.util.*;

public class InlineScannerRemoteExecutor implements Callable<JSONObject, Exception>, Serializable {

  private static final String INLINE_SCAN_IMAGE = "quay.io/sysdig/secure-inline-scan:2";
  private static final String DUMMY_ENTRYPOINT = "cat";
  private static final String[] MKDIR_COMMAND = new String[]{"mkdir", "/tmp/sysdig-inline-scan"};
  private static final String[] TOUCH_COMMAND = new String[]{"touch", "/tmp/sysdig-inline-scan/info.log"};
  private static final String[] TAIL_COMMAND = new String[]{"tail", "-f", "/tmp/sysdig-inline-scan/info.log"};
  private static final String SCAN_COMMAND = "/sysdig-inline-scan.sh";
  private static final String[] SCAN_ARGS = new String[] {
    "--storage-type=docker-daemon",
    "--format=JSON"};

  private static final int STOP_SECONDS = 1;

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

    SysdigLogger logger = new ConsoleLog(
      "InlineScanner",
      listener.getLogger(),
      config.getDebug());

    ContainerRunner runner = new DockerClientRunner(logger, config.getDebug());

    return scanImage(runner, logger);
  }

  @Override
  public void checkRoles(RoleChecker checker) throws SecurityException {

  }

  public JSONObject scanImage(ContainerRunner containerRunner, SysdigLogger logger) throws InterruptedException, ImageScanningException {

    //TODO(airadier): dockerFileContents
    List<String> args = new ArrayList<String>();
    args.add(SCAN_COMMAND);
    args.addAll(Arrays.asList(SCAN_ARGS));
    args.add(imageName);

    List<String> envVars = new ArrayList<String>();
    envVars.add("SYSDIG_API_TOKEN=" + this.config.getSysdigToken());
    envVars.add("SYSDIG_ADDED_BY=cicd-inline-scan");

    addProxyVars(envVars);

    Container inlineScanContainer = containerRunner.createContainer(INLINE_SCAN_IMAGE, Collections.singletonList(DUMMY_ENTRYPOINT), null, envVars);
    final StringBuilder builder = new StringBuilder();

    try {
      //TODO: Get exit code in run and exec?
      inlineScanContainer.runAsync(null);

      inlineScanContainer.exec(Arrays.asList(MKDIR_COMMAND), null, null);
      inlineScanContainer.exec(Arrays.asList(TOUCH_COMMAND), null, null);
      inlineScanContainer.execAsync(Arrays.asList(TAIL_COMMAND), null, frame -> this.sendToLog(logger, frame) );

      inlineScanContainer.exec(args, null, builder::append);
    } finally {
      inlineScanContainer.stop(STOP_SECONDS);
    }

    JSONObject scanOutput = JSONObject.fromObject(builder.toString());

    //TODO: For exit code 2 (wrong params), just show the output (should not happen, but just in case)

    //TODO: Only if exit code 0 or 1 or 3.
    if (scanOutput.has("error")) {
      throw new ImageScanningException(scanOutput.getString("error"));
    }

    return scanOutput;
  }

  private void addProxyVars(List<String> envVars) {
    Map<String,String> currentEnv = System.getenv();
    String http_proxy = currentEnv.get("http_proxy");

    if (Strings.isNullOrEmpty(http_proxy)) {
      http_proxy = currentEnv.get("HTTP_PROXY");
    }

    if (!Strings.isNullOrEmpty(http_proxy)) {
      envVars.add("http_proxy=" + http_proxy);
    }

    String https_proxy = currentEnv.get("https_proxy");

    if (Strings.isNullOrEmpty(https_proxy)) {
      https_proxy = currentEnv.get("HTTPS_PROXY");
    }

    if (Strings.isNullOrEmpty(https_proxy)) {
      https_proxy = http_proxy;
    }

    if (!Strings.isNullOrEmpty(https_proxy)) {
      envVars.add("https_proxy=" + https_proxy);
    }

    String no_proxy = currentEnv.get("no_proxy");

    if (Strings.isNullOrEmpty(no_proxy)) {
      no_proxy = currentEnv.get("NO_PROXY");
    }

    if (!Strings.isNullOrEmpty(no_proxy)) {
      envVars.add("no_proxy=" + no_proxy);
    }
  }

  private void sendToLog(SysdigLogger logger, String frame) {
    for (String line: frame.split("[\n\r]")) {
      logger.logInfo(line);
    }
  }
}
