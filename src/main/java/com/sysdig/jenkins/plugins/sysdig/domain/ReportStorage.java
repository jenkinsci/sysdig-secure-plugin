package com.sysdig.jenkins.plugins.sysdig.domain;

import com.sysdig.jenkins.plugins.sysdig.application.ui.report.PolicyEvaluationReport;
import com.sysdig.jenkins.plugins.sysdig.application.ui.report.PolicyEvaluationSummary;

import java.io.IOException;

public interface ReportStorage {
  void savePolicyReport(ImageScanningResult scanResult, PolicyEvaluationReport report) throws IOException, InterruptedException;

  void saveVulnerabilityReport(ImageScanningResult scanResult) throws IOException, InterruptedException;

  void saveRawVulnerabilityReport(ImageScanningResult scanResult) throws IOException, InterruptedException;

  void archiveResults(ImageScanningResult scanResult, PolicyEvaluationSummary gateSummary) throws IOException;
}
