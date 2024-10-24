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

import javax.annotation.Nonnull;
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

  public void addResult(@Nonnull PolicyEvaluationReportLine result) {
    resultsForEachImage.putIfAbsent(result.getImageID(), new ArrayList<>());
    resultsForEachImage
      .get(result.getImageID())
      .add(result);
  }

  public void addResult(
    String imageID,
    String repoTag,
    String triggerID,
    String gate,
    String trigger,
    String checkOutput,
    String gateAction,
    Boolean whitelisted,
    String policyID,
    String policyName
  ) {
    this.addResult(new PolicyEvaluationReportLine(
      imageID,
      repoTag,
      triggerID,
      gate,
      trigger,
      checkOutput,
      gateAction,
      whitelisted,
      policyID,
      policyName
    ));
  }

  public boolean isFailed() {
    return failed;
  }
}
