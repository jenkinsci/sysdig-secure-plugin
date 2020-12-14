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
import com.sysdig.jenkins.plugins.sysdig.log.ConsoleLog;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.scanner.ImageScanningResult;
import com.sysdig.jenkins.plugins.sysdig.scanner.Scanner;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.PluginWrapper;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.ArtifactArchiver;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

/**
 * A helper class to ensure concurrent jobs don't step on each other's toes. Sysdig Secure plugin instantiates a new instance of this class
 * for each individual job i.e. invocation of perform(). Global and project configuration at the time of execution is loaded into
 * worker instance via its constructor. That specific worker instance is responsible for the bulk of the plugin operations for a given
 * job.
 */
public class BuildWorker {

  private static final Logger LOG = Logger.getLogger(BuildWorker.class.getName());
  private static final String JENKINS_DIR_NAME_PREFIX = "SysdigSecureReport.";
  private static final String CVE_LISTING_FILENAME = "sysdig_secure_security.json";
  private static final String GATE_OUTPUT_FILENAME = "sysdig_secure_gates.json";

  // Private members
  Run<?, ?> build;
  FilePath workspace;
  Launcher launcher;
  TaskListener listener;
  BuildConfig config;

  /* Initialized by the constructor */
  protected SysdigLogger logger; // Log handler for logging to build console

  private String jenkinsOutputDirName;
  private JSONObject gateSummary;

  // FIXME can we get rid of this config?
  public BuildWorker(Run<?, ?> build, FilePath workspace, TaskListener listener, BuildConfig config)
    throws AbortException {
    try {
      if (listener == null) {
        LOG.warning("Sysdig Secure Container Image Scanner plugin cannot initialize Jenkins task listener");
        throw new AbortException("Cannot initialize Jenkins task listener. Aborting step");
      }
      if (config == null) {
        LOG.warning("Sysdig Secure Container Image Scanner cannot find the required configuration");
        throw new AbortException("Configuration for the plugin is invalid. Configure the plugin under Manage Jenkins->Configure System->Sysdig Secure Configuration first. Add the Sysdig Secure Container Image Scanner step in your project and retry");
      }

      this.build = build;
      this.workspace = workspace;
      this.listener = listener;
      this.config = config;

      // FIXME receive it as dependency injection
      // Initialize build logger to log output to consoleLog, use local logging methods only after this initializer completes
      logger = new ConsoleLog("SysdigWorker", this.listener.getLogger(), this.config.getDebug());
      logger.logDebug("Initializing build worker");

      // Verify and initialize Jenkins launcher for executing processes
      // TODO is this necessary? Can't we use the launcher reference that was passed in
      this.launcher = workspace.createLauncher(listener);

      printConfig();

      initializeJenkinsWorkspace();

      logger.logDebug("Build worker initialized");
    } catch (Exception e) {
      try {
        if (logger != null) {
          logger.logError("Failed to initialize worker for plugin execution", e);
        }
        cleanJenkinsWorkspaceQuietly();
      } catch (Exception innere) {
        // FIXME Why are we ignoring this exception?
      }
      throw new AbortException("Failed to initialize worker for plugin execution, check logs for corrective action");
    }
  }

  public Util.GATE_ACTION scanAndBuildReports(Scanner scanner) throws AbortException {
    Map<String, String> imagesAndDockerfiles = this.readImagesAndDockerfilesFromPath(workspace, config.getName());

    /* Run analysis */
    ArrayList<ImageScanningResult> scanResults = scanner.scanImages(imagesAndDockerfiles);

    /* Run gates */
    Util.GATE_ACTION finalAction = this.processPolicyEvaluation(scanResults);

    try {
      this.processVulnerabilities(scanResults);

      /* Setup reports */
      this.setupBuildReports(finalAction);

    } catch (AbortException e) {
      logger.logWarn("Recording failure to build reports and moving on with plugin operation", e);
    }

    return finalAction;
  }

