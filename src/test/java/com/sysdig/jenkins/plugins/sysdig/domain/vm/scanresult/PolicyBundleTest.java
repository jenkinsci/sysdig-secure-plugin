package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PolicyBundleTest {

    private ScanResult scanResult;

    @BeforeEach
    public void setUp() {
        scanResult = new ScanResult(
                EvaluationResult.Passed,
                ScanType.Docker,
                "test-pull-string",
                "test-image-id",
                "test-digest",
                new OperatingSystem(OperatingSystem.Family.Linux, "test-os"),
                null,
                Architecture.AMD64,
                null,
                null);
    }

    @Test
    public void testAddPkgVulnRule() {
        Policy policy = scanResult.addPolicy("test-policy-id", "test-policy", null, null);
        PolicyBundle bundle = scanResult.addPolicyBundle("test-bundle-id", "test-bundle", policy);

        PolicyBundleRulePkgVuln rule = bundle.addPkgVulnRule("test-rule-id", "test-rule-desc", EvaluationResult.Failed);

        assertEquals(1, bundle.rules().size());
        assertTrue(bundle.rules().contains(rule));
        assertEquals("test-rule-id", rule.id());
        assertEquals(bundle, rule.parent());
    }

    @Test
    public void testAddImageConfigRule() {
        Policy policy = scanResult.addPolicy("test-policy-id", "test-policy", null, null);
        PolicyBundle bundle = scanResult.addPolicyBundle("test-bundle-id", "test-bundle", policy);

        PolicyBundleRuleImageConfig rule =
                bundle.addImageConfigRule("test-rule-id", "test-rule-desc", EvaluationResult.Passed);

        assertEquals(1, bundle.rules().size());
        assertTrue(bundle.rules().contains(rule));
        assertEquals("test-rule-id", rule.id());
        assertEquals(bundle, rule.parent());
    }
}
