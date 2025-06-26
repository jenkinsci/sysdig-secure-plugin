package com.sysdig.jenkins.plugins.sysdig.domain.vm.report;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class PolicyBundleRule implements Serializable {
  private final String id;
  private final String description;
  private final EvaluationResult evaluationResult;
  private final PolicyBundle parent;
  private final List<PolicyBundleRuleFailure> failures;

  PolicyBundleRule(
    String id,
    String description,
    EvaluationResult evaluationResult,
    PolicyBundle parent
  ) {
    this.id = id;
    this.description = description;
    this.evaluationResult = evaluationResult;
    this.parent = parent;
    this.failures = new ArrayList<>();
  }

  public String id() {
    return id;
  }

  public String description() {
    return description;
  }

  public EvaluationResult evaluationResult() {
    return evaluationResult;
  }

  public PolicyBundle parent() {
    return parent;
  }

  public PolicyBundleRuleImageConfigFailure addImageConfigFailure(String remediation) {
    PolicyBundleRuleImageConfigFailure failure = new PolicyBundleRuleImageConfigFailure(remediation, this);
    failures.add(failure);
    return failure;
  }

  public PolicyBundleRulePkgVulnFailure addPkgVulnFailure(String description) {
    PolicyBundleRulePkgVulnFailure failure = new PolicyBundleRulePkgVulnFailure(description, this);
    failures.add(failure);
    return failure;
  }

  public List<PolicyBundleRuleFailure> failures() {
    return Collections.unmodifiableList(failures);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    PolicyBundleRule that = (PolicyBundleRule) o;
    return Objects.equals(id, that.id) && Objects.equals(description, that.description) && evaluationResult == that.evaluationResult;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, description, evaluationResult);
  }
}
