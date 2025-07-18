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
import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.Policy;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.ScanResult;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collection;
import java.util.stream.Stream;

public class PolicyReportProcessor implements ReportProcessor {
    private final SysdigLogger logger;

    public PolicyReportProcessor(SysdigLogger logger) {
        this.logger = logger;
    }

    @Override
    public PolicyEvaluationReport processPolicyEvaluation(ScanResult result) {
        Collection<Policy> evaluationPolicies = result.policies();
        logger.logDebug(String.format(
                "sysdig-secure-engine policies for '%s': %s ",
                result.metadata().pullString(), evaluationPolicies.toString()));
        return generatePolicyEvaluationReport(result);
    }

    private PolicyEvaluationReport generatePolicyEvaluationReport(ScanResult scanResult) {
        var result = new PolicyEvaluationReport(scanResult.evaluationResult().isFailed());

        Stream<PolicyEvaluationReportLine> rows = scanResult.policies().stream()
                .filter(policy -> policy.evaluationResult().isFailed())
                .flatMap(failedPolicy -> failedPolicy.bundles().stream()
                        .filter(bundle -> bundle.evaluationResult().isFailed())
                        .flatMap(failedBundle -> failedBundle.rules().stream()
                                .filter(rule -> rule.evaluationResult().isFailed())
                                .flatMap(failedRule -> failedRule.failures().stream()
                                        .map(failure -> new PolicyEvaluationReportLine(
                                                scanResult.metadata().imageID(),
                                                scanResult.metadata().pullString(),
                                                "trigger_id",
                                                failedBundle.name(),
                                                failedRule.description(),
                                                failure.description(),
                                                "STOP",
                                                false,
                                                failedPolicy.id(),
                                                failedPolicy.name())))));

        rows.forEach(result::addResult);

        return result;
    }

    @Override
    public PolicyEvaluationSummary generateGatesSummary(
            @NonNull PolicyEvaluationReport gatesJson, @NonNull ScanResult imageScanningResult) {
        logger.logDebug("Summarizing policy evaluation results");
        PolicyEvaluationSummary gateSummary = new PolicyEvaluationSummary();

        for (var imageKey : gatesJson.getResultsForEachImage().entrySet()) {
            int stop = 0, warn = 0, go = 0, stop_wl = 0, warn_wl = 0, go_wl = 0;
            for (PolicyEvaluationReportLine line : imageKey.getValue()) {
                switch (line.gateAction().toLowerCase()) {
                    case "stop":
                        stop++;
                        stop_wl += line.whitelisted() ? 1 : 0;
                        break;
                    case "warn":
                        warn++;
                        warn_wl += line.whitelisted() ? 1 : 0;
                        break;
                    case "go":
                        go++;
                        go_wl += line.whitelisted() ? 1 : 0;
                        break;
                    default:
                        break;
                }
            }

            var finalAction = gatesJson.isFailed() ? "STOP" : "GO";
            logger.logInfo(String.format(
                    "Policy evaluation summary for %s - stop: %d (+%d whitelisted), warn: %d (+%d whitelisted), go: %d (+%d whitelisted), final: %s",
                    imageScanningResult.metadata().pullString(),
                    stop - stop_wl,
                    stop_wl,
                    warn - warn_wl,
                    warn_wl,
                    go - go_wl,
                    go_wl,
                    finalAction));

            gateSummary.addSummaryLine(
                    imageScanningResult.metadata().pullString(),
                    (stop - stop_wl),
                    (warn - warn_wl),
                    (go - go_wl),
                    finalAction);
        }

        return gateSummary;
    }
}
