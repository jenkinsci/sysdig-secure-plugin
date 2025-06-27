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

import java.util.List;
import java.util.Optional;

class JsonResult {
  private String type;
  private JsonMetadata metadata;
  private Long exploitsCount;
  private List<JsonPackage> packages;
  private List<JsonLayer> layers;
  private List<JsonPolicyEvaluation> policyEvaluations;
  private String policyEvaluationsResult;
  private List<JsonAcceptedRisk> riskAcceptanceDefinitions;

  public Optional<String> getType() {
    return Optional.ofNullable(type);
  }

  public void setType(String type) {
    this.type = type;
  }

  public Optional<JsonMetadata> getMetadata() {
    return Optional.ofNullable(metadata);
  }

  public void setMetadata(JsonMetadata metadata) {
    this.metadata = metadata;
  }

  public Optional<Long> getExploitsCount() {
    return Optional.ofNullable(exploitsCount);
  }

  public void setExploitsCount(Long exploitsCount) {
    this.exploitsCount = exploitsCount;
  }

  public Optional<List<JsonPackage>> getPackages() {
    return Optional.ofNullable(packages);
  }

  public void setPackages(List<JsonPackage> packages) {
    this.packages = packages;
  }

  public Optional<List<JsonLayer>> getLayers() {
    return Optional.ofNullable(layers);
  }

  public void setLayers(List<JsonLayer> layers) {
    this.layers = layers;
  }

  public Optional<List<JsonPolicyEvaluation>> getPolicyEvaluations() {
    return Optional.ofNullable(policyEvaluations);
  }

  public void setPolicyEvaluations(List<JsonPolicyEvaluation> policyEvaluations) {
    this.policyEvaluations = policyEvaluations;
  }

  public Optional<String> getPolicyEvaluationsResult() {
    return Optional.ofNullable(policyEvaluationsResult);
  }

  public void setPolicyEvaluationsResult(String policyEvaluationsResult) {
    this.policyEvaluationsResult = policyEvaluationsResult;
  }

  public Optional<List<JsonAcceptedRisk>> getRiskAcceptanceDefinitions() {
    return Optional.ofNullable(riskAcceptanceDefinitions);
  }

  public void setRiskAcceptanceDefinitions(List<JsonAcceptedRisk> riskAcceptanceDefinitions) {
    this.riskAcceptanceDefinitions = riskAcceptanceDefinitions;
  }
}
