/*
Copyright (C) 2016-2024 Sysdig

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
package com.sysdig.jenkins.plugins.sysdig.application.vm.report;

import com.sysdig.jenkins.plugins.sysdig.application.vm.ReportProcessor;
import com.sysdig.jenkins.plugins.sysdig.domain.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.ImageScanningResult;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1beta3.Bundle;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1beta3.PolicyEvaluation;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1beta3.Predicate;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1beta3.Rule;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

public class PolicyReportProcessor implements ReportProcessor {
  private final SysdigLogger logger;

  public PolicyReportProcessor(SysdigLogger logger) {
    this.logger = logger;
  }

  @Override
  public PolicyEvaluationReport processPolicyEvaluation(ImageScanningResult result) {
    List<PolicyEvaluation> evaluationPolicies = result.getEvaluationPolicies();
    logger.logDebug(String.format("sysdig-secure-engine gate policies for '%s': %s ", result.getTag(), evaluationPolicies.toString()));
    return generatePolicyEvaluationReport(result);
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
        String policyName = policy.getName().orElseThrow(()-> new NoSuchElementException("name not found in policy"));
        Stream<Bundle> bundlesFromThePolicy = policy.getBundles()
          .stream()
          .flatMap(Collection::stream);

        return bundlesFromThePolicy
          .flatMap(bundle -> getRuleFailures(bundle, imageResult, policyName));
      });

    rows.forEach(result::addResult);

    return result;
  }


  @Override
  public PolicyEvaluationSummary generateGatesSummary(@NonNull PolicyEvaluationReport gatesJson, @NonNull ImageScanningResult imageScanningResult) {
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
        imageScanningResult.getTag(), stop - stop_wl, stop_wl, warn - warn_wl, warn_wl, go - go_wl, go_wl, finalAction
      ));

      gateSummary.addSummaryLine(imageScanningResult.getTag(), (stop - stop_wl), (warn - warn_wl), (go - go_wl), finalAction);
    }

    return gateSummary;
  }


  private Stream<PolicyEvaluationReportLine> getRuleFailures(Bundle bundle, ImageScanningResult imageResult, String policyName) {
    Stream<Rule> rules = bundle.getRules().orElseThrow(()-> new NoSuchElementException("rules not found in rules bundle"))
      .stream();

    Stream<Rule> failedRules = rules
      .filter(rule -> rule.getEvaluationResult().orElse("").equals("failed"));

    Stream<PolicyEvaluationReportLine> failedRulesConvertedToSecureGateResults = failedRules
      .flatMap(rule -> {
        String ruleName = bundle.getName().orElseThrow(()-> new NoSuchElementException("name not found"));
        String ruleString = getRuleString(rule.getPredicates().orElseThrow(()-> new NoSuchElementException("predicates not found in rule")));
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
      String type = p.getType().orElseThrow(()-> new NoSuchElementException("type not found in the predicate"));
      switch (type) {
        case "denyCVE":
          break;
        case "vulnSeverity":
          ruleResult.add("Severity greater than or equal " + p.getExtra().orElseThrow(()-> new NoSuchElementException("extra field not found in predicate")).getLevel().orElseThrow(()-> new NoSuchElementException("level field not found in extra field in predicates")));
          break;
        case "vulnSeverityEquals":
          ruleResult.add("Severity equal " + p.getExtra().orElseThrow(()-> new NoSuchElementException("extra feld not found in predicate")).getLevel().orElseThrow(()-> new NoSuchElementException("level not found in extra field in predicate")));
          break;
        case "vulnIsFixable":
          ruleResult.add("Fixable");
          break;
        case "vulnIsFixableWithAge":
          Long days = p.getExtra().orElseThrow(()-> new NoSuchElementException("extra field not found in predicate")).getAge().orElseThrow(()-> new NoSuchElementException("age not found in extra field in predicate"));
          String period = days < 2 ? " day" : " days";
          ruleResult.add("Fixable since " + days + period);
          break;
        case "vulnExploitable":
          ruleResult.add("Public Exploit available");
          break;
        case "vulnExploitableWithAge":
          days = p.getExtra().orElseThrow(()-> new NoSuchElementException("extra field not found in predicate")).getAge().orElseThrow(()-> new NoSuchElementException("age not found in extra field in predicate"));
          period = days < 2 ? " day" : " days";
          ruleResult.add("Public Exploit available since " + days + period);
          break;
        case "vulnAge":
          days = p.getExtra().orElseThrow(()-> new NoSuchElementException("extra field not found in predicate")).getAge().orElseThrow(()-> new NoSuchElementException("age not found in extra field in predicate"));
          period = days < 2 ? " day" : " days";
          ruleResult.add("Disclosure date older than or equal " + days + period);
          break;
        case "vulnCVSS":
          double cvssScore = Double.parseDouble(p.getExtra().orElseThrow(()-> new NoSuchElementException("extra field not found in predicate")).getValue().orElseThrow(()-> new NoSuchElementException("value not found in extra field in predicate")));
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

          String user = p.getExtra().orElseThrow(()-> new NoSuchElementException("extra field not found in predicate")).getUser().orElseThrow(()-> new NoSuchElementException("user not found in extra field in predicate"));
          ruleResult.add("User is not " + user);
          break;
        case "imageConfigLabelExists":
          String key = p.getExtra().orElseThrow(()-> new NoSuchElementException("extra field not found in predicate")).getKey().orElseThrow(()-> new NoSuchElementException("key not found in extra field in predicate"));
          ruleResult.add("Image label " + key + " exists");
          break;
        case "imageConfigLabelNotExists":
          key = p.getExtra().orElseThrow(()-> new NoSuchElementException("extra field not found in predicate")).getKey().orElseThrow(()-> new NoSuchElementException("key not found in extra field in predicate"));
          ruleResult.add("Image label " + key + " does not exist");
          break;
        case "imageConfigEnvVariableExists":
          key = p.getExtra().orElseThrow(()-> new NoSuchElementException("extra field not found in predicate")).getKey().orElseThrow(()-> new NoSuchElementException("key not found in extra field in predicate"));
          ruleResult.add("Variable " + key + " exist");
          break;
        case "imageConfigEnvVariableNotExists":
          key = p.getExtra().orElseThrow(()-> new NoSuchElementException("extra field not found in predicate")).getKey().orElseThrow(()-> new NoSuchElementException("key not found in extra field in predicate"));
          ruleResult.add("Variable " + key + " does not exist");
          break;
        case "imageConfigEnvVariableContains":
          String value = p.getExtra().orElseThrow(()-> new NoSuchElementException("extra field not found in predicate")).getValue().orElseThrow(()-> new NoSuchElementException("value not found in extra field in predicate"));
          key = p.getExtra().orElseThrow(()-> new NoSuchElementException("extra field not found in predicate")).getKey().orElseThrow(()-> new NoSuchElementException("key not found in extra field in predicate"));
          ruleResult.add("Variable " + key + " contains value " + value);
          break;
        case "imageConfigLabelNotContains":
          value = p.getExtra().orElseThrow(()-> new NoSuchElementException("extra field not found in predicate")).getValue().orElseThrow(()-> new NoSuchElementException("value not found in extra field in predicate"));
          key = p.getExtra().orElseThrow(()-> new NoSuchElementException("extra field not found in predicate")).getKey().orElseThrow(()-> new NoSuchElementException("key not found in extra field in predicate"));
          ruleResult.add("Value " + value + " not found in label " + key);
          break;
        case "imageConfigCreationDateWithAge":
          days = p.getExtra().orElseThrow(()-> new NoSuchElementException("extra field not found in predicate")).getAge().orElseGet(() -> Long.valueOf(0));
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
        getFailure(failure.getDescription().orElseThrow(()-> new NoSuchElementException("description not found in failure")), imageResult, policyName, ruleString, ruleName));
  }

  private Stream<PolicyEvaluationReportLine> getImageConfFailures(Rule rule, ImageScanningResult imageResult, String policyName, String ruleName, String ruleString) {
    return rule.getFailures()
      .stream()
      .flatMap(Collection::stream)
      .map(failure ->
        getFailure(failure.getRemediation().orElseThrow(()-> new NoSuchElementException("remediation not found in failure")).replaceAll("(\r\n|\n)", "<br />"), imageResult, policyName, ruleString, ruleName));
  }

  private PolicyEvaluationReportLine getFailure(String failure, ImageScanningResult imageResult, String policyName, String ruleString, String ruleName) {
    return new PolicyEvaluationReportLine(
      imageResult.getImageID(),
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
