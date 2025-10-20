package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.ScanResult;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.Severity;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.Vulnerability;
import java.util.Date;
import org.junit.jupiter.api.Test;

class ScanResultDiffTest {

    @Test
    void testScanResultDiff() {
        // Given
        ScanResult oldScanResult = new ScanResult(null, null, null, null, null, null, null, null, null);
        Vulnerability vuln1 =
                oldScanResult.addVulnerability("CVE-2021-0001", Severity.High, new Date(), null, false, null);
        Vulnerability vuln2 =
                oldScanResult.addVulnerability("CVE-2021-0002", Severity.Medium, new Date(), null, false, null);

        ScanResult newScanResult = new ScanResult(null, null, null, null, null, null, null, null, null);
        Vulnerability vuln3 =
                newScanResult.addVulnerability("CVE-2021-0002", Severity.Medium, new Date(), null, false, null);
        Vulnerability vuln4 =
                newScanResult.addVulnerability("CVE-2021-0003", Severity.Low, new Date(), null, false, null);

        // When
        ScanResultDiff diff = newScanResult.diffWithPrevious(oldScanResult);

        // Then
        assertEquals(1, diff.getVulnerabilitiesAdded().size());
        assertTrue(diff.getVulnerabilitiesAdded().contains(vuln4));

        assertEquals(1, diff.getVulnerabilitiesFixed().size());
        assertTrue(diff.getVulnerabilitiesFixed().contains(vuln1));
    }
}
