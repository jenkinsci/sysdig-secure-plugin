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
import com.sysdig.jenkins.plugins.sysdig.SysdigBuilder;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.Container;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.ContainerRunner;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.ContainerRunnerFactory;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.DockerClientContainerFactory;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.Util;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.remoting.Callable;
import org.jenkinsci.remoting.RoleChecker;

import java.io.*;
import java.util.*;

public class InlineScannerRemoteExecutor implements Callable<String, Exception>, Serializable {

  private static final String DUMMY_ENTRYPOINT = "cat";
  private static final String[] MKDIR_COMMAND = new String[]{"mkdir", "-p", "/tmp/sysdig-inline-scan/logs"};
  private static final String[] TOUCH_COMMAND = new String[]{"touch", "/tmp/sysdig-inline-scan/logs/info.log"};
  private static final String[] TAIL_COMMAND = new String[]{"tail", "-f", "/tmp/sysdig-inline-scan/logs/info.log"};
  private static final String SCAN_COMMAND = "/sysdig-inline-scan.sh";
  private static final String[] SCAN_ARGS = new String[] {
    "--storage-type=docker-daemon",
    "--format=JSON"};
  private static final String VERBOSE_ARG = "--verbose";
  private static final String SKIP_TLS_ARG = "--sysdig-skip-tls";
  private static final String SYSDIG_URL_ARG = "--sysdig-url=%s";
  private static final String ON_PREM_ARG = "--on-prem";
  private static final String DOCKERFILE_ARG = "--dockerfile=/tmp/";
  private static final String DOCKERFILE_MOUNTPOINT = "/tmp/";

  private static final int STOP_SECONDS = 1;

  // Use a default container runner factory, but allow overriding for mocks in tests
  private static ContainerRunnerFactory containerRunnerFactory = new DockerClientContainerFactory();

  public static void setContainerRunnerFactory(ContainerRunnerFactory containerRunnerFactory) {
    InlineScannerRemoteExecutor.containerRunnerFactory = containerRunnerFactory;
  }

  private final String imageName;
  private final String dockerFile;
  private final BuildConfig config;
  private final SysdigLogger logger;
  private final EnvVars envVars;

  public InlineScannerRemoteExecutor(String imageName, String dockerFile, BuildConfig config, SysdigLogger logger, EnvVars envVars) {
    this.imageName = imageName;
    this.dockerFile = dockerFile;
    this.config = config;
    this.logger = logger;
    this.envVars = envVars;
  }

  static final String DEFAULT_DOCKER_VOLUME = "/var/run/docker.sock";

  @Override
  public void checkRoles(RoleChecker checker) throws SecurityException { }
  @Override

