package com.sysdig.jenkins.plugins.sysdig.uireport;

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
