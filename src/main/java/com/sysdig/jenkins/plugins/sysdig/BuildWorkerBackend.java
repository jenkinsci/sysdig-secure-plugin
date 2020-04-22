package com.sysdig.jenkins.plugins.sysdig;

import com.google.common.base.Strings;
import com.sysdig.jenkins.plugins.sysdig.Util.GATE_ACTION;
import com.sysdig.jenkins.plugins.sysdig.Util.GATE_SUMMARY_COLUMN;
import com.sysdig.jenkins.plugins.sysdig.log.ConsoleLog;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
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
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

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
public class BuildWorkerBackend implements BuildWorker {

  private static final Logger LOG = Logger.getLogger(BuildWorkerBackend.class.getName());

  private static final String GATES_OUTPUT_PREFIX = "sysdig_secure_gates";
  private static final String CVE_LISTING_PREFIX = "sysdig_secure_security";
  private static final String JENKINS_DIR_NAME_PREFIX = "SysdigSecureReport.";
  private static final String JSON_FILE_EXTENSION = ".json";

  // Private members
  Run<?, ?> build;
  FilePath workspace;
  Launcher launcher;
  TaskListener listener;
  BuildConfig config;


  /* Initialized by the constructor */
  private SysdigLogger logger; // Log handler for logging to build console
  private boolean analyzed;

  private String jenkinsOutputDirName;
  private Map<String, String> queryOutputMap; // TODO rename
  private final Map<String, String> input_image_dfile = new LinkedHashMap<>();
  private final Map<String, String> input_image_imageDigest = new LinkedHashMap<>();
  private String gateOutputFileName;
  private GATE_ACTION finalAction;
  private JSONObject gateSummary;
  private String cveListingFileName;

  // FIXME can we get rid of this config? Also the launcher is not being used...
  public BuildWorkerBackend(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, BuildConfig config)
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

      // FIXME create an interface for this ConsoleLog and use it instead of the object itself
      // FIXME also receive it as dependency injection
      // Initialize build logger to log output to consoleLog, use local logging methods only after this initializer completes
      logger = new ConsoleLog("SysdigWorker", this.listener.getLogger(), this.config.getDebug());
      logger.logDebug("Initializing build worker");

      // Verify and initialize Jenkins launcher for executing processes
      // TODO is this necessary? Can't we use the launcher reference that was passed in
      this.launcher = workspace.createLauncher(listener);

      // Initialize analyzed flag to false to indicate that analysis step has not run
      this.analyzed = false;

      printConfig();
      checkConfig();

      initializeJenkinsWorkspace();
      initializeAnchoreWorkspace();

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

