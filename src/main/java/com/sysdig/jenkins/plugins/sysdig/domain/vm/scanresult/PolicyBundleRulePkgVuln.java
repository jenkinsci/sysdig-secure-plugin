package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PolicyBundleRulePkgVuln implements PolicyBundleRule {
    private final String id;
    private final String description;
    private final EvaluationResult evaluationResult;
    private final PolicyBundle parent;
    private final List<PolicyBundleRulePkgVulnFailure> failures;

    PolicyBundleRulePkgVuln(String id, String description, EvaluationResult evaluationResult, PolicyBundle parent) {
        this.id = id;
        this.description = description;
        this.evaluationResult = evaluationResult;
        this.parent = parent;
        this.failures = new ArrayList<>();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public EvaluationResult evaluationResult() {
        return evaluationResult;
    }

    @Override
    public PolicyBundle parent() {
        return parent;
    }

    public PolicyBundleRulePkgVulnFailure addFailure(String remediation, Package pkg, Vulnerability vuln) {
        PolicyBundleRulePkgVulnFailure failure = new PolicyBundleRulePkgVulnFailure(remediation, pkg, vuln, this);
        this.failures.add(failure);
        return failure;
    }

    @Override
    public List<PolicyBundleRulePkgVulnFailure> failures() {
        return Collections.unmodifiableList(failures);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PolicyBundleRulePkgVuln that = (PolicyBundleRulePkgVuln) o;
        return Objects.equals(id, that.id)
                && Objects.equals(description, that.description)
                && evaluationResult == that.evaluationResult
                && Objects.equals(parent, that.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, description, evaluationResult, parent);
    }
}
