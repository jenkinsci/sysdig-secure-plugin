package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

public class PolicyBundleRulePkgVulnFailure implements PolicyBundleRuleFailure {
    private final String remediation;
    private final PolicyBundleRule parent;

    public PolicyBundleRulePkgVulnFailure(String remediation, PolicyBundleRule parent) {
        this.remediation = remediation;
        this.parent = parent;
    }

    @Override
    public PolicyBundleRuleFailureType type() {
        return PolicyBundleRuleFailureType.PkgVulnFailure;
    }

    @Override
    public String description() {
        return remediation;
    }

    @Override
    public PolicyBundleRule parent() {
        return parent;
    }
}
