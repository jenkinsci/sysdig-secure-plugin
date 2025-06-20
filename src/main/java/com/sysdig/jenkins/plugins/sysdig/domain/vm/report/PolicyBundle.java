package com.sysdig.jenkins.plugins.sysdig.domain.vm.report;

import com.sysdig.jenkins.plugins.sysdig.domain.AggregateChild;

import java.util.*;

public class PolicyBundle implements AggregateChild<ScanResult> {
  private final ScanResult root;
  private final String id;
  private final String name;
  private final List<PolicyBundleRule> rules;
  private final Date createdAt;
  private final Date updatedAt;
  private final Set<Policy> foundInPolicies;

  PolicyBundle(String id, String name, List<PolicyBundleRule> rules, Date createdAt, Date updatedAt, ScanResult root) {
    this.id = id;
    this.name = name;
    this.rules = rules;
    this.root = root;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.foundInPolicies = new HashSet<>();
  }

  void addPolicy(Policy policy) {
    this.foundInPolicies.add(policy);
  }

  public Collection<Policy> getFoundInPolicies() {
    return Collections.unmodifiableCollection(foundInPolicies);
  }

  @Override
  public ScanResult root() {
    return root;
  }

  public String id() {
    return id;
  }

  public String name() {
    return name;
  }

  public Date createdAt() {
    return createdAt;
  }

  public Date updatedAt() {
    return updatedAt;
  }

  public List<PolicyBundleRule> rules() {
    return Collections.unmodifiableList(rules);
  }
}
