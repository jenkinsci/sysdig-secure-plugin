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
import com.sysdig.jenkins.plugins.sysdig.NewEngineBuildConfig;
import com.sysdig.jenkins.plugins.sysdig.SysdigBuilder;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.remoting.Callable;
import org.jenkinsci.remoting.RoleChecker;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NewEngineRemoteExecutor implements Callable<String, Exception>, Serializable {

  //TODO: ...
  private static final String SCAN_COMMAND = "TO-DO";
  private static final String VERBOSE_ARG = "--verbose";
  private static final String SKIP_TLS_ARG = "--sysdig-skip-tls";
  private static final String SYSDIG_URL_ARG = "--sysdig-url=%s";
  private static final String ON_PREM_ARG = "--on-prem";
  private static final String DOCKERFILE_ARG = "--dockerfile=/tmp/";
  private static final String DOCKERFILE_MOUNTPOINT = "/tmp/";

  private final String imageName;
  private final String dockerFile;
  private final NewEngineBuildConfig config;
  private final SysdigLogger logger;
  private final EnvVars envVars;

  public NewEngineRemoteExecutor(String imageName, String dockerFile, NewEngineBuildConfig config, SysdigLogger logger, EnvVars envVars) {
    this.imageName = imageName;
    this.dockerFile = dockerFile;
    this.config = config;
    this.logger = logger;
    this.envVars = envVars;
  }

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

    //TODO: Download

    //TODO: Prepare args and execute
//    List<String> args = new ArrayList<>();
//    args.add(SCAN_COMMAND);
//    args.addAll(Arrays.asList(SCAN_ARGS));
//    if (config.getDebug()) {
//      args.add(VERBOSE_ARG);
//    }
//    if (!config.getEngineverify()) {
//      args.add(SKIP_TLS_ARG);
//    }
//    args.add(imageName);
//    if (!config.getEngineurl().equals(SysdigBuilder.DescriptorImpl.DEFAULT_ENGINE_URL)) {
//      args.add(String.format(SYSDIG_URL_ARG, config.getEngineurl()));
//      args.add(ON_PREM_ARG);
//    }
//
//    List<String> containerEnvVars = new ArrayList<>();
//    containerEnvVars.add("SYSDIG_API_TOKEN=" + this.config.getSysdigToken());
//    containerEnvVars.add("SYSDIG_ADDED_BY=cicd-inline-scan");
//
//    logger.logDebug("System environment: " + System.getenv().toString());
//    logger.logDebug("Final environment: " + envVars);
//    logger.logDebug("Creating container with environment: " + containerEnvVars);
//    logger.logDebug("Bind mounts: " + bindMounts);
//
//    Container inlineScanContainer = containerRunner.createContainer(envVars.get("SYSDIG_OVERRIDE_INLINE_SCAN_IMAGE", config.getInlineScanImage()), Collections.singletonList(DUMMY_ENTRYPOINT), null, containerEnvVars, config.getRunAsUser(), bindMounts);
//
//    if (!Strings.isNullOrEmpty(config.getInlineScanExtraParams())) {
//      args.addAll(Arrays.asList(config.getInlineScanExtraParams().split(" ")));
//    }
//
//    final StringBuilder builder = new StringBuilder();
//
//    logger.logDebug("Executing command in container: " + args);
//    inlineScanContainer.exec(args, null, frame -> this.sendToBuilder(builder, frame), frame -> this.sendToDebugLog(logger, frame));

    //TODO: For exit code 2 (wrong params), just show the output (should not happen, but just in case)

    //return builder.toString();

    throw new AbortException("New engine not implemented");
  }

}
