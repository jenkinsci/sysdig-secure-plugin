package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1;

import static org.junit.jupiter.api.Assertions.*;

import com.sysdig.jenkins.plugins.sysdig.TestMother;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JsonScanResultTest {
    ScanResult scanResult;

    @BeforeEach
    void setUp() {
        JsonScanResultV1 jsonScanResult = TestMother.scanResultForUbuntu2204();
        scanResult = jsonScanResult.toDomain().get();
    }

    @Test
    void whenConvertingToDomainItHasTheGeneralStatistics() {
        assertEquals(1, scanResult.layers().size());
        assertEquals(101, scanResult.packages().size());
        assertEquals(6, scanResult.policies().size());
        assertEquals(2, scanResult.acceptedRisks().size());
        assertEquals(EvaluationResult.Failed, scanResult.evaluationResult());
    }

    @Test
    void whenConvertingToDomainItHasLayerInformation() {
        Layer layer = scanResult.layers().stream().findFirst().get();

        assertEquals(
                "/bin/sh -c #(nop) ADD file:82f38ebced7b2756311fb492d3d44cc131b22654e8620baa93883537a3e355aa in / ",
                layer.command());
        assertEquals(101, layer.packages().size());
        assertEquals(23, layer.vulnerabilities().size()); // unique vulns
        assertEquals(
                46,
                layer.packages().stream()
                        .flatMap(p -> p.vulnerabilities().stream())
                        .count()); // vulns per package
    }

    @Test
    void whenConvertingToDomainItHasVulnerabilityInformation() {
        Vulnerability vuln = scanResult.findVulnerabilityByCVE("CVE-2022-27943").get();

        assertEquals(1, vuln.foundInLayers().size());
        assertEquals(3, vuln.foundInPackages().size());
        assertEquals(1, vuln.acceptedRisks().size());
    }

    @Test
    void whenConvertingToDomainItHasExploitableVulnInformation() {
        List<Vulnerability> exploitableVulnerabilities = scanResult.vulnerabilities().stream()
                .filter(Vulnerability::exploitable)
                .toList();

        assertEquals(1, exploitableVulnerabilities.size());
    }

    @Test
    void whenConvertingToDomainItHasFixableVulnsInformation() {
        List<Vulnerability> fixableVulnerabilities = scanResult.vulnerabilities().stream()
                .filter(Vulnerability::fixable)
                .toList();

        assertEquals(6, fixableVulnerabilities.size()); // fixable unique vulns
        assertEquals(
                10,
                fixableVulnerabilities.stream()
                        .flatMap(v -> v.foundInPackages().stream())
                        .count()); // fixable vulns per package
    }

    @Test
    void whenConvertingToDomainItHasVulnerabilitiesPerPackageInformation() {
        List<Vulnerability> vulnerabilitiesPerPackage = scanResult.packages().stream()
                .flatMap(p -> p.vulnerabilities().stream())
                .toList();

        assertEquals(
                0,
                vulnerabilitiesPerPackage.stream()
                        .filter(v -> v.severity() == Severity.Critical)
                        .count());
        assertEquals(
                0,
                vulnerabilitiesPerPackage.stream()
                        .filter(v -> v.severity() == Severity.High)
                        .count());
        assertEquals(
                28,
                vulnerabilitiesPerPackage.stream()
                        .filter(v -> v.severity() == Severity.Low)
                        .count());
        assertEquals(
                15,
                vulnerabilitiesPerPackage.stream()
                        .filter(v -> v.severity() == Severity.Medium)
                        .count());
        assertEquals(
                3,
                vulnerabilitiesPerPackage.stream()
                        .filter(v -> v.severity() == Severity.Negligible)
                        .count());
    }

    @Test
    void whenConvertingToDomainItHasPolicyInformation() {
        Policy policy = scanResult.findPolicyByID("sysdig-best-practices").get();
        assertEquals(1, policy.bundles().size());

        PolicyBundle policyBundle = scanResult
                .findPolicyBundleByID("severe_vulnerabilities_with_a_fix")
                .get();
        assertEquals(1, policyBundle.foundInPolicies().size());
        assertEquals(3, policyBundle.rules().size());
        assertTrue(policy.bundles().contains(policyBundle));
        assertTrue(policyBundle.foundInPolicies().contains(policy));
    }

    @Test
    void whenConvertingToDomainItHasPolicieswithAcceptedRisks() {
        Policy policyWithAcceptedRisk =
                scanResult.findPolicyByID("policycardholder").get();
        assertEquals(1, policyWithAcceptedRisk.bundles().size());
        assertEquals(EvaluationResult.Passed, policyWithAcceptedRisk.evaluationResult());
    }

    @Test
    void whenConvertingToDomainItHasFailedPolicies() {
        Policy failedPolicy = scanResult.findPolicyByID("nist-sp-800-star").get();
        assertEquals(EvaluationResult.Failed, failedPolicy.evaluationResult());

        PolicyBundle failedBundle =
                scanResult.findPolicyBundleByID("nist-sp-800-190").get();
        assertTrue(failedPolicy.bundles().contains(failedBundle));
        assertEquals(EvaluationResult.Failed, failedBundle.evaluationResult());
        assertEquals(
                EvaluationResult.Failed,
                failedBundle.rules().stream().skip(1).findFirst().get().evaluationResult());
    }
}
