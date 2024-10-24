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

import com.sysdig.jenkins.plugins.sysdig.domain.vm.report.Package;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.report.PolicyEvaluation;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.report.Result;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Stream;

public class ImageScanningResult implements Serializable {
  public enum FinalAction {
    ActionPass,
    ActionFail;

    @Override
    public String toString() {
      switch (this) {
        case ActionPass:
          return "PASS";
        case ActionFail:
          return "FAIL";
        default:
          return super.toString();
      }
    }
  }

  private final String tag;
  private final String imageDigest;
  private final String evalStatus;
  private final List<Package> packages;
  private final List<PolicyEvaluation> evaluationPolicies;

  public ImageScanningResult(String tag, String imageDigest, String evalStatus, List<Package> vulnsReport, List<PolicyEvaluation> evaluationPolicies) {
    this.tag = tag;
    this.imageDigest = imageDigest;
    this.evalStatus = evalStatus;
    this.packages = vulnsReport;
    this.evaluationPolicies = evaluationPolicies;
  }

  public static ImageScanningResult fromReportResult(Result result) {
    final String tag = result.getMetadata().orElseThrow().getPullString().orElseThrow();
    final String imageDigest = result.getMetadata().orElseThrow().getDigest().orElseThrow();
    final String evalStatus = result.getPolicyEvaluationsResult().orElseThrow();
    final List<Package> packages = result.getPackages().orElseThrow();
    final List<PolicyEvaluation> policies = result.getPolicyEvaluations().orElseThrow();

    return new ImageScanningResult(tag, imageDigest, evalStatus, packages, policies);
  }

  public String getEvalStatus() {
    return evalStatus;
  }

  public List<Package> getVulnerabilityReport() {
    return packages;
  }

  public List<PolicyEvaluation> getEvaluationPolicies() {
    return evaluationPolicies;
  }

  public String getTag() {
    return tag;
  }

  public String getImageDigest() {
    return imageDigest;
  }


  public FinalAction getFinalAction() {
    if (Stream.of("pass", "passed", "accepted", "noPolicy").anyMatch(this.getEvalStatus()::equalsIgnoreCase)) {
      return FinalAction.ActionPass;
    }

    return FinalAction.ActionFail;
  }
}
