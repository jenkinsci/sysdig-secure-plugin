/*
Copyright (C) 2016-2020 Sysdig

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
package com.sysdig.jenkins.plugins.sysdig;

import com.google.common.base.Strings;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.scanner.ImageScanningResult;
import com.sysdig.jenkins.plugins.sysdig.scanner.NewEngineScanner;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.ArtifactArchiver;
import net.sf.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;

/**
 * A helper class to ensure concurrent jobs don't step on each other's toes. Sysdig Secure plugin instantiates a new instance of this class
 * for each individual job i.e. invocation of perform(). Global and project configuration at the time of execution is loaded into
 * worker instance via its constructor. That specific worker instance is responsible for the bulk of the plugin operations for a given
 * job.
 */
public class BuildWorker {

  private static final Logger LOG = Logger.getLogger(BuildWorker.class.getName());
  private static final String JENKINS_DIR_NAME_PREFIX = "SysdigSecureReport_";
  private static final String CVE_LISTING_FILENAME = "sysdig_secure_security.json";
  private static final String GATE_OUTPUT_FILENAME = "sysdig_secure_gates.json";
  private static final String RAW_VULN_REPORT_FILENAME = "sysdig_secure_raw_vulns_report-%s.json";
  private final ReportConverter reportConverter;
  private final NewEngineScanner scanner;
  /* Initialized by the constructor */
  protected SysdigLogger logger; // Log handler for logging to build console
  // Private members
  Run<?, ?> run;
  FilePath workspace;
  Launcher launcher;
  TaskListener listener;
  private String jenkinsOutputDirName;

  public BuildWorker(Run<?, ?> run, FilePath workspace, TaskListener listener, SysdigLogger logger, NewEngineScanner scanner, ReportConverter reportConverter) throws IOException, InterruptedException {
    try {
      if (listener == null) {
        LOG.warning("Sysdig Secure Container Image Scanner plugin cannot initialize Jenkins task listener");
        throw new AbortException("Cannot initialize Jenkins task listener. Aborting step");
      }

      this.run = run;
      this.workspace = workspace;
      this.listener = listener;
      this.logger = logger;

      logger.logDebug("Initializing build worker");

      // Verify and initialize Jenkins launcher for executing processes
      this.launcher = workspace.createLauncher(listener);

      initializeJenkinsWorkspace();

      logger.logDebug("Build worker initialized");

      this.scanner = scanner;
      this.reportConverter = reportConverter;

    } catch (Exception e) {
      try {
        if (logger != null) {
          logger.logError("Failed to initialize worker for plugin execution", e);
        }
        cleanJenkinsWorkspaceQuietly();
      } catch (Exception inner) {
      }
      throw e;
      //throw new AbortException("Failed to initialize worker for plugin execution, check logs for corrective action");
    }
  }

  public Util.GATE_ACTION scanAndBuildReports(String imageName) throws AbortException, InterruptedException {
    ImageScanningResult scanResult = scanner.scanImage(imageName);

    // FIXME(fede): do not use a list in those methods, just use the result
    Util.GATE_ACTION finalAction = reportConverter.getFinalAction(scanResult);
    logger.logInfo("Sysdig Secure Container Image Scanner Plugin step result - " + finalAction);

    try {
      FilePath outputDir = new FilePath(workspace, jenkinsOutputDirName);

      FilePath jenkinsGatesOutputFP = new FilePath(outputDir, GATE_OUTPUT_FILENAME);
      JSONObject gateSummary = reportConverter.processPolicyEvaluation(List.of(scanResult), jenkinsGatesOutputFP);

      FilePath jenkinsQueryOutputFP = new FilePath(outputDir, CVE_LISTING_FILENAME);
      reportConverter.processVulnerabilities(List.of(scanResult), jenkinsQueryOutputFP);

      for (ImageScanningResult result : List.of(scanResult)) {
        FilePath rawVulnerabilityReportFP = new FilePath(outputDir, String.format(RAW_VULN_REPORT_FILENAME, result.getImageDigest()));
        logger.logDebug(String.format("Writing raw vulnerability report to %s", rawVulnerabilityReportFP.getRemote()));
        rawVulnerabilityReportFP.write(result.getVulnerabilityReport().toString(), String.valueOf(StandardCharsets.UTF_8));
      }

      /* Setup reports */
      this.setupBuildReports(finalAction, gateSummary);

    } catch (Exception e) {
      logger.logError("Recording failure to build reports and moving on with plugin operation", e);
    }

    return finalAction;
  }

  private void setupBuildReports(Util.GATE_ACTION finalAction, JSONObject gateSummary) throws AbortException {
    try {
      // store sysdig secure output json files using jenkins archiver (for remote storage as well)
      logger.logDebug("Archiving results");
      ArtifactArchiver artifactArchiver = new ArtifactArchiver(jenkinsOutputDirName + "/");
      artifactArchiver.perform(run, workspace, launcher, listener);

      // add the link in jenkins UI for sysdig secure results
      logger.logDebug("Setting up build results");
      String finalActionStr = (finalAction != null) ? finalAction.toString() : "";
      run.addAction(new SysdigAction(run, finalActionStr, jenkinsOutputDirName, GATE_OUTPUT_FILENAME, gateSummary.toString(), CVE_LISTING_FILENAME));
    } catch (Exception e) { // caught unknown exception, log it and wrap it
      logger.logError("Failed to setup build results due to an unexpected error", e);
      throw new AbortException(
        "Failed to setup build results due to an unexpected error. Please refer to above logs for more information");
    }
  }

  public void cleanup() {
    try {
      logger.logDebug("Cleaning up build artifacts");

      if (!Strings.isNullOrEmpty(jenkinsOutputDirName)) {
        try {
          logger.logDebug("Deleting Jenkins workspace " + jenkinsOutputDirName);
          cleanJenkinsWorkspaceQuietly();
        } catch (IOException | InterruptedException e) {
          logger.logDebug("Unable to delete Jenkins workspace " + jenkinsOutputDirName, e);
        }
      }

    } catch (RuntimeException e) { // caught unknown exception, log it
      logger.logDebug("Failed to clean up build artifacts due to an unexpected error", e);
    }
  }

  private void initializeJenkinsWorkspace() throws IOException, InterruptedException {
    try {
      logger.logDebug("Initializing Jenkins workspace");

      jenkinsOutputDirName = JENKINS_DIR_NAME_PREFIX + run.getNumber();
      FilePath jenkinsReportDir = new FilePath(workspace, jenkinsOutputDirName);

      // Create output directories
      if (!jenkinsReportDir.exists()) {
        logger.logDebug(String.format("Creating workspace directory %s", jenkinsOutputDirName));
        jenkinsReportDir.mkdirs();
      }
    } catch (IOException |
             InterruptedException e) { // probably caught one of the thrown exceptions, let it pass through
      logger.logWarn("Failed to initialize Jenkins workspace", e);
      throw e;
    }
  }

  private void cleanJenkinsWorkspaceQuietly() throws IOException, InterruptedException {
    FilePath jenkinsOutputDirFP = new FilePath(workspace, jenkinsOutputDirName);
    jenkinsOutputDirFP.deleteRecursive();
  }

}
