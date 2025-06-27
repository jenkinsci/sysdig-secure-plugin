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

import com.sysdig.jenkins.plugins.sysdig.domain.vm.ScanResultArchiver;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.ScanResult;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.io.IOException;

public class ImageScanningArchiver implements ScanResultArchiver {
  private final ReportProcessor policyEvaluationReportProcessor;
  private final ReportStorage reportStorage;

  public ImageScanningArchiver(@NonNull ReportProcessor policyEvaluationReportProcessor, @NonNull ReportStorage reportStorage) {
    this.policyEvaluationReportProcessor = policyEvaluationReportProcessor;
    this.reportStorage = reportStorage;
  }

  @Override
  public void archiveScanResult(ScanResult scanResult) throws IOException, InterruptedException {
    var policyEvaluationReport = policyEvaluationReportProcessor.processPolicyEvaluation(scanResult);
    var policyEvaluationSummary = policyEvaluationReportProcessor.generateGatesSummary(policyEvaluationReport, scanResult);

    reportStorage.savePolicyReport(scanResult, policyEvaluationReport);
    reportStorage.saveVulnerabilityReport(scanResult);
    reportStorage.saveRawVulnerabilityReport(scanResult);
    reportStorage.archiveResults(scanResult, policyEvaluationSummary);
  }
}
