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

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PolicyEvaluationReport {
    private final Map<String, List<PolicyEvaluationReportLine>> resultsForEachImage;
    private final boolean failed;

    public PolicyEvaluationReport(boolean failed) {
        this.failed = failed;
        resultsForEachImage = new HashMap<>();
    }

    public Map<String, List<PolicyEvaluationReportLine>> getResultsForEachImage() {
        return resultsForEachImage;
    }

    public void addResult(@NonNull PolicyEvaluationReportLine result) {
        resultsForEachImage.putIfAbsent(result.imageID(), new ArrayList<>());
        resultsForEachImage.get(result.imageID()).add(result);
    }

    public boolean isFailed() {
        return failed;
    }

    public boolean isPassed() {
        return !isFailed();
    }
}
