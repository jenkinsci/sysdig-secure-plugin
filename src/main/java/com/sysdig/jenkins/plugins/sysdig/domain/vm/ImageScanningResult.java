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
package com.sysdig.jenkins.plugins.sysdig.domain.vm;

import com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1beta3.Package;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1beta3.PolicyEvaluation;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Stream;

public class ImageScanningResult implements Serializable {
  public enum FinalAction {
    ActionPass,
    ActionFail;

    @Override
    public String toString() {
	    return switch (this) {
		    case ActionPass -> "PASS";
		    case ActionFail -> "FAIL";
      };
    }
  }

  private final String tag;
  private final String imageID;
  private final String evalStatus;
  private final List<Package> packages;
  private final List<PolicyEvaluation> evaluationPolicies;

  public ImageScanningResult(String tag, String imageID, String evalStatus, List<Package> packages, List<PolicyEvaluation> evaluationPolicies) {
    this.tag = tag;
    this.imageID = imageID;
    this.evalStatus = evalStatus;
    this.packages = packages;
    this.evaluationPolicies = evaluationPolicies;
  }

  public String getEvalStatus() {
    return evalStatus;
  }

  public List<Package> getPackages() {
    return packages;
  }

  public List<PolicyEvaluation> getEvaluationPolicies() {
    return evaluationPolicies;
  }

  public String getTag() {
    return tag;
  }

  public String getImageID() {
    return imageID;
  }

  public FinalAction getFinalAction() {
    if (Stream.of("pass", "passed", "accepted", "noPolicy").anyMatch(this.getEvalStatus()::equalsIgnoreCase)) {
      return FinalAction.ActionPass;
    }

    return FinalAction.ActionFail;
  }
}