  @Override
  public void runAnalyzer() throws AbortException {
    String imageDigest;
    String username = config.getEngineuser();
    String password = config.getEnginepass();
    boolean sslverify = config.getEngineverify();

    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
    HttpClientContext context = HttpClientContext.create();
    context.setCredentialsProvider(credsProvider);

    try {
      for (Map.Entry<String, String> entry : input_image_dfile.entrySet()) {
        String tag = entry.getKey();
        String dfile = entry.getValue();

        logger.logInfo(String.format("Submitting %s for analysis", tag));

        try (CloseableHttpClient httpclient = makeHttpClient(sslverify)) {
          // Prep POST request
          String theurl = String.format("%s/images", config.getEngineurl().replaceAll("/+$", ""));

          // Prep request body
          JSONObject jsonBody = new JSONObject();
          jsonBody.put("tag", tag);
          if (null != dfile) {
            jsonBody.put("dockerfile", dfile);
          }

          String body = jsonBody.toString();

          // FIXME Move all content that contacts with the backend to a Client
          HttpPost httppost = new HttpPost(theurl);
          httppost.addHeader("Content-Type", "application/json");
          httppost.setEntity(new StringEntity(body));

          logger.logDebug(String.format("sysdig-secure-engine add image URL: %s", theurl));
          logger.logDebug(String.format("sysdig-secure-engine add image payload: %s", body));

          try (CloseableHttpResponse response = httpclient.execute(httppost, context)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
              String serverMessage = EntityUtils.toString(response.getEntity());
              logger.logError(String.format("sysdig-secure-engine add image failed. URL: %s, status: %s, error: %s", theurl, response.getStatusLine(), serverMessage));
              throw new AbortException(String.format("Failed to analyze %s due to error adding image to sysdig-secure-engine. Check above logs for errors from sysdig-secure-engine", tag));
            }

            // Read the response body.
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONArray respJson = JSONArray.fromObject(responseBody);
            imageDigest = JSONObject.fromObject(respJson.get(0)).getString("imageDigest");
            logger.logInfo(String.format("Analysis request accepted, received image digest %s", imageDigest));
            input_image_imageDigest.put(tag, imageDigest);

          }
        }
      }
      analyzed = true;
    } catch (AbortException e) { // probably caught one of the thrown exceptions, let it pass through
      throw e;
    } catch (Exception e) { // caught unknown exception, log it and wrap its
      logger.logError("Failed to add image(s) to sysdig-secure-engine due to an unexpected error", e);
      throw new AbortException("Failed to add image(s) to sysdig-secure-engine due to an unexpected error. Please refer to above logs for more information");
    }
  }

  private static CloseableHttpClient makeHttpClient(boolean verify) {
    CloseableHttpClient httpclient = null;
    if (verify) {
      httpclient = HttpClients.createDefault();
    } else {
      //SSLContextBuilder builder;

      //SSLConnectionSocketFactory sslsf=null;

      try {
        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(),
          SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
      } catch (Exception e) {
        System.out.println(e.toString());
      }
    }
    return (httpclient);
  }

  @Override
  public GATE_ACTION runGates() throws AbortException {
    String username = config.getEngineuser();
    String password = config.getEnginepass();
    boolean sslverify = config.getEngineverify();

    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
    HttpClientContext context = HttpClientContext.create();
    context.setCredentialsProvider(credsProvider);

    //Credentials defaultcreds = new UsernamePasswordCredentials(username, password);
    FilePath jenkinsOutputDirFP = new FilePath(workspace, jenkinsOutputDirName);
    FilePath jenkinsGatesOutputFP = new FilePath(jenkinsOutputDirFP, gateOutputFileName);

    finalAction = GATE_ACTION.PASS;
    if (analyzed) {
      try {
        JSONObject gate_results = new JSONObject();

        for (Map.Entry<String, String> entry : input_image_imageDigest.entrySet()) {
          String tag = entry.getKey();
          String imageDigest = entry.getValue();

          logger.logInfo("Waiting for analysis of " + tag + ", polling status periodically");

          boolean sysdig_secure_eval_status = false;
          String theurl = String.format("%s/images/%s/check?tag=%s&detail=true", config.getEngineurl().replaceAll("/+$", ""), imageDigest, tag);

          logger.logDebug("sysdig-secure-engine get policy evaluation URL: " + theurl);

          int tryCount = 0;
          int maxCount = Integer.parseInt(config.getEngineRetries());
          boolean done = false;
          HttpGet httpget = new HttpGet(theurl);
          httpget.addHeader("Content-Type", "application/json");
          int statusCode;
          String serverMessage = null;
          boolean sleep = false;

          do { // try this at least once regardless what the retry count is
            if (sleep) {
              logger.logDebug("Snoozing before retrying sysdig-secure-engine get policy evaluation");
              Thread.sleep(1000);
              sleep = false;
            }

            tryCount++;
            try (CloseableHttpClient httpclient = makeHttpClient(sslverify)) {
              logger.logDebug(String.format("Attempting sysdig-secure-engine get policy evaluation (%d/%d)", tryCount, maxCount));

              try (CloseableHttpResponse response = httpclient.execute(httpget, context)) {
                statusCode = response.getStatusLine().getStatusCode();

                if (statusCode != 200) {
                  serverMessage = EntityUtils.toString(response.getEntity());
                  logger.logDebug(String.format("sysdig-secure-engine get policy evaluation failed. URL: %s, status: %s, error: %s", theurl, response.getStatusLine(), serverMessage));
                  sleep = true;
                } else {
                  // Read the response body.
                  String responseBody = EntityUtils.toString(response.getEntity());
                  JSONArray respJson = JSONArray.fromObject(responseBody);
                  JSONObject tag_eval_obj = JSONObject.fromObject(JSONArray.fromObject(JSONArray.fromObject(JSONObject.fromObject(JSONObject.fromObject(respJson.get(0)).getJSONObject(imageDigest)))).get(0));
                  JSONArray tag_evals = null;
                  for (Object key : tag_eval_obj.keySet()) {
                    tag_evals = tag_eval_obj.getJSONArray((String) key);
                    break;
                  }

                  if (null == tag_evals) {
                    throw new AbortException(String.format("Failed to analyze %s due to missing tag eval records in sysdig-secure-engine policy evaluation response", tag));
                  }
                  if (tag_evals.size() < 1) {
                    // try again until we get an eval
                    logger.logDebug("sysdig-secure-engine get policy evaluation response contains no tag eval records. May snooze and retry");

                    sleep = true;
                  } else {
                    String eval_status = JSONObject.fromObject(JSONObject.fromObject(tag_evals.get(0))).getString("status");
                    JSONObject gate_result = JSONObject.fromObject(JSONObject.fromObject(JSONObject.fromObject(JSONObject.fromObject(tag_evals.get(0)).getJSONObject("detail")).getJSONObject("result")).getJSONObject("result"));

                    logger.logDebug(String.format("sysdig-secure-engine get policy evaluation status: %s", eval_status));
                    logger.logDebug(String.format("sysdig-secure-engine get policy evaluation result: %s", gate_result.toString()));
                    for (Object key : gate_result.keySet()) {
                      try {
                        gate_results.put((String) key, gate_result.getJSONObject((String) key));
                      } catch (Exception e) {
                        logger.logDebug("Ignoring error parsing policy evaluation result key: " + key);
                      }
                    }

                    // we actually got a real result
                    // this is the only way this gets flipped to true
                    if (eval_status.equals("pass")) {
                      sysdig_secure_eval_status = true;
                    }
                    done = true;
                    logger.logInfo("Completed analysis and processed policy evaluation result");
                  }
                }
              }
            }
          } while (!done && tryCount < maxCount);

          if (!done) {
            if (statusCode != 200) {
              logger.logWarn(String.format("sysdig-secure-engine get policy evaluation failed. HTTP method: GET, URL: %s, status: %d, error: %s", theurl, statusCode, serverMessage));
            }
            logger.logWarn(String.format("Exhausted all attempts polling sysdig-secure-engine. Analysis is incomplete for %s", imageDigest));
            throw new AbortException("Timed out waiting for sysdig-secure-engine analysis to complete (increasing engineRetries might help). Check above logs for errors from sysdig-secure-engine");
          } else {
            // only set to stop if an eval is successful and is reporting fail
            if (!sysdig_secure_eval_status) {
              finalAction = GATE_ACTION.FAIL;
            }
          }
        }

        try {
          logger.logDebug(String.format("Writing policy evaluation result to %s", jenkinsGatesOutputFP.getRemote()));
          try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(jenkinsGatesOutputFP.write(), StandardCharsets.UTF_8))) {
            bw.write(gate_results.toString());
          }
        } catch (IOException | InterruptedException e) {
          logger.logWarn(String.format("Failed to write policy evaluation output to %s", jenkinsGatesOutputFP.getRemote()), e);
          throw new AbortException(String.format("Failed to write policy evaluation output to %s", jenkinsGatesOutputFP.getRemote()));
        }

        generateGatesSummary(gate_results);
        logger.logInfo("Sysdig Secure Container Image Scanner Plugin step result - " + finalAction);
        return finalAction;
      } catch (AbortException e) { // probably caught one of the thrown exceptions, let it pass through
        throw e;
      } catch (Exception e) { // caught unknown exception, log it and wrap it
        logger.logError("Failed to execute sysdig-secure-engine policy evaluation due to an unexpected error", e);
        throw new AbortException("Failed to execute sysdig-secure-engine policy evaluation due to an unexpected error. Please refer to above logs for more information");
      }
    } else {
      logger.logError("Image(s) were not added to sysdig-secure-engine (or a prior attempt to add images may have failed). Re-submit image(s) to sysdig-secure-engine before attempting policy evaluation");
      throw new AbortException("Submit image(s) to sysdig-secure-engine for analysis before attempting policy evaluation");
    }
  }

  private void generateGatesSummary(JSONObject gatesJson) {
    logger.logDebug("Summarizing policy evaluation results");
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
                logger.logWarn(String.format("'header' element not found in gate output, skipping summary computation for %s", imageKey));
                continue;
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
                  logger.logWarn(String.format("Expected %d elements but got %d, skipping row %s in summary computation for %s", numColumns, row.size(), row, imageKey));
                }
              }

              if (!Strings.isNullOrEmpty(repoTag)) {
                logger.logInfo(String.format("Policy evaluation summary for %s - stop: %d (+%d whitelisted), warn: %d (+%d whitelisted), go: %d (+%d whitelisted), final: %s", repoTag, stop - stop_wl, stop_wl, warn - warn_wl, warn_wl, go - go_wl, go_wl, result.getString("final_action")));

                JSONObject summaryRow = new JSONObject();
                summaryRow.put(GATE_SUMMARY_COLUMN.Repo_Tag.toString(), repoTag);
                summaryRow.put(GATE_SUMMARY_COLUMN.Stop_Actions.toString(), (stop - stop_wl));
                summaryRow.put(GATE_SUMMARY_COLUMN.Warn_Actions.toString(), (warn - warn_wl));
                summaryRow.put(GATE_SUMMARY_COLUMN.Go_Actions.toString(), (go - go_wl));
                summaryRow.put(GATE_SUMMARY_COLUMN.Final_Action.toString(), result.getString("final_action"));
                summaryRows.add(summaryRow);
              } else {
                logger.logInfo(String.format("Policy evaluation summary for %s - stop: %d (+%d whitelisted), warn: %d (+%d whitelisted), go: %d (+%d whitelisted), final: %s", imageKey, stop - stop_wl, stop_wl, warn - warn_wl, warn_wl, go - go_wl, go_wl, result.getString("final_action")));
                JSONObject summaryRow = new JSONObject();
                summaryRow.put(GATE_SUMMARY_COLUMN.Repo_Tag.toString(), imageKey.toString());
                summaryRow.put(GATE_SUMMARY_COLUMN.Stop_Actions.toString(), (stop - stop_wl));
                summaryRow.put(GATE_SUMMARY_COLUMN.Warn_Actions.toString(), (warn - warn_wl));
                summaryRow.put(GATE_SUMMARY_COLUMN.Go_Actions.toString(), (go - go_wl));
                summaryRow.put(GATE_SUMMARY_COLUMN.Final_Action.toString(), result.getString("final_action"));
                summaryRows.add(summaryRow);

                //console.logWarn("Repo_Tag element not found in gate output, skipping summary computation for " + imageKey);
                logger.logWarn(String.format("Repo_Tag element not found in gate output, using imageId: %s", imageKey));
              }
            } else { // rows object not found
              logger.logWarn(String.format("'rows' element not found in gate output, skipping summary computation for %s", imageKey));
            }
          } else { // result object not found, log and move on
            logger.logWarn(String.format("'result' element not found in gate output, skipping summary computation for %s", imageKey));
          }
        } else { // no content found for a given image id, log and move on
          logger.logWarn(String.format("No mapped object found in gate output, skipping summary computation for %s", imageKey));
        }
      }

      gateSummary = new JSONObject();
      gateSummary.put("header", generateDataTablesColumnsForGateSummary());
      gateSummary.put("rows", summaryRows);

    } else { // could not load gates output to json object
      logger.logWarn("Invalid input to generate gates summary");
    }
  }

  @Override
  public void runQueries() throws AbortException {

    if (analyzed) {
      String username = config.getEngineuser();
      String password = config.getEnginepass();
      boolean sslverify = config.getEngineverify();

      CredentialsProvider credsProvider = new BasicCredentialsProvider();
      credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
      HttpClientContext context = HttpClientContext.create();
      context.setCredentialsProvider(credsProvider);

      try {
        JSONObject securityJson = new JSONObject();
        JSONArray columnsJson = new JSONArray();
        for (String column : Arrays.asList("Tag", "CVE ID", "Severity", "Vulnerability Package", "Fix Available", "URL")) {
          JSONObject columnJson = new JSONObject();
          columnJson.put("title", column);
          columnsJson.add(columnJson);
        }
        JSONArray dataJson = new JSONArray();

        for (Map.Entry<String, String> entry : input_image_imageDigest.entrySet()) {
          String input = entry.getKey();
          String digest = entry.getValue();

          try (CloseableHttpClient httpclient = makeHttpClient(sslverify)) {
            logger.logInfo("Querying vulnerability listing for " + input);
            String theurl = String.format("%s/images/%s/vuln/all", config.getEngineurl().replaceAll("/+$", ""), digest);
            HttpGet httpget = new HttpGet(theurl);
            httpget.addHeader("Content-Type", "application/json");

            logger.logDebug("sysdig-secure-engine get vulnerability listing URL: " + theurl);
            try (CloseableHttpResponse response = httpclient.execute(httpget, context)) {
              String responseBody = EntityUtils.toString(response.getEntity());
              JSONObject responseJson = JSONObject.fromObject(responseBody);
              JSONArray vulList = responseJson.getJSONArray("vulnerabilities");
              for (int i = 0; i < vulList.size(); i++) {
                JSONObject vulnJson = vulList.getJSONObject(i);
                JSONArray vulnArray = new JSONArray();
                vulnArray.addAll(Arrays.asList(
                  input,
                  vulnJson.getString("vuln"),
                  vulnJson.getString("severity"),
                  vulnJson.getString("package"),
                  vulnJson.getString("fix"),
                  String.format("<a href='%s'>%s</a>", vulnJson.getString("url"), vulnJson.getString("url"))));
                dataJson.add(vulnArray);
              }
            }
          }
        }
        securityJson.put("columns", columnsJson);
        securityJson.put("data", dataJson);

        cveListingFileName = CVE_LISTING_PREFIX + JSON_FILE_EXTENSION;
        FilePath jenkinsOutputDirFP = new FilePath(workspace, jenkinsOutputDirName);
        FilePath jenkinsQueryOutputFP = new FilePath(jenkinsOutputDirFP, cveListingFileName);
        try {
          logger.logDebug(String.format("Writing vulnerability listing result to %s", jenkinsQueryOutputFP.getRemote()));
          try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(jenkinsQueryOutputFP.write(), StandardCharsets.UTF_8))) {
            bw.write(securityJson.toString());
          }
        } catch (IOException | InterruptedException e) {
          logger.logWarn(String.format("Failed to write vulnerability listing to %s", jenkinsQueryOutputFP.getRemote()), e);
          throw new AbortException(String.format("Failed to write vulnerability listing to %s", jenkinsQueryOutputFP.getRemote()));
        }
      } catch (AbortException e) { // probably caught one of the thrown exceptions, let it pass through
        throw e;
      } catch (Exception e) { // caught unknown exception, log it and wrap it
        logger.logError("Failed to fetch vulnerability listing from sysdig-secure-engine due to an unexpected error", e);
        throw new AbortException("Failed to fetch vulnerability listing from sysdig-secure-engine due to an unexpected error. Please refer to above logs for more information");
      }
    } else {
      logger.logError("Image(s) were not added to sysdig-secure-engine (or a prior attempt to add images may have failed). Re-submit image(s) to sysdig-secure-engine before attempting vulnerability listing");
      throw new AbortException("Submit image(s) to sysdig-secure-engine for analysis before attempting vulnerability listing");
    }
  }

  @Override
  public void setupBuildReports() throws AbortException {
    try {
      // store sysdig secure output json files using jenkins archiver (for remote storage as well)
      logger.logDebug("Archiving results");
      ArtifactArchiver artifactArchiver = new ArtifactArchiver(jenkinsOutputDirName + "/");
      artifactArchiver.perform(build, workspace, launcher, listener);

      // add the link in jenkins UI for sysdig secure results
      logger.logDebug("Setting up build results");

      if (finalAction != null) {
        build.addAction(new AnchoreAction(build, finalAction.toString(), jenkinsOutputDirName, gateOutputFileName, queryOutputMap,
          gateSummary.toString(), cveListingFileName));
      } else {
        build.addAction(new AnchoreAction(build, "", jenkinsOutputDirName, gateOutputFileName, queryOutputMap, gateSummary.toString(),
          cveListingFileName));
      }
    } catch (Exception e) { // caught unknown exception, log it and wrap it
      logger.logError("Failed to setup build results due to an unexpected error", e);
      throw new AbortException(
        "Failed to setup build results due to an unexpected error. Please refer to above logs for more information");
    }
  }

  @Override
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
    if (Jenkins.getActiveInstance().getPluginManager() != null && (plugins = Jenkins.getActiveInstance().getPluginManager().getPlugins()) != null) {
      for (PluginWrapper plugin : plugins) {
        if (plugin.getShortName().equals("sysdig-secure")) { // artifact ID of the plugin, TODO is there a better way to get this
          logger.logInfo(String.format("%s version: %s", plugin.getDisplayName(), plugin.getVersion()));
          break;
        }
      }
    }
    config.print(logger);
  }

  /**
   * Checks for minimum required config for executing step
   */
  // FIXME: Is this really necessary? Can't we check if the config is correct at the moment of the creation?
  private void checkConfig() throws AbortException {
    if (Strings.isNullOrEmpty(config.getName())) {
      logger.logError("Image list file not found");
      throw new AbortException(
        "Image list file not specified. Please provide a valid image list file name in the Sysdig Secure Container Image Scanner step and try again");
    }

    try {
      if (!new FilePath(workspace, config.getName()).exists()) {
        logger.logError(String.format("Cannot find image list file \"%s\" under %s", config.getName(), workspace));
        throw new AbortException(String.format("Cannot find image list file '%s'. Please ensure that image list file is created prior to Sysdig Secure Container Image Scanner step", config.getName()));
      }
    } catch (AbortException e) {
      throw e;
    } catch (Exception e) {
      logger.logWarn(String.format("Unable to access image list file \"%s\" under %s", config.getName(), workspace), e);
      throw new AbortException(String.format("Unable to access image list file %s. Please ensure that image list file is created prior to Sysdig Secure Container Image Scanner step", config.getName()));
    }
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

      queryOutputMap = new LinkedHashMap<>(); // maintain the ordering of queries
      gateOutputFileName = GATES_OUTPUT_PREFIX + JSON_FILE_EXTENSION;

    } catch (AbortException e) { // probably caught one of the thrown exceptions, let it pass through
      throw e;
    } catch (Exception e) { // caught unknown exception, log it and wrap it
      logger.logWarn("Failed to initialize Jenkins workspace", e);
      throw new AbortException("Failed to initialize Jenkins workspace due to to an unexpected error");
    }
  }

  private void initializeAnchoreWorkspace() throws AbortException {


    try {
      logger.logDebug("Initializing Sysdig Secure workspace");

      // get the input and store it in tag/dockerfile map
      FilePath inputImageFP = new FilePath(workspace, config.getName()); // Already checked in checkConfig()
      try (BufferedReader br = new BufferedReader(new InputStreamReader(inputImageFP.read(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = br.readLine()) != null) {
          String imgId = null;
          String dfilecontents = null;
          Iterator<String> partIterator = Util.IMAGE_LIST_SPLITTER.split(line).iterator();

          if (partIterator.hasNext()) {
            imgId = partIterator.next();

            if (partIterator.hasNext()) {
              String jenkinsDFile = partIterator.next();

              StringBuilder b = new StringBuilder();
              FilePath myfp = new FilePath(workspace, jenkinsDFile);
              try (BufferedReader mybr = new BufferedReader(new InputStreamReader(myfp.read(), StandardCharsets.UTF_8))) {
                String myline;
                while ((myline = mybr.readLine()) != null) {
                  b.append(myline).append('\n');
                }
              }
              logger.logDebug(String.format("Dockerfile contents: %s", b.toString()));
              byte[] encodedBytes = Base64.encodeBase64(b.toString().getBytes(StandardCharsets.UTF_8));
              dfilecontents = new String(encodedBytes, StandardCharsets.UTF_8);

            }
          }
          if (null != imgId) {
            logger.logDebug(String.format("Image tag/digest: %s", imgId));
            logger.logDebug(String.format("Base64 encoded Dockerfile contents: %s", dfilecontents));
            input_image_dfile.put(imgId, dfilecontents);
          }
        }
      }
    } catch (AbortException e) { // probably caught one of the thrown exceptions, let it pass through
      throw e;
    } catch (Exception e) { // caught unknown exception, console.log it and wrap it
      logger.logError("Failed to initialize Sysdig Secure workspace due to an unexpected error", e);
      throw new AbortException("Failed to initialize Sysdig Secure workspace due to an unexpected error. Please refer to above logs for more information");
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

  private void cleanJenkinsWorkspaceQuietly() throws IOException, InterruptedException {
    FilePath jenkinsOutputDirFP = new FilePath(workspace, jenkinsOutputDirName);
    jenkinsOutputDirFP.deleteRecursive();
  }
}
