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
package com.sysdig.jenkins.plugins.sysdig.domain.vm.report;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public class Rule implements Serializable {
  private String ruleType;
  private String failureType;
  private String description;
  private List<Failure> failures;
  private String evaluationResult;
  private List<Predicate> predicates;

  public Optional<String> getRuleType() {
    return Optional.ofNullable(ruleType);
  }

  public void setRuleType(String ruleType) {
    this.ruleType = ruleType;
  }

  public Optional<String> getFailureType() {
    return Optional.ofNullable(failureType);
  }

  public void setFailureType(String failureType) {
    this.failureType = failureType;
  }

  public Optional<String> getDescription() {
    return Optional.ofNullable(description);
  }

  public void setDescription(String description) {
    this.description = description;
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
