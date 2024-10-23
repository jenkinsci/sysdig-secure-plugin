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
package com.sysdig.jenkins.plugins.sysdig.domain;

import com.sysdig.jenkins.plugins.sysdig.application.ui.report.PolicyEvaluationReportProcessor;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * A helper class to ensure concurrent jobs don't step on each other's toes. Sysdig Secure plugin instantiates a new instance of this class
 * for each individual job i.e. invocation of perform(). Global and project configuration at the time of execution is loaded into
 * worker instance via its constructor. That specific worker instance is responsible for the bulk of the plugin operations for a given
 * job.
 */
public class ImageScanningService {
  private final ImageScanner scanner;
  private final ReportStorage reportStorage;
  protected SysdigLogger logger;

  public ImageScanningService(@Nonnull ImageScanner scanner, @Nonnull ReportStorage reportStorage, @Nonnull SysdigLogger logger) {
    this.scanner = scanner;
    this.reportStorage = reportStorage;
    this.logger = logger;
  }

  public ImageScanningResult.FinalAction scanAndBuildReports(String imageName) throws InterruptedException {
    ImageScanningResult scanResult = scanner.scanImage(imageName);

    ImageScanningResult.FinalAction finalAction = scanResult.getFinalAction();
    logger.logInfo("Sysdig Secure Container Image Scanner Plugin step result - " + finalAction);

    try {
      archiveScanResultReporting(scanResult);
    } catch (Exception e) {
      logger.logError("Recording failure to build reports and moving on with plugin operation", e);
    }

    return finalAction;
  }

  private void archiveScanResultReporting(ImageScanningResult scanResult) throws IOException, InterruptedException {
    var policyEvaluationReportProcessor = new PolicyEvaluationReportProcessor(this.logger); // FIXME(fede): domain must not use application code.
    var policyEvaluationReport = policyEvaluationReportProcessor.processPolicyEvaluation(scanResult);
    var policyEvaluationSummary = policyEvaluationReportProcessor.generateGatesSummary(policyEvaluationReport, scanResult);

    reportStorage.savePolicyReport(scanResult, policyEvaluationReport);
    reportStorage.saveVulnerabilityReport(scanResult);
    reportStorage.saveRawVulnerabilityReport(scanResult);
    reportStorage.archiveResults(scanResult, policyEvaluationSummary);
  }
}
