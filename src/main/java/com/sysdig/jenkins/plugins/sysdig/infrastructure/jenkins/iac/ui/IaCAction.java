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
package com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.iac.ui;

import com.sysdig.jenkins.plugins.sysdig.domain.iac.scanresult.Finding;
import com.sysdig.jenkins.plugins.sysdig.domain.iac.scanresult.IaCScanResult;
import com.sysdig.jenkins.plugins.sysdig.domain.iac.scanresult.Metadata;
import com.sysdig.jenkins.plugins.sysdig.domain.iac.scanresult.Resource;
import com.sysdig.jenkins.plugins.sysdig.domain.iac.scanresult.ScanError;
import com.sysdig.jenkins.plugins.sysdig.domain.iac.scanresult.Severity;
import com.sysdig.jenkins.plugins.sysdig.domain.iac.scanresult.UnsupportedResource;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.json.GsonBuilder;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.iac.v1.JsonIaCScanResultV1;
import hudson.model.Action;
import hudson.model.Run;
import java.util.Collection;
import java.util.List;

/**
 * Build-page action that renders the IaC scan result as tables (server-side, via the Jelly views).
 *
 * <p>Actions are persisted with the build via XStream (not Java serialization), so this class does
 * not implement {@link java.io.Serializable} — matching the VM {@code SysdigAction}. Only the raw
 * scanner JSON is stored, keeping the payload small; the {@link IaCScanResult} domain graph is
 * re-parsed lazily on render.
 */
public class IaCAction implements Action {

    private static final String BASE_URL_NAME = "sysdig-secure-iac-results";
    private static final String BASE_DISPLAY_NAME = "Sysdig Secure IaC Report";
    /** The scanner reports a resource's module as {@code "source file: <path>"}. */
    private static final String SCANNER_LOCATION_PREFIX = "source file:";

    private final Run<?, ?> build;
    private final String rawScanResultJson;
    /** Path the step was configured to scan, to tell several reports in one build apart. */
    private final String scannedPath;
    /**
     * Position of this action among the build's IaC actions, starting at 1. Keeps the url name unique
     * even when two steps scan the same path. Zero for actions persisted before this field existed.
     */
    private final int ordinal;

    private transient IaCScanResult cachedScanResult;

    public IaCAction(Run<?, ?> build, String rawScanResultJson, String scannedPath, int ordinal) {
        this.build = build;
        this.rawScanResultJson = rawScanResultJson;
        this.scannedPath = scannedPath;
        this.ordinal = ordinal;
    }

    /**
     * Builds an action whose url name does not clash with the IaC reports already attached to the
     * build, so every step of a multi-scan build stays reachable from the build page.
     */
    public static IaCAction createFor(Run<?, ?> build, String rawScanResultJson, String scannedPath) {
        return new IaCAction(
                build,
                rawScanResultJson,
                scannedPath,
                build.getActions(IaCAction.class).size() + 1);
    }

    public Run<?, ?> getBuild() {
        return build;
    }

    public String getScannedPath() {
        return scannedPath;
    }

    public IaCScanResult getScanResult() {
        if (cachedScanResult == null && rawScanResultJson != null) {
            JsonIaCScanResultV1 jsonResult = GsonBuilder.build().fromJson(rawScanResultJson, JsonIaCScanResultV1.class);
            if (jsonResult != null) {
                cachedScanResult = jsonResult.toDomain().orElse(null);
            }
        }
        return cachedScanResult;
    }

    public Metadata getMetadata() {
        IaCScanResult result = getScanResult();
        return result == null ? null : result.metadata();
    }

    public Collection<Finding> getFindings() {
        IaCScanResult result = getScanResult();
        return result == null ? List.of() : result.findings();
    }

    public Collection<UnsupportedResource> getUnsupportedResources() {
        IaCScanResult result = getScanResult();
        return result == null ? List.of() : result.unsupportedResources();
    }

    public Collection<ScanError> getErrors() {
        IaCScanResult result = getScanResult();
        return result == null ? List.of() : result.errors();
    }

    public boolean getHasFindings() {
        return !getFindings().isEmpty();
    }

    // Failed controls (findings) per severity. These sum to getTotalFindings().
    public int getHighFindings() {
        return findingsCount(Severity.High);
    }

    public int getMediumFindings() {
        return findingsCount(Severity.Medium);
    }

    public int getLowFindings() {
        return findingsCount(Severity.Low);
    }

    public int getTotalFindings() {
        return getFindings().size();
    }

    /**
     * Per-severity failed-control counts, ordered most severe first and limited to severities that
     * actually occur. Drives the summary tiles and the severity filter, so both adapt to whatever
     * severities the scanner reports (not just high/medium/low).
     */
    public List<SeverityCount> getSeverityBreakdown() {
        IaCScanResult result = getScanResult();
        if (result == null) {
            return List.of();
        }
        List<SeverityCount> breakdown = new java.util.ArrayList<>();
        for (Severity severity : Severity.values()) { // declaration order is most-severe-first
            int count = result.findingsCountBySeverity(severity);
            if (count > 0) {
                breakdown.add(new SeverityCount(severity, count));
            }
        }
        return breakdown;
    }

    /** View model for one severity bucket, exposing the CSS suffix used by the tiles/badges. */
    public static final class SeverityCount {

        private final String name;
        private final int count;

        SeverityCount(Severity severity, int count) {
            this.name = severity.toString();
            this.count = count;
        }

        public String getName() {
            return name;
        }

        public int getCount() {
            return count;
        }

        public String getCssClass() {
            return name.toLowerCase();
        }
    }

    /**
     * Number of control/resource pairs, i.e. one per row of the findings table. A failed control can
     * hit several resources, and the same resource can fail several controls, so this is a count of
     * violations and not of distinct resources.
     */
    public int getResourceViolations() {
        int total = 0;
        for (Finding finding : getFindings()) {
            total += finding.resources().size();
        }
        return total;
    }

    /**
     * Module or folder the resource was declared in, as the scanner reports it: the raw value carries a
     * {@code "source file: "} prefix and degrades to {@code "/"} when the scan root itself is scanned,
     * which reads like a missing value.
     */
    public String locationOf(Resource resource) {
        String raw =
                resource.location() == null || resource.location().isBlank() ? resource.source() : resource.location();
        if (raw == null) {
            return "scan root";
        }
        String path = raw.trim();
        if (path.startsWith(SCANNER_LOCATION_PREFIX)) {
            path = path.substring(SCANNER_LOCATION_PREFIX.length()).trim();
        }
        return path.isEmpty() || path.equals("/") ? "scan root" : path;
    }

    private int findingsCount(Severity severity) {
        IaCScanResult result = getScanResult();
        return result == null ? 0 : result.findingsCountBySeverity(severity);
    }

    @Override
    public String getIconFileName() {
        return jenkins.model.Jenkins.RESOURCE_PATH + "/plugin/sysdig-secure/images/sysdig-shovel.png";
    }

    @Override
    public String getDisplayName() {
        StringBuilder name = new StringBuilder(BASE_DISPLAY_NAME);
        if (scannedPath != null && !scannedPath.isBlank() && !scannedPath.trim().equals(".")) {
            name.append(" (").append(scannedPath.trim()).append(")");
        }
        if (ordinal > 1) {
            name.append(" #").append(ordinal);
        }
        return name.toString();
    }

    @Override
    public String getUrlName() {
        return ordinal > 1 ? BASE_URL_NAME + "-" + ordinal : BASE_URL_NAME;
    }
}
