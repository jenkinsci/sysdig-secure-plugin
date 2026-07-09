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
    void whenConvertingToDomainItHasFpkevInformation() {
        Vulnerability fpkevVuln =
                scanResult.findVulnerabilityByCVE("CVE-2016-2781").get();
        assertTrue(fpkevVuln.fpkev());

        Vulnerability regularVuln =
                scanResult.findVulnerabilityByCVE("CVE-2022-27943").get();
        assertFalse(regularVuln.fpkev());
    }

    @Test
    void whenConvertingToDomainItHasVulndbCvssTemporalScoreInformation() {
        Vulnerability vulnWithTemporalScore =
                scanResult.findVulnerabilityByCVE("CVE-2016-2781").get();
        assertEquals(6.2f, vulnWithTemporalScore.cvssTemporalScore().get());

        Vulnerability vulnWithoutTemporalScore =
                scanResult.findVulnerabilityByCVE("CVE-2016-20013").get();
        assertTrue(vulnWithoutTemporalScore.cvssTemporalScore().isEmpty());
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

    @Test
    void whenGlobalEvaluationIsPassedTheResultIsPassed() {
        JsonScanResultV1 jsonScanResult = TestMother.scanResultWithWholeImageAcceptedRisk();
        scanResult = jsonScanResult.toDomain().get();

        assertEquals(EvaluationResult.Passed, scanResult.evaluationResult());
        assertTrue(scanResult.policies().stream()
                .anyMatch(p -> p.evaluationResult().isFailed()));
    }

    @Test
    void whenParsingRealScanner1_27_2OutputItHasFpkevAndTemporalScore() {
        // real (untouched) output from sysdig-cli-scanner 1.27.2, the first version
        // emitting fpkev and providersMetadata.vulndb.cvssScore.temporal_score
        ScanResult result = TestMother.scanResultFromScanner1_27_2().toDomain().get();

        assertEquals(200, result.vulnerabilities().size());

        List<Vulnerability> fpkevVulnerabilities =
                result.vulnerabilities().stream().filter(Vulnerability::fpkev).toList();
        assertEquals(4, fpkevVulnerabilities.size());

        Vulnerability looneyTunables =
                result.findVulnerabilityByCVE("CVE-2023-4911").get();
        assertTrue(looneyTunables.fpkev());
        assertEquals(7.2f, looneyTunables.cvssTemporalScore().get());

        Vulnerability vulnWithoutNewFields =
                result.findVulnerabilityByCVE("CVE-2022-3219").get();
        assertFalse(vulnWithoutNewFields.fpkev());
        assertTrue(vulnWithoutNewFields.cvssTemporalScore().isEmpty());
    }

    @Test
    void whenParsingNewestScannerOutputThePluginProducesAValidPopulatedResult() {
        // Recorded raw output from the newest supported CLI scanner (TestMother.NEWEST_FIXTURE_VERSION).
        // Proves the plugin still parses the latest output format into a usable domain result.
        ScanResult result = TestMother.scanResultFromNewestScanner().toDomain().orElseThrow();

        assertPopulatedScanResult(result);
        // The newest format carries fpkev and CVSS temporal score; at least one vuln should expose them.
        assertTrue(result.vulnerabilities().stream().anyMatch(Vulnerability::fpkev));
        assertTrue(result.vulnerabilities().stream()
                .anyMatch(v -> v.cvssTemporalScore().isPresent()));
    }

    @Test
    void whenParsingOldestMaintainedScannerOutputThePluginProducesAValidPopulatedResult() {
        // Recorded raw output from the oldest still-maintained CLI scanner (TestMother.OLDEST_FIXTURE_VERSION).
        // Proves the plugin keeps working against an older output format.
        ScanResult result = TestMother.scanResultFromOldestScanner().toDomain().orElseThrow();

        assertPopulatedScanResult(result);
        // Fields introduced in newer scanners may be absent; they must default gracefully, not throw.
        assertDoesNotThrow(() -> result.vulnerabilities().forEach(v -> {
            v.fpkev();
            v.cvssTemporalScore();
        }));
    }

    private static void assertPopulatedScanResult(ScanResult result) {
        assertNotNull(result.type());
        assertNotNull(result.evaluationResult());

        // A real scan always carries image metadata.
        Metadata metadata = result.metadata();
        assertNotNull(metadata);
        assertFalse(metadata.pullString().isBlank());
        assertFalse(metadata.imageID().isBlank());
        assertNotNull(metadata.architecture());
        assertNotNull(metadata.baseOS());
        assertTrue(metadata.sizeInBytes().signum() > 0);

        // Core aggregates are non-empty.
        assertFalse(result.layers().isEmpty());
        assertFalse(result.packages().isEmpty());
        assertFalse(result.vulnerabilities().isEmpty());
        assertFalse(result.policies().isEmpty());

        // Every package parsed with its identifying fields.
        result.packages().forEach(pkg -> {
            assertNotNull(pkg.type());
            assertFalse(pkg.name().isBlank());
            assertFalse(pkg.version().isBlank());
        });

        // Every vulnerability parsed with severity/CVE/disclosure date; fixable ones expose a fix version.
        result.vulnerabilities().forEach(vuln -> {
            assertFalse(vuln.cve().isBlank());
            assertNotNull(vuln.severity());
            assertNotNull(vuln.disclosureDate());
            if (vuln.fixable()) {
                assertTrue(vuln.fixVersion().isPresent(), () -> vuln.cve() + " is fixable but has no fix version");
            }
        });

        // Referential integrity: vulnerabilities are linked back to the packages they were found in.
        assertTrue(result.vulnerabilities().stream()
                .anyMatch(vuln -> !vuln.foundInPackages().isEmpty()));

        // Every policy parsed with a computable evaluation result.
        result.policies().forEach(policy -> {
            assertFalse(policy.id().isBlank());
            assertFalse(policy.name().isBlank());
            assertNotNull(policy.evaluationResult());
        });
    }

    @Test
    void whenAPackageHasNoLayerRefItShouldNotThrowNPE() {
        // cli-scanner >= 1.25.0 emits meta-packages (base OS distro, container image)
        // without a layerRef, so result().layers().get(layerRef) returns null and
        // jsonLayer.digest() NPEs.
        JsonScanResultV1 jsonScanResult = TestMother.scanResultWithPackageWithoutLayer();

        ScanResult result = assertDoesNotThrow(() -> jsonScanResult.toDomain().get());

        assertEquals(2, result.packages().size());
        assertTrue(result.packages().stream().anyMatch(p -> p.name().equals("ubuntu")));
        assertTrue(result.packages().stream().anyMatch(p -> p.name().equals("libcurl")));
    }
}
