package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

public class PolicyBundleRuleImageConfigFailure implements PolicyBundleRuleFailure {
  private final String description;
  private final PolicyBundleRule parent;

  PolicyBundleRuleImageConfigFailure(String description, PolicyBundleRule parent) {
    this.description = description;
    this.parent = parent;
  }

  @Override
  public PolicyBundleRuleFailureType type() {
    return PolicyBundleRuleFailureType.ImageConfigFailure;
  }

  @Override
  public String description() {
    return description;
  }

  @Override
  public PolicyBundleRule parent() {
    return parent;
  }
}
