package com.sysdig.jenkins.plugins.sysdig.uireport;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SysdigSecureGates {
  private final Map<String, List<SysdigSecureGateResult>> resultsForEachImage;
  private final boolean failed;

  public SysdigSecureGates(boolean failed) {
    this.failed = failed;
    resultsForEachImage = new HashMap<>();
  }

  public Map<String, List<SysdigSecureGateResult>> getResultsForEachImage() {
    return resultsForEachImage;
  }

  public void addResult(@Nonnull SysdigSecureGateResult result) {
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
    this.addResult(new SysdigSecureGateResult(
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
