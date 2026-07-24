package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.iac.v1;

import com.sysdig.jenkins.plugins.sysdig.domain.iac.scanresult.IaCScanResult;
import com.sysdig.jenkins.plugins.sysdig.domain.iac.scanresult.IaCScanType;
import com.sysdig.jenkins.plugins.sysdig.domain.iac.scanresult.Metadata;
import com.sysdig.jenkins.plugins.sysdig.domain.iac.scanresult.Resource;
import com.sysdig.jenkins.plugins.sysdig.domain.iac.scanresult.Severity;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Deserialized shape of the {@code sysdig-cli-scanner --iac --output-json} report (v1) and its
 * mapping to the {@link IaCScanResult} domain model. Parallels
 * {@code com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1.JsonScanResultV1}.
 */
public record JsonIaCScanResultV1(JsonIaCInfo info, JsonIaCScanner scanner, JsonIaCResult result) {

    public Optional<IaCScanResult> toDomain() {
        if (result() == null) {
            return Optional.empty();
        }

        IaCScanResult scanResult = createScanResult();

        addFindingsTo(scanResult);
        addUnsupportedResourcesTo(scanResult);
        addErrorsTo(scanResult);

        return Optional.of(scanResult);
    }

    private IaCScanResult createScanResult() {
        return new IaCScanResult(scanTypeFromString(result().type()), metadataToDomain(), findingsSummaryToDomain());
    }

    private Metadata metadataToDomain() {
        JsonIaCMetadata metadata = result().metadata();
        if (metadata == null) {
            return new Metadata(List.of(), 0, 0, 0);
        }
        return new Metadata(
                metadata.sources() == null ? List.of() : metadata.sources(),
                orZero(metadata.totalModules()),
                orZero(metadata.totalResource()),
                orZero(metadata.totalFolders()));
    }

    private Map<Severity, Integer> findingsSummaryToDomain() {
        Map<Severity, Integer> summary = new EnumMap<>(Severity.class);
        JsonIaCFindingsSummary jsonSummary = result().findingsSummaryBySeverity();
        if (jsonSummary == null) {
            return summary;
        }
        if (jsonSummary.critical() != null) summary.put(Severity.Critical, jsonSummary.critical());
        if (jsonSummary.high() != null) summary.put(Severity.High, jsonSummary.high());
        if (jsonSummary.medium() != null) summary.put(Severity.Medium, jsonSummary.medium());
        if (jsonSummary.low() != null) summary.put(Severity.Low, jsonSummary.low());
        if (jsonSummary.negligible() != null) summary.put(Severity.Negligible, jsonSummary.negligible());
        return summary;
    }

    private void addFindingsTo(IaCScanResult scanResult) {
        if (result().findings() == null) {
            return;
        }
        result().findings().forEach(jsonFinding -> {
            List<Resource> resources = jsonFinding.resources() == null
                    ? List.of()
                    : jsonFinding.resources().stream()
                            .map(r -> new Resource(r.name(), r.type(), r.location(), r.source()))
                            .toList();

            scanResult.addFinding(
                    orZero(jsonFinding.controlId()),
                    jsonFinding.name(),
                    severityFromString(jsonFinding.severity()),
                    resources,
                    jsonFinding.policies() == null ? List.of() : jsonFinding.policies(),
                    jsonFinding.requirements() == null ? List.of() : jsonFinding.requirements());
        });
    }

    private void addUnsupportedResourcesTo(IaCScanResult scanResult) {
        if (result().unsupportedResources() == null) {
            return;
        }
        result().unsupportedResources()
                .forEach(jsonUnsupported ->
                        scanResult.addUnsupportedResource(jsonUnsupported.source(), jsonUnsupported.detailsList()));
    }

    private void addErrorsTo(IaCScanResult scanResult) {
        if (result().errors() == null) {
            return;
        }
        result().errors().forEach(jsonError -> scanResult.addError(jsonError.source(), jsonError.detailsList()));
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static IaCScanType scanTypeFromString(String type) {
        return "IaCGitScan".equalsIgnoreCase(type) ? IaCScanType.IaCGitScan : IaCScanType.Unknown;
    }

    private static Severity severityFromString(String severity) {
        if (severity == null) {
            return Severity.Unknown;
        }
        return switch (severity.toLowerCase()) {
            case "critical" -> Severity.Critical;
            case "high" -> Severity.High;
            case "medium" -> Severity.Medium;
            case "low" -> Severity.Low;
            case "negligible" -> Severity.Negligible;
            default -> Severity.Unknown;
        };
    }
}
