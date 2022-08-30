package com.sysdig.jenkins.plugins.sysdig;

import com.google.common.base.Strings;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.scanner.ImageScanningResult;
import hudson.FilePath;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class NewEngineReportConverter extends ReportConverter {

  public NewEngineReportConverter(SysdigLogger logger) {
    super(logger);
  }

  @Override
  public JSONObject processPolicyEvaluation(List<ImageScanningResult> resultList, FilePath jenkinsGatesOutputFP)
    throws IOException, InterruptedException {
    JSONObject fullGateResults = new JSONObject();

    for (ImageScanningResult result : resultList) {
      JSONObject gateResult = result.getGateResult();
      JSONArray gatePolicies = result.getGatePolicies();

      if (logger.isDebugEnabled()) {
        logger.logDebug(String.format("sysdig-secure-engine gate policies for '%s': %s ", result.getTag(), gatePolicies.toString()));
        logger.logDebug(String.format("sysdig-secure-engine get policy evaluation result for '%s': %s ", result.getTag(), gateResult.toString()));
      }
      fullGateResults.put(result.getImageDigest(), generateCompatibleGatesResult(result));
    }

    logger.logDebug(String.format("Writing policy evaluation result to %s", jenkinsGatesOutputFP.getRemote()));
    jenkinsGatesOutputFP.write(fullGateResults.toString(), String.valueOf(StandardCharsets.UTF_8));

    return generateGatesSummary(fullGateResults);
  }

  private String getPkgVulnFailuresString(JSONObject failure) {
    return failure.getString("vulnerabilityName") + " in " + failure.getString("packageName") + "-"
      + failure.getString("packageVersion");
  }

  private JSONArray getFailure(String failure, ImageScanningResult imageResult, String policyName, String ruleString, String ruleName) {
    JSONArray row = new JSONArray();
    row.element(imageResult.getImageDigest());
    row.element(imageResult.getTag());
    row.element("trigger_id");
    row.element(ruleName);
    row.element(ruleString);
    row.element(failure);
    row.element("STOP");
    row.element(false);
    row.element("");
    row.element(policyName);
    return row;
  }

  private JSONArray getPkgVulnFailures(JSONObject rule, ImageScanningResult imageResult, String policyName, String ruleName, String ruleString) {
    return rule.getJSONArray("pkgVulnFailures").stream().map(failure -> {
        String failureName = getPkgVulnFailuresString((JSONObject) failure);
        return getFailure(failureName, imageResult, policyName, ruleString, ruleName);
      })
      .collect(Collectors.toCollection(JSONArray::new));
  }

  private JSONArray getImageConfFailures(JSONObject rule, ImageScanningResult imageResult, String policyName, String ruleName, String ruleString) {
    return rule.getJSONArray("imageConfFailures").stream().map(failure -> {
        String failureName = ((JSONObject) failure).getString("remediationText").replaceAll("(\r\n|\n)", "<br />");
        return getFailure(failureName, imageResult, policyName, ruleString, ruleName);
      })
      .collect(Collectors.toCollection(JSONArray::new));
  }

  private JSONArray getRuleFailures(JSONObject item, ImageScanningResult imageResult, String policyName) {
    return item.getJSONArray("rules")
      .stream()
      .filter(rule -> ((JSONObject) rule).getInt("failuresCount") > 0)
      .map(rule -> {
        JSONArray results = new JSONArray();
        String ruleName = item.getString("name");
        String ruleString = getRuleString(((JSONObject) rule).getJSONArray("predicates"));
        boolean hasPkgVulnFailures = ((JSONObject) rule).has("pkgVulnFailures");
        boolean hasImageConfFailures = ((JSONObject) rule).has("imageConfFailures");

        if (hasPkgVulnFailures) {
          results = getPkgVulnFailures((JSONObject) rule, imageResult, policyName, ruleName, ruleString);
          return results;
        }
        if (hasImageConfFailures) {
          results = getImageConfFailures((JSONObject) rule, imageResult, policyName, ruleName, ruleString);
        }
        return results;
      })
      .flatMap(Collection::stream)
      .collect(Collectors.toCollection(JSONArray::new));
  }

  private JSONObject generateCompatibleGatesResult(ImageScanningResult imageResult) {
    JSONObject oldEngineResult = new JSONObject();
    String[] headersList = {
      "Image_Id",
      "Repo_Tag",
      "Trigger_Id",
      "Gate",
      "Trigger",
      "Check_Output",
      "Gate_Action",
      "Whitelisted",
      "Policy_Id",
      "Policy_Name"
    };

    JSONArray headers = JSONArray.fromObject(headersList);
    JSONArray gateList = imageResult.getGateResult().optJSONArray("list") != null ? imageResult.getGateResult().getJSONArray("list") : new JSONArray();
    JSONObject result = new JSONObject();

    JSONArray rows = gateList
      .stream()
      .filter(policy -> ((JSONObject) policy).getInt("failuresCount") > 0)
      .map(policy -> {
        String policyName = ((JSONObject) policy).getString("name");
        JSONArray bundles = ((JSONObject) policy).getJSONArray("bundle");

        return bundles
          .stream()
          .filter(item -> ((JSONObject) item).getInt("failuresCount") > 0)
          .map(item -> getRuleFailures(((JSONObject) item), imageResult, policyName))
          .flatMap(Collection::stream)
          .collect(Collectors.toCollection(JSONArray::new));
      })
      .flatMap(Collection::stream)
      .collect(Collectors.toCollection(JSONArray::new));

    result.put("header", headers);
    String finalAction = imageResult.getEvalStatus().equalsIgnoreCase("failed") ? "STOP" : "GO";
    result.put("final_action", finalAction);
    result.put("rows", rows);

    oldEngineResult.put("result", result);

    return oldEngineResult;
  }

  @Override
  protected JSONObject generateGatesSummary(JSONObject gatesJson) {
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
      if (logger.isDebugEnabled()) {
        logger.logDebug(gatesJson.toString());
      }

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
        logger.logWarn(String.format(
          "Either 'header' element has no columns or column indices (for Repo_Tag, Gate, Gate_Action) not initialized, skipping summary computation for %s", imageKey));
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
                  stop_wl += (whitelistedIndex != -1 && !(row.getString(whitelistedIndex).equalsIgnoreCase("none") || row.getString(whitelistedIndex).equalsIgnoreCase("false"))) ? 1 : 0;
                  break;
                case "warn":
                  warn++;
                  warn_wl += (whitelistedIndex != -1 && !(row.getString(whitelistedIndex).equalsIgnoreCase("none") || row.getString(whitelistedIndex).equalsIgnoreCase("false"))) ? 1 : 0;
                  break;
                case "go":
                  go++;
                  go_wl += (whitelistedIndex != -1 && !(row.getString(whitelistedIndex).equalsIgnoreCase("none") || row.getString(whitelistedIndex).equalsIgnoreCase("false"))) ? 1 : 0;
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
          logger.logInfo(String.format(
            "Policy evaluation summary for %s - stop: %d (+%d whitelisted), warn: %d (+%d whitelisted), go: %d (+%d whitelisted), final: %s",
            repoTag, stop - stop_wl, stop_wl, warn - warn_wl, warn_wl, go - go_wl, go_wl, result.getString("final_action")
          ));

          JSONObject summaryRow = new JSONObject();
          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Repo_Tag.toString(), repoTag);
          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Stop_Actions.toString(), (stop - stop_wl));
          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Warn_Actions.toString(), (warn - warn_wl));
          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Go_Actions.toString(), (go - go_wl));
          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Final_Action.toString(), result.getString("final_action"));
          summaryRows.add(summaryRow);
        } else {
          logger.logInfo(String.format(
            "Policy evaluation summary for %s - stop: %d (+%d whitelisted), warn: %d (+%d whitelisted), go: %d (+%d whitelisted), final: %s",
            imageKey, stop - stop_wl, stop_wl, warn - warn_wl, warn_wl, go - go_wl, go_wl, result.getString("final_action")
          ));

          JSONObject summaryRow = new JSONObject();
          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Repo_Tag.toString(), imageKey.toString());
          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Stop_Actions.toString(), (stop - stop_wl));
          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Warn_Actions.toString(), (warn - warn_wl));
          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Go_Actions.toString(), (go - go_wl));
          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Final_Action.toString(), result.getString("final_action"));
          summaryRows.add(summaryRow);

          // computation for " + imageKey);
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

  @Override
  protected JSONArray getVulnerabilitiesArray(String tag, JSONObject vulnsReport) {
    JSONArray dataJson = new JSONArray();
    JSONArray vulList = vulnsReport.getJSONArray("list");
    for (int i = 0; i < vulList.size(); i++) {
      JSONObject packageJson = vulList.getJSONObject(i);
      packageJson.getJSONArray("vulnerabilities").forEach(item -> {
        JSONObject vulnJson = (JSONObject) item;
        JSONArray vulnArray = new JSONArray();
        vulnArray.addAll(Arrays.asList
          (
            tag,
            vulnJson.getString("name"),
            vulnJson.getJSONObject("severity").getString("label"),
            packageJson.getString("name"),
            packageJson.get("suggestedFix") == JSONNull.getInstance() ? "None" : packageJson.getString("suggestedFix"),
            vulnJson.getJSONObject("severity").has("sourceUrl") ? vulnJson.getJSONObject("severity").getString("sourceUrl") : "",
            packageJson.getString("type"),
            packageJson.containsKey("packagePath") ? packageJson.get("packagePath") == JSONNull.getInstance() ? "N/A" : packageJson.getString("packagePath") : "N/A",
            vulnJson.getString("disclosureDate"),
            vulnJson.get("solutionDate") == JSONNull.getInstance() ? "None" : vulnJson.getString("solutionDate"))
        );
        dataJson.add(vulnArray);
      });
    }

    return dataJson;
  }

  private String getRuleString(JSONArray rule) {
    ArrayList<String> ruleResult = new ArrayList<>();

    for (Object p : rule.toArray()) {
      JSONObject predicate = (JSONObject) p;
      String type = predicate.getString("type");
      JSONObject extra = predicate.getJSONObject("extra");
      switch (type) {
        case "denyCVE":
          break;
        case "vulnSeverity":
          ruleResult.add(" Severity is " + extra.getString("level"));
          break;
        case "vulnIsFixable":
          ruleResult.add(" Fixable");
          break;
        case "vulnIsFixableWithAge":
          int days = extra.getInt("age");
          String period = " days";
          if (days < 2) {
            period = " day";
          }
          ruleResult.add(" Fixable since " + days + period);
          break;
        case "vulnExploitable":
          ruleResult.add(" Public Exploit available");
          break;
        case "vulnExploitableWithAge":
          days = extra.getInt("age");
          period = days < 2 ? " days" : " day";
          ruleResult.add(" Public Exploit available and age older than " + days + period);
          break;
        case "vulnAge":
          days = extra.getInt("age");
          period = " days";
          if (days < 2) {
            period = " day";
          }
          ruleResult.add(" Disclosure date older than " + days + period);
          break;
        case "vulnCVSS":
          double cvssScore = extra.getDouble("value");
          ruleResult.add(" CVSS Score higher or equal to %.1f" + cvssScore);
          break;
        case "vulnExploitableViaNetwork":
          ruleResult.add(" Exploitable via network attack");
          break;
        case "vulnExploitableNoUser":
          ruleResult.add("No user interaction required");
          break;
        case "vulnExploitableNoAdmin":
          ruleResult.add(" No administrative privileges required");
          break;
        case "imageConfigDefaultUserIsRoot":
          ruleResult.add(" User is root");
          break;
        case "imageConfigInstructionNotRecommended":
          ruleResult.add(" Use of ADD instruction found");
          break;
        case "imageConfigInstructionIsPkgManager":
          ruleResult.add(" Package manager instructions (eg. apk, npm, rpm, etc) are found");
          break;
        case "imageConfigSensitiveInformationAndSecrets":
          ruleResult.add(" Sensitive information and secrets in the image metadata are found");
          break;
        case "imageConfigDefaultUserIsNot":
          String user = extra.getString("user");
          ruleResult.add(" User is not " + user);
          break;
        case "imageConfigCreationDateWithAge":
          days = extra.has("age") ? extra.getInt("age") : 0;
          period = days < 2 ? " days" : " day";
          String daysString = days == 0 ? "N/A" : String.valueOf(days);
          ruleResult.add(" Creation date is older than or not specified " + daysString + period);
          break;
        case "imageConfigLabelExists":
        case "imageConfigLabelNotExists":
          String key = extra.getString("key");
          boolean isNotExist = type == "imageConfigLabelNotExists";
          if (isNotExist) {
            ruleResult.add(" Image label " + key + " does not exist");
          } else {
            ruleResult.add(" Image label " + key + " exists");
          }
          break;
        case "imageConfigLabelNotContains":
          String value = extra.getString("value");
          key = extra.getString("key");
          ruleResult.add(" Image label " + key + " does not exist or does not contain " + value);
          break;
        case "imageConfigEnvVariableNotExists":
        case "imageConfigEnvVariableExists":
          key = extra.getString("key");
          isNotExist = type == "imageConfigEnvVariableNotExists";
          if (isNotExist) {
            ruleResult.add(" Variable " + key + " does not exist");
          } else {
            ruleResult.add(" Variable " + key + " exist");
          }
          break;
        case "imageConfigEnvVariableContains":
          value = extra.getString("value");
          key = extra.getString("key");
          ruleResult.add(" Variable " + key + " exists and contains " + value);
          break;
        default:
          ruleResult.add(" ");
      }
    }

    return String.join(" AND ", ruleResult);
  }
}
