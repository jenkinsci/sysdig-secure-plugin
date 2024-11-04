package com.sysdig.jenkins.plugins.sysdig.e2e;

import hudson.model.Result;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class ImageScanningE2EPipelineTests {
  @Rule
  public JenkinsRule jenkins = new JenkinsRule();
  private final JenkinsTestHelpers helpers = new JenkinsTestHelpers(jenkins);

  @Before
  public void setUp() throws Exception {
    helpers.configureSysdigCredentials();
  }

  @Test
  public void testPipelineWithDefaultConfig() throws Exception {
    var job = helpers.createPipelineJobWithScript(
      "sysdigImageScan imageName: 'nginx'"
    ).buildWithRemoteExecution();

    var build = jenkins.buildAndAssertStatus(Result.FAILURE, job);

    jenkins.assertLogContains("API Credentials not defined", build);
  }

  @Test
  public void testPipelineWithCredentialsAndAssertLogOutput() throws Exception {
    var job = helpers.createPipelineJobWithScript(
      "sysdigImageScan engineCredentialsId: 'sysdig-secure', imageName: 'alpine'"
    ).buildWithRemoteExecution();

    var build = jenkins.buildAndAssertStatus(Result.FAILURE, job);
    jenkins.waitUntilNoActivity();

    jenkins.assertLogContains("Using new-scanning engine", build);
    jenkins.assertLogContains("Image Name: alpine", build);
    jenkins.assertLogContains("Downloading inlinescan v1.16.1", build);
    jenkins.assertLogContains("Check that the API token is provided and is valid for the specified URL.", build);
  }

  @Test
  public void testPipelineWithAllConfigs() throws Exception {
    var job = helpers.createPipelineJobWithScript(
        "sysdigImageScan engineCredentialsId: 'sysdig-secure',\n" +
        "                  engineURL: 'https://custom-engine-url.com',\n" +
        "                  engineVerify: false,\n" +
        "                  imageName: 'nginx',\n" +
        "                  inlineScanExtraParams: '--severity high',\n" +
        "                  customCliVersion: '2.0.0',\n" +
        "                  cliVersionToApply: 'custom',\n" +
        "                  policiesToApply: 'custom-policy-name',\n" +
        "                  bailOnFail: false,\n" +
        "                  bailOnPluginFail: false"
    ).buildWithRemoteExecution();

    var build = jenkins.buildAndAssertSuccess(job);

    jenkins.assertLogContains("Starting Sysdig Secure Container Image Scanner step, project: test-pipeline, job: 1", build);
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
  public void testPipelineWithMinimalConfigButAlsoGlobalConfig() throws Exception {
    var job = helpers.createPipelineJobWithScript(
      "sysdigImageScan 'nginx'"
    ).withGlobalConfig(b -> {
      b.setEngineCredentialsId("sysdig-secure");
      b.setEngineURL("https://custom-engine-url.com");
      b.setEngineVerify(false);
      b.setInlineScanExtraParams("--severity high");
      b.setCustomCliVersion("2.0.0");
      b.setCliVersionToApply("custom");
      b.setPoliciesToApply("custom-policy-name");
    }).buildWithRemoteExecution();

    var build = jenkins.buildAndAssertStatus(Result.FAILURE, job);

    jenkins.assertLogContains("Starting Sysdig Secure Container Image Scanner step, project: test-pipeline, job: 1", build);
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
