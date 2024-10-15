package com.sysdig.jenkins.plugins.sysdig.uireport;

import com.sysdig.jenkins.plugins.sysdig.json.GsonBuilder;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.scanner.ImageScanningResult;
import com.sysdig.jenkins.plugins.sysdig.scanner.report.Bundle;
import com.sysdig.jenkins.plugins.sysdig.scanner.report.PolicyEvaluation;
import com.sysdig.jenkins.plugins.sysdig.scanner.report.Predicate;
import com.sysdig.jenkins.plugins.sysdig.scanner.report.Rule;
import hudson.FilePath;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class PolicyEvaluationReportProcessor {
  private final SysdigLogger logger;

  public PolicyEvaluationReportProcessor(SysdigLogger logger) {
    this.logger = logger;
  }

  public PolicyEvaluationSummary processPolicyEvaluation(ImageScanningResult result, FilePath jenkinsGatesOutputFP) throws IOException, InterruptedException {
    List<PolicyEvaluation> evaluationPolicies = result.getEvaluationPolicies();

    logger.logDebug(String.format("sysdig-secure-engine gate policies for '%s': %s ", result.getTag(), evaluationPolicies.toString()));
    logger.logDebug(String.format("Writing policy evaluation result to %s", jenkinsGatesOutputFP.getRemote()));

    PolicyEvaluationReport policyEvaluationReport = generatePolicyEvaluationReport(result);
    jenkinsGatesOutputFP.write(GsonBuilder.build().toJson(policyEvaluationReport), String.valueOf(StandardCharsets.UTF_8));

    return generateGatesSummary(policyEvaluationReport, result.getTag());
  }

  private PolicyEvaluationReport generatePolicyEvaluationReport(ImageScanningResult imageResult) {
    boolean failed = imageResult.getEvalStatus().equalsIgnoreCase("failed");
    var result = new PolicyEvaluationReport(failed);

    Stream<PolicyEvaluation> policyEvaluations = imageResult
      .getEvaluationPolicies()
      .stream();

    Stream<PolicyEvaluation> failedPolicyEvaluations = policyEvaluations
      .filter(policy -> policy.getEvaluationResult().orElse("").equals("failed"));

    Stream<PolicyEvaluationReportLine> rows = failedPolicyEvaluations
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


  protected PolicyEvaluationSummary generateGatesSummary(@Nonnull PolicyEvaluationReport gatesJson, String tag) {
    logger.logDebug("Summarizing policy evaluation results");
    PolicyEvaluationSummary gateSummary = new PolicyEvaluationSummary();

    for (var imageKey : gatesJson.getResultsForEachImage().entrySet()) {
      int stop = 0, warn = 0, go = 0, stop_wl = 0, warn_wl = 0, go_wl = 0;
      for (PolicyEvaluationReportLine line : imageKey.getValue()) {
        switch (line.getGateAction().toLowerCase()) {
          case "stop":
            stop++;
            stop_wl += line.getWhitelisted() ? 1 : 0;
            break;
          case "warn":
            warn++;
            warn_wl += line.getWhitelisted() ? 1 : 0;
            break;
          case "go":
            go++;
            go_wl += line.getWhitelisted() ? 1 : 0;
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

      gateSummary.addSummaryLine(tag, (stop - stop_wl), (warn - warn_wl), (go - go_wl), finalAction);
    }

    return gateSummary;
  }


  private Stream<PolicyEvaluationReportLine> getRuleFailures(Bundle bundle, ImageScanningResult imageResult, String policyName) {
    Stream<Rule> rules = bundle.getRules().orElseThrow()
      .stream();

    Stream<Rule> failedRules = rules
      .filter(rule -> rule.getEvaluationResult().orElse("").equals("failed"));

    Stream<PolicyEvaluationReportLine> failedRulesConvertedToSecureGateResults = failedRules
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


  private Stream<PolicyEvaluationReportLine> getPkgVulnFailures(Rule rule, ImageScanningResult imageResult, String policyName, String ruleName, String ruleString) {
    return rule.getFailures()
      .stream()
      .flatMap(Collection::stream)
      .map(failure ->
        getFailure(failure.getDescription().orElseThrow(), imageResult, policyName, ruleString, ruleName));
  }

  private Stream<PolicyEvaluationReportLine> getImageConfFailures(Rule rule, ImageScanningResult imageResult, String policyName, String ruleName, String ruleString) {
    return rule.getFailures()
      .stream()
      .flatMap(Collection::stream)
      .map(failure ->
        getFailure(failure.getRemediation().orElseThrow().replaceAll("(\r\n|\n)", "<br />"), imageResult, policyName, ruleString, ruleName));
  }

  private PolicyEvaluationReportLine getFailure(String failure, ImageScanningResult imageResult, String policyName, String ruleString, String ruleName) {
    return new PolicyEvaluationReportLine(
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