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

import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.*;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.Package;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.Severity;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;

public class JsonScanResult implements Serializable {
  private Result result;

  public void setResult(Result result) {
    this.result = result;
  }

  public Optional<ScanResult> toDomain() {
    if (result == null) {
      return Optional.empty();
    }

    ScanResult scanResult = createScanResult();
    addLayersWithDigestTo(scanResult);
    addPackagesTo(scanResult);
    addPolicyEvaluationsTo(scanResult);
    addAcceptedRisksTo(scanResult);

    return Optional.of(scanResult);
  }

  private ScanResult createScanResult() {
    Metadata metadata = result.getMetadata().orElseThrow(() -> new IllegalStateException("Metadata not present in result"));

    return new ScanResult(ScanType.Docker,
      metadata.getPullString().get(),
      metadata.getImageId().get(),
      metadata.getDigest().get(),
      new OperatingSystem(
        osTypeFromString(metadata.getOs().get()),
        metadata.getBaseOs().get()
      ),
      new BigInteger(metadata.getSize().get().toString()),
      archFromString(metadata.getArchitecture().get()),
      metadata.labels().get(),
      dateFromISO8601String(metadata.getCreatedAt().get()));
  }

  private void addLayersWithDigestTo(ScanResult scanResult) {
    result.getLayers().stream().flatMap(Collection::stream)
      .filter(l -> l.getDigest().isPresent())
      .forEach(layer ->
        scanResult.addLayer(layer.getDigest().get(), BigInteger.valueOf(layer.getSize().get()), layer.getCommand().get())
      );
  }

  private void addPackagesTo(ScanResult scanResult) {
    result.getPackages().stream().flatMap(Collection::stream).forEach(p -> {
      String layerDigest = p.getLayerDigest().get();

      Package addedPackage = scanResult.addPackage(
        packageTypeFromString(p.getType().get().toLowerCase()),
        p.getName().get(),
        p.getVersion().get(),
        p.getPath().get(),
        scanResult.findLayerByDigest(layerDigest).get()
      );

      p.getVulns().stream().flatMap(Collection::stream).forEach(vulnerability -> {
        addVulnerabilityTo(scanResult, vulnerability, addedPackage);
      });
    });
  }

  private void addPolicyEvaluationsTo(ScanResult scanResult) {
    result.getPolicyEvaluations().stream().flatMap(Collection::stream).forEach(policyEvaluation -> {
      Policy addedPolicy = scanResult.addPolicy(
        policyEvaluation.getIdentifier().get(),
        policyEvaluation.getName().get(),
        policyEvaluation.getType().map(JsonScanResult::policyTypeFromString).orElse(PolicyType.Unknown),
        dateFromISO8601String(policyEvaluation.getCreatedAt().get()),
        dateFromISO8601String(policyEvaluation.getUpdatedAt().get())
      );

      addPolicyBundlesTo(scanResult, policyEvaluation, addedPolicy);
    });
  }

  private void addPolicyBundlesTo(ScanResult scanResult, PolicyEvaluation policyEvaluation, Policy addedPolicy) {
    policyEvaluation.getBundles().stream().flatMap(Collection::stream).forEach(b -> {
      PolicyBundle policyBundle = scanResult.addPolicyBundle(
        b.getIdentifier().get(),
        b.getName().get(),
        dateFromISO8601String(b.getCreatedAt().get()),
        dateFromISO8601String(b.getUpdatedAt().get()),
        addedPolicy
      );

      b.getRules().stream().flatMap(Collection::stream).forEach(r -> {
        PolicyBundleRule policyBundleRule = policyBundle.addRule(
          r.getRuleId().orElse(0L).toString(),
          r.getDescription().get(),
          r.getEvaluationResult().orElse("passed").equalsIgnoreCase("failed") ? EvaluationResult.Failed : EvaluationResult.Passed
        );

        r.getFailures().stream().flatMap(Collection::stream).forEach(f -> {
          switch (r.getFailureType().get()) {
            case "imageConfigFailure" -> policyBundleRule.addImageConfigFailure(f.getRemediation().get());
            case "pkgVulnFailure" -> policyBundleRule.addPkgVulnFailure(f.getDescription().get());
            default -> throw new IllegalStateException("Unexpected value: " + r.getFailureType().get());
          }
        });
      });
    });
  }

