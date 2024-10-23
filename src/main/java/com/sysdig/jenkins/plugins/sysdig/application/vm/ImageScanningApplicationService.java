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

import com.sysdig.jenkins.plugins.sysdig.application.RunContext;
import com.sysdig.jenkins.plugins.sysdig.domain.ImageScanningResult;
import com.sysdig.jenkins.plugins.sysdig.domain.ImageScanningService;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.JenkinsReportStorage;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.ImageScanner;
import hudson.AbortException;

import javax.annotation.Nonnull;
import java.util.Optional;

public class ImageScanningApplicationService {

  public static void runScan(@Nonnull RunContext runContext, @Nonnull ImageScanningConfig config) throws AbortException {
    /* Instantiate config and a new build worker */
    var logger = runContext.getLogger();
    logger.logWarn(String.format("Starting Sysdig Secure Container Image Scanner step, project: %s, job: %d", runContext.getProjectName(), runContext.getJobNumber()));
    config.printWith(logger);

    Optional<ImageScanningResult.FinalAction> finalAction = getFinalAction(runContext, config);

    /* Evaluate result of step based on gate action */
    if (finalAction.isEmpty()) {
      logger.logInfo("Marking Sysdig Secure Container Image Scanner step as successful, no final result");
    } else if (config.getBailOnFail() && ImageScanningResult.FinalAction.ActionFail.equals(finalAction.get())) {
      logger.logWarn("Failing Sysdig Secure Container Image Scanner Plugin step due to final result " + finalAction.get());
      throw new AbortException("Failing Sysdig Secure Container Image Scanner Plugin step due to final result " + finalAction.get());
    } else {
      logger.logInfo("Marking Sysdig Secure Container Image Scanner step as successful, final result " + finalAction.get());
    }
  }

  private static Optional<ImageScanningResult.FinalAction> getFinalAction(@Nonnull RunContext runContext, @Nonnull ImageScanningConfig config) throws AbortException {
    var logger = runContext.getLogger();

    ImageScanner scanner = new ImageScanner(runContext, config);
    JenkinsReportStorage reportStorage = new JenkinsReportStorage(runContext);
    ImageScanningService imageScanningService = new ImageScanningService(scanner, reportStorage, logger);
    Optional<ImageScanningResult.FinalAction> finalAction = Optional.empty();

    try (reportStorage) {
      finalAction = Optional.ofNullable(imageScanningService.scanAndBuildReports(config.getImageName()));
    } catch (Exception e) {
      if (config.getBailOnPluginFail()) {
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
