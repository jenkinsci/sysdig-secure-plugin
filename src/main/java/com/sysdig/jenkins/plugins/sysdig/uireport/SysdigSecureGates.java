package com.sysdig.jenkins.plugins.sysdig.uireport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SysdigSecureGates {
  final Map<String, ArrayList<SysdigSecureGateResult>> resultsForEachImage;

  public SysdigSecureGates() {
    resultsForEachImage = new HashMap<>();
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
    resultsForEachImage.putIfAbsent(imageID, new ArrayList<>());
    resultsForEachImage
      .get(imageID)
      .add(new SysdigSecureGateResult(
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
}
