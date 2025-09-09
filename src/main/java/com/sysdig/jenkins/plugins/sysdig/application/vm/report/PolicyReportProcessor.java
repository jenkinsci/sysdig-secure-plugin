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
}
