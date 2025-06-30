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

record JsonRule(
  Long ruleId,
  String ruleType,
  String failureType,
  String description,
  List<JsonFailure> failures,
  String evaluationResult,
  List<JsonPredicate> predicates
) {
  @Override
  public Long ruleId() {
    return Optional.ofNullable(ruleId).orElse(0L);
  }

  @Override
  public List<JsonFailure> failures() {
    return Optional.ofNullable(failures).orElse(List.of());
  }
}
