package com.sysdig.jenkins.plugins.sysdig.uireport;

import com.sysdig.jenkins.plugins.sysdig.Util;
import com.sysdig.jenkins.plugins.sysdig.json.GsonBuilder;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.scanner.ImageScanningResult;
import com.sysdig.jenkins.plugins.sysdig.scanner.report.Bundle;
import com.sysdig.jenkins.plugins.sysdig.scanner.report.PolicyEvaluation;
import com.sysdig.jenkins.plugins.sysdig.scanner.report.Predicate;
import com.sysdig.jenkins.plugins.sysdig.scanner.report.Rule;
import hudson.FilePath;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class PolicyReport {
  private final SysdigLogger logger;

  public PolicyReport(SysdigLogger logger) {
    this.logger = logger;
  }

  public JSONObject processPolicyEvaluation(ImageScanningResult result, FilePath jenkinsGatesOutputFP) throws IOException, InterruptedException {
    List<PolicyEvaluation> gatePolicies = result.getEvaluationPolicies();

    logger.logDebug(String.format("sysdig-secure-engine gate policies for '%s': %s ", result.getTag(), gatePolicies.toString()));
    logger.logDebug(String.format("Writing policy evaluation result to %s", jenkinsGatesOutputFP.getRemote()));

    SysdigSecureGates fullGateResults = generateCompatibleGatesResult(result);
    jenkinsGatesOutputFP.write(GsonBuilder.build().toJson(fullGateResults), String.valueOf(StandardCharsets.UTF_8));

    return generateGatesSummary(fullGateResults, result.getTag());
  }

  private SysdigSecureGates generateCompatibleGatesResult(ImageScanningResult imageResult) {
    boolean failed = imageResult.getEvalStatus().equalsIgnoreCase("failed");
    var result = new SysdigSecureGates(failed);

    Stream<PolicyEvaluation> policyEvaluations = imageResult
      .getEvaluationPolicies()
      .stream();

    Stream<PolicyEvaluation> failedPolicyEvaluations = policyEvaluations
      .filter(policy -> policy.getEvaluationResult().orElse("").equals("failed"));

    Stream<SysdigSecureGateResult> rows = failedPolicyEvaluations
      .flatMap(policy -> {
        String policyName = policy.getName().orElseThrow();
        Stream<Bundle> bundlesFromThePolicy = policy.getBundles()
          .stream()
          .flatMap(Collection::stream);

        return bundlesFromThePolicy
          .flatMap(bundle -> getRuleFailures(bundle, imageResult, policyName));
      });

    rows.forEach(result::addResult);

    return result;
  }


  protected JSONObject generateGatesSummary(SysdigSecureGates gatesJson, String tag) {
    logger.logDebug("Summarizing policy evaluation results");
    JSONObject gateSummary = new JSONObject();

    if (gatesJson == null) { // could not load gates output to json object
      logger.logWarn("Invalid input to generate gates summary");
      return gateSummary;
    }

    JSONArray summaryRows = new JSONArray();
    for (var imageKey : gatesJson.getResultsForEachImage().entrySet()) {
      if (logger.isDebugEnabled()) {
        logger.logDebug(gatesJson.toString());
      }

      List<SysdigSecureGateResult> rows = imageKey.getValue();

      int stop = 0, warn = 0, go = 0, stop_wl = 0, warn_wl = 0, go_wl = 0;
      String imageDigest = imageKey.getKey();

      for (SysdigSecureGateResult row : rows) {
        switch (row.getGateAction().toLowerCase()) {
          case "stop":
            stop++;
            stop_wl += row.getWhitelisted() ? 1 : 0;
            break;
          case "warn":
            warn++;
            warn_wl += row.getWhitelisted() ? 1 : 0;
            break;
          case "go":
            go++;
            go_wl += row.getWhitelisted() ? 1 : 0;
            break;
          default:
            break;
        }

      }


      var finalAction = gatesJson.isFailed() ? "STOP" : "GO";
      logger.logInfo(String.format(
        "Policy evaluation summary for %s - stop: %d (+%d whitelisted), warn: %d (+%d whitelisted), go: %d (+%d whitelisted), final: %s",
        tag, stop - stop_wl, stop_wl, warn - warn_wl, warn_wl, go - go_wl, go_wl, finalAction
      ));

      JSONObject summaryRow = new JSONObject();
      summaryRow.put(Util.GATE_SUMMARY_COLUMN.Repo_Tag.toString(), tag);
      summaryRow.put(Util.GATE_SUMMARY_COLUMN.Stop_Actions.toString(), (stop - stop_wl));
      summaryRow.put(Util.GATE_SUMMARY_COLUMN.Warn_Actions.toString(), (warn - warn_wl));
      summaryRow.put(Util.GATE_SUMMARY_COLUMN.Go_Actions.toString(), (go - go_wl));
      summaryRow.put(Util.GATE_SUMMARY_COLUMN.Final_Action.toString(), finalAction);
      summaryRows.add(summaryRow);
    }

    gateSummary.put("header", generateDataTablesColumnsForGateSummary());
    gateSummary.put("rows", summaryRows);

    return gateSummary;
  }


  private Stream<SysdigSecureGateResult> getRuleFailures(Bundle bundle, ImageScanningResult imageResult, String policyName) {
    Stream<Rule> rules = bundle.getRules().orElseThrow()
      .stream();

    Stream<Rule> failedRules = rules
      .filter(rule -> rule.getEvaluationResult().orElse("").equals("failed"));

    Stream<SysdigSecureGateResult> failedRulesConvertedToSecureGateResults = failedRules
      .flatMap(rule -> {
        String ruleName = bundle.getName().orElseThrow();
        String ruleString = getRuleString(rule.getPredicates().orElseThrow());
        boolean hasPkgVulnFailures = rule.getFailureType().orElse("unknown").equals("pkgVulnFailure");

        return hasPkgVulnFailures ?
          getPkgVulnFailures(rule, imageResult, policyName, ruleName, ruleString) :
          getImageConfFailures(rule, imageResult, policyName, ruleName, ruleString);
      });

    return failedRulesConvertedToSecureGateResults;
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


  private Stream<SysdigSecureGateResult> getPkgVulnFailures(Rule rule, ImageScanningResult imageResult, String policyName, String ruleName, String ruleString) {
    return rule.getFailures()
      .stream()
      .flatMap(Collection::stream)
      .map(failure ->
        getFailure(failure.getDescription().orElseThrow(), imageResult, policyName, ruleString, ruleName));
  }

  private Stream<SysdigSecureGateResult> getImageConfFailures(Rule rule, ImageScanningResult imageResult, String policyName, String ruleName, String ruleString) {
    return rule.getFailures()
      .stream()
      .flatMap(Collection::stream)
      .map(failure ->
        getFailure(failure.getRemediation().orElseThrow().replaceAll("(\r\n|\n)", "<br />"), imageResult, policyName, ruleString, ruleName));
  }

  private SysdigSecureGateResult getFailure(String failure, ImageScanningResult imageResult, String policyName, String ruleString, String ruleName) {
    return new SysdigSecureGateResult(
      imageResult.getImageDigest(),
      imageResult.getTag(),
      "trigger_id",
      ruleName,
      ruleString,
      failure,
      "STOP",
      false,
      "",
      policyName
    );
  }
}
