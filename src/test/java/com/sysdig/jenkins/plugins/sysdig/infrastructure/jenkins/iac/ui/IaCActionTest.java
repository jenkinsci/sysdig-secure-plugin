package com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.iac.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.sysdig.jenkins.plugins.sysdig.TestMother;
import com.sysdig.jenkins.plugins.sysdig.domain.iac.scanresult.Resource;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.XStream2;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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

    /** A scan whose findings carry a severity the old high/medium/low wording could not express. */
    private static String jsonWithACriticalFinding() {
        return """
            {
              "result": {
                "type": "IaCGitScan",
                "metadata": {"sources": ["/"], "totalModules": 1, "totalResource": 2, "totalFolders": 1},
                "findingsSummaryBySeverity": {"critical": 1, "high": 1},
                "findings": [
                  {
                    "controlId": 1,
                    "name": "S3 - Block Public Access",
                    "severity": "Critical",
                    "resources": [
                      {"name": "aws_s3_bucket.one", "type": "AWS_S3_BUCKET",
                       "location": "source file: /", "source": "/"}
                    ],
                    "policies": ["All Posture Findings"],
                    "requirements": []
                  },
                  {
                    "controlId": 2,
                    "name": "ECR - Enabled Vulnerability Scanning",
                    "severity": "High",
                    "resources": [
                      {"name": "aws_ecr_repository.two", "type": "AWS_ECR_REPOSITORY",
                       "location": "source file: /", "source": "/"}
                    ],
                    "policies": ["All Posture Findings"],
                    "requirements": []
                  }
                ]
              }
            }
            """;
    }

    /** Shape a real Kubernetes scan produces: the location names a field, not a path. */
    private static String kubernetesStyleJson() {
        return """
            {
              "result": {
                "type": "IaCGitScan",
                "metadata": {"sources": ["/terraform"], "totalModules": 1, "totalResource": 3, "totalFolders": 1},
                "findingsSummaryBySeverity": {"high": 1},
                "findings": [
                  {
                    "controlId": 3,
                    "name": "Workload - Container Running As Root",
                    "severity": "High",
                    "resources": [
                      {"name": "kubernetes_deployment_v1.nemoclaw", "type": "Deployment",
                       "location": "runAsUser in container dind", "source": "/terraform"}
                    ],
                    "policies": ["All Posture Findings"],
                    "requirements": []
                  }
                ]
              }
            }
            """;
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
        IaCAction.attachTo(build, sampleJson(), "infra");
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
        IaCAction.attachTo(build, sampleJson(), "infra");
        build.save();

        JenkinsRule.WebClient wc = jenkins.createWebClient();
        Page page = wc.getPage(build);
        String text = page.getWebResponse().getContentAsString();

        assertTrue(text.contains("Sysdig Secure IaC Report"), "summary link");
        assertTrue(text.contains("failed control(s)"), "summary counts");
        assertTrue(text.contains("resource violation(s)"), "violations wording");
    }

    /**
     * The per-severity counts have to add up to the total next to them, so the summary breaks the
     * findings down by whatever severities the scan reports: a hardcoded high/medium/low list drops a
     * Critical finding from a line that claims to be the breakdown.
     */
    @Test
    void theBuildPageSummaryCountsEverySeverityTheScanReports() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        IaCAction.attachTo(build, jsonWithACriticalFinding(), "infra");
        build.save();

        JenkinsRule.WebClient wc = jenkins.createWebClient();
        String text = wc.getPage(build).getWebResponse().getContentAsString();

        assertTrue(text.contains("2 failed control(s)"), "total");
        assertTrue(text.contains("Critical=1"), "the critical finding is in the breakdown");
        assertTrue(text.contains("High=1"), "and so is the high one");
        assertTrue(text.contains("2 resource violation(s)"), "one row per control/resource pair");
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
        IaCAction.attachTo(build, jsonWithFindingNamed("FIRST-SCAN-CONTROL"), "infra");
        IaCAction.attachTo(build, jsonWithFindingNamed("SECOND-SCAN-CONTROL"), "infra");
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

    /**
     * Parallel pipeline branches attach their reports from different threads, and the build's action
     * list gives no atomicity, so the ordinal has to be picked and used under a lock.
     */
    @Test
    void concurrentAttachesStillGetDistinctUrls() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        int branches = 16;
        var start = new CountDownLatch(1);
        var done = new CountDownLatch(branches);
        var pool = Executors.newFixedThreadPool(branches);
        try {
            for (int i = 0; i < branches; i++) {
                String path = "infra-" + i;
                pool.submit(() -> {
                    try {
                        start.await();
                        IaCAction.attachTo(build, jsonWithFindingNamed("CONTROL"), path);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS), "all branches attached");
        } finally {
            pool.shutdownNow();
        }

        var urls = build.getActions(IaCAction.class).stream()
                .map(IaCAction::getUrlName)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(branches, build.getActions(IaCAction.class).size());
        assertEquals(branches, urls.size(), "every attached report needs its own url: " + urls);
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

    /**
     * A build persisted by an earlier version carries a report with no ordinal at all, which owns the
     * base url. The next scan of that build (a resumed pipeline, say) has to move out of its way.
     */
    @Test
    void attachingNextToALegacyReportDoesNotReuseItsUrl() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        XStream2 xstream = new XStream2();
        String legacyXml = xstream.toXML(new IaCAction(build, sampleJson(), "infra", 1))
                .replaceAll("\\s*<ordinal>.*</ordinal>", "")
                .replaceAll("\\s*<scannedPath>.*</scannedPath>", "");
        build.addAction((IaCAction) xstream.fromXML(legacyXml));

        IaCAction attached = IaCAction.attachTo(build, jsonWithFindingNamed("AFTER-UPGRADE"), "infra");

        assertEquals(
                "sysdig-secure-iac-results",
                build.getActions(IaCAction.class).get(0).getUrlName());
        assertEquals("sysdig-secure-iac-results-2", attached.getUrlName());

        JenkinsRule.WebClient wc = jenkins.createWebClient();
        String page = wc.getPage(build, attached.getUrlName()).getWebResponse().getContentAsString();
        assertTrue(page.contains("AFTER-UPGRADE"), "the new report is the one served");
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
     * The module path comes from the resource's {@code source}, which is always a path relative to the
     * scanned one. Its {@code location} is not: for Kubernetes resources the scanner puts the offending
     * field there ({@code "runAsUser in container dind"}) and only falls back to
     * {@code "source file: <path>"}, a copy of the source, when it has nothing better to say.
     */
    @Test
    void modulePathComesFromTheSourceAndIsRelativeToTheScannedPath() {
        IaCAction action = new IaCAction(null, sampleJson(), "/home/jenkins/demo", 1);

        assertEquals("infra", action.modulePathOf(resourceAt("source file: /infra", "/infra")));
        assertEquals("terraform", action.modulePathOf(resourceAt("runAsUser in container dind", "/terraform")));
        assertEquals("nested", action.modulePathOf(resourceAt("source file: /nested/deep.tf", "/nested")));
        assertEquals("scan root", action.modulePathOf(resourceAt("source file: /", "/")));

        // Without a source, only a location that looks like one can stand in for it.
        assertEquals("infra", action.modulePathOf(resourceAt("source file: /infra", null)));
        assertEquals("", action.modulePathOf(resourceAt("runAsUser in container dind", null)));
        assertEquals("", action.modulePathOf(resourceAt(null, null)));
    }

    /** The whole path, for the cell's tooltip: short cells, full detail one hover away. */
    @Test
    void fullModulePathJoinsTheScannedPathWithTheModule() {
        IaCAction action = new IaCAction(null, sampleJson(), "/home/jenkins/demo", 1);

        assertEquals("/home/jenkins/demo/infra", action.fullModulePathOf(resourceAt("source file: /infra", "/infra")));
        assertEquals("/home/jenkins/demo", action.fullModulePathOf(resourceAt("source file: /", "/")));
        assertEquals(
                "/home/jenkins/demo/terraform",
                action.fullModulePathOf(resourceAt("runAsUser in container dind", "/terraform")));

        // Nothing to join when the step did not configure a path: what the scanner reports is all we know.
        IaCAction noPath = new IaCAction(null, sampleJson(), "", 1);
        assertEquals("infra", noPath.fullModulePathOf(resourceAt("source file: /infra", "/infra")));
        assertEquals("scan root", noPath.fullModulePathOf(resourceAt("source file: /", "/")));
    }

    /** What failed inside the resource, when the scanner says more than where the resource lives. */
    @Test
    void detailKeepsTheOffendingFieldAndDropsThePathRepetition() {
        IaCAction action = new IaCAction(null, sampleJson(), "/home/jenkins/demo", 1);

        assertEquals(
                "runAsUser in container dind",
                action.detailOf(resourceAt("runAsUser in container dind", "/terraform")));
        assertEquals("`runAsGroup` in workload", action.detailOf(resourceAt("`runAsGroup` in workload", "/terraform")));
        assertEquals("", action.detailOf(resourceAt("source file: /infra", "/infra")), "already the module path");
        assertEquals("", action.detailOf(resourceAt("", "/infra")));
        assertEquals("", action.detailOf(resourceAt(null, "/infra")));
    }

    @Test
    void findingsTablePairsTheResourceWithWhatFailedInIt() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        IaCAction.attachTo(build, kubernetesStyleJson(), "/home/jenkins/nemoclaw");
        build.save();

        JenkinsRule.WebClient wc = jenkins.createWebClient();
        String text =
                wc.getPage(build, "sysdig-secure-iac-results").getWebResponse().getContentAsString();

        assertTrue(text.contains("kubernetes_deployment_v1.nemoclaw"), "resource name");
        assertTrue(text.contains("runAsUser in container dind"), "offending field kept");
        assertTrue(
                text.contains("title=\"/home/jenkins/nemoclaw/terraform\">terraform</td>"),
                "module path comes from the source");
        assertFalse(text.contains("\"/home/jenkins/nemoclaw/runAsUser"), "no field description turned into a path");
    }

    /** An absolute agent path is unreadable in a sidebar, so the name keeps only its last segment. */
    @Test
    void displayNameShortensTheScannedPathToItsLastSegment() {
        assertEquals(
                "Sysdig Secure IaC Report (demo-agentic)",
                new IaCAction(null, sampleJson(), "/Users/fede/Documents/demo-agentic", 1).getDisplayName());
        assertEquals(
                "Sysdig Secure IaC Report (infra)", new IaCAction(null, sampleJson(), "infra/", 1).getDisplayName());
        assertEquals("Sysdig Secure IaC Report", new IaCAction(null, sampleJson(), "/", 1).getDisplayName());
    }

    @Test
    void findingsTableShowsLocationWithoutTheScannerPrefix() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        IaCAction.attachTo(build, sampleJson(), "/home/jenkins/demo-agentic");
        build.save();

        JenkinsRule.WebClient wc = jenkins.createWebClient();
        String text =
                wc.getPage(build, "sysdig-secure-iac-results").getWebResponse().getContentAsString();

        assertTrue(text.contains("Resource violations"), "tile renamed away from affected resources");
        assertFalse(text.contains("source file:"), "scanner prefix stripped");
        // On the cell itself: the fixture also mentions /infra in the unsupported-resources table, so
        // a looser assertion would hold even if the column rendered something else entirely.
        assertTrue(text.contains("title=\"/home/jenkins/demo-agentic/infra\">infra</td>"), "module path cell");

        // The scan root is stated once, so a relative cell has something to be relative to.
        assertTrue(text.contains("Scanned: "), "scanned path label");
        assertTrue(text.contains("/home/jenkins/demo-agentic"), "scanned path");
        assertTrue(text.contains("Sysdig Secure IaC Report (demo-agentic)"), "heading identifies the scan");
    }

    /**
     * The unsupported-resources and parse-error tables carry the same kind of scanner path as the
     * findings table, so they read the same way instead of showing the raw leading-slash value.
     */
    @Test
    void unsupportedResourcesAndParseErrorsShowPathsTheSameWay() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        IaCAction.attachTo(build, sampleJson(), "/home/jenkins/demo-agentic");
        build.save();

        JenkinsRule.WebClient wc = jenkins.createWebClient();
        String text =
                wc.getPage(build, "sysdig-secure-iac-results").getWebResponse().getContentAsString();

        assertTrue(
                text.contains("title=\"/home/jenkins/demo-agentic/infra/infra\">infra/infra</td>"),
                "unsupported resource path");
        assertTrue(
                text.contains(
                        "title=\"/home/jenkins/demo-agentic/.venv/lib/python3.13/site-packages/markdown_it/port.yaml\">"
                                + ".venv/lib/python3.13/site-packages/markdown_it/port.yaml</td>"),
                "parse error path");
        assertFalse(text.contains("<td>/"), "no raw leading-slash path left in any table");
    }

    @Test
    void pathsAreRelativeEvenWithoutAConfiguredScanRoot() {
        IaCAction action = new IaCAction(null, sampleJson(), "", 1);

        assertEquals("infra/infra", action.pathOf("/infra/infra"));
        assertEquals("infra/infra", action.fullPathOf("/infra/infra"));
        assertEquals("scan root", action.pathOf("/"));
        assertEquals("", action.pathOf(null));
    }

    /** With no configured path there is no root to state, so the header stays as it was. */
    @Test
    void pageWithoutAConfiguredPathStatesNoScanRoot() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        IaCAction.attachTo(build, sampleJson(), "");
        build.save();

        JenkinsRule.WebClient wc = jenkins.createWebClient();
        String text =
                wc.getPage(build, "sysdig-secure-iac-results").getWebResponse().getContentAsString();

        assertFalse(text.contains("Scanned: "), "no root to state");
        assertTrue(text.contains("Resources scanned:"), "the rest of the meta line is still there");
    }

    private static Resource resourceAt(String location, String source) {
        return new Resource("aws_s3_bucket.example", "AWS_S3_BUCKET", location, source);
    }
}
