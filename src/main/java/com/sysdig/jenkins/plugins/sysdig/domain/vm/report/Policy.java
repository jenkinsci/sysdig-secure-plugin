package com.sysdig.jenkins.plugins.sysdig.domain.vm.report;

import com.sysdig.jenkins.plugins.sysdig.domain.AggregateChild;

import java.util.*;

public class Policy implements AggregateChild<ScanResult> {
  private final String id;
  private final String name;
  private final PolicyType type;
  private final Date createdAt;
  private final Date updatedAt;
  private final ScanResult root;
  private final Set<PolicyBundle> bundles;

  public Policy(String id, String name, PolicyType type, Date createdAt, Date updatedAt, ScanResult root) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.bundles = new HashSet<>();
    this.root = root;
  }

  public String id() {
    return id;
  }

  public String name() {
    return name;
  }

  public PolicyType type() {
    return type;
  }

  public Date createdAt() {
    return createdAt;
  }

  public Date updatedAt() {
    return updatedAt;
  }

  @Override
  public ScanResult root() {
    return root;
  }

  void addBundle(PolicyBundle policyBundle) {
    if (this.bundles.add(policyBundle)) {
      policyBundle.addPolicy(this);
    }
  }

  public Set<PolicyBundle> bundles() {
    return Collections.unmodifiableSet(bundles);
  }

  public EvaluationResult evaluationResult() {
    boolean allBundlesPassed = bundles().stream()
      .allMatch(b -> b.evaluationResult() == EvaluationResult.Passed);
    return allBundlesPassed ? EvaluationResult.Passed : EvaluationResult.Failed;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Policy policy = (Policy) o;
    return Objects.equals(id, policy.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
