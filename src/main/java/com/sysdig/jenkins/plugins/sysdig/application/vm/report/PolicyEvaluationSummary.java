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
import java.util.List;

public class PolicyEvaluationSummary {
  private final ArrayList<PolicyEvaluationSummaryLine> lines;

  public PolicyEvaluationSummary() {
    lines = new ArrayList<>();
  }

  public void addSummaryLine(@Nonnull PolicyEvaluationSummaryLine line) {
    this.lines.add(line);
  }

  public void addSummaryLine(String imageTag, int nonWhitelistedStopActions, int nonWhitelistedWarnActions, int nonWhitelistedGoActions, String finalAction) {
    addSummaryLine(new PolicyEvaluationSummaryLine(
      imageTag,
      nonWhitelistedStopActions,
      nonWhitelistedWarnActions,
      nonWhitelistedGoActions,
      finalAction
    ));
  }

  public List<PolicyEvaluationSummaryLine> getLines() {
    return lines;
  }
}
