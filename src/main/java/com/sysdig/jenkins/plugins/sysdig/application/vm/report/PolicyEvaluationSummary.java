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
