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
  private Set<PolicyBundle> bundles;

  public Policy(String id, String name, PolicyType type, Date createdAt, Date updatedAt, ScanResult root) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.bundles = new HashSet<>();
    this.root = root;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public PolicyType getType() {
    return type;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  @Override
  public ScanResult root() {
    return root;
  }

  void addBundle(PolicyBundle policyBundle) {
    this.bundles.add(policyBundle);
  }

  public Collection<PolicyBundle> getBundles() {
    return Collections.unmodifiableCollection(bundles);
  }
}
