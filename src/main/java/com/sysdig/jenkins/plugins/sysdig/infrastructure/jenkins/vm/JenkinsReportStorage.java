package com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.vm;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.RunContext;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.vm.ui.SysdigAction;
import com.sysdig.jenkins.plugins.sysdig.application.vm.report.PolicyEvaluationReport;
import com.sysdig.jenkins.plugins.sysdig.application.vm.report.PolicyEvaluationSummary;
import com.sysdig.jenkins.plugins.sysdig.application.vm.report.VulnerabilityReportProcessor;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.ImageScanningResult;
import com.sysdig.jenkins.plugins.sysdig.application.vm.ReportStorage;
import com.sysdig.jenkins.plugins.sysdig.domain.SysdigLogger;
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
    } catch (IOException |
             InterruptedException e) {
      logger.logWarn("Failed to initialize Jenkins workspace", e);
    }
  }

  @Override
  public void savePolicyReport(ImageScanningResult scanResult, PolicyEvaluationReport report) throws IOException, InterruptedException {
    FilePath outPath = runContext.getPathFromWorkspace(jenkinsOutputDirName, String.format(POLICY_REPORT_FILENAME_FORMAT, scanResult.getImageDigest()));
    logger.logDebug(String.format("Writing policy evaluation result to %s", outPath.getRemote()));
    outPath.write(GsonBuilder.build().toJson(report), String.valueOf(StandardCharsets.UTF_8));
  }

  @Override
  public void saveVulnerabilityReport(ImageScanningResult scanResult) throws IOException, InterruptedException {
    FilePath outPath = runContext.getPathFromWorkspace(jenkinsOutputDirName, String.format(CVE_LISTING_FILENAME_FORMAT, scanResult.getImageDigest()));
    JsonObject securityJson = VulnerabilityReportProcessor.generateVulnerabilityReport(scanResult);
    logger.logDebug(String.format("Writing vulnerability report to %s", outPath.getRemote()));
    outPath.write(securityJson.toString(), String.valueOf(StandardCharsets.UTF_8));

  }

  @Override
  public void saveRawVulnerabilityReport(ImageScanningResult scanResult) throws IOException, InterruptedException {
    String outFilename = String.format(RAW_VULN_REPORT_FILENAME_FORMAT, scanResult.getImageDigest());
    FilePath outPath = runContext.getPathFromWorkspace(jenkinsOutputDirName, outFilename);
    logger.logDebug(String.format("Writing raw vulnerability report to %s", outPath.getRemote()));
    outPath.write(GsonBuilder.build().toJson(scanResult.getVulnerabilityReport()), String.valueOf(StandardCharsets.UTF_8));
  }

  @Override
  public void archiveResults(ImageScanningResult scanResult, PolicyEvaluationSummary policyEvaluationSummary) throws IOException {
    try {
      logger.logDebug("Archiving results");
      runContext.perform(new ArtifactArchiver(jenkinsOutputDirName + "/"));

      logger.logDebug("Setting up build results in the UI");
      String policyReportFilename = String.format(POLICY_REPORT_FILENAME_FORMAT, scanResult.getImageDigest());
      String cveListingFileName = String.format(CVE_LISTING_FILENAME_FORMAT, scanResult.getImageDigest());
      runContext.getRun().addAction(new SysdigAction(runContext.getRun(), scanResult, jenkinsOutputDirName, policyReportFilename, policyEvaluationSummary, cveListingFileName));
    } catch (Exception e) {
      logger.logError("Failed to setup build results due to an unexpected error", e);
      throw new AbortException("Failed to setup build results due to an unexpected error. Please refer to above logs for more information");
    }
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