  public String call() throws InterruptedException, AbortException {

    if (!Strings.isNullOrEmpty(dockerFile)) {
      File f = new File(dockerFile);
      if (!f.exists()) {
        throw new AbortException("Dockerfile '" + dockerFile + "' does not exist");
      }
    }

    List<String> bindMounts = new ArrayList<>();
    String dockerVolumeInContainer = null;

    // see https://github.com/jenkinsci/sysdig-secure-plugin/pull/55 discussion
    if (envVars.containsKey("DOCKER_HOST")) {
      String candidateVolumeHostPath = envVars.get("DOCKER_HOST");
      if (!candidateVolumeHostPath.startsWith("/")){
        dockerVolumeInContainer = candidateVolumeHostPath;
      } else {
        if (Util.isExistingFile(candidateVolumeHostPath)) {
          bindMounts.add(candidateVolumeHostPath + ":" + DEFAULT_DOCKER_VOLUME);
        } else {
          throw new AbortException("Daemon socket '" + candidateVolumeHostPath + "' does not exist");
        }
      }
    } else {
      bindMounts.add(DEFAULT_DOCKER_VOLUME + ":" + DEFAULT_DOCKER_VOLUME);
    }

    ContainerRunner containerRunner = containerRunnerFactory.getContainerRunner(logger, envVars, dockerVolumeInContainer);
    Timer cmdExecPingTimer = null;

    List<String> args = new ArrayList<>();
    args.add(SCAN_COMMAND);
    args.addAll(Arrays.asList(SCAN_ARGS));
    if (config.getDebug()) {
      args.add(VERBOSE_ARG);
    }
    if (!config.getEngineverify()) {
      args.add(SKIP_TLS_ARG);
    }
    args.add(imageName);
    if (!config.getEngineurl().equals(SysdigBuilder.DescriptorImpl.DEFAULT_ENGINE_URL)) {
      args.add(String.format(SYSDIG_URL_ARG, config.getEngineurl()));
      args.add(ON_PREM_ARG);
    }

    List<String> containerEnvVars = new ArrayList<>();
    containerEnvVars.add("SYSDIG_API_TOKEN=" + this.config.getSysdigToken());
    containerEnvVars.add("SYSDIG_ADDED_BY=cicd-inline-scan");
    addProxyVars(envVars, containerEnvVars, logger);



    logger.logDebug("System environment: " + System.getenv().toString());
    logger.logDebug("Final environment: " + envVars);
    logger.logDebug("Creating container with environment: " + containerEnvVars);
    logger.logDebug("Bind mounts: " + bindMounts);

    Container inlineScanContainer = containerRunner.createContainer(envVars.get("SYSDIG_OVERRIDE_INLINE_SCAN_IMAGE", config.getInlineScanImage()), Collections.singletonList(DUMMY_ENTRYPOINT), null, containerEnvVars, config.getRunAsUser(), bindMounts);

    if (!Strings.isNullOrEmpty(dockerFile)) {
      File f = new File(dockerFile);
      logger.logDebug("Copying Dockerfile from " + f.getAbsolutePath() + " to " + DOCKERFILE_MOUNTPOINT + f.getName() + " inside container");
      inlineScanContainer.copy(dockerFile, DOCKERFILE_MOUNTPOINT);
      args.add(DOCKERFILE_ARG + f.getName());
    }

    if (!Strings.isNullOrEmpty(config.getInlineScanExtraParams())) {
      args.addAll(Arrays.asList(config.getInlineScanExtraParams().split(" ")));
    }

    final StringBuilder builder = new StringBuilder();

    try {
      //TODO: Get exit code in run and exec?
      inlineScanContainer.runAsync(frame -> this.sendToLog(logger, frame), frame -> this.sendToLog(logger, frame));

      inlineScanContainer.exec(Arrays.asList(MKDIR_COMMAND), null, frame -> this.sendToLog(logger, frame), frame -> this.sendToLog(logger, frame));
      inlineScanContainer.exec(Arrays.asList(TOUCH_COMMAND), null,  frame -> this.sendToLog(logger, frame), frame -> this.sendToLog(logger, frame));
      inlineScanContainer.execAsync(Arrays.asList(TAIL_COMMAND), null, frame -> this.sendToLog(logger, frame), frame -> this.sendToLog(logger, frame));

      if (this.envVars.get("DOCKER_CMD_EXEC_PING_DELAY")!=null) {
        String pingDelayStr = this.envVars.get("DOCKER_CMD_EXEC_PING_DELAY");
        try {
          long pingDelay = Long.parseLong(pingDelayStr);
          cmdExecPingTimer = new Timer();
          cmdExecPingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
              inlineScanContainer.ping();
            }
          }, pingDelay * 1000, pingDelay * 1000);
          logger.logDebug("Starting pinging to keep connection alive during command execution...");
        } catch (NumberFormatException e) {
          logger.logWarn(String.format("DOCKER_CMD_EXEC_PING_DELAY=%s is not valid", pingDelayStr));
        }
      }

      logger.logDebug("Executing command in container: " + args);
      inlineScanContainer.exec(args, null, frame -> this.sendToBuilder(builder, frame), frame -> this.sendToDebugLog(logger, frame));
    } finally {
      if (cmdExecPingTimer!=null){
        cmdExecPingTimer.cancel();
      }
      inlineScanContainer.stop(STOP_SECONDS);
    }

    //TODO: For exit code 2 (wrong params), just show the output (should not happen, but just in case)

    return builder.toString();
  }

  private void addProxyVars(EnvVars currentEnv, List<String> envVars, SysdigLogger logger) {
    String http_proxy = currentEnv.get("http_proxy");

    if (Strings.isNullOrEmpty(http_proxy)) {
      http_proxy = currentEnv.get("HTTP_PROXY");
      if (!Strings.isNullOrEmpty(http_proxy)) {
        logger.logDebug("HTTP proxy setting from env var HTTP_PROXY (http_proxy empty): " + http_proxy);
      }
    } else {
      logger.logDebug("HTTP proxy setting from env var http_proxy: " + http_proxy);
    }

    if (!Strings.isNullOrEmpty(http_proxy)) {
      envVars.add("http_proxy=" + http_proxy);
    }

    String https_proxy = currentEnv.get("https_proxy");

    if (Strings.isNullOrEmpty(https_proxy)) {
      https_proxy = currentEnv.get("HTTPS_PROXY");
      if (!Strings.isNullOrEmpty(https_proxy)) {
        logger.logDebug("HTTPS proxy setting from env var HTTPS_PROXY (https_proxy empty): " + https_proxy);
      }
    } else {
      logger.logDebug("HTTPS proxy setting from env var https_proxy: " + https_proxy);
    }

    if (Strings.isNullOrEmpty(https_proxy)) {
      https_proxy = http_proxy;
      if (!Strings.isNullOrEmpty(https_proxy)) {
        logger.logDebug("HTTPS proxy setting from env var http_proxy (https_proxy and HTTPS_PROXY empty): " + https_proxy);
      }
    }

    if (!Strings.isNullOrEmpty(https_proxy)) {
      envVars.add("https_proxy=" + https_proxy);
    }

    String no_proxy = currentEnv.get("no_proxy");

    if (Strings.isNullOrEmpty(no_proxy)) {
      no_proxy = currentEnv.get("NO_PROXY");
      if (!Strings.isNullOrEmpty(no_proxy)) {
        logger.logDebug("NO proxy setting from env var NO_PROXY (no_proxy empty): " + no_proxy);
      }
    } else {
      logger.logDebug("NO proxy setting from env var no_proxy: " + no_proxy);
    }

    if (!Strings.isNullOrEmpty(no_proxy)) {
      envVars.add("no_proxy=" + no_proxy);
    }
  }

  private void sendToBuilder(StringBuilder builder, String frame) {
    for (String line: frame.split("[\n\r]")) {
      // Workaround for older versions of inline-scan which can include some verbose output from "set -x", starting with "+ " in the stdout
      if (!line.startsWith("+ ")) {
        builder.append(line);
      }
    }
  }

  private void sendToLog(SysdigLogger logger, String frame) {
    for (String line: frame.split("[\n\r]")) {
      logger.logInfo(line);
    }
  }

  private void sendToDebugLog(SysdigLogger logger, String frame) {
    for (String line: frame.split("[\n\r]")) {
      logger.logDebug(line);
    }
  }
}
