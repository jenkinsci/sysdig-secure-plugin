package com.sysdig.jenkins.plugins.sysdig.application.ui.report;

import com.sysdig.jenkins.plugins.sysdig.TestMother;
import com.sysdig.jenkins.plugins.sysdig.application.vm.report.PolicyEvaluationReportLine;
import com.sysdig.jenkins.plugins.sysdig.application.vm.report.PolicyReportProcessor;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.log.NopLogger;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyReportProcessorTest {
  private final PolicyReportProcessor policyReport = new PolicyReportProcessor(new NopLogger());
  private final String imageID = "sha256:39286ab8a5e14aeaf5fdd6e2fac76e0c8d31a0c07224f0ee5e6be502f12e93f3";

  @Test
  void testPolicyEvaluationReportIsGeneratedCorrectly() throws IOException {
    // Given
    var result = TestMother.rawScanResult();
    var imageScanningResult = result.toDomain().get();

    // When
    var policyEvaluationReport = policyReport.processPolicyEvaluation(imageScanningResult);

    // Then
    assertTrue(policyEvaluationReport.isFailed());

    var resultsForEachImage = policyEvaluationReport.getResultsForEachImage();
    assertTrue(resultsForEachImage.containsKey(imageID));

    var policyEvaluationReportLines = resultsForEachImage.get(imageID);
    assertEquals(45, policyEvaluationReportLines.size());
    assertTrue(policyEvaluationReportLines.contains(new PolicyEvaluationReportLine(
      imageID,
      "nginx",
      "trigger_id",
      "PCI DSS (Payment Card Industry Data Security Standard) v4.0",
      "Severity greater than or equal high",
      "CVE-2023-31484 found in pkg 'perl-base:5.36.0-7+deb12u1'",
      "STOP",
      false,
      "cardholder-policy-david",
      "Cardholder Policy (David)")));
  }

  @Test
  void testPolicyEvaluationSummaryIsGeneratedCorrectly() throws IOException {
    // Given
    var result = TestMother.rawScanResult();
    var imageScanningResult = result.toDomain().get();
    var policyEvaluationReport = policyReport.processPolicyEvaluation(imageScanningResult);

    // When
    var policyEvaluationSummary = policyReport.generateGatesSummary(policyEvaluationReport, imageScanningResult);

    // Then
    assertEquals(1, policyEvaluationSummary.getLines().size());

    var policyEvaluationSummaryLine = policyEvaluationSummary.getLines().get(0);
    assertEquals("nginx", policyEvaluationSummaryLine.imageTag());
    assertEquals(45, policyEvaluationSummaryLine.nonWhitelistedStopActions());
    assertEquals(0, policyEvaluationSummaryLine.nonWhitelistedWarnActions());
    assertEquals(0, policyEvaluationSummaryLine.nonWhitelistedGoActions());
    assertEquals("STOP", policyEvaluationSummaryLine.finalAction());
  }
}
