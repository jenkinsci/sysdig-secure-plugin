package com.sysdig.jenkins.plugins.sysdig.uireport;

public class PolicyEvaluationSummaryLine {
  private final String imageTag;
  private final int nonWhitelistedStopActions;
  private final int nonWhitelistedWarnActions;
  private final int nonWhitelistedGoActions;
  private final String finalAction;

  public PolicyEvaluationSummaryLine(String imageTag, int nonWhitelistedStopActions, int nonWhitelistedWarnActions, int nonWhitelistedGoActions, String finalAction) {
    this.imageTag = imageTag;
    this.nonWhitelistedStopActions = nonWhitelistedStopActions;
    this.nonWhitelistedWarnActions = nonWhitelistedWarnActions;
    this.nonWhitelistedGoActions = nonWhitelistedGoActions;
    this.finalAction = finalAction;
  }

  public String getImageTag() {
    return imageTag;
  }

  public int getNonWhitelistedStopActions() {
    return nonWhitelistedStopActions;
  }

  public int getNonWhitelistedWarnActions() {
    return nonWhitelistedWarnActions;
  }

  public int getNonWhitelistedGoActions() {
    return nonWhitelistedGoActions;
  }

  public String getFinalAction() {
    return finalAction;
  }
}
