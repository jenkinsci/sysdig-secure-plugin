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

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public class Rule implements Serializable {
  private String failureType;
  private List<Failure> failures;
  private String evaluationResult;
  private List<Predicate> predicates;

  public Optional<String> getFailureType() {
    return Optional.ofNullable(failureType);
  }

  public void setFailureType(String failureType) {
    this.failureType = failureType;
  }

  public Optional<List<Failure>> getFailures() {
    return Optional.ofNullable(failures);
  }

  public void setFailures(List<Failure> failures) {
    this.failures = failures;
  }

  public Optional<String> getEvaluationResult() {
    return Optional.ofNullable(evaluationResult);
  }

  public void setEvaluationResult(String evaluationResult) {
    this.evaluationResult = evaluationResult;
  }

  public Optional<List<Predicate>> getPredicates() {
    return Optional.ofNullable(predicates);
  }

  public void setPredicates(List<Predicate> predicates) {
    this.predicates = predicates;
  }
}
