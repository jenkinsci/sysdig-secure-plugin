package com.sysdig.jenkins.plugins.sysdig.e2e;

import hudson.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class IaCScanE2EPipelineTests {
    private JenkinsRule jenkins;
    private JenkinsTestHelpers helpers;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        jenkins = rule;
        helpers = new JenkinsTestHelpers(jenkins);
        helpers.configureSysdigCredentials();
    }

    @Test
    void testPipelineWithDefaultConfig() throws Exception {
        var job = helpers.createPipelineJobWithScript("sysdigIaCScan()").buildWithRemoteExecution();

        var build = jenkins.buildAndAssertStatus(Result.FAILURE, job);

        jenkins.assertLogContains("Attempting to download CLI", build);
        jenkins.assertLogContains("Starting scan", build);
        jenkins.assertLogContains(
                "API Credentials not defined. Make sure credentials are defined globally or in job.", build);
    }

    @Test
    void testPipelineWithNonExistingToken() throws Exception {
        var job = helpers.createPipelineJobWithScript("sysdigIaCScan engineCredentialsId: 'non-existing-token'")
                .buildWithRemoteExecution();

        var build = jenkins.buildAndAssertStatus(Result.FAILURE, job);

        jenkins.assertLogContains("Attempting to download CLI", build);
        jenkins.assertLogContains("Starting scan", build);
        jenkins.assertLogContains(
                "Cannot find Jenkins credentials by ID: 'non-existing-token'. Ensure credentials are defined in Jenkins before using them",
                build);
    }

    @Test
    void testPipelineWithCredentialsAndAssertLogOutput() throws Exception {
        var job = helpers.createPipelineJobWithScript("sysdigIaCScan engineCredentialsId: 'sysdig-secure'")
                .buildWithRemoteExecution();

        var build = jenkins.buildAndAssertStatus(Result.FAILURE, job);

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

    @Test
    void testPipelineWithAllConfigs() throws Exception {
        var job = helpers.createPipelineJobWithScript("""
                        sysdigIaCScan engineCredentialsId: 'sysdig-secure',
                                      path: 'custom/path/to/scan',
                                      listUnsupported: true,
                                      isRecursive: false,
                                      severityThreshold: 'm',
                                      sysdigEnv: 'https://us2.app.sysdig.com',
                                      version: '1.22.6'""") // oldest-version-marker
                .buildWithRemoteExecution();

        var build = jenkins.buildAndAssertStatus(Result.FAILURE, job);

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
