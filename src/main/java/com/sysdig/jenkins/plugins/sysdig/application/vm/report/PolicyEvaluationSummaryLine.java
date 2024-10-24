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
