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
package com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.vm.ui;

import com.sysdig.jenkins.plugins.sysdig.application.vm.report.PolicyEvaluationSummary;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.ScanResult;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.json.GsonBuilder;
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
    private final PolicyEvaluationSummary gateSummary;
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
            PolicyEvaluationSummary policyEvaluationSummary,
            String cveListingFileName) {
        this.build = run;
        this.gateStatus = scanResult.evaluationResult().toString();
        this.gateSummary = policyEvaluationSummary;
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

    public String getGateSummary() {
        return GsonBuilder.build().toJson(this.gateSummary);
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
