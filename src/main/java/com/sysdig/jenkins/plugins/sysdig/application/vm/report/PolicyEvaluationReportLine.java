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

import java.util.Objects;

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

  public PolicyEvaluationReportLine(String imageID, String repoTag, String triggerID, String gate, String trigger, String checkOutput, String gateAction, Boolean whitelisted, String policyID, String policyName) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PolicyEvaluationReportLine)) return false;
    PolicyEvaluationReportLine line = (PolicyEvaluationReportLine) o;
    return Objects.equals(imageID, line.imageID) &&
      Objects.equals(repoTag, line.repoTag) &&
      Objects.equals(triggerID, line.triggerID) &&
      Objects.equals(gate, line.gate) &&
      Objects.equals(trigger, line.trigger) &&
      Objects.equals(checkOutput, line.checkOutput) &&
      Objects.equals(gateAction, line.gateAction) &&
      Objects.equals(whitelisted, line.whitelisted) &&
      Objects.equals(policyID, line.policyID) &&
      Objects.equals(policyName, line.policyName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(imageID, repoTag, triggerID, gate, trigger, checkOutput, gateAction, whitelisted, policyID, policyName);
  }
}
