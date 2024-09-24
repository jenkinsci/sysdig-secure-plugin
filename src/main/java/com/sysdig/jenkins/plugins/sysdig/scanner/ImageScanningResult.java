/*
Copyright (C) 2016-2020 Sysdig

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
package com.sysdig.jenkins.plugins.sysdig.scanner;

import com.sysdig.jenkins.plugins.sysdig.scanner.report.Package;
import com.sysdig.jenkins.plugins.sysdig.scanner.report.PolicyEvaluation;

import java.io.Serializable;
import java.util.List;

public class ImageScanningResult implements Serializable {
  private final String tag;
  private final String imageDigest;
  private final String evalStatus;
  private final List<Package> packages; // FIXME(fede) Change this to not export the raw JSON but a structure with relevant data.
  private final List<PolicyEvaluation> evaluationPolicies; // FIXME(fede) Change this to not export the raw JSON but a structure with relevant data.

  public ImageScanningResult(String tag, String imageDigest, String evalStatus, List<Package> vulnsReport, List<PolicyEvaluation> evaluationPolicies) {
    this.tag = tag;
    this.imageDigest = imageDigest;
    this.evalStatus = evalStatus;
    this.packages = vulnsReport;
    this.evaluationPolicies = evaluationPolicies;
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
}
