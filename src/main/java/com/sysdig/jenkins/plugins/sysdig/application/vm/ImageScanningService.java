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
package com.sysdig.jenkins.plugins.sysdig.application.vm;

import com.google.common.base.Strings;
import com.sysdig.jenkins.plugins.sysdig.BuildWorker;
import com.sysdig.jenkins.plugins.sysdig.NewEngineBuildConfig;
import com.sysdig.jenkins.plugins.sysdig.application.RunContext;
import com.sysdig.jenkins.plugins.sysdig.domain.ImageScanningResult;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.JenkinsReportStorage;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.NewEngineScanner;
import hudson.AbortException;

import javax.annotation.Nonnull;
import java.util.Optional;

public class ImageScanningService {

  // FIXME(fede) Do not use the builder, use the config.
  public static void runScan(@Nonnull RunContext runContext, @Nonnull ImageScanningBuilder builder) throws AbortException {
    /* Instantiate config and a new build worker */
    var logger = runContext.getLogger();

    logger.logWarn(String.format("Starting Sysdig Secure Container Image Scanner step, project: %s, job: %d", runContext.getProjectName(), runContext.getJobNumber()));

    /* Fetch Jenkins creds first, can't push this lower down the chain since it requires Jenkins instance object */
    //Prefer the job credentials set by the user and fallback to the global ones
    String credID = !Strings.isNullOrEmpty(builder.getEngineCredentialsId()) ? builder.getEngineCredentialsId() : builder.getDescriptor().getEngineCredentialsId();
    final String sysdigToken = runContext.getSysdigTokenFromCredentials(credID);

    NewEngineBuildConfig config = new NewEngineBuildConfig(builder, sysdigToken);
    config.printWith(logger);

    Optional<ImageScanningResult.FinalAction> finalAction = getFinalAction(runContext, builder, config);

    /* Evaluate result of step based on gate action */
    if (finalAction.isEmpty()) {
      logger.logInfo("Marking Sysdig Secure Container Image Scanner step as successful, no final result");
    } else if ((config.getBailOnFail() || builder.getBailOnFail()) && ImageScanningResult.FinalAction.ActionFail.equals(finalAction.get())) {
      logger.logWarn("Failing Sysdig Secure Container Image Scanner Plugin step due to final result " + finalAction.get());
      throw new AbortException("Failing Sysdig Secure Container Image Scanner Plugin step due to final result " + finalAction.get());
    } else {
      logger.logInfo("Marking Sysdig Secure Container Image Scanner step as successful, final result " + finalAction.get());
    }
  }

  private static Optional<ImageScanningResult.FinalAction> getFinalAction(@Nonnull RunContext runContext, @Nonnull ImageScanningBuilder step, @Nonnull NewEngineBuildConfig config) throws AbortException {
    var logger = runContext.getLogger();

    NewEngineScanner scanner = new NewEngineScanner(runContext, config);
    JenkinsReportStorage reportStorage = new JenkinsReportStorage(runContext);
    BuildWorker worker = new BuildWorker(scanner, reportStorage, logger);
    Optional<ImageScanningResult.FinalAction> finalAction = Optional.empty();

    try (reportStorage) {
      finalAction = Optional.ofNullable(worker.scanAndBuildReports(config.getImageName()));
    } catch (Exception e) {
      if (config.getBailOnPluginFail() || step.getBailOnPluginFail()) {
        logger.logError("Failing Sysdig Secure Container Image Scanner Plugin step due to errors in plugin execution", e);
        throw new AbortException("Failing Sysdig Secure Container Image Scanner Plugin step due to errors in plugin execution");
      } else {
        logger.logWarn("Marking Sysdig Secure Container Image Scanner step as successful despite errors in plugin execution");
      }
    }


    logger.logInfo("Completed Sysdig Secure Container Image Scanner step");
    logger.logWarn(String.format("Completed Sysdig Secure Container Image Scanner step, project: %s, job: %d", runContext.getProjectName(), runContext.getJobNumber()));

    return finalAction;
  }

}
