package com.sysdig.jenkins.plugins.sysdig.application.ui.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sysdig.jenkins.plugins.sysdig.TestMother;
import com.sysdig.jenkins.plugins.sysdig.application.vm.report.PolicyEvaluationReportLine;
import com.sysdig.jenkins.plugins.sysdig.application.vm.report.PolicyReportProcessor;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.log.NopLogger;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class PolicyReportProcessorTest {
    private final PolicyReportProcessor policyReport = new PolicyReportProcessor(new NopLogger());
    private final String imageID = "sha256:b103ac8bf22ec7fe2abe740f86dccd1e7d086e0ad8d064d07bd9c8a7961a5d7a";

    @Test
    void testPolicyEvaluationReportIsGeneratedCorrectly() throws IOException {
        // Given
        var result = TestMother.scanResultForUbuntu2204();
        var imageScanningResult = result.toDomain().get();

        // When
        var policyEvaluationReport = policyReport.processPolicyEvaluation(imageScanningResult);

        // Then
        assertTrue(policyEvaluationReport.isFailed());

        var resultsForEachImage = policyEvaluationReport.getResultsForEachImage();
        assertTrue(resultsForEachImage.containsKey(imageID));

        var policyEvaluationReportLines = resultsForEachImage.get(imageID);
        assertEquals(20, policyEvaluationReportLines.size());
        assertTrue(policyEvaluationReportLines.contains(new PolicyEvaluationReportLine(
                imageID,
                "ubuntu:22.04",
                "trigger_id",
                "NIST SP 800-190 (Application Container Security Guide)",
                "Severity greater than or equal medium", // FIXME(fede): this should be filled, but for v1 result in
                // sysdig-cli-scanner 1.22.3 the rule description is empty
                "CVE-2025-6020 found in libpam-modules (1.4.0-11ubuntu2.5)",
                "STOP",
                false,
                "nist-sp-800-star",
                "NIST SP 800-Star")));
    }

    @Test
    void testPolicyEvaluationSummaryIsGeneratedCorrectly() throws IOException {
        // Given
        var result = TestMother.scanResultForUbuntu2204();
        var imageScanningResult = result.toDomain().get();
        var policyEvaluationReport = policyReport.processPolicyEvaluation(imageScanningResult);

        // When
        var policyEvaluationSummary = policyReport.generateGatesSummary(policyEvaluationReport, imageScanningResult);

        // Then
        assertEquals(1, policyEvaluationSummary.getLines().size());

        var policyEvaluationSummaryLine = policyEvaluationSummary.getLines().get(0);
        assertEquals("ubuntu:22.04", policyEvaluationSummaryLine.imageTag());
        assertEquals(20, policyEvaluationSummaryLine.nonWhitelistedStopActions());
        assertEquals(0, policyEvaluationSummaryLine.nonWhitelistedWarnActions());
        assertEquals(0, policyEvaluationSummaryLine.nonWhitelistedGoActions());
        assertEquals("STOP", policyEvaluationSummaryLine.finalAction());
    }
}
