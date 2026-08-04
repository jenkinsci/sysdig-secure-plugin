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
    /** Stands in for the scanned path itself, which the scanner reports as {@code "/"}. */
    private static final String SCAN_ROOT_LABEL = "scan root";
    /** Guards the ordinal-then-attach sequence in {@link #attachTo}. */
    private static final Object ATTACH_LOCK = new Object();

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

    IaCAction(Run<?, ?> build, String rawScanResultJson, String scannedPath, int ordinal) {
        this.build = build;
        this.rawScanResultJson = rawScanResultJson;
        this.scannedPath = scannedPath;
        this.ordinal = ordinal;
    }

    /**
     * Attaches a report whose url name does not clash with the IaC reports already on the build, so
     * every step of a multi-scan build stays reachable from the build page.
     *
     * <p>Adding to the build's action list is thread safe on its own, but reading it to pick the
     * ordinal and then adding is not: parallel branches of a pipeline run their steps on different
     * threads and would otherwise both settle on the same url name, so the whole sequence takes a
     * lock. The ordinal counts the reports already attached <em>and</em> stays above the highest one
     * of them, because a report persisted before this field existed deserializes with ordinal zero
     * and would otherwise be handed the same url name as the next one.
     */
    public static IaCAction attachTo(Run<?, ?> build, String rawScanResultJson, String scannedPath) {
        synchronized (ATTACH_LOCK) {
            // The raw action list, not getActions(Class): that one also runs every TransientActionFactory
            // in the instance, which is third-party code we should not call while holding a lock.
            int ordinal = 1;
            for (Action attached : build.getActions()) {
                if (attached instanceof IaCAction report) {
                    ordinal = Math.max(ordinal + 1, report.ordinal + 1);
                }
            }
            IaCAction action = new IaCAction(build, rawScanResultJson, scannedPath, ordinal);
            build.addAction(action);
            return action;
        }
    }

    public Run<?, ?> getBuild() {
        return build;
    }

    /** Path the step was configured to scan, empty when it scanned whatever the default is. */
    public String getScannedPath() {
        return scannedPath == null ? "" : scannedPath.trim();
    }

    /** Whether there is a scan root worth stating on the report page. */
    public boolean getHasScannedPath() {
        String path = getScannedPath();
        return !path.isEmpty() && !path.equals(".") && !path.equals("/");
    }

    /**
     * Last segment of the scanned path. The configured path is often absolute and agent-specific
     * ({@code /home/jenkins/agent/workspace/demo}), which is unreadable in a sidebar or a breadcrumb.
     */
    public String getScannedPathName() {
        String path = withoutTrailingSlash(getScannedPath());
        int lastSeparator = path.lastIndexOf('/');
        return lastSeparator < 0 ? path : path.substring(lastSeparator + 1);
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
     * Module, folder or file the resource was declared in, shown relative to the scanned path. The
     * scanner reports it as {@code "source file: <path>"} with a leading slash, which reads like a
     * filesystem root; the report page states the scan root instead, once.
     */
    public String locationOf(Resource resource) {
        String raw =
                resource.location() == null || resource.location().isBlank() ? resource.source() : resource.location();
        String path = pathOf(raw);
        return path.isEmpty() ? SCAN_ROOT_LABEL : path;
    }

    /**
     * Any path the scanner reports — the source of an unsupported resource or of a parse error as much
     * as a finding's module — shown relative to the scanned path, so every table reads the same way.
     */
    public String pathOf(String scannerPath) {
        if (scannerPath == null) {
            return "";
        }
        String path = scannerPath.trim();
        if (path.startsWith(SCANNER_LOCATION_PREFIX)) {
            path = path.substring(SCANNER_LOCATION_PREFIX.length()).trim();
        }
        if (path.equals("/")) {
            return SCAN_ROOT_LABEL;
        }
        return path.startsWith("/") ? path.substring(1) : path;
    }

    /**
     * The same path prefixed with the scan root, for a cell's tooltip: the tables stay narrow and the
     * whole path is one hover away. Falls back to the relative path alone when the step configured none,
     * since then the root is whatever the scanner defaulted to.
     */
    public String fullPathOf(String scannerPath) {
        return withScanRoot(pathOf(scannerPath));
    }

    public String fullLocationOf(Resource resource) {
        return withScanRoot(locationOf(resource));
    }

    private String withScanRoot(String path) {
        if (!getHasScannedPath()) {
            return path;
        }
        String root = withoutTrailingSlash(getScannedPath());
        return path.isEmpty() || path.equals(SCAN_ROOT_LABEL) ? root : root + "/" + path;
    }

    private static String withoutTrailingSlash(String path) {
        return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
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
        if (getHasScannedPath()) {
            name.append(" (").append(getScannedPathName()).append(")");
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
