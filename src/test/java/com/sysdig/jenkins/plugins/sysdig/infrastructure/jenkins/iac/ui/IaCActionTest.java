package com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.iac.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.sysdig.jenkins.plugins.sysdig.TestMother;
import com.sysdig.jenkins.plugins.sysdig.domain.iac.scanresult.Resource;
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

    /** Minimal report with a single finding, to tell one attached action apart from another. */
    private static String jsonWithFindingNamed(String controlName) {
        return """
            {
              "result": {
                "type": "IaCGitScan",
                "metadata": {"sources": ["/"], "totalModules": 1, "totalResource": 1, "totalFolders": 1},
                "findingsSummaryBySeverity": {"high": 1},
                "findings": [
                  {
                    "controlId": 1,
                    "name": "%s",
                    "severity": "High",
                    "resources": [
                      {"name": "aws_s3_bucket.only", "type": "AWS_S3_BUCKET",
                       "location": "source file: /", "source": "/"}
                    ],
                    "policies": ["All Posture Findings"],
                    "requirements": []
                  }
                ]
              }
            }
            """.formatted(controlName);
    }

    @Test
    void actionSurvivesXStreamRoundTripAndReparses() {
        // The action is stored in the build's XStream-serialized build.xml. Only the raw JSON string is
        // persisted; after a round-trip the action must still re-parse it into the domain result.
        IaCAction original = new IaCAction(null, sampleJson(), "infra", 1);

        XStream2 xstream = new XStream2();
        IaCAction restored = (IaCAction) xstream.fromXML(xstream.toXML(original));

        assertNotNull(restored.getScanResult());
        assertEquals(15, restored.getFindings().size());
        assertEquals(6, restored.getErrors().size());
        assertEquals(2, restored.getHighFindings()); // failed controls with High severity
        assertEquals(22, restored.getResourceViolations()); // one row per control/resource pair
        assertEquals("infra", restored.getScannedPath());

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
        build.addAction(IaCAction.createFor(build, sampleJson(), "infra"));
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
        build.addAction(IaCAction.createFor(build, sampleJson(), "infra"));
        build.save();

        JenkinsRule.WebClient wc = jenkins.createWebClient();
        Page page = wc.getPage(build);
        String text = page.getWebResponse().getContentAsString();

        assertTrue(text.contains("Sysdig Secure IaC Report"), "summary link");
        assertTrue(text.contains("failed control(s)"), "summary counts");
        assertTrue(text.contains("resource violation(s)"), "violations wording");
    }

    /**
     * Several IaC steps in one build each attach their own action. Jenkins resolves an action URL to
     * the first action whose url name matches, so a shared url name would hide every report but the
     * first one.
     */
    @Test
    void eachActionInTheSameBuildGetsItsOwnUrlAndPage() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        // Same path on purpose: the url must stay unique even when two steps scan the same directory.
        build.addAction(IaCAction.createFor(build, jsonWithFindingNamed("FIRST-SCAN-CONTROL"), "infra"));
        build.addAction(IaCAction.createFor(build, jsonWithFindingNamed("SECOND-SCAN-CONTROL"), "infra"));
        build.save();

        var actions = build.getActions(IaCAction.class);
        assertEquals(2, actions.size());
        assertEquals("sysdig-secure-iac-results", actions.get(0).getUrlName());
        assertEquals("sysdig-secure-iac-results-2", actions.get(1).getUrlName());
        assertNotEquals(actions.get(0).getDisplayName(), actions.get(1).getDisplayName());

        JenkinsRule.WebClient wc = jenkins.createWebClient();
        String first =
                wc.getPage(build, actions.get(0).getUrlName()).getWebResponse().getContentAsString();
        String second =
                wc.getPage(build, actions.get(1).getUrlName()).getWebResponse().getContentAsString();

        assertTrue(first.contains("FIRST-SCAN-CONTROL"), "first report reachable");
        assertFalse(first.contains("SECOND-SCAN-CONTROL"), "first report is not the second one");
        assertTrue(second.contains("SECOND-SCAN-CONTROL"), "second report reachable");
    }

    @Test
    void displayNameIdentifiesTheScannedPath() {
        assertEquals(
                "Sysdig Secure IaC Report (infra)", new IaCAction(null, sampleJson(), "infra", 1).getDisplayName());
        assertEquals(
                "Sysdig Secure IaC Report (infra) #2", new IaCAction(null, sampleJson(), "infra", 2).getDisplayName());
        assertEquals("Sysdig Secure IaC Report", new IaCAction(null, sampleJson(), "", 1).getDisplayName());
        assertEquals("Sysdig Secure IaC Report", new IaCAction(null, sampleJson(), ".", 1).getDisplayName());
    }

    /** Actions persisted by earlier plugin versions have neither ordinal nor scanned path. */
    @Test
    void legacyPersistedActionKeepsTheBaseUrlAndName() {
        XStream2 xstream = new XStream2();
        String legacyXml = xstream.toXML(new IaCAction(null, sampleJson(), "infra", 2))
                .replaceAll("\\s*<ordinal>.*</ordinal>", "")
                .replaceAll("\\s*<scannedPath>.*</scannedPath>", "");

        IaCAction restored = (IaCAction) xstream.fromXML(legacyXml);

        assertEquals("sysdig-secure-iac-results", restored.getUrlName());
        assertEquals("Sysdig Secure IaC Report", restored.getDisplayName());
        assertNotNull(restored.getScanResult());
    }

    /**
     * The scanner reports the module/folder the resource was found in as {@code "source file: <path>"},
     * which reads like a broken value when the whole repository is scanned (path {@code "/"}).
     */
    @Test
    void locationDropsTheScannerPrefixAndNamesTheScanRoot() {
        IaCAction action = new IaCAction(null, sampleJson(), "infra", 1);

        assertEquals("/infra", action.locationOf(resourceAt("source file: /infra", "/infra")));
        assertEquals("scan root", action.locationOf(resourceAt("source file: /", "/")));
        assertEquals("scan root", action.locationOf(resourceAt("", "")));
        assertEquals("/infra", action.locationOf(resourceAt(null, "/infra")));
        assertEquals("scan root", action.locationOf(resourceAt(null, null)));
    }

    @Test
    void findingsTableShowsLocationWithoutTheScannerPrefix() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        build.addAction(IaCAction.createFor(build, sampleJson(), "infra"));
        build.save();

        JenkinsRule.WebClient wc = jenkins.createWebClient();
        String text =
                wc.getPage(build, "sysdig-secure-iac-results").getWebResponse().getContentAsString();

        assertTrue(text.contains("Resource violations"), "tile renamed away from affected resources");
        assertFalse(text.contains("source file:"), "scanner prefix stripped");
        assertTrue(text.contains("/infra"), "module path still shown");
    }

    private static Resource resourceAt(String location, String source) {
        return new Resource("aws_s3_bucket.example", "AWS_S3_BUCKET", location, source);
    }
}
