package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1beta3;

import com.sysdig.jenkins.plugins.sysdig.domain.vm.report.*;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.report.Layer;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.report.Severity;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.json.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

class JsonScanResultTest {
  ScanResult scanResult;

  @BeforeEach
  void setUp() {
    JsonScanResult jsonScanResult = jsonScanResultFromImage("ubuntu_22.04");
    scanResult = jsonScanResult.toDomain().get();
  }

  @Test
  void whenConvertingToDomainItHasTheGeneralStatistics() {
    assertEquals(1, scanResult.layers().size());
    assertEquals(101, scanResult.packages().size());
    assertEquals(8, scanResult.policies().size());
    assertEquals(2, scanResult.acceptedRisks().size());
    assertEquals(EvaluationResult.Failed, scanResult.evaluationResult());
  }

  @Test
    void whenConvertingToDomainItHasLayerInformation() {
    Layer layer = scanResult.layers().stream().findFirst().get();

    assertEquals("/bin/sh -c #(nop) ADD file:82f38ebced7b2756311fb492d3d44cc131b22654e8620baa93883537a3e355aa in / ", layer.command());
    assertEquals(101, layer.packages().size());
    assertEquals(18, layer.vulnerabilities().size()); // unique vulns
    assertEquals(41, layer.packages().stream().flatMap(p -> p.vulnerabilities().stream()).count()); // vulns per package
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
    List<Vulnerability> exploitableVulnerabilities = scanResult.vulnerabilities().stream().filter(Vulnerability::exploitable).toList();

    assertEquals(1, exploitableVulnerabilities.size());

  }

  @Test
  void whenConvertingToDomainItHasFixableVulnsInformation() {
    List<Vulnerability> fixableVulnerabilities = scanResult.vulnerabilities().stream().filter(Vulnerability::fixable).toList();

    assertEquals(2, fixableVulnerabilities.size()); // fixable unique vulns
    assertEquals(6, fixableVulnerabilities.stream().flatMap(v-> v.foundInPackages().stream()).count()); // fixable vulns per package

  }

  @Test
  void whenConvertingToDomainItHasVulnerabilitiesPerPackageInformation() {
    List<Vulnerability> vulnerabilitiesPerPackage = scanResult.packages().stream().flatMap(p -> p.vulnerabilities().stream()).toList();

    assertEquals(0, vulnerabilitiesPerPackage.stream().filter(v -> v.severity() == Severity.Critical).count());
    assertEquals(0, vulnerabilitiesPerPackage.stream().filter(v -> v.severity() == Severity.High).count());
    assertEquals(28, vulnerabilitiesPerPackage.stream().filter(v -> v.severity() == Severity.Low).count());
    assertEquals(10, vulnerabilitiesPerPackage.stream().filter(v -> v.severity() == Severity.Medium).count());
    assertEquals(3, vulnerabilitiesPerPackage.stream().filter(v -> v.severity() == Severity.Negligible).count());

  }

  @Test
  void whenConvertingToDomainItHasPolicyInformation() {
    Policy policy = scanResult.findPolicyByID("aasim-policy").get();
    assertEquals(2, policy.bundles().size());

    PolicyBundle policyBundle = scanResult.findPolicyBundleByID("pci-dss-v4-0").get();
    assertEquals(2, policyBundle.foundInPolicies().size());
    assertEquals(3, policyBundle.rules().size());
    assertTrue(policy.bundles().contains(policyBundle));
    assertTrue(policyBundle.foundInPolicies().contains(policy));


  }

  @Test
  void whenConvertingToDomainItHasPolicieswithAcceptedRisks() {
    Policy policyWithAcceptedRisk = scanResult.findPolicyByID("cardholder-policy-pci-dss").get();
    assertEquals(1, policyWithAcceptedRisk.bundles().size());
    assertEquals(EvaluationResult.Passed, policyWithAcceptedRisk.evaluationResult());

  }

  @Test
  void whenConvertingToDomainItHasFailedPolicies() {
    Policy failedPolicy = scanResult.findPolicyByID("nist-sp-800-star").get();
    assertEquals(EvaluationResult.Failed, failedPolicy.evaluationResult());

    PolicyBundle failedBundle = scanResult.findPolicyBundleByID("nist-sp-800-190").get();
    assertTrue(failedPolicy.bundles().contains(failedBundle));
    assertEquals(EvaluationResult.Failed, failedBundle.evaluationResult());
    assertEquals(EvaluationResult.Failed, failedBundle.rules().stream().skip(1).findFirst().get().evaluationResult());
  }

  private JsonScanResult jsonScanResultFromImage(String image) {
    String resourcePath = "com/sysdig/jenkins/plugins/sysdig/infrastructure/scanner/report/v1beta3/%s.json".formatted(image);
    InputStream imageStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
    assertNotNull(imageStream);

    return GsonBuilder.build().fromJson(new InputStreamReader(imageStream, StandardCharsets.UTF_8), JsonScanResult.class);
  }
}