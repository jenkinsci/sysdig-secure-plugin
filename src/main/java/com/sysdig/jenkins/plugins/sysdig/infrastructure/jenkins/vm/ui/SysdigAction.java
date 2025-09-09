package com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.vm.ui;

import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.ScanResult;
import hudson.model.Action;
import hudson.model.Run;
import java.util.Map;
import jenkins.model.Jenkins;

/**
 * Sysdig Secure plugin results for a given build are stored and subsequently retrieved from an instance of this class. Rendering/display of
 * the results is defined in the appropriate index and summary jelly files. This Jenkins Action is associated with a build (and not the
 * project which is one level up)
 */
public class SysdigAction implements Action {

    private final Run<?, ?> build;
    private final String gateStatus;
    private final String gateOutputUrl;
    private final String cveListingUrl;

    // For backwards compatibility
    @Deprecated
    private String gateReportUrl;

    @Deprecated
    private Map<String, String> queries;

    private final String imageTag;
    private final String imageDigest;

    public SysdigAction(
            Run<?, ?> run,
            ScanResult scanResult,
            String jenkinsOutputDirName,
            String policyReportFilename,
            String cveListingFileName) {
        this.build = run;
        this.gateStatus = scanResult.evaluationResult().toString();
        this.imageTag = scanResult.metadata().pullString();
        this.imageDigest = scanResult.metadata().imageID().replace(':', '-');
        this.gateOutputUrl = String.format("../artifact/%s/%s", jenkinsOutputDirName, policyReportFilename);
        this.cveListingUrl = String.format("../artifact/%s/%s", jenkinsOutputDirName, cveListingFileName);
    }

    @Override
    public String getIconFileName() {
        return Jenkins.RESOURCE_PATH + "/plugin/sysdig-secure/images/sysdig-shovel.png";
    }

    @Override
    public String getDisplayName() {
        return String.format("Sysdig Secure Report (%s) (%s)", imageTag, gateStatus);
    }

    @Override
    public String getUrlName() {
        return String.format("sysdig-secure-results-%s", imageDigest);
    }

    public Run<?, ?> getBuild() {
        return this.build;
    }

    public String getGateStatus() {
        return gateStatus;
    }

    public String getGateOutputUrl() {
        return this.gateOutputUrl;
    }

    public String getCveListingUrl() {
        return cveListingUrl;
    }

    public String getGateReportUrl() {
        return this.gateReportUrl;
    }

    public Map<String, String> getQueries() {
        return this.queries;
    }
}
