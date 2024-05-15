package com.sysdig.jenkins.plugins.sysdig;

import com.google.common.base.Strings;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.scanner.ImageScanningResult;
import hudson.AbortException;
import hudson.FilePath;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportConverter {
  private static final String LEGACY_PASSED_STATUS = "pass";
  private static final String PASSED_STATUS = "passed";
  private static final String ACCEPTED_STATUS = "accepted";
  private static final String NO_POLICY_STATUS = "noPolicy";

  protected final SysdigLogger logger;

  public ReportConverter(SysdigLogger logger) {
    this.logger = logger;
  }

  public Util.GATE_ACTION getFinalAction(List<ImageScanningResult> results) throws AbortException {
    Util.GATE_ACTION finalAction = Util.GATE_ACTION.PASS;

    for (ImageScanningResult result : results) {
      String evalStatus = result.getEvalStatus();

      logger.logDebug(String.format("Get policy evaluation status for image '%s': %s", result.getTag(), evalStatus));

      if (!evalStatus.equalsIgnoreCase(LEGACY_PASSED_STATUS) && !evalStatus.equalsIgnoreCase(PASSED_STATUS) && !evalStatus.equalsIgnoreCase(ACCEPTED_STATUS) && !evalStatus.equalsIgnoreCase(NO_POLICY_STATUS)) {
        finalAction = Util.GATE_ACTION.FAIL;
      }
    }

    return finalAction;
  }

  public JSONObject processPolicyEvaluation(List<ImageScanningResult> resultList, FilePath jenkinsGatesOutputFP) throws IOException, InterruptedException {
    JSONObject fullGateResults = new JSONObject();
    Map<String, String> imageDigestsToTags = new HashMap<>(resultList.size());

    for (ImageScanningResult result : resultList) {
      JSONObject gateResult = result.getGateResult();
      JSONArray gatePolicies = result.getGatePolicies();

      if (logger.isDebugEnabled()) {
        logger.logDebug(String.format("sysdig-secure-engine gate policies for '%s': %s ", result.getTag(), gatePolicies.toString()));
        logger.logDebug(String.format("sysdig-secure-engine get policy evaluation result for '%s': %s ", result.getTag(), gateResult.toString()));

      }
      HashMap<String, String> policieNames = new HashMap<>();
      gatePolicies.forEach(item -> {
        JSONObject obj = (JSONObject) item;
        policieNames.put(obj.getString("id"), obj.getString("name"));
      });

      for (Object key : gateResult.keySet()) {

        try {
          JSONObject processedResult = gateResult.getJSONObject((String) key);
          processedResult.getJSONObject("result").getJSONArray("header").element("Policy_Name");

          for (Object row : processedResult.getJSONObject("result").getJSONArray("rows")) {
            ((JSONArray) row).element(policieNames.get(((JSONArray) row).getString(processedResult.getJSONObject("result").getJSONArray("header").indexOf("Policy_Id"))));
          }
          fullGateResults.put((String) key, gateResult.getJSONObject((String) key));
          imageDigestsToTags.put(result.getImageDigest(), result.getTag());
        } catch (Exception e) {
          logger.logDebug("Ignoring error parsing policy evaluation result key: " + key);
        }
      }
    }

    logger.logDebug(String.format("Writing policy evaluation result to %s", jenkinsGatesOutputFP.getRemote()));
    jenkinsGatesOutputFP.write(fullGateResults.toString(), String.valueOf(StandardCharsets.UTF_8));

    return generateGatesSummary(fullGateResults, imageDigestsToTags);
  }

  protected JSONObject generateGatesSummary(JSONObject gatesJson, final Map<String, String> digestsToTags) {
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
          String imageName = digestsToTags.get(imageKey.toString());
          if (Strings.isNullOrEmpty(imageName)){
            imageName = imageKey.toString();
          }

          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Repo_Tag.toString(), imageName);
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

  protected static JSONArray generateDataTablesColumnsForGateSummary() {
    JSONArray headers = new JSONArray();
    for (Util.GATE_SUMMARY_COLUMN column : Util.GATE_SUMMARY_COLUMN.values()) {
      JSONObject header = new JSONObject();
      header.put("data", column.toString());
      header.put("title", column.toString().replaceAll("_", " "));
      headers.add(header);
    }
    return headers;
  }

  public void processVulnerabilities(List<ImageScanningResult> scanResults, FilePath jenkinsQueryOutputFP) throws IOException, InterruptedException {

    JSONArray dataJson = new JSONArray();
    for (ImageScanningResult entry : scanResults) {
      String tag = entry.getTag();
      dataJson.addAll(getVulnerabilitiesArray(tag, entry.getVulnerabilityReport()));
    }

    JSONObject securityJson = new JSONObject();
    JSONArray columnsJson = new JSONArray();

    for (String column : Arrays.asList("Tag", "CVE ID", "Severity", "Vulnerability Package", "Fix Available", "URL", "Package Type", "Package Path", "Disclosure Date", "Solution Date")) {
      JSONObject columnJson = new JSONObject();
      columnJson.put("title", column);
      columnsJson.add(columnJson);
    }

    securityJson.put("columns", columnsJson);
    securityJson.put("data", dataJson);

    jenkinsQueryOutputFP.write(securityJson.toString(), String.valueOf(StandardCharsets.UTF_8));
  }

  protected JSONArray getVulnerabilitiesArray(String tag, JSONObject vulnsReport) {
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
        vulnJson.getString("url"),
        vulnJson.getString("package_type"),
        vulnJson.getString("package_path"),
        vulnJson.has("disclosure_date") ? vulnJson.getString("disclosure_date") : "",
        vulnJson.has("solution_date") ? vulnJson.getString("solution_date") : ""
      ));
      dataJson.add(vulnArray);
    }

    return dataJson;
  }

}
