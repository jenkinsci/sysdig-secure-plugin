/*
Copyright (C) 2016-2024 Sysdig

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1beta3;

import com.sysdig.jenkins.plugins.sysdig.domain.vm.report.*;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.report.Layer;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.report.Package;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.report.Severity;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

public class JsonScanResult implements Serializable {
  private Result result;

  public Optional<Result> getResult() {
    return Optional.ofNullable(result);
  }

  public void setResult(Result result) {
    this.result = result;
  }

  public ScanResult toDomain() {
    ScanResult scanResult = createScanResult();

    addLayersTo(scanResult);
    addPackagesTo(scanResult);
    addPolicyEvaluationsTo(scanResult);
    addAcceptedRisksTo(scanResult);

    return scanResult;
  }

  private void addPolicyEvaluationsTo(ScanResult scanResult) {
    result.getPolicyEvaluations().stream().flatMap(Collection::stream).forEach(policyEvaluation -> {
      String id = policyEvaluation.getIdentifier().get();
      String name = policyEvaluation.getName().get();
      PolicyType type = policyEvaluation.getType().map(policyType -> switch (policyType.toLowerCase()) {
        case "custom" -> PolicyType.Custom;
        default -> PolicyType.Unknown;
      }).orElse(PolicyType.Unknown);

      String createdAtString = policyEvaluation.getCreatedAt().get();
      String updatedAtString = policyEvaluation.getUpdatedAt().get();

      Date createdAt = Date.from(Instant.parse(createdAtString));
      Date updatedAt = Date.from(Instant.parse(updatedAtString));

      Policy addedPolicy = scanResult.addPolicy(
        id,
        name,
        type,
        createdAt,
        updatedAt
      );

      addPolicyBundlesTo(scanResult, policyEvaluation, addedPolicy);
    });
  }

  private void addPolicyBundlesTo(ScanResult scanResult, PolicyEvaluation policyEvaluation, Policy addedPolicy) {
    policyEvaluation.getBundles().stream().flatMap(Collection::stream).forEach(bundle -> {
      String id = bundle.getIdentifier().get();
      String name = bundle.getName().get();
      List<PolicyBundleRule> rules = bundle.getRules().stream().flatMap(Collection::stream).map(r -> {
        String ruleId = r.getRuleId().get().toString();
        String description = r.getDescription().get();
        EvaluationResult evaluationResult = r.getEvaluationResult().orElse("passed").equalsIgnoreCase("failed") ? EvaluationResult.Failed : EvaluationResult.Passed;

        return new PolicyBundleRule(ruleId, description, evaluationResult);
      }).toList();

      String createdAtString = bundle.getCreatedAt().get();
      String updatedAtString = bundle.getUpdatedAt().get();

      Date createdAt = Date.from(Instant.parse(createdAtString));
      Date updatedAt = Date.from(Instant.parse(updatedAtString));

      scanResult.addPolicyBundle(
        id,
        name,
        rules,
        createdAt,
        updatedAt,
        addedPolicy
      );
    });
  }

  private void addPackagesTo(ScanResult scanResult) {
    result.getPackages().stream().flatMap(Collection::stream).forEach(aPackage -> {
      String layerDigest = aPackage.getLayerDigest().get();
      Layer layer = scanResult.findLayerByDigest(layerDigest).get();

      String packageTypeString = aPackage.getType().get().toLowerCase();
      PackageType packageType = packageTypeFromString(packageTypeString);
      Package addedPackage = scanResult.addPackage(packageType, aPackage.getName().get(), aPackage.getVersion().get(), aPackage.getPath().get(), layer);

      aPackage.getVulns().stream().flatMap(Collection::stream).forEach(vulnerability -> {
        addVulnerabilityTo(scanResult, vulnerability, addedPackage);
      });
    });
  }

  private void addVulnerabilityTo(ScanResult scanResult, Vuln vulnerability, Package addedPackage) {
    String name = vulnerability.getName().get();
    String disclosureDateString = vulnerability.getDisclosureDate().get();
    String severityString = vulnerability.getSeverity().flatMap(s -> s.getValue()).get();
    Optional<String> solutionDateString = vulnerability.getSolutionDate();
    boolean exploitable = vulnerability.getExploitable().orElse(false);
    String fixedInVersion = vulnerability.getFixedInVersion().orElse(null);

    Severity severity = switch (severityString.toLowerCase()) {
      case "critical" -> Severity.Critical;
      case "high" -> Severity.High;
      case "medium" -> Severity.Medium;
      case "low" -> Severity.Low;
      case "negligible" -> Severity.Negligible;
      default -> Severity.Unknown;
    };
    Date disclosureDate = Date.from(LocalDate.parse(disclosureDateString).atStartOfDay(ZoneId.systemDefault()).toInstant());
    Date solutionDate = solutionDateString
      .map(str -> Date.from(LocalDate.parse(str).atStartOfDay(ZoneId.systemDefault()).toInstant()))
      .orElse(null);

    Vulnerability addedVulnerability = scanResult.addVulnerability(name, severity, disclosureDate, solutionDate, exploitable, fixedInVersion);
    addedPackage.addVulnerabilityFound(addedVulnerability);

    vulnerability.getAcceptedRisks().stream().flatMap(Collection::stream).forEach(riskRef -> {
      Optional<AcceptedRisk> acceptedRisk = result.getRiskAcceptanceDefinitions().stream().flatMap(Collection::stream).filter(riskDef -> riskDef.getId().orElse("one").equals(riskRef.getId().orElse("other"))).findFirst();
      if (acceptedRisk.isEmpty()) {
        return;
      }
      AcceptedRisk risk = acceptedRisk.get();

      addAcceptedRiskForVulnerability(scanResult, addedPackage, risk, addedVulnerability);
    });
  }

  private void addAcceptedRiskForVulnerability(ScanResult scanResult, Package addedPackage, AcceptedRisk risk, Vulnerability addedVulnerability) {
    var addedAcceptedRisk = addDomainAcceptedRisk(scanResult, risk);
    addedAcceptedRisk.addForVulnerability(addedVulnerability);
    addedAcceptedRisk.addForPackage(addedPackage);
  }

  private static com.sysdig.jenkins.plugins.sysdig.domain.vm.report.AcceptedRisk addDomainAcceptedRisk(ScanResult scanResult, AcceptedRisk risk) {
    String id = risk.getId().get();
    boolean active = risk.getStatus().map(s -> s.equals("active")).orElse(false);
    String reasonString = risk.getReason().get();
    String description = risk.getDescription().get();
    String expirationDateString = risk.getExpirationDate().get();
    String createdAtString = risk.getCreatedAt().get();
    String updateAtString = risk.getUpdatedAt().get();

    AcceptedRiskReason reason = switch (reasonString) {
      case "RiskOwned" -> AcceptedRiskReason.RiskOwned;
      case "RiskTransferred" -> AcceptedRiskReason.RiskTransferred;
      case "RiskAvoided" -> AcceptedRiskReason.RiskAvoided;
      case "RiskMitigated" -> AcceptedRiskReason.RiskMitigated;
      case "RiskNotRelevant" -> AcceptedRiskReason.RiskNotRelevant;
      case "Custom" -> AcceptedRiskReason.Custom;
      default -> AcceptedRiskReason.Unknown;
    };

    Date expirationDate = Date.from(LocalDate.parse(expirationDateString).atStartOfDay(ZoneId.systemDefault()).toInstant());
    Date createdAt = Date.from(Instant.parse(createdAtString));
    Date updatedAt = Date.from(Instant.parse(updateAtString));

    return scanResult.addAcceptedRisk(id, reason, description, expirationDate, active, createdAt, updatedAt);
  }


  private static PackageType packageTypeFromString(String string) {
    return switch (string) {
      case "C#" -> PackageType.CSharp;
      case "golang" -> PackageType.Golang;
      case "java" -> PackageType.Java;
      case "javascript" -> PackageType.Javascript;
      case "os" -> PackageType.OS;
      case "php" -> PackageType.PHP;
      case "python" -> PackageType.Python;
      case "ruby" -> PackageType.Ruby;
      case "rust" -> PackageType.Rust;
      default -> PackageType.Unknown;
    };
  }

  private void addLayersTo(ScanResult scanResult) {
    result.getLayers().stream().flatMap(Collection::stream).forEach(layer -> {
      if (layer.getDigest().isEmpty()) {
        return;
      }

      scanResult.addLayer(layer.getDigest().get(), BigInteger.valueOf(layer.getSize().get()), layer.getCommand().get());
    });
  }

  private void addAcceptedRisksTo(ScanResult scanResult) {
    result.getRiskAcceptanceDefinitions().stream().flatMap(Collection::stream).forEach(acceptedRisk -> {
      addDomainAcceptedRisk(scanResult, acceptedRisk);
    });
  }

  private ScanResult createScanResult() {
    Metadata metadata = result.getMetadata().orElseThrow(() -> new IllegalStateException("Metadata not present in result"));

    String pullString = metadata.getPullString().get();
    String imageID = metadata.getImageId().get();
    String digest = metadata.getDigest().get();
    String os = metadata.getOs().get();
    String baseOS = metadata.getBaseOs().get();
    Long size = metadata.getSize().get();
    String architecture = metadata.getArchitecture().get();
    Map<String, String> labels = metadata.labels().get();
    String createdAtStr = metadata.getCreatedAt().get();

    OperatingSystem.Family osFamily = switch (os.toLowerCase()) {
      case "linux" -> OperatingSystem.Family.Linux;
      case "darwin" -> OperatingSystem.Family.Darwin;
      case "windows" -> OperatingSystem.Family.Windows;
      default -> OperatingSystem.Family.Unknown;
    };

    Architecture arch = switch (architecture.toLowerCase()) {
      case "amd64" -> Architecture.AMD64;
      case "arm64" -> Architecture.ARM64;
      default -> Architecture.Unknown;
    };

    Date createdAt = Date.from(Instant.parse(createdAtStr));

    return new ScanResult(ScanType.Docker, pullString, imageID, digest, new OperatingSystem(osFamily, baseOS), new BigInteger(size.toString()), arch, labels, createdAt);
  }
}
