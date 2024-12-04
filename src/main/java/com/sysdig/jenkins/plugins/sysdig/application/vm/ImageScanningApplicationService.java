/*
Copyright (C) 2016-2024 Sysdig

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

import com.sysdig.jenkins.plugins.sysdig.application.vm.report.PolicyReportProcessor;
import com.sysdig.jenkins.plugins.sysdig.domain.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.ImageScanner;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.ImageScanningResult;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.ImageScanningService;
import hudson.AbortException;

import javax.annotation.Nonnull;
import java.util.Optional;

public class ImageScanningApplicationService {
  private final ReportStorage reportStorage;
  private final ImageScanner scanner;
  private final SysdigLogger logger;

  public ImageScanningApplicationService(ReportStorage reportStorage, ImageScanner scanner, SysdigLogger logger) {
    this.reportStorage = reportStorage;
    this.scanner = scanner;
    this.logger = logger;
  }

  public void runScan(@Nonnull ImageScanningConfig config) throws AbortException {
    config.printWith(logger);

    Optional<ImageScanningResult.FinalAction> finalAction = getFinalAction(config);

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

  private Optional<ImageScanningResult.FinalAction> getFinalAction(@Nonnull ImageScanningConfig config) throws AbortException {
    PolicyReportProcessor reportProcessor = new PolicyReportProcessor(logger);
    ImageScanningArchiver imageScanningArchiver = new ImageScanningArchiver(reportProcessor, reportStorage);

    ImageScanningService imageScanningService = new ImageScanningService(scanner, imageScanningArchiver, logger);
    Optional<ImageScanningResult.FinalAction> finalAction = Optional.empty();

    try {
      finalAction = Optional.ofNullable(imageScanningService.scanAndArchiveResult(config.getImageName()));
    } catch (Exception e) {
      if (config.getBailOnPluginFail()) {
        logger.logError("Failing Sysdig Secure Container Image Scanner Plugin step due to errors in plugin execution", e);
        throw new AbortException("Failing Sysdig Secure Container Image Scanner Plugin step due to errors in plugin execution");
      } else {
        logger.logWarn("Marking Sysdig Secure Container Image Scanner step as successful despite errors in plugin execution");
      }
    }

    logger.logInfo("Completed Sysdig Secure Container Image Scanner step");
    return finalAction;
  }

}
