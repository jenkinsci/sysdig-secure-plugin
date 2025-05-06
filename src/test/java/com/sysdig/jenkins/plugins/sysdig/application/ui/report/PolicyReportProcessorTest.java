package com.sysdig.jenkins.plugins.sysdig.application.ui.report;

import com.sysdig.jenkins.plugins.sysdig.TestMother;
import com.sysdig.jenkins.plugins.sysdig.application.vm.report.PolicyEvaluationReportLine;
import com.sysdig.jenkins.plugins.sysdig.application.vm.report.PolicyReportProcessor;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.log.NopLogger;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.ImageScanningResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyReportProcessorTest {
  private final PolicyReportProcessor policyReport = new PolicyReportProcessor(new NopLogger());
  private final String imageID = "sha256:04ba374043ccd2fc5c593885c0eacddebabd5ca375f9323666f28dfd5a9710e3";

  @Test
  void testPolicyEvaluationReportIsGeneratedCorrectly() throws IOException {
    // Given
    var result = TestMother.rawScanResult();
    var imageScanningResult = ImageScanningResult.fromReportResult(result);

    // When
    var policyEvaluationReport = policyReport.processPolicyEvaluation(imageScanningResult);

    // Then
    assertTrue(policyEvaluationReport.isFailed());

    var resultsForEachImage = policyEvaluationReport.getResultsForEachImage();
    assertTrue(resultsForEachImage.containsKey(imageID));

    var policyEvaluationReportLines = resultsForEachImage.get(imageID);
    assertEquals(45, policyEvaluationReportLines.size());
    assertEquals(policyEvaluationReportLines.get(0), new PolicyEvaluationReportLine(
      imageID,
      "nginx",
      "trigger_id",
      "PCI DSS (Payment Card Industry Data Security Standard) v4.0",
      "Severity greater than or equal high",
      "CVE-2023-31484 found in pkg 'perl-base:5.36.0-7+deb12u1'",
      "STOP",
      false,
      "",
      "Cardholder Policy (David)"));
  }

  @Test
  void testPolicyEvaluationSummaryIsGeneratedCorrectly() throws IOException {
    // Given
    var result = TestMother.rawScanResult();
    var imageScanningResult = ImageScanningResult.fromReportResult(result);
    var policyEvaluationReport = policyReport.processPolicyEvaluation(imageScanningResult);

    // When
    var policyEvaluationSummary = policyReport.generateGatesSummary(policyEvaluationReport, imageScanningResult);

    // Then
    assertEquals(1, policyEvaluationSummary.getLines().size());

    var policyEvaluationSummaryLine = policyEvaluationSummary.getLines().get(0);
    assertEquals("nginx", policyEvaluationSummaryLine.getImageTag());
    assertEquals(45, policyEvaluationSummaryLine.getNonWhitelistedStopActions());
    assertEquals(0, policyEvaluationSummaryLine.getNonWhitelistedWarnActions());
    assertEquals(0, policyEvaluationSummaryLine.getNonWhitelistedGoActions());
    assertEquals("STOP", policyEvaluationSummaryLine.getFinalAction());
  }
}
