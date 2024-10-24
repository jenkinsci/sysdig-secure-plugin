package com.sysdig.jenkins.plugins.sysdig.application.vm;

import com.sysdig.jenkins.plugins.sysdig.domain.vm.ImageScanningResult;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.ScanResultArchiver;

import javax.annotation.Nonnull;
import java.io.IOException;

public class ImageScanningArchiver implements ScanResultArchiver {
  private final ReportProcessor policyEvaluationReportProcessor;
  private final ReportStorage reportStorage;

  public ImageScanningArchiver(@Nonnull ReportProcessor policyEvaluationReportProcessor, @Nonnull ReportStorage reportStorage) {
    this.policyEvaluationReportProcessor = policyEvaluationReportProcessor;
    this.reportStorage = reportStorage;
  }

  @Override
  public void archiveScanResult(ImageScanningResult scanResult) throws IOException, InterruptedException {
    var policyEvaluationReport = policyEvaluationReportProcessor.processPolicyEvaluation(scanResult);
    var policyEvaluationSummary = policyEvaluationReportProcessor.generateGatesSummary(policyEvaluationReport, scanResult);

    reportStorage.savePolicyReport(scanResult, policyEvaluationReport);
    reportStorage.saveVulnerabilityReport(scanResult);
    reportStorage.saveRawVulnerabilityReport(scanResult);
    reportStorage.archiveResults(scanResult, policyEvaluationSummary);
  }
}
