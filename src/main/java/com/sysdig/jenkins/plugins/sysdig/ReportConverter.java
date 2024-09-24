package com.sysdig.jenkins.plugins.sysdig;

import com.google.common.base.Strings;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.scanner.ImageScanningResult;
import com.sysdig.jenkins.plugins.sysdig.scanner.report.Package;
import com.sysdig.jenkins.plugins.sysdig.scanner.report.*;
import hudson.FilePath;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// FIXME(fede): Wow this is unmaintainable. We should not be handling jsons but report structures.
public class ReportConverter {
  private static final String LEGACY_PASSED_STATUS = "pass";
  private static final String PASSED_STATUS = "passed";
  private static final String ACCEPTED_STATUS = "accepted";
  private static final String NO_POLICY_STATUS = "noPolicy";

  protected final SysdigLogger logger;

  public ReportConverter(SysdigLogger logger) {
    this.logger = logger;
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

  public Util.GATE_ACTION getFinalAction(ImageScanningResult result) {
    String evalStatus = result.getEvalStatus();
    logger.logDebug(String.format("Get policy evaluation status for image '%s': %s", result.getTag(), evalStatus));

    if (Stream.of(LEGACY_PASSED_STATUS, PASSED_STATUS, ACCEPTED_STATUS, NO_POLICY_STATUS).anyMatch(evalStatus::equalsIgnoreCase)) {
      return Util.GATE_ACTION.PASS;
    }

    return Util.GATE_ACTION.FAIL;
  }

  public void processVulnerabilities(ImageScanningResult result, FilePath jenkinsQueryOutputFP) throws IOException, InterruptedException {


    JSONObject securityJson = new JSONObject();
    JSONArray columnsJson = new JSONArray();

    for (String column : Arrays.asList("Tag", "CVE ID", "Severity", "Vulnerability Package", "Fix Available", "URL", "Package Type", "Package Path", "Disclosure Date", "Solution Date")) {
      JSONObject columnJson = new JSONObject();
      columnJson.put("title", column);
      columnsJson.add(columnJson);
    }

    securityJson.put("columns", columnsJson);

    JSONArray dataJson = new JSONArray();
    dataJson.addAll(getVulnerabilitiesArray(result.getTag(), result.getVulnerabilityReport()));
    securityJson.put("data", dataJson);

    jenkinsQueryOutputFP.write(securityJson.toString(), String.valueOf(StandardCharsets.UTF_8));
  }

  public JSONObject processPolicyEvaluation(ImageScanningResult result, FilePath jenkinsGatesOutputFP) throws IOException, InterruptedException {
    List<PolicyEvaluation> gatePolicies = result.getEvaluationPolicies();

    logger.logDebug(String.format("sysdig-secure-engine gate policies for '%s': %s ", result.getTag(), gatePolicies.toString()));
    logger.logDebug(String.format("Writing policy evaluation result to %s", jenkinsGatesOutputFP.getRemote()));

    JSONObject fullGateResults = new JSONObject();
    fullGateResults.put(result.getImageDigest(), generateCompatibleGatesResult(result));
    jenkinsGatesOutputFP.write(fullGateResults.toString(), String.valueOf(StandardCharsets.UTF_8));

    return generateGatesSummary(fullGateResults, result.getTag());
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

  private JSONArray getPkgVulnFailures(Rule rule, ImageScanningResult imageResult, String policyName, String ruleName, String ruleString) {
    return rule.getFailures()
      .stream()
      .flatMap(Collection::stream)
      .map(failure ->
        getFailure(failure.getDescription().orElseThrow(), imageResult, policyName, ruleString, ruleName))
      .collect(Collectors.toCollection(JSONArray::new));
  }

  private JSONArray getImageConfFailures(Rule rule, ImageScanningResult imageResult, String policyName, String ruleName, String ruleString) {
    return rule.getFailures()
      .stream()
      .flatMap(Collection::stream)
      .map(failure ->
        getFailure(failure.getRemediation().orElseThrow().replaceAll("(\r\n|\n)", "<br />"), imageResult, policyName, ruleString, ruleName)
      ).collect(Collectors.toCollection(JSONArray::new));
  }

  private JSONArray getRuleFailures(Bundle item, ImageScanningResult imageResult, String policyName) {
    return item.getRules().orElseThrow()
      .stream()
      .filter(rule -> rule.getEvaluationResult().orElse("").equals("failed"))
      .map(rule -> {
        JSONArray results = new JSONArray();
        String ruleName = item.getName().orElseThrow();
        String ruleString = getRuleString(rule.getPredicates().orElseThrow());
        boolean hasPkgVulnFailures = rule.getFailureType().orElse("unknown").equals("pkgVulnFailure");
        boolean hasImageConfFailures = rule.getFailureType().orElse("unknown").equals("imageConfigFailure");

        if (hasPkgVulnFailures) {
          results = getPkgVulnFailures(rule, imageResult, policyName, ruleName, ruleString);
          return results;
        }
        if (hasImageConfFailures) {
          results = getImageConfFailures(rule, imageResult, policyName, ruleName, ruleString);
        }
        return results;
      })
      .flatMap(Collection::stream)
      .collect(Collectors.toCollection(JSONArray::new));
  }

  private JSONObject generateCompatibleGatesResult(ImageScanningResult imageResult) {
    JSONObject newEngineResult = new JSONObject();
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
    JSONObject result = new JSONObject();

    var gateList = imageResult.getEvaluationPolicies();


    JSONArray rows = gateList
      .stream()
      .filter(policy -> policy.getEvaluationResult().orElse("").equals("failed"))
      .map(policy -> {
        String policyName = policy.getName().orElseThrow();
        List<Bundle> bundles = policy.getBundles().orElseThrow();

        return bundles
          .stream()
          .map(item -> getRuleFailures(item, imageResult, policyName))
          .flatMap(Collection::stream)
          .collect(Collectors.toCollection(JSONArray::new));
      })
      .flatMap(Collection::stream)
      .collect(Collectors.toCollection(JSONArray::new));

    result.put("header", headers);
    String finalAction = imageResult.getEvalStatus().equalsIgnoreCase("failed") ? "STOP" : "GO";
    result.put("final_action", finalAction);
    result.put("rows", rows);

    newEngineResult.put("result", result);

    return newEngineResult;
  }

  protected JSONObject generateGatesSummary(JSONObject gatesJson, String tag) {
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

          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Repo_Tag.toString(), tag);
          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Stop_Actions.toString(), (stop - stop_wl));
          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Warn_Actions.toString(), (warn - warn_wl));
          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Go_Actions.toString(), (go - go_wl));
          summaryRow.put(Util.GATE_SUMMARY_COLUMN.Final_Action.toString(), result.getString("final_action"));
          summaryRows.add(summaryRow);
        }
      } else { // rows object not found
        logger.logWarn(String.format("'rows' element not found in gate output, skipping summary computation for %s", imageKey));
      }
    }

    gateSummary.put("header", generateDataTablesColumnsForGateSummary());
    gateSummary.put("rows", summaryRows);

    return gateSummary;
  }

  protected JSONArray getVulnerabilitiesArray(@Nonnull String tag, @Nonnull List<Package> vulList) {
    JSONArray dataJson = new JSONArray();

    for (Package packageJson : vulList) {
      packageJson.getVulns().orElseGet(List::of).forEach(vulnJson -> {
        JSONArray vulnArray = new JSONArray();
        vulnArray.addAll(Arrays.asList
          (
            tag,
            vulnJson.getName().orElseThrow(),
            vulnJson.getSeverity().orElseThrow().getValue().orElseThrow(),
            packageJson.getName().orElseThrow(),
            packageJson.getSuggestedFix().orElse("None"),
            vulnJson.getSeverity().orElseThrow().getSourceName().orElse(""),
            packageJson.getType().orElseThrow(),
            packageJson.getPath().orElse("N/A"),
            vulnJson.getDisclosureDate().orElseThrow(),
            vulnJson.getSolutionDate().orElse("None")
          )
        );
        dataJson.add(vulnArray);
      });

    }

    return dataJson;
  }

  private String getRuleString(List<Predicate> predicates) {
    ArrayList<String> ruleResult = new ArrayList<>();

    for (Predicate p : predicates) {
      String type = p.getType().orElseThrow();
      switch (type) {
        case "denyCVE":
          break;
        case "vulnSeverity":
          ruleResult.add("Severity greater than or equal " + p.getExtra().orElseThrow().getLevel().orElseThrow());
          break;
        case "vulnSeverityEquals":
          ruleResult.add("Severity equal " + p.getExtra().orElseThrow().getLevel().orElseThrow());
          break;
        case "vulnIsFixable":
          ruleResult.add("Fixable");
          break;
        case "vulnIsFixableWithAge":
          Long days = p.getExtra().orElseThrow().getAge().orElseThrow();
          String period = days < 2 ? " day" : " days";
          ruleResult.add("Fixable since " + days + period);
          break;
        case "vulnExploitable":
          ruleResult.add("Public Exploit available");
          break;
        case "vulnExploitableWithAge":
          days = p.getExtra().orElseThrow().getAge().orElseThrow();
          period = days < 2 ? " day" : " days";
          ruleResult.add("Public Exploit available since " + days + period);
          break;
        case "vulnAge":
          days = p.getExtra().orElseThrow().getAge().orElseThrow();
          period = days < 2 ? " day" : " days";
          ruleResult.add("Disclosure date older than or equal " + days + period);
          break;
        case "vulnCVSS":
          double cvssScore = Double.parseDouble(p.getExtra().orElseThrow().getValue().orElseThrow());
          ruleResult.add("CVSS Score greater than or equal to %.1f" + cvssScore);
          break;
        case "vulnExploitableViaNetwork":
          ruleResult.add("Network attack vector");
          break;
        case "vulnExploitableNoUser":
          ruleResult.add("No User interaction required");
          break;
        case "vulnExploitableNoAdmin":
          ruleResult.add("No administrative privileges required");
          break;
        case "imageConfigDefaultUserIsRoot":
          ruleResult.add("User is root");
          break;
        case "imageConfigDefaultUserIsNot":
          String user = p.getExtra().orElseThrow().getUser().orElseThrow();
          ;
          ruleResult.add("User is not " + user);
          break;
        case "imageConfigLabelExists":
          String key = p.getExtra().orElseThrow().getKey().orElseThrow();
          ruleResult.add("Image label " + key + " exists");
          break;
        case "imageConfigLabelNotExists":
          key = p.getExtra().orElseThrow().getKey().orElseThrow();
          ruleResult.add("Image label " + key + " does not exist");
          break;
        case "imageConfigEnvVariableExists":
          key = p.getExtra().orElseThrow().getKey().orElseThrow();
          ruleResult.add("Variable " + key + " exist");
          break;
        case "imageConfigEnvVariableNotExists":
          key = p.getExtra().orElseThrow().getKey().orElseThrow();
          ruleResult.add("Variable " + key + " does not exist");
          break;
        case "imageConfigEnvVariableContains":
          String value = p.getExtra().orElseThrow().getValue().orElseThrow();
          key = p.getExtra().orElseThrow().getKey().orElseThrow();
          ruleResult.add("Variable " + key + " contains value " + value);
          break;
        case "imageConfigLabelNotContains":
          value = p.getExtra().orElseThrow().getValue().orElseThrow();
          key = p.getExtra().orElseThrow().getKey().orElseThrow();
          ruleResult.add("Value " + value + " not found in label " + key);
          break;
        case "imageConfigCreationDateWithAge":
          days = p.getExtra().orElseThrow().getAge().orElseGet(() -> Long.valueOf(0));
          period = days < 2 ? " day" : " days";
          ruleResult.add("Image is older than " + days + period + " or Creation date is not present");
          break;
        case "imageConfigInstructionNotRecommended":
          ruleResult.add("Forbid the use of discouraged instructions");
          break;
        case "imageConfigInstructionIsPkgManager":
          ruleResult.add("Forbid the use of package manager instructions (eg. apk, npm, rpm, etc)");
          break;
        case "imageConfigSensitiveInformationAndSecrets":
          ruleResult.add("Forbid sensitive information and secrets in the image metadata");
          break;
        default:
          ruleResult.add(" ");
      }
    }

    return String.join(" AND ", ruleResult);
  }

}
