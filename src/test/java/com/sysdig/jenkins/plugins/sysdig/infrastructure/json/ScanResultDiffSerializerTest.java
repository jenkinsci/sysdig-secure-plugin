package com.sysdig.jenkins.plugins.sysdig.infrastructure.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.sysdig.jenkins.plugins.sysdig.TestMother;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.*;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.diff.ScanResultDiff;
import java.util.Date;
import org.junit.jupiter.api.Test;

class ScanResultDiffSerializerTest {
    private final Gson gson = GsonBuilder.build();

    @Test
    void testSerializeEmptyDiff() throws Exception {
        // Given
        ScanResult scanResult = TestMother.scanResultForUbuntu2204().toDomain().get();
        ScanResultDiff diff = ScanResultDiff.betweenPreviousAndNew(scanResult, scanResult);

        // When
        JsonElement jsonElement = gson.toJsonTree(diff);

        // Then
        JsonElement expectedJson = JsonParser.parseString("{\"vulnerabilitiesAdded\":[],\"vulnerabilitiesFixed\":[]}");
        assertEquals(expectedJson, jsonElement, "Expected serialized JSON to be empty for an empty diff");
    }

    @Test
    void testSerializeDiffWithResults() {
        // Given
        ScanResult oldScan = new ScanResult(null, null, null, null, null, null, null, null, null);
        Layer layer = oldScan.addLayer("digest", null, "command");
        com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.Package pkg1 =
                oldScan.addPackage(PackageType.OS, "pkg-1", "1.0", "/path", layer);
        com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.Package pkg2 =
                oldScan.addPackage(PackageType.OS, "pkg-2", "2.0", "/path", layer);
        Vulnerability vuln1 = oldScan.addVulnerability("CVE-2023-0001", Severity.High, new Date(), null, true, "1.1");
        vuln1.addFoundInPackage(pkg1);
        Vulnerability vuln2 = oldScan.addVulnerability("CVE-2023-0002", Severity.Medium, new Date(), null, false, null);
        vuln2.addFoundInPackage(pkg2);

        ScanResult newScan = new ScanResult(null, null, null, null, null, null, null, null, null);
        Layer newLayer = newScan.addLayer("digest", null, "command");
        com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.Package newPkg2 =
                newScan.addPackage(PackageType.OS, "pkg-2", "2.0", "/path", newLayer);
        com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.Package newPkg3 =
                newScan.addPackage(PackageType.OS, "pkg-3", "3.0", "/path", newLayer);
        Vulnerability newVuln2 =
                newScan.addVulnerability("CVE-2023-0002", Severity.Medium, new Date(), null, false, null);
        newVuln2.addFoundInPackage(newPkg2);
        Vulnerability newVuln3 =
                newScan.addVulnerability("CVE-2023-0003", Severity.Low, new Date(), null, false, "3.1");
        newVuln3.addFoundInPackage(newPkg3);

        ScanResultDiff diff = ScanResultDiff.betweenPreviousAndNew(oldScan, newScan);

        // When
        JsonElement jsonElement = gson.toJsonTree(diff);

        // Then
        JsonElement expectedJson = JsonParser.parseString("{"
                + "\"vulnerabilitiesAdded\":["
                + "{\"cve\":\"CVE-2023-0003\",\"severity\":\"Low\",\"fixVersion\":\"3.1\",\"packageName\":\"pkg-3\",\"packageVersion\":\"3.0\",\"packageType\":\"OS\",\"exploitable\":false}"
                + "],"
                + "\"vulnerabilitiesFixed\":["
                + "{\"cve\":\"CVE-2023-0001\",\"severity\":\"High\",\"fixVersion\":\"1.1\",\"packageName\":\"pkg-1\",\"packageVersion\":\"1.0\",\"packageType\":\"OS\",\"exploitable\":true}"
                + "]"
                + "}");

        assertEquals(expectedJson, jsonElement, "Serialized JSON does not match expected output.");
    }
}
