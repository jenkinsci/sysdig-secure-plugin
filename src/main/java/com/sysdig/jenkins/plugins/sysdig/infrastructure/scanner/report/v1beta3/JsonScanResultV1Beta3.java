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
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Optional;

public class JsonScanResultV1Beta3 {
  @SuppressFBWarnings(value = "NP_UNWRITTEN_FIELD", justification = "Field set via Gson deserialization")
  private JsonResult result;

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
    return new ScanResult(ScanType.Docker,
      result.metadata().pullString(),
      result.metadata().imageId(),
      result.metadata().digest(),
      new OperatingSystem(
        osTypeFromString(result.metadata().os()),
        result.metadata().baseOs()
      ),
      new BigInteger(result.metadata().size().toString()),
      archFromString(result.metadata().architecture()),
      result.metadata().labels(),
      dateFromISO8601String(result.metadata().createdAt()));
  }

  private void addLayersWithDigestTo(ScanResult scanResult) {
    result.layers().stream()
      .filter(l -> l.digest() != null && !l.digest().isBlank())
      .forEach(layer ->
        scanResult.addLayer(layer.digest(), BigInteger.valueOf(layer.size()), layer.command())
      );
  }

  private void addPackagesTo(ScanResult scanResult) {
    result.packages().stream().forEach(p -> {
      String layerDigest = p.layerDigest();

      Package addedPackage = scanResult.addPackage(
        packageTypeFromString(p.type().toLowerCase()),
        p.name(),
        p.version(),
        p.path(),
        scanResult.findLayerByDigest(layerDigest).get()
      );

      p.vulns().stream().forEach(vulnerability -> {
        addVulnerabilityTo(scanResult, vulnerability, addedPackage);
      });
    });
  }

  private void addPolicyEvaluationsTo(ScanResult scanResult) {
    result.policyEvaluations().stream().forEach(policyEvaluation -> {
      Policy addedPolicy = scanResult.addPolicy(
        policyEvaluation.identifier(),
        policyEvaluation.name(),
        dateFromISO8601String(policyEvaluation.createdAt()),
        dateFromISO8601String(policyEvaluation.updatedAt())
      );

      addPolicyBundlesTo(scanResult, policyEvaluation, addedPolicy);
    });
  }

  private void addPolicyBundlesTo(ScanResult scanResult, JsonPolicyEvaluation policyEvaluation, Policy addedPolicy) {
    policyEvaluation.bundles().stream().forEach(b -> {
      PolicyBundle policyBundle = scanResult.addPolicyBundle(
        b.identifier(),
        b.name(),
        addedPolicy
      );

      b.rules().stream().forEach(r -> {
        PolicyBundleRule policyBundleRule = policyBundle.addRule(
          r.ruleId().toString(),
          r.description(),
          r.evaluationResult().equalsIgnoreCase("failed") ? EvaluationResult.Failed : EvaluationResult.Passed
        );

        r.failures().stream().forEach(f -> {
          switch (r.failureType()) {
            case "imageConfigFailure" -> policyBundleRule.addImageConfigFailure(f.remediation());
            case "pkgVulnFailure" -> policyBundleRule.addPkgVulnFailure(f.description());
            default -> throw new IllegalStateException("Unexpected value: " + r.failureType());
          }
        });
      });
    });
  }

  private void addVulnerabilityTo(ScanResult scanResult, JsonVuln vulnerability, Package addedPackage) {
    Vulnerability addedVulnerability = scanResult.addVulnerability(
      vulnerability.name(),
      severityFromString(vulnerability.severity().value()),
      dateFromShortString(vulnerability.disclosureDate()),
      vulnerability.optSolutionDate().map(JsonScanResultV1Beta3::dateFromShortString).orElse(null),
      vulnerability.exploitable(),
      vulnerability.optFixedInVersion().orElse(null)
    );
    addedPackage.addVulnerabilityFound(addedVulnerability);

    vulnerability.acceptedRisks().stream().forEach(riskRef -> {
      JsonAcceptedRisk risk = result.riskAcceptanceDefinitions().stream()
        .filter(r -> r.optId().isPresent() && r.optId().get().equals(riskRef.id()))
        .findFirst()
        .get();

      var addedAcceptedRisk = scanResult.addAcceptedRisk(
        risk.id(),
        acceptedRiskReasonFromString(risk.reason()),
        risk.description(),
        dateFromShortString(risk.expirationDate()),
        "active".equalsIgnoreCase(risk.status()),
        dateFromISO8601String(risk.createdAt()),
        dateFromISO8601String(risk.updatedAt())
      );
      addedAcceptedRisk.addForVulnerability(addedVulnerability);
      addedAcceptedRisk.addForPackage(addedPackage);
    });
  }

  private void addAcceptedRisksTo(ScanResult scanResult) {
    result.riskAcceptanceDefinitions().stream().forEach(acceptedRisk -> {
      scanResult.addAcceptedRisk(
        acceptedRisk.id(),
        acceptedRiskReasonFromString(acceptedRisk.reason()),
        acceptedRisk.description(),
        dateFromShortString(acceptedRisk.expirationDate()),
        "active".equalsIgnoreCase(acceptedRisk.status()),
        dateFromISO8601String(acceptedRisk.createdAt()),
        dateFromISO8601String(acceptedRisk.updatedAt())
      );
    });
  }
}
