package com.sysdig.jenkins.plugins.sysdig.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import hudson.FilePath;
import hudson.model.Result;
import hudson.slaves.WorkspaceList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class IaCScanE2EFreestyleTests {
    private JenkinsRule jenkins;
    private JenkinsTestHelpers helpers;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        jenkins = rule;
        helpers = new JenkinsTestHelpers(jenkins);
        helpers.configureSysdigCredentials();
    }

    @Test
    void testFreestyleWithDefaultConfig() throws Exception {
        var project = helpers.createFreestyleProjectWithIaCScanBuilder().build();

        var build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

        jenkins.assertLogContains("Attempting to download CLI", build);
        jenkins.assertLogContains("Starting scan", build);
        jenkins.assertLogContains(
                "API Credentials not defined. Make sure credentials are defined globally or in job.", build);
    }

    @Test
    void testFreestyleWithNonExistingToken() throws Exception {
        var project = helpers.createFreestyleProjectWithIaCScanBuilder()
                .withConfig(c -> c.setEngineCredentialsId("non-existing-token"))
                .build();

        var build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

        jenkins.assertLogContains("Attempting to download CLI", build);
        jenkins.assertLogContains("Starting scan", build);
        jenkins.assertLogContains(
                "Cannot find Jenkins credentials by ID: 'non-existing-token'. Ensure credentials are defined in Jenkins before using them",
                build);
    }

    @Test
    void testFreestyleWithCredentialsAndAssertLogOutput() throws Exception {
        var project = helpers.createFreestyleProjectWithIaCScanBuilder()
                .withConfig(b -> b.setEngineCredentialsId("sysdig-secure"))
                .build();

        var build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

        jenkins.assertLogContains("Attempting to download CLI", build);
        jenkins.assertLogContains(
                "Downloading https://download.sysdig.com/scanning/bin/sysdig-cli-scanner/1.28.0", // newest-version-marker
                build);
        jenkins.assertLogContains("Starting scan", build);
        jenkins.assertLogContains("--iac --apiurl=https://secure.sysdig.com --loglevel=info", build);
        jenkins.assertLogContains("--output-json=", build);
        jenkins.assertLogContains("--recursive --severity-threshold=high", build);
        jenkins.assertLogContains("Process finished with status 3", build);
        jenkins.assertLogContains("(status 401):", build);
    }

    /**
     * The report file is created before the credentials are resolved, so the step must clean it up on
     * every exit path. Random names make a leak accumulate across builds instead of being overwritten,
     * and the report must never land inside the scanned workspace.
     */
    @Test
    void testTheReportFileLeavesNothingBehindWhenCredentialsAreMissing() throws Exception {
        var project = helpers.createFreestyleProjectWithIaCScanBuilder().build();

        jenkins.buildAndAssertStatus(Result.FAILURE, project);

        var workspace = project.getSomeWorkspace();
        assertNotNull(workspace);
        assertEquals(List.of(), reportFilesIn(workspace), "workspace");
        assertEquals(List.of(), reportFilesIn(WorkspaceList.tempDir(workspace)), "workspace @tmp");
    }

    @Test
    void testTheReportFileIsWrittenOutsideTheScannedWorkspaceAndDeleted() throws Exception {
        var project = helpers.createFreestyleProjectWithIaCScanBuilder()
                .withConfig(b -> b.setEngineCredentialsId("sysdig-secure"))
                .build();

        var build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

        var workspace = project.getSomeWorkspace();
        assertNotNull(workspace);
        jenkins.assertLogContains(
                "--output-json=" + WorkspaceList.tempDir(workspace).getRemote(), build);
        assertEquals(List.of(), reportFilesIn(workspace), "workspace");
        assertEquals(List.of(), reportFilesIn(WorkspaceList.tempDir(workspace)), "workspace @tmp");
    }

    private static List<String> reportFilesIn(FilePath directory) throws Exception {
        if (directory == null || !directory.exists()) {
            return List.of();
        }
        return Arrays.stream(directory.list("sysdig-iac-scan-result*"))
                .map(FilePath::getName)
                .toList();
    }

    /**
     * With no path configured the scan has to walk the workspace: the process runs there and gets the
     * current directory as its path, instead of an empty path in whatever directory the agent's JVM
     * happens to sit in.
     */
    @Test
    void testTheDefaultPathScansTheWorkspace() throws Exception {
        var project = helpers.createFreestyleProjectWithIaCScanBuilder()
                .withConfig(b -> b.setEngineCredentialsId("sysdig-secure"))
                .build();

        var build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

        var workspace = project.getSomeWorkspace();
        assertNotNull(workspace);
        jenkins.assertLogContains("--recursive --severity-threshold=high .", build);
        jenkins.assertLogContains("[" + workspace.getName() + "] $ ", build);
        jenkins.assertLogContains("Scanning paths paths=[\".\"]", build);
    }

    @Test
    void testFreestyleWithAllConfigs() throws Exception {
        var project = helpers.createFreestyleProjectWithIaCScanBuilder()
                .withConfig(b -> {
                    b.setEngineCredentialsId("sysdig-secure");
                    b.setPath("custom/path/to/scan");
                    b.setListUnsupported(true);
                    b.setIsRecursive(false);
                    b.setSeverityThreshold("m");
                    b.setSysdigEnv("https://us2.app.sysdig.com");
                    b.setVersion("1.22.6"); // oldest-version-marker
                })
                .build();

        var build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

        jenkins.assertLogContains("Attempting to download CLI", build);
        jenkins.assertLogContains(
                "Downloading https://download.sysdig.com/scanning/bin/sysdig-cli-scanner/1.22.6", // oldest-version-marker
                build);
        jenkins.assertLogContains("Starting scan", build);
        jenkins.assertLogContains("--iac --apiurl=https://us2.app.sysdig.com --loglevel=info", build);
        jenkins.assertLogContains("--output-json=", build);
        jenkins.assertLogContains(
                "--list-unsupported-resources --severity-threshold=medium custom/path/to/scan", build);
        jenkins.assertLogContains("Process finished with status 3", build);
    }
}
