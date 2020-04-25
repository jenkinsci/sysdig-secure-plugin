package com.sysdig.jenkins.plugins.sysdig;

import com.google.common.base.Strings;
import com.sysdig.jenkins.plugins.sysdig.Util.GATE_ACTION;
import com.sysdig.jenkins.plugins.sysdig.Util.GATE_SUMMARY_COLUMN;
import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningSubmission;
import com.sysdig.jenkins.plugins.sysdig.log.ConsoleLog;
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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

/**
 * A helper class to ensure concurrent jobs don't step on each other's toes. Sysdig Secure plugin instantiates a new instance of this class
 * for each individual job i.e. invocation of perform(). Global and project configuration at the time of execution is loaded into
 * worker instance via its constructor. That specific worker instance is responsible for the bulk of the plugin operations for a given
 * job.
 */
public class BuildWorkerInline implements BuildWorker {

  private static final Logger LOG = Logger.getLogger(BuildWorkerInline.class.getName());

  // TODO refactor
  private static final String ANCHORE_BINARY = "sysdig";
  private static final String GATES_OUTPUT_PREFIX = "sysdig_secure_gates";
  private static final String QUERY_OUTPUT_PREFIX = "sysdig_secure_query_";
  private static final String JENKINS_DIR_NAME_PREFIX = "SysdigSecureReport.";
  private static final String JSON_FILE_EXTENSION = ".json";

  // Private members
  Run<?, ?> build;
  FilePath workspace;
  Launcher launcher;
  TaskListener listener;
  BuildConfig config;


  /* Initialized by the constructor */
  private ConsoleLog console; // Log handler for logging to build console
  private boolean analyzed;

  // Initialized by Jenkins workspace prep
  private String buildId;
  private String jenkinsOutputDirName;
  private Map<String, String> queryOutputMap; // TODO rename
  private String gateOutputFileName;
  private GATE_ACTION finalAction;
  private JSONObject gateSummary;
  private String cveListingFileName;

  // Initialized by Sysdig Secure workspace prep
  private String anchoreWorkspaceDirName;
  private String anchoreImageFileName; //TODO rename
  private List<String> anchoreInputImages;

  public BuildWorkerInline(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, BuildConfig config)
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

      // Initialize build logger to log output to consoleLog, use local logging methods only after this initializer completes
      console = new ConsoleLog("AnchoreWorker", this.listener.getLogger(), this.config.getDebug());
      console.logDebug("Initializing build worker");

      // Verify and initialize Jenkins launcher for executing processes
      // TODO is this necessary? Can't we use the launcher reference that was passed in
      this.launcher = workspace.createLauncher(listener);

      // Initialize analyzed flag to false to indicate that analysis step has not run
      this.analyzed = false;

      printConfig();
      checkConfig();

      initializeJenkinsWorkspace();
      initializeAnchoreWorkspace();