  public Util.GATE_ACTION processPolicyEvaluation(List<ImageScanningResult> resultList) throws AbortException {
    FilePath jenkinsOutputDirFP = new FilePath(workspace, jenkinsOutputDirName);
    FilePath jenkinsGatesOutputFP = new FilePath(jenkinsOutputDirFP, GATE_OUTPUT_FILENAME);

    Util.GATE_ACTION finalAction = Util.GATE_ACTION.PASS;
    if (resultList.isEmpty()) {
      logger.logError("Image(s) were not added to sysdig-secure-engine (or a prior attempt to add images may have failed). Re-submit image(s) to sysdig-secure-engine before attempting policy evaluation");
      throw new AbortException("Submit image(s) to sysdig-secure-engine for analysis before attempting policy evaluation");
    }

    try {
      JSONObject fullGateResults = new JSONObject();

      for (ImageScanningResult result : resultList) {
        JSONObject gateResult = result.getGateResult();
        String evalStatus = result.getEvalStatus();
        if (!"pass".equals(evalStatus)) {
          finalAction = Util.GATE_ACTION.FAIL;
        }

        logger.logDebug(String.format("sysdig-secure-engine get policy evaluation status: %s", evalStatus));
        logger.logDebug(String.format("sysdig-secure-engine get policy evaluation result: %s", gateResult.toString()));

        for (Object key : gateResult.keySet()) {
          try {
            fullGateResults.put((String) key, gateResult.getJSONObject((String) key));
          } catch (Exception e) {
            logger.logDebug("Ignoring error parsing policy evaluation result key: " + key);
          }
        }
      }

      logger.logDebug(String.format("Writing policy evaluation result to %s", jenkinsGatesOutputFP.getRemote()));
      jenkinsGatesOutputFP.write(fullGateResults.toString(), String.valueOf(StandardCharsets.UTF_8));

      gateSummary = generateGatesSummary(fullGateResults);
      logger.logInfo("Sysdig Secure Container Image Scanner Plugin step result - " + finalAction);
      return finalAction;

    } catch (InterruptedException | IOException e) {
      logger.logError("Failed to execute sysdig-secure-engine policy evaluation due to an unexpected error", e);
      throw new AbortException("Failed to execute sysdig-secure-engine policy evaluation due to an unexpected error. Please refer to above logs for more information");
    }
  }

