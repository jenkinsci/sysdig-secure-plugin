package com.sysdig.jenkins.plugins.sysdig.uireport;

import com.sysdig.jenkins.plugins.sysdig.json.GsonBuilder;
import com.sysdig.jenkins.plugins.sysdig.log.NopLogger;
import com.sysdig.jenkins.plugins.sysdig.scanner.ImageScanningResult;
import com.sysdig.jenkins.plugins.sysdig.scanner.report.Result;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PolicyEvaluationReportProcessorTest {
  private final PolicyEvaluationReportProcessor policyReport = new PolicyEvaluationReportProcessor(new NopLogger());
  private final String imageID = "sha256:04ba374043ccd2fc5c593885c0eacddebabd5ca375f9323666f28dfd5a9710e3";

  @Test
  public void testPolicyEvaluationReportIsGeneratedCorrectly() throws IOException {
    // Given
    var result = getTestingReportResult();
    var imageScanningResult = ImageScanningResult.fromReportResult(result);

    // When
    var policyEvaluationReport = policyReport.processPolicyEvaluation(imageScanningResult);

    // Then
    assertTrue(policyEvaluationReport.isFailed());

    var resultsForEachImage = policyEvaluationReport.getResultsForEachImage();
    assertTrue(resultsForEachImage.containsKey(imageID));

    var policyEvaluationReportLines = resultsForEachImage.get(imageID);
    assertEquals(policyEvaluationReportLines.size(), 45);
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
  public void testPolicyEvaluationSummaryIsGeneratedCorrectly() throws IOException {
    // Given
    var result = getTestingReportResult();
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

  private Result getTestingReportResult() throws IOException {
    var jsonContents = Objects.requireNonNull(getClass().getResourceAsStream("gates1.json"));
    return GsonBuilder.build().fromJson(IOUtils.toString(jsonContents, StandardCharsets.UTF_8), Result.class);
  }
}