      console.logDebug("Build worker initialized");
    } catch (Exception e) {
      try {
        if (console != null) {
          console.logError("Failed to initialize worker for plugin execution", e);
        }
        cleanJenkinsWorkspaceQuietly();
        cleanAnchoreWorkspaceQuietly();
      } catch (Exception innere) {
        // FIXME Why are we ignoring this exception?
      }
      throw new AbortException("Failed to initialize worker for plugin execution, check logs for corrective action");
    }
  }

  @Override
  public ArrayList<ImageScanningSubmission> scanImages(Map<String, String> imagesAndDockerfiles) throws AbortException {
    try {
      console.logInfo("Running Sysdig Secure Analyzer");

      int rc = executeAnchoreCommand(String.format("analyze --skipgates --imagefile %s", anchoreImageFileName));
      if (rc != 0) {
        console.logError("Sysdig Secure analyzer failed with return code " + rc + ", check output above for details");
        throw new AbortException("Sysdig Secure analyzer failed, check output above for details");
      }
      console.logDebug("Sysdig Secure analyzer completed successfully");
      analyzed = true;
    } catch (AbortException e) { // probably caught one of the thrown exceptions, let it pass through
      throw e;
    } catch (Exception e) { // caught unknown exception, log it and wrap its
      console.logError("Failed to run Sysdig Secure analyzer due to an unexpected error", e);
      throw new AbortException(
        "Failed to run Sysdig Secure analyzer due to an unexpected error. Please refer to above logs for more information");
    }
    return new ArrayList<>(); // FIXME Once this is implemented correctly, return the correct arrayList
  }

  @Override
  public GATE_ACTION runGates(List<ImageScanningSubmission> submissionList) throws AbortException {
    if (analyzed) {
      try {
        console.logInfo("Running Sysdig Secure Gates");

        FilePath jenkinsOutputDirFP = new FilePath(workspace, jenkinsOutputDirName);
        FilePath jenkinsGatesOutputFP = new FilePath(jenkinsOutputDirFP, gateOutputFileName);
        String cmd = String.format("--json gate --imagefile %s --show-triggerids --show-whitelisted", anchoreImageFileName);

        try {
          int rc = executeAnchoreCommand(cmd, jenkinsGatesOutputFP.write());
          switch (rc) {
            case 0:
              finalAction = GATE_ACTION.GO;
              break;
            case 2:
              finalAction = GATE_ACTION.WARN;
              break;
            default:
              finalAction = GATE_ACTION.STOP;
          }

          console.logDebug("Sysdig Secure gate execution completed successfully, final action: " + finalAction);
        } catch (IOException | InterruptedException e) {
          console.logWarn("Failed to write gates output to " + jenkinsGatesOutputFP.getRemote(), e);
          throw new AbortException("Failed to write gates output to " + jenkinsGatesOutputFP.getRemote());
        }

        generateGatesSummary(jenkinsGatesOutputFP);

        return finalAction;
      } catch (AbortException e) { // probably caught one of the thrown exceptions, let it pass through
        throw e;
      } catch (Exception e) { // caught unknown exception, log it and wrap it
        console.logError("Failed to run Sysdig Secure gates due to an unexpected error", e);
        throw new AbortException(
          "Failed to run Sysdig Secure gates due to an unexpected error. Please refer to above logs for more information");
      }
    } else {
      console.logError("Analysis step has not been executed (or may have failed in a prior attempt). Rerun analyzer before gates");
      throw new AbortException(
        "Analysis step has not been executed (or may have failed in a prior attempt). Rerun analyzer before gates");
    }
  }

  private void generateGatesSummary(JSONObject gatesJson) {
    console.logDebug("Summarizing policy evaluation results");
    if (gatesJson != null) {
      JSONArray summaryRows = new JSONArray();
      // Populate once and reuse
      int numColumns = 0, repoTagIndex = -1, gateNameIndex = -1, gateActionIndex = -1, whitelistedIndex = -1;

      for (Object imageKey : gatesJson.keySet()) {
        JSONObject content = gatesJson.getJSONObject((String) imageKey);
        if (null != content) {
          JSONObject result = content.getJSONObject("result");
          if (null != result) {
            // populate data from header element once, most likely for the first image
            if (numColumns <= 0 || repoTagIndex < 0 || gateNameIndex < 0 || gateActionIndex < 0 || whitelistedIndex < 0) {
              JSONArray header = result.getJSONArray("header");
              if (null != header) {
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
              } else {
                console.logWarn("'header' element not found in gate output, skipping summary computation for " + imageKey);
                continue;
              }
            }

            if (numColumns <= 0 || repoTagIndex < 0 || gateNameIndex < 0 || gateActionIndex < 0) {
              console.logWarn("Either 'header' element has no columns or column indices (for Repo_Tag, Gate, Gate_Action) not "
                + "initialized, skipping summary computation for " + imageKey);
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
                        stop_wl = (whitelistedIndex != -1 && !(row.getString(whitelistedIndex).equalsIgnoreCase("none") || row
                          .getString(whitelistedIndex).equalsIgnoreCase("false"))) ? ++stop_wl : stop_wl;
                        break;
                      case "warn":
                        warn++;
                        warn_wl = (whitelistedIndex != -1 && !(row.getString(whitelistedIndex).equalsIgnoreCase("none") || row
                          .getString(whitelistedIndex).equalsIgnoreCase("false"))) ? ++warn_wl : warn_wl;
                        break;
                      case "go":
                        go++;
                        go_wl = (whitelistedIndex != -1 && !(row.getString(whitelistedIndex).equalsIgnoreCase("none") || row
                          .getString(whitelistedIndex).equalsIgnoreCase("false"))) ? ++go_wl : go_wl;
                        break;
                      default:
                        break;
                    }
                  }
                } else {
                  console.logWarn("Expected " + numColumns + " elements but got " + row.size() + ", skipping row " + row
                    + " in summary computation for " + imageKey);
                }
              }

              if (!Strings.isNullOrEmpty(repoTag)) {
                console.logInfo("Policy evaluation summary for " + repoTag + " - stop: " + (stop - stop_wl) + " (+" + stop_wl
                  + " whitelisted), warn: " + (warn - warn_wl) + " (+" + warn_wl + " whitelisted), go: " + (go - go_wl) + " (+"
                  + go_wl + " whitelisted), final: " + result.getString("final_action"));

                JSONObject summaryRow = new JSONObject();
                summaryRow.put(GATE_SUMMARY_COLUMN.Repo_Tag.toString(), repoTag);
                summaryRow.put(GATE_SUMMARY_COLUMN.Stop_Actions.toString(), (stop - stop_wl));
                summaryRow.put(GATE_SUMMARY_COLUMN.Warn_Actions.toString(), (warn - warn_wl));
                summaryRow.put(GATE_SUMMARY_COLUMN.Go_Actions.toString(), (go - go_wl));
                summaryRow.put(GATE_SUMMARY_COLUMN.Final_Action.toString(), result.getString("final_action"));
                summaryRows.add(summaryRow);
              } else {
                console.logInfo("Policy evaluation summary for " + imageKey + " - stop: " + (stop - stop_wl) + " (+" + stop_wl
                  + " whitelisted), warn: " + (warn - warn_wl) + " (+" + warn_wl + " whitelisted), go: " + (go - go_wl) + " (+"
                  + go_wl + " whitelisted), final: " + result.getString("final_action"));
                JSONObject summaryRow = new JSONObject();
                summaryRow.put(GATE_SUMMARY_COLUMN.Repo_Tag.toString(), imageKey.toString());
                summaryRow.put(GATE_SUMMARY_COLUMN.Stop_Actions.toString(), (stop - stop_wl));
                summaryRow.put(GATE_SUMMARY_COLUMN.Warn_Actions.toString(), (warn - warn_wl));
                summaryRow.put(GATE_SUMMARY_COLUMN.Go_Actions.toString(), (go - go_wl));
                summaryRow.put(GATE_SUMMARY_COLUMN.Final_Action.toString(), result.getString("final_action"));
                summaryRows.add(summaryRow);

                //console.logWarn("Repo_Tag element not found in gate output, skipping summary computation for " + imageKey);
                console.logWarn("Repo_Tag element not found in gate output, using imageId: " + imageKey);
              }
            } else { // rows object not found
              console.logWarn("'rows' element not found in gate output, skipping summary computation for " + imageKey);
            }
          } else { // result object not found, log and move on
            console.logWarn("'result' element not found in gate output, skipping summary computation for " + imageKey);
          }
        } else { // no content found for a given image id, log and move on
          console.logWarn("No mapped object found in gate output, skipping summary computation for " + imageKey);
        }
      }

      gateSummary = new JSONObject();
      gateSummary.put("header", generateDataTablesColumnsForGateSummary());
      gateSummary.put("rows", summaryRows);

    } else { // could not load gates output to json object
      console.logWarn("Invalid input to generate gates summary");
    }
  }

  private void generateGatesSummary(FilePath jenkinsGatesOutputFP) throws AbortException {
    // Parse gate output and generate summary json
    try {
      console.logDebug("Parsing gate output from " + jenkinsGatesOutputFP.getRemote());
      if (jenkinsGatesOutputFP.exists() && jenkinsGatesOutputFP.length() > 0) {
        JSONObject gatesJson = JSONObject.fromObject(jenkinsGatesOutputFP.readToString());
        if (gatesJson != null) {
          generateGatesSummary(gatesJson);
        } else { // could not load gates output to json object
          console.logWarn("Failed to load/parse gate output from " + jenkinsGatesOutputFP.getRemote());
        }

      } else {
        console.logError("Gate output file not found or empty: " + jenkinsGatesOutputFP.getRemote());
        throw new AbortException("Gate output file not found or empty: " + jenkinsGatesOutputFP.getRemote());
      }
    } catch (AbortException e) { // probably caught one of the thrown exceptions, let it pass through
      throw e;
    } catch (Exception e) {
      console.logError("Failed to generate gate output summary", e);
      throw new AbortException("Failed to generate gate output summary");
    }
  }

  @Override
  public void runQueries() {
  }

  @Override
  public void setupBuildReports() throws AbortException {
    try {
      // store sysdig secure output json files using jenkins archiver (for remote storage as well)
      console.logDebug("Archiving results");
      ArtifactArchiver artifactArchiver = new ArtifactArchiver(jenkinsOutputDirName + "/");
      artifactArchiver.perform(build, workspace, launcher, listener);

      // add the link in jenkins UI for sysdig secure results
      console.logDebug("Setting up build results");

      if (finalAction != null) {
        build.addAction(new AnchoreAction(build, finalAction.toString(), jenkinsOutputDirName, gateOutputFileName, queryOutputMap,
          gateSummary.toString(), cveListingFileName));
      } else {
        build.addAction(new AnchoreAction(build, "", jenkinsOutputDirName, gateOutputFileName, queryOutputMap, gateSummary.toString(),
          cveListingFileName));
      }
    } catch (Exception e) { // caught unknown exception, log it and wrap it
      console.logError("Failed to setup build results due to an unexpected error", e);
      throw new AbortException(
        "Failed to setup build results due to an unexpected error. Please refer to above logs for more information");
    }
  }

  @Override
  public void cleanup() {
    try {
      console.logDebug("Cleaning up build artifacts");
      int rc;

      // Clear Jenkins workspace
      if (!Strings.isNullOrEmpty(jenkinsOutputDirName)) {
        try {
          console.logDebug("Deleting Jenkins workspace " + jenkinsOutputDirName);
          cleanJenkinsWorkspaceQuietly();
        } catch (IOException | InterruptedException e) {
          console.logDebug("Unable to delete Jenkins workspace " + jenkinsOutputDirName, e);
        }
      }

      // Clear Sysdig Secure Container workspace
      if (!Strings.isNullOrEmpty(anchoreWorkspaceDirName)) {
        try {
          console.logDebug("Deleting Sysdig Secure container workspace " + anchoreWorkspaceDirName);
          rc = cleanAnchoreWorkspaceQuietly();
          if (rc != 0) {
            console.logWarn("Unable to delete Sysdig Secure container workspace " + anchoreWorkspaceDirName + ", process returned " + rc);
          }
        } catch (Exception e) {
          console.logWarn("Failed to recursively delete Sysdig Secure container workspace " + anchoreWorkspaceDirName, e);
        }
      }
    } catch (RuntimeException e) { // caught unknown exception, log it
      console.logDebug("Failed to clean up build artifacts due to an unexpected error", e);
    }
  }

  @Override
  public Map<String, String> readImagesAndDockerfilesFromPath(FilePath file) throws AbortException {
    return null;
  }

  /**
   * Print versions info and configuration
   */
  private void printConfig() {
    console.logInfo(String.format("Jenkins version: %s", Jenkins.VERSION));
    List<PluginWrapper> plugins;
    if (Jenkins.getActiveInstance().getPluginManager() != null && (plugins = Jenkins.getActiveInstance().getPluginManager().getPlugins()) != null) {
      for (PluginWrapper plugin : plugins) {
        if (plugin.getShortName()
          .equals("sysdig-secure")) { // artifact ID of the plugin, TODO is there a better way to get this
          console.logInfo(plugin.getDisplayName() + " version: " + plugin.getVersion());
          break;
        }
      }
    }
    config.print(console);
  }

  /**
   * Checks for minimum required config for executing step
   */
  // FIXME: Is this really necessary? Can't we check if the config is correct at the moment of the creation?
  private void checkConfig() throws AbortException {
    if (Strings.isNullOrEmpty(config.getName())) {
      console.logError("Image list file not found");
      throw new AbortException(
        "Image list file not specified. Please provide a valid image list file name in the Sysdig Secure Container Image Scanner step "
          + "and try again");
    }

    try {
      if (!new FilePath(workspace, config.getName()).exists()) {
        console.logError("Cannot find image list file \"" + config.getName() + "\" under " + workspace);
        throw new AbortException("Cannot find image list file '" + config.getName()
          + "'. Please ensure that image list file is created prior to Sysdig Secure Container Image Scanner step");
      }
    } catch (AbortException e) {
      throw e;
    } catch (Exception e) {
      console.logWarn("Unable to access image list file \"" + config.getName() + "\" under " + workspace, e);
      throw new AbortException("Unable to access image list file " + config.getName()
        + ". Please ensure that image list file is created prior to Sysdig Secure Container Image Scanner step");
    }

    if (Strings.isNullOrEmpty(config.getContainerId())) {
      console.logError("Sysdig Secure Container ID not found");
      throw new AbortException(
        "Please configure \"Sysdig Secure Container ID\" under Manage Jenkins->Configure System->Sysdig Secure Configuration and retry. If the"
          + " container is not running, the plugin will launch it");
    }

    // TODO docker and image checks necessary here? check with Dan
  }

  private void initializeJenkinsWorkspace() throws AbortException {
    try {
      console.logDebug("Initializing Jenkins workspace");

      if (Strings.isNullOrEmpty(buildId = build.getParent().getDisplayName() + "_" + build.getNumber())) {
        console.logWarn("Unable to generate a unique identifier for this build due to invalid configuration");
        throw new AbortException("Unable to generate a unique identifier for this build due to invalid configuration");
      }

      jenkinsOutputDirName = JENKINS_DIR_NAME_PREFIX + buildId;
      FilePath jenkinsReportDir = new FilePath(workspace, jenkinsOutputDirName);

      // Create output directories
      if (!jenkinsReportDir.exists()) {
        console.logDebug("Creating workspace directory " + jenkinsOutputDirName);
        jenkinsReportDir.mkdirs();
      }

      queryOutputMap = new LinkedHashMap<>(); // maintain the ordering of queries
      gateOutputFileName = GATES_OUTPUT_PREFIX + JSON_FILE_EXTENSION;

    } catch (AbortException e) { // probably caught one of the thrown exceptions, let it pass through
      throw e;
    } catch (Exception e) { // caught unknown exception, log it and wrap it
      console.logWarn("Failed to initialize Jenkins workspace", e);
      throw new AbortException("Failed to initialize Jenkins workspace due to to an unexpected error");
    }
  }

  private void initializeAnchoreWorkspace() throws AbortException {

    try {
      console.logDebug("Initializing Sysdig Secure workspace");

      // Setup the container first
      setupAnchoreContainer();

      // Initialize sysdig secure workspace variables
      anchoreWorkspaceDirName = "/root/anchore." + buildId;
      anchoreImageFileName = anchoreWorkspaceDirName + "/images";
      anchoreInputImages = new ArrayList<>();

      // setup staging directory in sysdig secure container
      console.logDebug(
        String.format("Creating build artifact directory %s in Sysdig Secure container %s", anchoreWorkspaceDirName, config.getContainerId()));
      int rc = executeCommand(String.format("docker exec %s mkdir -p %s", config.getContainerId(), anchoreWorkspaceDirName));
      if (rc != 0) {
        console.logError(String.format("Failed to create build artifact directory %s in Sysdig Secure container %s", anchoreWorkspaceDirName, config
          .getContainerId()));
        throw new AbortException(
          String.format("Failed to create build artifact directory %s in Sysdig Secure container %s", anchoreWorkspaceDirName, config
            .getContainerId()));
      }

      // Sanitize the input image list
      // - Copy dockerfile for images to sysdig secure container
      // - Create a staging file with adjusted paths
      console.logDebug("Staging image file in Jenkins workspace");

      FilePath jenkinsOutputDirFP = new FilePath(workspace, jenkinsOutputDirName);
      FilePath jenkinsStagedImageFP = new FilePath(jenkinsOutputDirFP, String.format("staged_images.%s", buildId));
      FilePath inputImageFP = new FilePath(workspace, config.getName()); // Already checked in checkConfig()

      try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(jenkinsStagedImageFP.write(), StandardCharsets.UTF_8))) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputImageFP.read(), StandardCharsets.UTF_8))) {
          String line;
          int count = 0;
          while ((line = br.readLine()) != null) {
            // TODO check for a later libriary of guava that lets your slit strings into a list
            Iterator<String> partIterator = Util.IMAGE_LIST_SPLITTER.split(line).iterator();

            if (partIterator.hasNext()) {
              String imgId = partIterator.next();
              String lineToBeAdded = imgId;

              if (partIterator.hasNext()) {
                String jenkinsDFile = partIterator.next();
                String anchoreDFile = String.format("%s/dfile.%d", anchoreWorkspaceDirName, ++count);

                // Copy file from Jenkins to Sysdig Secure container
                console.logDebug(String.format("Copying Dockerfile from Jenkins workspace: %s, to Sysdig Secure workspace: %s", jenkinsDFile, anchoreDFile));
                rc = executeCommand(String.format("docker cp %s %s:%s", jenkinsDFile, config.getContainerId(), anchoreDFile));
                if (rc != 0) {
                  // TODO check with Dan if operation should continue for other images
                  console.logError(
                    String.format("Failed to copy Dockerfile from Jenkins workspace: %s, to Sysdig Secure workspace: %s", jenkinsDFile, anchoreDFile));
                  throw new AbortException(
                    String.format("Failed to copy Dockerfile from Jenkins workspace: %s, to Sysdig Secure workspace: %s. Please ensure that Dockerfile is present in the Jenkins workspace prior to running Sysdig Secure plugin", jenkinsDFile, anchoreDFile));
                }
                lineToBeAdded += " " + anchoreDFile;
              } else {
                console
                  .logWarn(String.format("No dockerfile specified for image %s. Sysdig Secure analyzer will attempt to construct dockerfile", imgId));
              }

              console.logDebug(String.format("Staging sanitized entry: \"%s\"", lineToBeAdded));

              lineToBeAdded += "\n";

              bw.write(lineToBeAdded);
              anchoreInputImages.add(imgId);
            } else {
              console.logWarn(String.format("Cannot parse: \"%s\". Format for each line in input image file is \"imageId /path/to/Dockerfile\", where the Dockerfile is optional", line));
            }
          }
        }
      }

      if (anchoreInputImages.isEmpty()) {
        // nothing to analyze here
        console.logError("List of input images to be analyzed is empty");
        throw new AbortException("List of input images to be analyzed is empty. Please ensure that image file is populated with a list of images to be analyzed. Format for each line is \"imageId /path/to/Dockerfile\", where the Dockerfile is optional");
      }

      // finally, stage the rest of the files

      // Copy the staged images file from Jenkins workspace to Sysdig Secure container
      console.logDebug(
        String.format("Copying staged image file from Jenkins workspace: %s, to Sysdig Secure workspace: %s", jenkinsStagedImageFP.getRemote(), anchoreImageFileName));
      rc = executeCommand(String.format("docker cp %s %s:%s", jenkinsStagedImageFP.getRemote(), config.getContainerId(), anchoreImageFileName));
      if (rc != 0) {
        console.logError(
          String.format("Failed to copy staged image file from Jenkins workspace: %s, to Sysdig Secure workspace: %s", jenkinsStagedImageFP.getRemote(), anchoreImageFileName));
        throw new AbortException(
          String.format("Failed to copy staged image file from Jenkins workspace: %s, to Sysdig Secure workspace: %s", jenkinsStagedImageFP.getRemote(), anchoreImageFileName));
      }
    } catch (AbortException e) { // probably caught one of the thrown exceptions, let it pass through
      throw e;
    } catch (Exception e) { // caught unknown exception, console.log it and wrap it
      console.logError("Failed to initialize Sysdig Secure workspace due to an unexpected error", e);
      throw new AbortException(
        "Failed to initialize Sysdig Secure workspace due to an unexpected error. Please refer to above logs for more information");
    }
  }


  private void setupAnchoreContainer() throws AbortException {
    String containerId = config.getContainerId();

    if (!isAnchoreRunning()) {
      console.logDebug("Sysdig Secure container " + containerId + " is not running");
      String containerImageId = config.getContainerImageId();

      if (isAnchoreImageAvailable()) {
        console.logInfo("Launching Sysdig Secure container " + containerId + " from image " + containerImageId);

        String cmd = "docker run -d -v /var/run/docker.sock:/var/run/docker.sock";
        if (!Strings.isNullOrEmpty(config.getLocalVol())) {
          cmd = String.format("%s -v %s:/root/.anchore", cmd, config.getLocalVol());
        }

        if (!Strings.isNullOrEmpty(config.getModulesVol())) {
          cmd = String.format("%s -v %s:/root/sysdig_secure_modules", cmd, config.getModulesVol());
        }
        cmd = String.format("%s --name %s %s", cmd, containerId, containerImageId);

        int rc = executeCommand(cmd);

        if (rc == 0) {
          console.logDebug(String.format("Sysdig Secure container %s has been launched", containerId));
        } else {
          console.logError(String.format("Failed to launch Sysdig Secure container %s ", containerId));
          throw new AbortException(String.format("Failed to launch Sysdig Secure container %s", containerId));
        }

      } else { // image is not available
        console.logError(
          String.format("Sysdig Secure container image %s not found on local dockerhost, cannot launch Sysdig Secure container %s", containerImageId, containerId));
        throw new AbortException(
          String.format("Sysdig Secure container image %s not found on local dockerhost, cannot launch Sysdig Secure container %s. Please make the anchore/jenkins image available to the local dockerhost and retry", containerImageId, containerId));
      }
    } else {
      console.logDebug(String.format("Sysdig Secure container %s is already running", containerId));
    }
  }

  private boolean isAnchoreRunning() throws AbortException {
    console.logDebug(String.format("Checking container %s", config.getContainerId()));
    if (!Strings.isNullOrEmpty(config.getContainerId())) {
      return executeCommand(String.format("docker start %s", config.getContainerId())) == 0;
    } else {
      console.logError("Sysdig Secure Container ID not found");
      throw new AbortException("Please configure \"Sysdig Secure Container ID\" under Manage Jenkins->Configure System->Sysdig Secure Configuration and retry. If the container is not running, the plugin will launch it");
    }
  }

  private boolean isAnchoreImageAvailable() throws AbortException {
    console.logDebug(String.format("Checking container image %s", config.getContainerImageId()));
    if (!Strings.isNullOrEmpty(config.getContainerImageId())) {
      return executeCommand(String.format("docker inspect %s", config.getContainerImageId())) == 0;
    } else {
      console.logError("Sysdig Secure Container Image ID not found");
      throw new AbortException("Please configure \"Sysdig Secure Container Image ID\" under Manage Jenkins->Configure System->Sysdig Secure Configuration and retry");
    }
  }

  private JSONArray generateDataTablesColumnsForGateSummary() {
    JSONArray headers = new JSONArray();
    for (GATE_SUMMARY_COLUMN column : GATE_SUMMARY_COLUMN.values()) {
      JSONObject header = new JSONObject();
      header.put("data", column.toString());
      header.put("title", column.toString().replaceAll("_", " "));
      headers.add(header);
    }
    return headers;
  }

  private int executeAnchoreCommand(String cmd, String... envOverrides) throws AbortException {
    return executeAnchoreCommand(cmd, config.getDebug() ? console.getLogger() : null, console.getLogger(), envOverrides);
  }

  private int executeAnchoreCommand(String cmd, OutputStream out, String... envOverrides) throws AbortException {
    return executeAnchoreCommand(cmd, out, console.getLogger(), envOverrides);
  }

  /**
   * Helper for executing Sysdig Secure CLI. Abstracts docker and debug options out for the caller
   */
  private int executeAnchoreCommand(String cmd, OutputStream out, OutputStream error, String... envOverrides) throws AbortException {
    String dockerCmd = String.format("docker exec %s %s", config.getContainerId(), ANCHORE_BINARY);

    if (config.getDebug()) {
      dockerCmd += " --debug";
    }

    dockerCmd += String.format(" %s", cmd);

    return executeCommand(dockerCmd, out, error, envOverrides);
  }

  private int executeCommand(String cmd, String... envOverrides) throws AbortException {
    // log stdout to console only if debug is turned on
    // always log stderr to console
    return executeCommand(cmd, config.getDebug() ? console.getLogger() : null, console.getLogger(), envOverrides);
  }

  private int executeCommand(String cmd, OutputStream out, OutputStream error, String... envOverrides) throws AbortException {
    int rc;

    Launcher.ProcStarter ps = launcher.launch();

    ps.envs(envOverrides);
    ps.cmdAsSingleString(cmd);
    ps.stdin(null);
    if (null != out) {
      ps.stdout(out);
    }
    if (null != error) {
      ps.stderr(error);
    }

    try {
      console.logDebug(String.format("Executing \"%s\"", cmd));
      rc = ps.join();
      console.logDebug(String.format("Execution of \"%s\" returned %d", cmd, rc));
      return rc;
    } catch (Exception e) {
      console.logWarn(String.format("Failed to execute \"%s\"", cmd), e);
      throw new AbortException(String.format("Failed to execute \"%s\"", cmd));
    }
  }

  private void cleanJenkinsWorkspaceQuietly() throws IOException, InterruptedException {
    FilePath jenkinsOutputDirFP = new FilePath(workspace, jenkinsOutputDirName);
    jenkinsOutputDirFP.deleteRecursive();
  }

  private int cleanAnchoreWorkspaceQuietly() throws AbortException {
    return executeCommand(String.format("docker exec %s rm -rf %s", config.getContainerId(), anchoreWorkspaceDirName));
  }
}