  private JSONObject generateGatesSummary(JSONObject gatesJson) {
    logger.logDebug("Summarizing policy evaluation results");
    JSONObject gateSummary = new JSONObject();

    if (gatesJson == null) { // could not load gates output to json object
      logger.logWarn("Invalid input to generate gates summary");
      return gateSummary;
    }

    JSONArray summaryRows = new JSONArray();
    // Populate once and reuse
    int numColumns = 0, repoTagIndex = -1, gateNameIndex = -1, gateActionIndex = -1, whitelistedIndex = -1;

    for (Object imageKey : gatesJson.keySet()) {
      JSONObject content = gatesJson.getJSONObject((String) imageKey);
      if (null == content) { // no content found for a given image id, log and move on
        logger.logWarn(String.format("No mapped object found in gate output, skipping summary computation for %s", imageKey));
        continue;
      }

      JSONObject result = content.getJSONObject("result");
      if (null == result) { // result object not found, log and move on
        logger.logWarn(String.format("'result' element not found in gate output, skipping summary computation for %s", imageKey));
        continue;
      }

      // populate data from header element once, most likely for the first image
      if (numColumns <= 0 || repoTagIndex < 0 || gateNameIndex < 0 || gateActionIndex < 0 || whitelistedIndex < 0) {
        JSONArray header = result.getJSONArray("header");
        if (null == header) {
          logger.logWarn(String.format("'header' element not found in gate output, skipping summary computation for %s", imageKey));
          continue;
        }

        numColumns = header.size();
        for (int i = 0; i < header.size(); i++) {
          switch (header.getString(i)) {
            case "Repo_Tag":
              repoTagIndex = i;
              break;
            case "Gate":
              gateNameIndex = i;
              break;
            case "Gate_Action":
              gateActionIndex = i;
              break;
            case "Whitelisted":
              whitelistedIndex = i;
              break;
            default:
              break;
          }
        }
      }

      if (numColumns <= 0 || repoTagIndex < 0 || gateNameIndex < 0 || gateActionIndex < 0) {
        logger.logWarn(String.format("Either 'header' element has no columns or column indices (for Repo_Tag, Gate, Gate_Action) not initialized, skipping summary computation for %s", imageKey));
        continue;
      }

      JSONArray rows = result.getJSONArray("rows");
      if (null != rows) {
        int stop = 0, warn = 0, go = 0, stop_wl = 0, warn_wl = 0, go_wl = 0;
        String repoTag = null;

        for (int i = 0; i < rows.size(); i++) {
          JSONArray row = rows.getJSONArray(i);
          if (row.size() == numColumns) {
            if (Strings.isNullOrEmpty(repoTag)) {
              repoTag = row.getString(repoTagIndex);
            }
            if (!row.getString(gateNameIndex).equalsIgnoreCase("FINAL")) {
              switch (row.getString(gateActionIndex).toLowerCase()) {
                case "stop":
                  stop++;
                  stop_wl += (whitelistedIndex != -1 && !(row.getString(whitelistedIndex).equalsIgnoreCase("none") || row
                    .getString(whitelistedIndex).equalsIgnoreCase("false"))) ? 1 : 0;
                  break;
                case "warn":
                  warn++;
                  warn_wl += (whitelistedIndex != -1 && !(row.getString(whitelistedIndex).equalsIgnoreCase("none") || row
                    .getString(whitelistedIndex).equalsIgnoreCase("false"))) ? 1 : 0;
                  break;
                case "go":
                  go++;
                  go_wl += (whitelistedIndex != -1 && !(row.getString(whitelistedIndex).equalsIgnoreCase("none") || row
                    .getString(whitelistedIndex).equalsIgnoreCase("false"))) ? 1 : 0;
                  break;
                default:
                  break;
              }
            }
          } else {
            logger.logWarn(String.format("Expected %d elements but got %d, skipping row %s in summary computation for %s", numColumns, row.size(), row, imageKey));
          }
        }

        if (!Strings.isNullOrEmpty(repoTag)) {
          logger.logInfo(String.format("Policy evaluation summary for %s - stop: %d (+%d whitelisted), warn: %d (+%d whitelisted), go: %d (+%d whitelisted), final: %s", repoTag, stop - stop_wl, stop_wl, warn - warn_wl, warn_wl, go - go_wl, go_wl, result.getString("final_action")));

          JSONObject summaryRow = new JSONObject();
          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Repo_Tag.toString(), repoTag);
          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Stop_Actions.toString(), (stop - stop_wl));
          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Warn_Actions.toString(), (warn - warn_wl));
          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Go_Actions.toString(), (go - go_wl));
          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Final_Action.toString(), result.getString("final_action"));
          summaryRows.add(summaryRow);
        } else {
          logger.logInfo(String.format("Policy evaluation summary for %s - stop: %d (+%d whitelisted), warn: %d (+%d whitelisted), go: %d (+%d whitelisted), final: %s", imageKey, stop - stop_wl, stop_wl, warn - warn_wl, warn_wl, go - go_wl, go_wl, result.getString("final_action")));
          JSONObject summaryRow = new JSONObject();
          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Repo_Tag.toString(), imageKey.toString());
          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Stop_Actions.toString(), (stop - stop_wl));
          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Warn_Actions.toString(), (warn - warn_wl));
          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Go_Actions.toString(), (go - go_wl));
          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Final_Action.toString(), result.getString("final_action"));
          summaryRows.add(summaryRow);

          //console.logWarn("Repo_Tag element not found in gate output, skipping summary computation for " + imageKey);
          logger.logWarn(String.format("Repo_Tag element not found in gate output, using imageId: %s", imageKey));
        }
      } else { // rows object not found
        logger.logWarn(String.format("'rows' element not found in gate output, skipping summary computation for %s", imageKey));
      }

    }


    gateSummary.put("header", generateDataTablesColumnsForGateSummary());
    gateSummary.put("rows", summaryRows);

    return gateSummary;
  }

  public JSONArray getVulnerabilitiesArray(String tag, JSONObject vulnsReport) {
    JSONArray dataJson = new JSONArray();
    JSONArray vulList = vulnsReport.getJSONArray("vulnerabilities");
    for (int i = 0; i < vulList.size(); i++) {
      JSONObject vulnJson = vulList.getJSONObject(i);
      JSONArray vulnArray = new JSONArray();
      vulnArray.addAll(Arrays.asList(
        tag,
        vulnJson.getString("vuln"),
        vulnJson.getString("severity"),
        vulnJson.getString("package"),
        vulnJson.getString("fix"),
        String.format("<a href='%s'>%s</a>", vulnJson.getString("url"), vulnJson.getString("url"))));
      dataJson.add(vulnArray);
    }

    return dataJson;
  }

  public void processVulnerabilities(List<ImageScanningResult> submissionList) throws AbortException {
    JSONArray dataJson = new JSONArray();
    for (ImageScanningResult entry : submissionList) {
      String tag = entry.getTag();
      dataJson.addAll(getVulnerabilitiesArray(tag, entry.getVulnerabilityReport()));
    }

    JSONObject securityJson = new JSONObject();
    JSONArray columnsJson = new JSONArray();

    for (String column : Arrays.asList("Tag", "CVE ID", "Severity", "Vulnerability Package", "Fix Available", "URL")) {
      JSONObject columnJson = new JSONObject();
      columnJson.put("title", column);
      columnsJson.add(columnJson);
    }

    securityJson.put("columns", columnsJson);
    securityJson.put("data", dataJson);

    FilePath jenkinsQueryOutputFP = new FilePath(new FilePath(workspace, jenkinsOutputDirName), CVE_LISTING_FILENAME);

    try {
      logger.logDebug(String.format("Writing vulnerability listing result to %s", jenkinsQueryOutputFP.getRemote()));
      jenkinsQueryOutputFP.write(securityJson.toString(), String.valueOf(StandardCharsets.UTF_8));
    } catch (IOException | InterruptedException e) {
      logger.logWarn(String.format("Failed to write vulnerability listing to %s", jenkinsQueryOutputFP.getRemote()), e);
      throw new AbortException(String.format("Failed to write vulnerability listing to %s", jenkinsQueryOutputFP.getRemote()));
    }

  }

  public void setupBuildReports(Util.GATE_ACTION finalAction) throws AbortException {
    try {
      // store sysdig secure output json files using jenkins archiver (for remote storage as well)
      logger.logDebug("Archiving results");
      ArtifactArchiver artifactArchiver = new ArtifactArchiver(jenkinsOutputDirName + "/");
      artifactArchiver.perform(build, workspace, launcher, listener);

      // add the link in jenkins UI for sysdig secure results
      logger.logDebug("Setting up build results");
      String finalActionStr = (finalAction != null) ? finalAction.toString() : "";
      build.addAction(new SysdigAction(build, finalActionStr, jenkinsOutputDirName, GATE_OUTPUT_FILENAME, gateSummary.toString(),
        CVE_LISTING_FILENAME));
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

  /**
   * Print versions info and configuration
   */
  private void printConfig() {
    logger.logInfo("Jenkins version: " + Jenkins.VERSION);
    List<PluginWrapper> plugins;
    if (Jenkins.get().getPluginManager() != null && (plugins = Jenkins.get().getPluginManager().getPlugins()) != null) {
      for (PluginWrapper plugin : plugins) {
        if (plugin.getShortName().equals("sysdig-secure")) { // artifact ID of the plugin, TODO is there a better way to get this
          logger.logInfo(String.format("%s version: %s", plugin.getDisplayName(), plugin.getVersion()));
          break;
        }
      }
    }
    config.print(logger);
  }

  private void initializeJenkinsWorkspace() throws AbortException {
    try {
      logger.logDebug("Initializing Jenkins workspace");

      // Initialized by Jenkins workspace prep
      String buildId;
      if (Strings.isNullOrEmpty(buildId = build.getParent().getDisplayName() + "_" + build.getNumber())) {
        logger.logWarn("Unable to generate a unique identifier for this build due to invalid configuration");
        throw new AbortException("Unable to generate a unique identifier for this build due to invalid configuration");
      }

      jenkinsOutputDirName = JENKINS_DIR_NAME_PREFIX + buildId;
      FilePath jenkinsReportDir = new FilePath(workspace, jenkinsOutputDirName);

      // Create output directories
      if (!jenkinsReportDir.exists()) {
        logger.logDebug(String.format("Creating workspace directory %s", jenkinsOutputDirName));
        jenkinsReportDir.mkdirs();
      }
    } catch (AbortException e) { // probably caught one of the thrown exceptions, let it pass through
      throw e;
    } catch (Exception e) { // caught unknown exception, log it and wrap it
      logger.logWarn("Failed to initialize Jenkins workspace", e);
      throw new AbortException("Failed to initialize Jenkins workspace due to to an unexpected error");
    }
  }

  public Map<String, String> readImagesAndDockerfilesFromPath(FilePath workspace, String manifestFile) throws AbortException {

    Map<String, String> imageDockerfileMap = new HashMap<>();
    logger.logDebug("Initializing Sysdig Secure workspace");

    // get the input and store it in tag/dockerfile map
    FilePath filePath = new FilePath(workspace, manifestFile);
    try {
      String[] fileLines = filePath.readToString().split("\\r?\\n");
      for (String line : fileLines) {
        String[] lineSplit = line.split(" ", 1);
        String tag = lineSplit[0];
        String dockerFileContents = (lineSplit.length > 1) ? new String(Base64.encodeBase64(new FilePath(workspace, lineSplit[1]).readToString().getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8) : "";
        imageDockerfileMap.put(tag, dockerFileContents);
      }

    } catch (Exception e) { // caught unknown exception, console.log it and wrap it
      logger.logError("Failed to initialize Sysdig Secure workspace due to an unexpected error", e);
      throw new AbortException("Failed to initialize Sysdig Secure workspace due to an unexpected error. Please refer to above logs for more information");
    }

    return imageDockerfileMap;
  }

  private static JSONArray generateDataTablesColumnsForGateSummary() {
    JSONArray headers = new JSONArray();
    for (Util.GATE_SUMMARY_COLUMN column : Util.GATE_SUMMARY_COLUMN.values()) {
      JSONObject header = new JSONObject();
      header.put("data", column.toString());
      header.put("title", column.toString().replaceAll("_", " "));
      headers.add(header);
    }
    return headers;
  }

  private void cleanJenkinsWorkspaceQuietly() throws IOException, InterruptedException {
    FilePath jenkinsOutputDirFP = new FilePath(workspace, jenkinsOutputDirName);
    jenkinsOutputDirFP.deleteRecursive();
  }

}
