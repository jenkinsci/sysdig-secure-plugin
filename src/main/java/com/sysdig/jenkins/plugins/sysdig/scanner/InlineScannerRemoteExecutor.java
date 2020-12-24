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
import com.sysdig.jenkins.plugins.sysdig.ImageScanningException;
import com.sysdig.jenkins.plugins.sysdig.SysdigBuilder;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.Container;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.ContainerRunner;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.ContainerRunnerFactory;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.DockerClientContainerFactory;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.EnvVars;
import hudson.remoting.Callable;
import org.jenkinsci.remoting.RoleChecker;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class InlineScannerRemoteExecutor implements Callable<String, Exception>, Serializable {

  private static final String INLINE_SCAN_IMAGE = "quay.io/sysdig/secure-inline-scan:2";
  private static final String DUMMY_ENTRYPOINT = "cat";
  private static final String[] MKDIR_COMMAND = new String[]{"mkdir", "/tmp/sysdig-inline-scan"};
  private static final String[] TOUCH_COMMAND = new String[]{"touch", "/tmp/sysdig-inline-scan/info.log"};
  private static final String[] TAIL_COMMAND = new String[]{"tail", "-f", "/tmp/sysdig-inline-scan/info.log"};
  private static final String SCAN_COMMAND = "/sysdig-inline-scan.sh";
  private static final String[] SCAN_ARGS = new String[] {"--storage-type=docker-daemon","--format=JSON"};
  private static final String VERBOSE_ARG = "--verbose";
  private static final String SKIP_TLS_ARG = "--sysdig-skip-tls";
  private static final String SYSDIG_URL_ARG = "--sysdig-url=%s";
  private static final String ON_PREM_ARG = "--on-prem";
  private static final String DOCKERFILE_ARG = "--dockerfile=/tmp/Dockerfile";
  private static final String DOCKERFILE_MOUNTPOINT = "/tmp/Dockerfile";

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
  private final EnvVars nodeEnvVars;

  public InlineScannerRemoteExecutor(String imageName, String dockerFile, BuildConfig config, SysdigLogger logger, EnvVars nodeEnvVars) {
    this.imageName = imageName;
    this.dockerFile = dockerFile;
    this.config = config;
    this.logger = logger;
    this.nodeEnvVars = nodeEnvVars;
  }

  @Override
  public void checkRoles(RoleChecker checker) throws SecurityException { }

  @Override
  public String call() throws ImageScanningException, InterruptedException {
    ContainerRunner containerRunner = containerRunnerFactory.getContainerRunner(logger);

    List<String> args = new ArrayList<>();
    args.add(SCAN_COMMAND);
    args.addAll(Arrays.asList(SCAN_ARGS));
    if (config.getDebug()) {
      args.add(VERBOSE_ARG);
    }
    if (!config.getEngineTLSVerify()) {
      args.add(SKIP_TLS_ARG);
    }
    args.add(imageName);
    if (!config.getEngineURL().equals(SysdigBuilder.DescriptorImpl.DEFAULT_ENGINE_URL)) {
      args.add(String.format(SYSDIG_URL_ARG, config.getEngineURL()));
      args.add(ON_PREM_ARG);
    }

    List<String> envVars = new ArrayList<>();
    envVars.add("SYSDIG_API_TOKEN=" + this.config.getSysdigToken());
    envVars.add("SYSDIG_ADDED_BY=cicd-inline-scan");
    addProxyVars(nodeEnvVars, envVars, logger);

    List<String> bindMounts = new ArrayList<>();
    bindMounts.add("/var/run/docker.sock:/var/run/docker.sock");

    if (!Strings.isNullOrEmpty(dockerFile)) {
      args.add(DOCKERFILE_ARG);
      bindMounts.add(String.format("%s:%s", dockerFile, DOCKERFILE_MOUNTPOINT));
    }

    logger.logDebug("System environment: " + System.getenv().toString());
    logger.logDebug("Node environment: " + nodeEnvVars.toString());
    logger.logDebug("Creating container with environment: " + envVars.toString());
    logger.logDebug("Bind mounts: " + bindMounts.toString());

    Container inlineScanContainer;
    try {
      inlineScanContainer = containerRunner.createContainer(INLINE_SCAN_IMAGE, Collections.singletonList(DUMMY_ENTRYPOINT), null, envVars, bindMounts);
    } catch (InterruptedException e) {
      throw new ImageScanningException("Error creating inline-scan container", e);
    }

    try {
      //TODO: Get exit code in run and exec?
      prepareInlineScanExecution(inlineScanContainer);
    } catch (InterruptedException e) {
      throw new ImageScanningException("Error executing command in inline-scan container", e);
    }

    try {
      ExecutorService executor = Executors.newSingleThreadExecutor();
      Future<String> scanTask = executor.submit(() -> executeInlineScan(inlineScanContainer, args));

      //TODO(airadier): return an exit code too

      // This way the main thread can be interrupted to abort the scanning
      return scanTask.get();
    } catch (ExecutionException e) {
      throw new ImageScanningException("Error executing command in inline-scan container", e);
    } finally {
      inlineScanContainer.stop(STOP_SECONDS);
    }
  }

  private void prepareInlineScanExecution(Container inlineScanContainer) throws InterruptedException {
    inlineScanContainer.runAsync(frame -> this.sendToLog(logger, frame), frame -> this.sendToLog(logger, frame));

    logger.logDebug("Creating TMP dir in container: " + Arrays.asList(MKDIR_COMMAND).toString());
    inlineScanContainer.exec(Arrays.asList(MKDIR_COMMAND), null, frame -> this.sendToLog(logger, frame), frame -> this.sendToLog(logger, frame));
    logger.logDebug("Creating log file: " + Arrays.asList(TOUCH_COMMAND).toString());
    inlineScanContainer.exec(Arrays.asList(TOUCH_COMMAND), null,  frame -> this.sendToLog(logger, frame), frame -> this.sendToLog(logger, frame));
    logger.logDebug("Tailing log file output: " + Arrays.asList(TAIL_COMMAND).toString());
    inlineScanContainer.execAsync(Arrays.asList(TAIL_COMMAND), null, frame -> this.sendToLog(logger, frame), frame -> this.sendToLog(logger, frame));
  }

  private String executeInlineScan(Container inlineScanContainer, List<String> args) throws InterruptedException {
    logger.logDebug("Executing inline-scan script in container: " + args.toString());
    final StringBuilder builder = new StringBuilder();
    inlineScanContainer.exec(args, null, builder::append, frame -> this.sendToLog(logger, frame));
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

  private void sendToLog(SysdigLogger logger, String frame) {
    for (String line: frame.split("[\n\r]")) {
      logger.logInfo(line);
    }
  }
}
