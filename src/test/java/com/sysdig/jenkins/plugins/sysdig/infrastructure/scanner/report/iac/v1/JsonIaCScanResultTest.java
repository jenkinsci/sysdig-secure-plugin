package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.iac.v1;

import static org.junit.jupiter.api.Assertions.*;

import com.sysdig.jenkins.plugins.sysdig.TestMother;
import com.sysdig.jenkins.plugins.sysdig.domain.iac.scanresult.Finding;
import com.sysdig.jenkins.plugins.sysdig.domain.iac.scanresult.IaCScanResult;
import com.sysdig.jenkins.plugins.sysdig.domain.iac.scanresult.IaCScanType;
import com.sysdig.jenkins.plugins.sysdig.domain.iac.scanresult.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JsonIaCScanResultTest {
    IaCScanResult scanResult;

    @BeforeEach
    void setUp() {
        scanResult = TestMother.iacScanResult().toDomain().orElseThrow();
    }

    @Test
    void whenConvertingToDomainItHasTheScanTypeAndMetadata() {
        assertEquals(IaCScanType.IaCGitScan, scanResult.type());
        assertEquals(1, scanResult.metadata().sources().size());
        assertEquals("/infra", scanResult.metadata().sources().get(0));
        assertEquals(1, scanResult.metadata().totalModules());
        assertEquals(20, scanResult.metadata().totalResources());
        assertEquals(1, scanResult.metadata().totalFolders());
    }

    @Test
    void whenConvertingToDomainItHasTheReportedFindingsSummary() {
        assertEquals(3, scanResult.reportedFindingsCount(Severity.High));
        assertEquals(16, scanResult.reportedFindingsCount(Severity.Medium));
        assertEquals(3, scanResult.reportedFindingsCount(Severity.Low));
        assertEquals(0, scanResult.reportedFindingsCount(Severity.Negligible));
    }

    @Test
    void whenConvertingToDomainItHasAllFindings() {
        assertTrue(scanResult.hasFindings());
        assertEquals(15, scanResult.findings().size());

        assertEquals(
                2,
                scanResult.findings().stream()
                        .filter(f -> f.severity() == Severity.High)
                        .count());
        assertEquals(
                11,
                scanResult.findings().stream()
                        .filter(f -> f.severity() == Severity.Medium)
                        .count());
        assertEquals(
                2,
                scanResult.findings().stream()
                        .filter(f -> f.severity() == Severity.Low)
                        .count());
    }

    @Test
    void theReportedSummaryEqualsTheResourcesAffectedPerSeverity() {
        // The summary counts affected resources, not findings; a control can fail against many resources.
        for (Severity severity : new Severity[] {Severity.High, Severity.Medium, Severity.Low}) {
            long affectedResources = scanResult.findings().stream()
                    .filter(f -> f.severity() == severity)
                    .mapToLong(f -> f.resources().size())
                    .sum();
            assertEquals(
                    scanResult.reportedFindingsCount(severity),
                    (int) affectedResources,
                    "summary should match affected resources for " + severity);
        }
    }

    @Test
    void whenConvertingToDomainItPopulatesFindingDetails() {
        Finding finding = scanResult.findings().stream()
                .filter(f -> f.controlId() == 7046)
                .findFirst()
                .orElseThrow();

        assertEquals("ECR - Enabled Vulnerability Scanning", finding.name());
        assertEquals(Severity.High, finding.severity());
        assertEquals(1, finding.resources().size());
        assertEquals("aws_ecr_repository.soc_agent", finding.resources().get(0).name());
        assertEquals("AWS_ECR_REPOSITORY", finding.resources().get(0).type());
        assertTrue(finding.policies().contains("All Posture Findings"));
        assertTrue(finding.requirements().contains("AWS Controls"));
    }

    @Test
    void whenConvertingToDomainItHasUnsupportedResources() {
        assertEquals(1, scanResult.unsupportedResources().size());
        var unsupported = scanResult.unsupportedResources().iterator().next();
        assertEquals("/infra/infra", unsupported.source());
        assertTrue(unsupported.details().contains("aws_kms_alias"));
        assertEquals(14, unsupported.details().size());
    }

    @Test
    void whenConvertingToDomainItHasParseErrors() {
        assertEquals(6, scanResult.errors().size());
        assertTrue(scanResult.errors().stream()
                .anyMatch(e -> e.source().equals("/.venv/lib/python3.13/site-packages/markdown_it/port.yaml")));
    }

    @Test
    void collectionsAreUnmodifiable() {
        assertThrows(
                UnsupportedOperationException.class, () -> scanResult.findings().add(null));
        assertThrows(
                UnsupportedOperationException.class,
                () -> scanResult.metadata().sources().add("x"));
    }

    @Test
    void whenResultIsMissingToDomainIsEmpty() {
        JsonIaCScanResultV1 empty = new JsonIaCScanResultV1(null, null, null);
        assertTrue(empty.toDomain().isEmpty());
    }
}
