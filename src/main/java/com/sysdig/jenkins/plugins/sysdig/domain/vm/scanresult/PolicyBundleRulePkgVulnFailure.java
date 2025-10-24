package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

import java.util.Objects;

public class PolicyBundleRulePkgVulnFailure implements PolicyBundleRuleFailure {
    private final String remediation;
    private final Package pkg;
    private final Vulnerability vuln;
    private final PolicyBundleRule parent;

    PolicyBundleRulePkgVulnFailure(String remediation, Package pkg, Vulnerability vuln, PolicyBundleRule parent) {
        this.remediation = remediation;
        this.pkg = pkg;
        this.vuln = vuln;
        this.parent = parent;
    }

    @Override
    public PolicyBundleRuleFailureType type() {
        return PolicyBundleRuleFailureType.PkgVulnFailure;
    }

    @Override
    public String description() {
        return String.format("%s found in %s (%s)", this.vuln.cve(), this.pkg.name(), this.pkg.version());
    }

    public String remediation() {
        return remediation;
    }

    public Package pkg() {
        return pkg;
    }

    public Vulnerability vuln() {
        return vuln;
    }

    @Override
    public PolicyBundleRule parent() {
        return parent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PolicyBundleRulePkgVulnFailure that = (PolicyBundleRulePkgVulnFailure) o;
        return Objects.equals(remediation, that.remediation)
                && Objects.equals(pkg, that.pkg)
                && Objects.equals(vuln, that.vuln)
                && Objects.equals(parent, that.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(remediation, pkg, vuln, parent);
    }
}
