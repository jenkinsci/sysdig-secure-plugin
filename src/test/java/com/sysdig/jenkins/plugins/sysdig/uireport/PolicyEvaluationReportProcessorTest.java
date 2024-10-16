package com.sysdig.jenkins.plugins.sysdig.uireport;

import com.sysdig.jenkins.plugins.sysdig.json.GsonBuilder;
import com.sysdig.jenkins.plugins.sysdig.log.NopLogger;
import com.sysdig.jenkins.plugins.sysdig.scanner.ImageScanningResult;
import com.sysdig.jenkins.plugins.sysdig.scanner.report.Result;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.util.Assert.notEmpty;
import static org.springframework.util.Assert.notNull;

public class PolicyEvaluationReportProcessorTest {
  private final PolicyEvaluationReportProcessor policyReport = new PolicyEvaluationReportProcessor(new NopLogger());

  @Test
  public void testPolicyEvaluationReportIsGeneratedCorrectly() throws IOException, InterruptedException {
    // Given
    var result = GsonBuilder.build()
      .fromJson(
        IOUtils.toString(getClass().getResourceAsStream("gates1.json"), StandardCharsets.UTF_8),
        Result.class);
    var imageScanningResult = new ImageScanningResult("foo-tag1", "foo-digest1", "pass",
      result.getPackages().orElseThrow(),
      result.getPolicyEvaluations().orElseThrow());

    // When
    PolicyEvaluationReport report = policyReport.processPolicyEvaluation(imageScanningResult);

    // Then
    assertFalse(report.isFailed());

    var results = report.getResultsForEachImage();
    notEmpty(results, "no results found");

    List<PolicyEvaluationReportLine> reportLines = results.get("foo-digest1");
    notNull(reportLines, "report lines is null");
    assertEquals(reportLines.size(), 45);
    assertEquals(reportLines.get(0), new PolicyEvaluationReportLine(
      "foo-digest1",
      "foo-tag1",
      "trigger_id",
      "PCI DSS (Payment Card Industry Data Security Standard) v4.0",
      "Severity greater than or equal high",
      "CVE-2023-31484 found in pkg 'perl-base:5.36.0-7+deb12u1'",
      "STOP",
      false,
      "",
      "Cardholder Policy (David)"));
  }
}
