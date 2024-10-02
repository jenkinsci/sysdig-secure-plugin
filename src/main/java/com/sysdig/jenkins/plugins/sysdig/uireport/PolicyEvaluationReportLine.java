package com.sysdig.jenkins.plugins.sysdig.uireport;

public class PolicyEvaluationReportLine {
  private final String imageID;
  private final String repoTag;
  private final String triggerID;
  private final String gate;
  private final String trigger;
  private final String checkOutput;
  private final String gateAction;
  private final Boolean whitelisted;
  private final String policyID;
  private final String policyName;

  PolicyEvaluationReportLine(String imageID, String repoTag, String triggerID, String gate, String trigger, String checkOutput, String gateAction, Boolean whitelisted, String policyID, String policyName) {
    this.imageID = imageID;
    this.repoTag = repoTag;
    this.triggerID = triggerID;
    this.gate = gate;
    this.trigger = trigger;
    this.checkOutput = checkOutput;
    this.gateAction = gateAction;
    this.whitelisted = whitelisted;
    this.policyID = policyID;
    this.policyName = policyName;
  }

  public String getImageID() {
    return imageID;
  }

  public String getRepoTag() {
    return repoTag;
  }

  public String getTriggerID() {
    return triggerID;
  }

  public String getGate() {
    return gate;
  }

  public String getTrigger() {
    return trigger;
  }

  public String getCheckOutput() {
    return checkOutput;
  }

  public String getGateAction() {
    return gateAction;
  }

  public Boolean getWhitelisted() {
    return whitelisted;
  }

  public String getPolicyID() {
    return policyID;
  }

  public String getPolicyName() {
    return policyName;
  }
}
