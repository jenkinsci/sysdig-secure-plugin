package com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.iac.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.sysdig.jenkins.plugins.sysdig.TestMother;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.XStream2;
import org.htmlunit.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class IaCActionTest {
    private JenkinsRule jenkins;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    private static String sampleJson() {
        return TestMother.iacScanResultJson();
    }

    @Test
    void actionSurvivesXStreamRoundTripAndReparses() {
        // The action is stored in the build's XStream-serialized build.xml. Only the raw JSON string is
        // persisted; after a round-trip the action must still re-parse it into the domain result.
        IaCAction original = new IaCAction(null, sampleJson());

        XStream2 xstream = new XStream2();
        IaCAction restored = (IaCAction) xstream.fromXML(xstream.toXML(original));

        assertNotNull(restored.getScanResult());
        assertEquals(15, restored.getFindings().size());
        assertEquals(6, restored.getErrors().size());
        assertEquals(2, restored.getHighFindings()); // failed controls with High severity
        assertEquals(22, restored.getAffectedResources()); // reported resources: 3 high + 16 medium + 3 low

        // Breakdown adapts to the severities present (fixture has High/Medium/Low only), most severe first.
        var breakdown = restored.getSeverityBreakdown();
        assertEquals(3, breakdown.size());
        assertEquals("High", breakdown.get(0).getName());
        assertEquals(2, breakdown.get(0).getCount());
        assertEquals("high", breakdown.get(0).getCssClass());
    }

    @Test
    void actionPageRendersTables() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        build.addAction(new IaCAction(build, sampleJson()));
        build.save();

        JenkinsRule.WebClient wc = jenkins.createWebClient();
        Page page = wc.getPage(build, "sysdig-secure-iac-results");
        String text = page.getWebResponse().getContentAsString();

        assertTrue(text.contains("Sysdig Secure IaC Report"), "title");
        // Findings table content
        assertTrue(text.contains("ECR - Enabled Vulnerability Scanning"), "finding name");
        assertTrue(text.contains("aws_ecr_repository.soc_agent"), "resource name");
        assertTrue(text.contains("AWS_ECR_REPOSITORY"), "resource type");
        assertTrue(text.contains("All Posture Findings"), "policy");
        // Unsupported resources + errors tables
        assertTrue(text.contains("Unsupported resources"), "unsupported header");
        assertTrue(text.contains("aws_kms_alias"), "unsupported kind");
        assertTrue(text.contains("Parse errors"), "errors header");
    }

    @Test
    void summaryTileAppearsOnBuildPage() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        build.addAction(new IaCAction(build, sampleJson()));
        build.save();

        JenkinsRule.WebClient wc = jenkins.createWebClient();
        Page page = wc.getPage(build);
        String text = page.getWebResponse().getContentAsString();

        assertTrue(text.contains("Sysdig Secure IaC Report"), "summary link");
        assertTrue(text.contains("failed control(s)"), "summary counts");
    }
}
