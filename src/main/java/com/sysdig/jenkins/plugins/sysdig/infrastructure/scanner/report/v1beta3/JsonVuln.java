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
import java.util.Map;
import java.util.Optional;

record JsonVuln(
  String name,
  JsonSeverity severity,
  String disclosureDate,
  String solutionDate,
  boolean exploitable,
  List<JsonAcceptedRiskReference> acceptedRisks,
  String fixedInVersion,
  Map<String, String> annotations
) {
  public Optional<String> optSolutionDate() {
    return Optional.ofNullable(solutionDate);
  }

  public Optional<String> optFixedInVersion() {
    return Optional.ofNullable(fixedInVersion);
  }

  @Override
  public List<JsonAcceptedRiskReference> acceptedRisks() {
    return Optional.ofNullable(acceptedRisks).orElse(List.of());
  }
}
