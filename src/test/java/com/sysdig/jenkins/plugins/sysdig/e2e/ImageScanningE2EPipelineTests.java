package com.sysdig.jenkins.plugins.sysdig.e2e;

import hudson.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ImageScanningE2EPipelineTests {
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
        var job = helpers.createPipelineJobWithScript("sysdigImageScan imageName: 'nginx'")
                .buildWithRemoteExecution();

        var build = jenkins.buildAndAssertStatus(Result.FAILURE, job);

        jenkins.assertLogContains("API Credentials not defined", build);
    }

    @Test
    void testPipelineWithCredentialsAndAssertLogOutput() throws Exception {
        var job = helpers.createPipelineJobWithScript(
                        "sysdigImageScan engineCredentialsId: 'sysdig-secure', imageName: 'alpine'")
                .buildWithRemoteExecution();

        var build = jenkins.buildAndAssertStatus(Result.FAILURE, job);
        jenkins.waitUntilNoActivity();

        jenkins.assertLogContains("Using new-scanning engine", build);
        jenkins.assertLogContains("Image Name: alpine", build);
        jenkins.assertLogContains("Downloading inlinescan v1", build);
        jenkins.assertLogContains("Check that the API token is provided and is valid for the specified URL.", build);
    }

    @Test
    void testPipelineWithAllConfigs() throws Exception {
        var job = helpers.createPipelineJobWithScript(
                        """
        sysdigImageScan engineCredentialsId: 'sysdig-secure',
                          engineURL: 'https://custom-engine-url.com',
                          engineVerify: false,
                          imageName: 'nginx',
                          inlineScanExtraParams: '--severity high',
                          customCliVersion: '2.0.0',
                          cliVersionToApply: 'custom',
                          policiesToApply: 'custom-policy-name',
                          bailOnFail: false,
                          bailOnPluginFail: false""")
                .buildWithRemoteExecution();

        var build = jenkins.buildAndAssertSuccess(job);

        jenkins.assertLogContains(
                "Starting Sysdig Secure Container Image Scanner step, project: test-pipeline, job: 1", build);
        jenkins.assertLogContains("Using new-scanning engine", build);
        jenkins.assertLogContains("Image Name: nginx", build);
        jenkins.assertLogContains("EngineURL: https://custom-engine-url.com", build);
        jenkins.assertLogContains("EngineVerify: false", build);
        jenkins.assertLogContains("Policies: custom-policy-name", build);
        jenkins.assertLogContains("InlineScanExtraParams: --severity high", build);
        jenkins.assertLogContains("BailOnFail: false", build);
        jenkins.assertLogContains("BailOnPluginFail: false", build);
        jenkins.assertLogContains("Downloading inlinescan v2.0.0", build);
    }

    @Test
    void testPipelineWithMinimalConfigButAlsoGlobalConfig() throws Exception {
        var job = helpers.createPipelineJobWithScript("sysdigImageScan 'nginx'")
                .withGlobalConfig(b -> {
                    b.setEngineCredentialsId("sysdig-secure");
                    b.setEngineURL("https://custom-engine-url.com");
                    b.setEngineVerify(false);
                    b.setInlineScanExtraParams("--severity high");
                    b.setCustomCliVersion("2.0.0");
                    b.setCliVersionToApply("custom");
                    b.setPoliciesToApply("custom-policy-name");
                })
                .buildWithRemoteExecution();

        var build = jenkins.buildAndAssertStatus(Result.FAILURE, job);

        jenkins.assertLogContains(
                "Starting Sysdig Secure Container Image Scanner step, project: test-pipeline, job: 1", build);
        jenkins.assertLogContains("Using new-scanning engine", build);
        jenkins.assertLogContains("Image Name: nginx", build);
        jenkins.assertLogContains("EngineURL: https://custom-engine-url.com", build);
        jenkins.assertLogContains("EngineVerify: false", build);
        jenkins.assertLogContains("Policies: custom-policy-name", build);
        jenkins.assertLogContains("InlineScanExtraParams: --severity high", build);
        jenkins.assertLogContains("BailOnFail: true", build);
        jenkins.assertLogContains("BailOnPluginFail: true", build);
        jenkins.assertLogContains("Downloading inlinescan v2.0.0", build);
    }
}
