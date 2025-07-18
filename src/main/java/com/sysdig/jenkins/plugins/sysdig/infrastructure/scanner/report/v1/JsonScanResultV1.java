package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1;

import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.Package;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.*;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Optional;

public record JsonScanResultV1(
        JsonInfo info,
        JsonScanner scanner,
        JsonResult result) {
    public Optional<ScanResult> toDomain() {
        if (result() == null) {
            return Optional.empty();
        }

        ScanResult scanResult = createScanResult();

        addLayersTo(scanResult);
        addRiskAcceptsTo(scanResult);
        addVulnerabilitiesTo(scanResult);
        addPackagesTo(scanResult);
        addPoliciesTo(scanResult);

        return Optional.of(scanResult);
    }

    private ScanResult createScanResult() {
        return new ScanResult(
                ScanType.Docker,
                result().metadata().pullString(),
                result().metadata().imageId(),
                result().metadata().digest(),
                new OperatingSystem(
                        osFamilyFromString(result().metadata().os()),
                        result().metadata().baseOs()),
                BigInteger.valueOf(result().metadata().size()),
                archFromString(result().metadata().architecture()),
                result().metadata().labels(),
                dateFromISO8601String(result().metadata().createdAt()));
    }

    private void addLayersTo(ScanResult scanResult) {
        result().layers().values().stream()
                .filter(jsonLayer -> !jsonLayer.digest().isBlank())
                .forEach(jsonLayer -> scanResult.addLayer(
                        jsonLayer.digest(),
                        BigInteger.valueOf(jsonLayer.size()),
                        jsonLayer.command()));
    }

    private void addRiskAcceptsTo(ScanResult scanResult) {
        result().riskAccepts().values().stream().forEach(jsonRisk -> {
            scanResult.addAcceptedRisk(
                    jsonRisk.id(),
                    acceptedRiskReasonFromString(jsonRisk.reason()),
                    jsonRisk.description(),
                    dateFromShortString(jsonRisk.expirationDate()),
                    "active".equalsIgnoreCase(jsonRisk.status()),
                    dateFromISO8601String(jsonRisk.createdAt()),
                    dateFromISO8601String(jsonRisk.updatedAt()));
        });
    }

    private void addVulnerabilitiesTo(ScanResult scanResult) {
        result().vulnerabilities().values()
                .forEach(jsonVuln -> {
                    Vulnerability vuln = scanResult.addVulnerability(
                            jsonVuln.name(),
                            severityFromString(jsonVuln.severity()),
                            dateFromShortString(jsonVuln.disclosureDate()),
                            jsonVuln.optSolutionDate().map(JsonScanResultV1::dateFromShortString).orElse(null),
                            jsonVuln.exploitable(),
                            jsonVuln.fixVersion());

                    jsonVuln.riskAcceptRefs().stream()
                            .map(jsonRiskRef -> result().riskAccepts().get(jsonRiskRef))
                            .map(jsonRisk -> scanResult.findAcceptedRiskByID(jsonRisk.id()).get())
                            .forEach(vuln::addAcceptedRisk);
                });
    }

    private void addPackagesTo(ScanResult scanResult) {
        result().packages().values().stream().forEach(jsonPkg -> {
            JsonLayer jsonLayer = result().layers().get(jsonPkg.layerRef());
            var layerWhereThisPackageIsFound = scanResult.findLayerByDigest(jsonLayer.digest()).get();

            Package addedPackage = scanResult.addPackage(
                    packageTypeFromString(jsonPkg.type()),
                    jsonPkg.name(),
                    jsonPkg.version(),
                    jsonPkg.path(),
                    layerWhereThisPackageIsFound);

            jsonPkg.vulnerabilitiesRefs().stream()
                    .map(jsonVulnRef -> this.result().vulnerabilities().get(jsonVulnRef))
                    .map(jsonVuln -> scanResult.findVulnerabilityByCVE(jsonVuln.name()).get())
                    .forEach(addedPackage::addVulnerabilityFound);

            jsonPkg.vulnerabilitiesRefs().stream()
                    .map(jsonVulnRef -> this.result().vulnerabilities().get(jsonVulnRef))
                    .flatMap(jsonVuln -> jsonVuln.riskAcceptRefs().stream())
                    .map(jsonRiskRef -> result().riskAccepts().get(jsonRiskRef))
                    .map(jsonRisk -> scanResult.findAcceptedRiskByID(jsonRisk.id()).get())
                    .forEach(addedPackage::addAcceptedRisk);
        });
    }

    private void addPoliciesTo(ScanResult scanResult) {
        result().policies().evaluations().stream().forEach(jsonPolicy -> {
            Policy policy = scanResult.addPolicy(
                    jsonPolicy.identifier(),
                    jsonPolicy.name(),
                    dateFromISO8601String(jsonPolicy.createdAt()),
                    dateFromISO8601String(jsonPolicy.updatedAt()));

            jsonPolicy.bundles().stream().forEach(jsonBundle -> {
                PolicyBundle policyBundle = scanResult.addPolicyBundle(
                        jsonBundle.identifier(),
                        jsonBundle.name(),
                        policy);

                jsonBundle.rules().stream().forEach(jsonRule -> {
                    PolicyBundleRule rule = policyBundle.addRule(
                            jsonRule.ruleId(),
                            jsonRule.description(),
                            jsonRule.evaluationResult().equalsIgnoreCase("failed") ? EvaluationResult.Failed
                                    : EvaluationResult.Passed);

                    jsonRule.failures().stream().forEach(jsonFailure -> {
                        switch (jsonRule.failureType()) {
                            case "imageConfigFailure" -> rule.addImageConfigFailure(jsonFailure.remediation());
                            case "pkgVulnFailure" ->
                                rule.addPkgVulnFailure(
                                        failureMessageFor(jsonFailure.packageRef(), jsonFailure.vulnerabilityRef()));
                            default -> throw new IllegalStateException("Unexpected value: " + jsonRule.failureType());
                        }
                    });
                });
            });
        });
    }

    private String failureMessageFor(String jsonPackageRef, String jsonVulnerabilityRef) {
        JsonPackage jsonPackage = result().packages().get(jsonPackageRef);
        JsonVulnerability jsonVulnerability = result().vulnerabilities().get(jsonVulnerabilityRef);
        return "%s found in %s (%s)".formatted(
                jsonVulnerability.name(),
                jsonPackage.name(),
                jsonPackage.version());
    }

    /**
     * Obtains an instance of {@code Date} from a text string such as
     * {@code 2007-12-03}.
     * <p>
     * The string must represent a valid date and is parsed using
     * {@link DateTimeFormatter#ISO_LOCAL_DATE}.
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

    private static OperatingSystem.Family osFamilyFromString(String os) {
        return switch (os.toLowerCase()) {
            case "linux" -> OperatingSystem.Family.Linux;
            case "darwin" -> OperatingSystem.Family.Darwin;
            case "windows" -> OperatingSystem.Family.Windows;
            default -> OperatingSystem.Family.Unknown;
        };
    }
}