  private void addVulnerabilityTo(ScanResult scanResult, Vuln vulnerability, Package addedPackage) {
    Vulnerability addedVulnerability = scanResult.addVulnerability(
      vulnerability.getName().get(),
      severityFromString(vulnerability.getSeverity().flatMap(s -> s.getValue()).get()),
      dateFromShortString(vulnerability.getDisclosureDate().get()),
      vulnerability.getSolutionDate().map(JsonScanResult::dateFromShortString).orElse(null),
      vulnerability.getExploitable().orElse(false),
      vulnerability.getFixedInVersion().orElse(null)
    );
    addedPackage.addVulnerabilityFound(addedVulnerability);

    vulnerability.getAcceptedRisks().stream().flatMap(Collection::stream).forEach(riskRef -> {
      AcceptedRisk risk = result.getRiskAcceptanceDefinitions().stream().flatMap(Collection::stream)
        .filter(r -> r.getId().isPresent() && r.getId().equals(riskRef.getId()))
        .findFirst()
        .get();

      var addedAcceptedRisk = scanResult.addAcceptedRisk(
        risk.getId().get(),
        acceptedRiskReasonFromString(risk.getReason().get()),
        risk.getDescription().get(),
        dateFromShortString(risk.getExpirationDate().get()),
        risk.getStatus().map(s -> s.equals("active")).orElse(false),
        dateFromISO8601String(risk.getCreatedAt().get()),
        dateFromISO8601String(risk.getUpdatedAt().get())
      );
      addedAcceptedRisk.addForVulnerability(addedVulnerability);
      addedAcceptedRisk.addForPackage(addedPackage);
    });
  }

  private void addAcceptedRisksTo(ScanResult scanResult) {
    result.getRiskAcceptanceDefinitions().stream().flatMap(Collection::stream).forEach(acceptedRisk -> {
      scanResult.addAcceptedRisk(
        acceptedRisk.getId().get(),
        acceptedRiskReasonFromString(acceptedRisk.getReason().get()),
        acceptedRisk.getDescription().get(),
        dateFromShortString(acceptedRisk.getExpirationDate().get()),
        acceptedRisk.getStatus().map(s -> s.equals("active")).orElse(false),
        dateFromISO8601String(acceptedRisk.getCreatedAt().get()),
        dateFromISO8601String(acceptedRisk.getUpdatedAt().get())
      );
    });
  }

  /**
   * Obtains an instance of {@code Date} from a text string such as {@code 2007-12-03}.
   * <p>
   * The string must represent a valid date and is parsed using
   * {@link java.time.format.DateTimeFormatter#ISO_LOCAL_DATE}.
   *
   * @param str the text to parse such as "2007-12-03", not null
   * @return the parsed local date, not null
   * @throws DateTimeParseException if the text cannot be parsed
   */
  private static Date dateFromShortString(@NonNull String str) {
    return Date.from(LocalDate.parse(str).atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  /**
   * Obtains an instance of {@code Date} from a text string such as
   * {@code 2007-12-03T10:15:30.00Z}.
   * <p>
   * The string must represent a valid instant in UTC and is parsed using
   * {@link DateTimeFormatter#ISO_INSTANT}.
   *
   * @param str the text to parse, not null
   * @return the parsed date, not null
   * @throws DateTimeParseException if the text cannot be parsed
   */
  private static Date dateFromISO8601String(@NonNull String str) {
    return Date.from(Instant.parse(str));
  }

  private static PolicyType policyTypeFromString(String policyType) {
    return switch (policyType.toLowerCase()) {
      case "custom" -> PolicyType.Custom;
      default -> PolicyType.Unknown;
    };
  }

  private static Severity severityFromString(String severityString) {
    return switch (severityString.toLowerCase()) {
      case "critical" -> Severity.Critical;
      case "high" -> Severity.High;
      case "medium" -> Severity.Medium;
      case "low" -> Severity.Low;
      case "negligible" -> Severity.Negligible;
      default -> Severity.Unknown;
    };
  }

  private static AcceptedRiskReason acceptedRiskReasonFromString(String reasonString) {
    return switch (reasonString) {
      case "RiskOwned" -> AcceptedRiskReason.RiskOwned;
      case "RiskTransferred" -> AcceptedRiskReason.RiskTransferred;
      case "RiskAvoided" -> AcceptedRiskReason.RiskAvoided;
      case "RiskMitigated" -> AcceptedRiskReason.RiskMitigated;
      case "RiskNotRelevant" -> AcceptedRiskReason.RiskNotRelevant;
      case "Custom" -> AcceptedRiskReason.Custom;
      default -> AcceptedRiskReason.Unknown;
    };
  }

  private static Architecture archFromString(String architecture) {
    return switch (architecture.toLowerCase()) {
      case "amd64" -> Architecture.AMD64;
      case "arm64" -> Architecture.ARM64;
      default -> Architecture.Unknown;
    };
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

  private static OperatingSystem.Family osTypeFromString(String os) {
    return switch (os.toLowerCase()) {
      case "linux" -> OperatingSystem.Family.Linux;
      case "darwin" -> OperatingSystem.Family.Darwin;
      case "windows" -> OperatingSystem.Family.Windows;
      default -> OperatingSystem.Family.Unknown;
    };
  }
}
