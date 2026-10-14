package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ComponentTest {

    private ScanResult scanResult;
    private Layer layer;

    @BeforeEach
    void setUp() {
        scanResult = new ScanResult(
                EvaluationResult.Failed,
                ScanType.Docker,
                "test-pull-string",
                "test-image-id",
                "test-digest",
                new OperatingSystem(OperatingSystem.Family.Linux, "alpine 3.18"),
                BigInteger.ONE,
                Architecture.AMD64,
                null,
                new Date());
        layer = scanResult.addLayer("sha256:layer", BigInteger.ONE, "COPY . .");
    }

    @Test
    void applicationsTakePartInEveryRelationshipLikePackages() {
        Package pkg = scanResult.addPackage("pkg", PackageType.Golang, "golang.org/x/net", "0.1.0", "/app", layer);
        Application app = scanResult.addApplication("app", "Web Server", "httpd", "2.4.49", "/bin/httpd", layer);
        Vulnerability vuln = scanResult.addVulnerability(
                "CVE-2021-41773", Severity.Critical, new Date(), null, true, "2.4.51", true, null);
        AcceptedRisk risk = scanResult.addAcceptedRisk(
                "risk", AcceptedRiskReason.RiskMitigated, "mitigated", null, true, new Date(), new Date());

        app.addVulnerabilityFound(vuln);
        pkg.addVulnerabilityFound(vuln);
        app.addAcceptedRisk(risk);

        assertEquals(Set.of(pkg, app), vuln.foundInComponents());
        assertEquals(Set.of(app), vuln.foundInApplications());
        assertEquals(Set.of(pkg), vuln.foundInPackages());
        assertEquals(Set.of(layer), vuln.foundInLayers());
        assertEquals(Set.of(app), risk.assignedToComponents());
        assertEquals(List.of(app), List.copyOf(layer.applications()));
        assertEquals(List.of(pkg), List.copyOf(layer.packages()));
        assertEquals(Set.of(vuln), Set.copyOf(layer.vulnerabilities()));

        Policy policy = scanResult.addPolicy("policy", "policy", new Date(), new Date());
        PolicyBundleRulePkgVuln rule = scanResult
                .addPolicyBundle("bundle", "bundle", policy)
                .addPkgVulnRule("rule", "Severity >= critical", EvaluationResult.Failed);
        PolicyBundleRulePkgVulnFailure failure = rule.addFailure("upgrade", app, vuln);

        assertEquals(app, failure.component());
        assertEquals("CVE-2021-41773 found in httpd (2.4.49)", failure.description());
    }

    @Test
    void imageComponentsHaveNoLayer() {
        ImageComponent os =
                scanResult.addImageComponent("os", Component.ComponentKind.OperatingSystem, "os", "alpine", "3.18");
        Vulnerability vuln =
                scanResult.addVulnerability("CVE-2024-0001", Severity.High, null, null, false, null, false, null);

        os.addVulnerabilityFound(vuln);

        assertTrue(os.foundInLayer().isEmpty());
        assertTrue(vuln.foundInLayers().isEmpty());
        assertEquals(Set.of(os), vuln.foundInComponents());
        assertEquals(List.of(os), List.copyOf(scanResult.imageComponents()));
        assertTrue(scanResult.packages().isEmpty());
    }

    @Test
    void imageComponentsOnlyAcceptImageKinds() {
        assertThrows(
                IllegalArgumentException.class,
                () -> scanResult.addImageComponent("x", Component.ComponentKind.Application, "app", "x", "1"));
    }

    @Test
    void componentsOfDifferentKindsAreDistinctAndLookedUpById() {
        Package pkg = scanResult.addPackage("pkg", PackageType.Java, "log4j", "2.14.1", "/app.jar", layer);
        Application app = scanResult.addApplication("app", "Database", "postgres", "16.1", "/bin/postgres", layer);

        assertEquals(pkg, scanResult.findComponentById("pkg").orElseThrow());
        assertEquals(app, scanResult.findComponentById("app").orElseThrow());
        assertEquals(Set.of(pkg, app), Set.copyOf(scanResult.components()));
        assertEquals("Java", pkg.typeName());
        assertEquals("Database", app.typeName());
    }
}
