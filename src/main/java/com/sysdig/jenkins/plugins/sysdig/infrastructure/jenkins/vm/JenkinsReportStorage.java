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
package com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.vm;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.sysdig.jenkins.plugins.sysdig.application.vm.ReportStorage;
import com.sysdig.jenkins.plugins.sysdig.application.vm.report.PolicyEvaluationReport;
import com.sysdig.jenkins.plugins.sysdig.application.vm.report.VulnerabilityReportProcessor;
import com.sysdig.jenkins.plugins.sysdig.domain.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.ScanResult;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.diff.ScanResultDiff;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.RunContext;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.vm.ui.SysdigAction;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.json.GsonBuilder;
import hudson.AbortException;
import hudson.FilePath;
import hudson.tasks.ArtifactArchiver;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class JenkinsReportStorage implements ReportStorage, AutoCloseable {
    private static final String JENKINS_DIR_NAME_PREFIX = "SysdigSecureReport_";
    private static final String CVE_LISTING_FILENAME_FORMAT = "sysdig_secure_security-%s.json";
    private static final String POLICY_REPORT_FILENAME_FORMAT = "sysdig_secure_gates-%s.json";
    private static final String RAW_VULN_REPORT_FILENAME_FORMAT = "sysdig_secure_raw_vulns_report-%s.json";

    private final RunContext runContext;
    private final SysdigLogger logger;
    private final String jenkinsOutputDirName;

    public JenkinsReportStorage(RunContext runContext) {
        this.runContext = runContext;
        this.logger = runContext.getLogger();
        this.jenkinsOutputDirName = JENKINS_DIR_NAME_PREFIX + runContext.getJobNumber();
        this.initializeJenkinsWorkspace();
    }

    private void initializeJenkinsWorkspace() {
        try {
            logger.logDebug("Initializing Jenkins workspace");
            FilePath jenkinsReportDir = runContext.getPathFromWorkspace(jenkinsOutputDirName);
            if (!jenkinsReportDir.exists()) {
                logger.logDebug(String.format("Creating workspace directory %s", jenkinsOutputDirName));
                jenkinsReportDir.mkdirs();
            }
        } catch (IOException | InterruptedException e) {
            logger.logWarn("Failed to initialize Jenkins workspace", e);
        }
    }

    @Override
    public void savePolicyReport(ScanResult scanResult, PolicyEvaluationReport report)
            throws IOException, InterruptedException {
        FilePath outPath = runContext.getPathFromWorkspace(
                jenkinsOutputDirName,
                String.format(
                        POLICY_REPORT_FILENAME_FORMAT, scanResult.metadata().imageID()));
        logger.logDebug(String.format("Writing policy evaluation result to %s", outPath.getRemote()));
        outPath.write(GsonBuilder.build().toJson(report), String.valueOf(StandardCharsets.UTF_8));
    }

    @Override
    public void saveVulnerabilityReport(ScanResult scanResult) throws IOException, InterruptedException {
        FilePath outPath = runContext.getPathFromWorkspace(
                jenkinsOutputDirName,
                String.format(CVE_LISTING_FILENAME_FORMAT, scanResult.metadata().imageID()));
        JsonObject securityJson = VulnerabilityReportProcessor.generateVulnerabilityReport(scanResult);
        logger.logDebug(String.format("Writing vulnerability report to %s", outPath.getRemote()));
        outPath.write(securityJson.toString(), String.valueOf(StandardCharsets.UTF_8));
    }

    @Override
    public void saveRawVulnerabilityReport(ScanResult scanResult) throws IOException, InterruptedException {
        String outFilename = String.format(
                RAW_VULN_REPORT_FILENAME_FORMAT, scanResult.metadata().imageID());
        FilePath outPath = runContext.getPathFromWorkspace(jenkinsOutputDirName, outFilename);
        logger.logDebug(String.format("Writing raw vulnerability report to %s", outPath.getRemote()));
        //    outPath.write(GsonBuilder.build().toJson(scanResult.packages()), String.valueOf(StandardCharsets.UTF_8));
    }

    @Override
    public void archiveResults(ScanResult scanResult) throws IOException {
        try {
            logger.logDebug("Archiving results");
            runContext.perform(new ArtifactArchiver(jenkinsOutputDirName + "/"));

            logger.logDebug("Setting up build results in the UI");
            String policyReportFilename = String.format(
                    POLICY_REPORT_FILENAME_FORMAT, scanResult.metadata().imageID());
            String cveListingFileName = String.format(
                    CVE_LISTING_FILENAME_FORMAT, scanResult.metadata().imageID());
            runContext
                    .getRun()
                    .addAction(new SysdigAction(
                            runContext.getRun(),
                            scanResult,
                            jenkinsOutputDirName,
                            policyReportFilename,
                            cveListingFileName));
        } catch (Exception e) {
            logger.logError("Failed to setup build results due to an unexpected error", e);
            throw new AbortException(
                    "Failed to setup build results due to an unexpected error. Please refer to above logs for more information");
        }
    }

    @Override
    public void saveImageDiff(ScanResultDiff diff) throws IOException, InterruptedException {
        logger.logDebug("Saving image diff");
        // TODO: implement
    }

    @Override
    public void close() {
        logger.logDebug("Cleaning up build artifacts");
        if (!Strings.isNullOrEmpty(jenkinsOutputDirName)) {
            logger.logDebug("Deleting Jenkins workspace " + jenkinsOutputDirName);
            cleanJenkinsWorkspaceQuietly();
        }
    }

    private void cleanJenkinsWorkspaceQuietly() {
        FilePath jenkinsOutputDirFP = runContext.getPathFromWorkspace(jenkinsOutputDirName);
        try {
            jenkinsOutputDirFP.deleteRecursive();
        } catch (IOException | InterruptedException e) {
            logger.logDebug("Unable to delete Jenkins workspace " + jenkinsOutputDirName, e);
        }
    }
}
