package com.sysdig.jenkins.plugins.sysdig.domain.iac.scanresult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Aggregate root of an Infrastructure as Code scan. Holds the policy findings, the resources the
 * scanner could not evaluate, the parse errors and a per-severity summary of the findings, mirroring
 * the way {@code com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.ScanResult} models an image scan.
 */
public class IaCScanResult implements Serializable {
    private final IaCScanType type;
    private final Metadata metadata;
    private final Map<Severity, Integer> findingsSummary;
    private final List<Finding> findings;
    private final List<UnsupportedResource> unsupportedResources;
    private final List<ScanError> errors;

    public IaCScanResult(IaCScanType type, Metadata metadata, Map<Severity, Integer> findingsSummary) {
        this.type = type;
        this.metadata = metadata;
        this.findingsSummary = new EnumMap<>(Severity.class);
        if (findingsSummary != null) {
            findingsSummary.forEach((severity, count) -> {
                if (severity != null && count != null) {
                    this.findingsSummary.put(severity, count);
                }
            });
        }
        this.findings = new ArrayList<>();
        this.unsupportedResources = new ArrayList<>();
        this.errors = new ArrayList<>();
    }

    public Finding addFinding(
            int controlId,
            String name,
            Severity severity,
            List<Resource> resources,
            List<String> policies,
            List<String> requirements) {
        Finding finding = new Finding(controlId, name, severity, resources, policies, requirements);
        this.findings.add(finding);
        return finding;
    }

    public UnsupportedResource addUnsupportedResource(String source, List<String> details) {
        UnsupportedResource unsupportedResource = new UnsupportedResource(source, details);
        this.unsupportedResources.add(unsupportedResource);
        return unsupportedResource;
    }

    public ScanError addError(String source, List<String> details) {
        ScanError error = new ScanError(source, details);
        this.errors.add(error);
        return error;
    }

    public IaCScanType type() {
        return type;
    }

    public Metadata metadata() {
        return metadata;
    }

    public Collection<Finding> findings() {
        return Collections.unmodifiableList(findings);
    }

    public Collection<UnsupportedResource> unsupportedResources() {
        return Collections.unmodifiableList(unsupportedResources);
    }

    public Collection<ScanError> errors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Per-severity finding counts as reported by the scanner. Note this counts affected resources,
     * so it may be larger than {@link #findings()} (a single control can fail against many resources).
     */
    public Map<Severity, Integer> findingsSummary() {
        return Collections.unmodifiableMap(findingsSummary);
    }

    /**
     * Number of affected resources for a severity, as reported by the scanner's summary. A single
     * failed control can affect several resources, so these counts sum to the number of affected
     * resources, not to {@link #findings()}.
     */
    public int reportedFindingsCount(Severity severity) {
        return findingsSummary.getOrDefault(severity, 0);
    }

    /**
     * Number of failed controls (findings) with the given severity. These sum to {@link #findings()}.
     */
    public int findingsCountBySeverity(Severity severity) {
        return (int) findings.stream().filter(f -> f.severity() == severity).count();
    }

    /**
     * Number of control/resource pairs. A failed control can hit several resources, and the same
     * resource can fail several controls, so this counts violations and not distinct resources.
     */
    public int resourceViolations() {
        return findings.stream().mapToInt(finding -> finding.resources().size()).sum();
    }

    /**
     * Whether the scan produced any policy finding at all.
     */
    public boolean hasFindings() {
        return !findings.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IaCScanResult that = (IaCScanResult) o;
        return type == that.type
                && Objects.equals(metadata, that.metadata)
                && Objects.equals(findingsSummary, that.findingsSummary)
                && Objects.equals(findings, that.findings)
                && Objects.equals(unsupportedResources, that.unsupportedResources)
                && Objects.equals(errors, that.errors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, metadata, findingsSummary, findings, unsupportedResources, errors);
    }
}
