package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

import com.sysdig.jenkins.plugins.sysdig.domain.AggregateChild;
import java.io.Serializable;
import java.util.*;

public class PolicyBundle implements AggregateChild<ScanResult>, Serializable {
    private final ScanResult root;
    private final String id;
    private final String name;
    private final LinkedHashSet<PolicyBundleRule> rules;
    private final Set<Policy> foundInPolicies;

    PolicyBundle(String id, String name, ScanResult root) {
        this.id = id;
        this.name = name;
        this.rules = new LinkedHashSet<>();
        this.root = root;
        this.foundInPolicies = new HashSet<>();
    }

    void addPolicy(Policy policy) {
        if (this.foundInPolicies.add(policy)) {
            policy.addBundle(this);
        }
    }

    public PolicyBundleRulePkgVuln addPkgVulnRule(String id, String description, EvaluationResult evaluationResult) {
        PolicyBundleRulePkgVuln rule = new PolicyBundleRulePkgVuln(id, description, evaluationResult, this);
        this.rules.add(rule);
        return rule;
    }

    public PolicyBundleRuleImageConfig addImageConfigRule(
            String id, String description, EvaluationResult evaluationResult) {
        PolicyBundleRuleImageConfig rule = new PolicyBundleRuleImageConfig(id, description, evaluationResult, this);
        this.rules.add(rule);
        return rule;
    }

    public Set<Policy> foundInPolicies() {
        return Collections.unmodifiableSet(foundInPolicies);
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

    public Set<PolicyBundleRule> rules() {
        return Collections.unmodifiableSet(rules);
    }

    public EvaluationResult evaluationResult() {
        boolean allRulesPassed =
                rules().stream().allMatch(r -> r.evaluationResult().isPassed());
        return allRulesPassed ? EvaluationResult.Passed : EvaluationResult.Failed;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PolicyBundle that = (PolicyBundle) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
