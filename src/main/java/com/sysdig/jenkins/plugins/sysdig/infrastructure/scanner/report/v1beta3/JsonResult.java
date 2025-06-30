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

record JsonResult(
  String type,
  JsonMetadata metadata,
  Long exploitsCount,
  List<JsonPackage> packages,
  List<JsonLayer> layers,
  List<JsonPolicyEvaluation> policyEvaluations,
  String policyEvaluationsResult,
  List<JsonAcceptedRisk> riskAcceptanceDefinitions
) {
  @Override
  public List<JsonPackage> packages() {
    return Optional.ofNullable(packages).orElse(List.of());
  }

  @Override
  public List<JsonLayer> layers() {
    return Optional.ofNullable(layers).orElse(List.of());
  }

  @Override
  public List<JsonPolicyEvaluation> policyEvaluations() {
    return Optional.ofNullable(policyEvaluations).orElse(List.of());
  }

  @Override
  public List<JsonAcceptedRisk> riskAcceptanceDefinitions() {
    return Optional.ofNullable(riskAcceptanceDefinitions).orElse(List.of());
  }
}
