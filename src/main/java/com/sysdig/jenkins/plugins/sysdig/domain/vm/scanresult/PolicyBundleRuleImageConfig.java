package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PolicyBundleRuleImageConfig implements PolicyBundleRule {
    private final String id;
    private final String description;
    private final EvaluationResult evaluationResult;
    private final PolicyBundle parent;
    private final List<PolicyBundleRuleImageConfigFailure> failures;

    PolicyBundleRuleImageConfig(String id, String description, EvaluationResult evaluationResult, PolicyBundle parent) {
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

    public PolicyBundleRuleImageConfigFailure addFailure(String remediation) {
        PolicyBundleRuleImageConfigFailure failure = new PolicyBundleRuleImageConfigFailure(remediation, this);
        this.failures.add(failure);
        return failure;
    }

    @Override
    public List<PolicyBundleRuleImageConfigFailure> failures() {
        return Collections.unmodifiableList(failures);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PolicyBundleRuleImageConfig that = (PolicyBundleRuleImageConfig) o;
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
