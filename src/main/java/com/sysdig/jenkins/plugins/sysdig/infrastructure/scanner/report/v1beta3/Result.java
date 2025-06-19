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
package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1beta3;

import com.sysdig.jenkins.plugins.sysdig.domain.vm.ImageScanningResult;

import java.io.Serializable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class Result implements Serializable {
  private String type;
  private Metadata metadata;
  private VulnTotalBySeverity vulnTotalBySeverity;
  private FixableVulnTotalBySeverity fixableVulnTotalBySeverity;
  private Long exploitsCount;
  private List<Package> packages;
  private List<Layer> layers;
  private List<PolicyEvaluation> policyEvaluations;
  private String policyEvaluationsResult;

  public Optional<String> getType() {
    return Optional.ofNullable(type);
  }

  public void setType(String type) {
    this.type = type;
  }

  public Optional<Metadata> getMetadata() {
    return Optional.ofNullable(metadata);
  }

  public void setMetadata(Metadata metadata) {
    this.metadata = metadata;
  }

  public Optional<VulnTotalBySeverity> getVulnTotalBySeverity() {
    return Optional.ofNullable(vulnTotalBySeverity);
  }

  public void setVulnTotalBySeverity(VulnTotalBySeverity vulnTotalBySeverity) {
    this.vulnTotalBySeverity = vulnTotalBySeverity;
  }

  public Optional<FixableVulnTotalBySeverity> getFixableVulnTotalBySeverity() {
    return Optional.ofNullable(fixableVulnTotalBySeverity);
  }

  public void setFixableVulnTotalBySeverity(FixableVulnTotalBySeverity fixableVulnTotalBySeverity) {
    this.fixableVulnTotalBySeverity = fixableVulnTotalBySeverity;
  }

  public Optional<Long> getExploitsCount() {
    return Optional.ofNullable(exploitsCount);
  }

  public void setExploitsCount(Long exploitsCount) {
    this.exploitsCount = exploitsCount;
  }

  public Optional<List<Package>> getPackages() {
    return Optional.ofNullable(packages);
  }

  public void setPackages(List<Package> packages) {
    this.packages = packages;
  }

  public Optional<List<Layer>> getLayers() {
    return Optional.ofNullable(layers);
  }

  public void setLayers(List<Layer> layers) {
    this.layers = layers;
  }

  public Optional<List<PolicyEvaluation>> getPolicyEvaluations() {
    return Optional.ofNullable(policyEvaluations);
  }

  public void setPolicyEvaluations(List<PolicyEvaluation> policyEvaluations) {
    this.policyEvaluations = policyEvaluations;
  }

  public Optional<String> getPolicyEvaluationsResult() {
    return Optional.ofNullable(policyEvaluationsResult);
  }

  public void setPolicyEvaluationsResult(String policyEvaluationsResult) {
    this.policyEvaluationsResult = policyEvaluationsResult;
  }


  public ImageScanningResult toImageScanningResult() {
    Metadata metadata = this.getMetadata()
      .orElseThrow(() -> new NoSuchElementException("metadata field not found in result"));

    final String tag = metadata.getPullString()
      .orElseThrow(() -> new NoSuchElementException("pull string not found in metadata"));
    final String imageID = metadata.getImageId()
      .orElseThrow(() -> new NoSuchElementException("imageid not found in metadata"));
    final String evalStatus = this.getPolicyEvaluationsResult()
      .orElseThrow(() -> new NoSuchElementException("policy evaluations result not found in result"));
    final List<Package> packages = this.getPackages()
      .orElseThrow(() -> new NoSuchElementException("packages not found in result"));
    final List<PolicyEvaluation> policies = this.getPolicyEvaluations()
      .orElseThrow(() -> new NoSuchElementException("policy evaluations not found in result"));

    return new ImageScanningResult(tag, imageID, evalStatus, packages, policies);
  }
}
