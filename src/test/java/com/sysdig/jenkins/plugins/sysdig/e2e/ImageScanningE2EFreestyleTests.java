package com.sysdig.jenkins.plugins.sysdig.e2e;

import hudson.Functions;
import hudson.model.Result;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assume.assumeFalse;

public class ImageScanningE2EFreestyleTests {
  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

  private final JenkinsTestHelpers helpers = new JenkinsTestHelpers(jenkins);

  @Before
  public void setUp() throws Exception {
    assumeFalse(Functions.isWindows());
    helpers.configureSysdigCredentials();
  }

  @Test
  public void testFreestyleWithDefaultConfig() throws Exception {
    var project = helpers.createFreestyleProjectWithImageScanningBuilder("nginx").build();

    var build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

    jenkins.assertLogContains("API Credentials not defined", build);
  }

  @Test
  public void testFreestyleWithCredentialsAndAssertLogOutput() throws Exception {
    var project = helpers.createFreestyleProjectWithImageScanningBuilder("alpine")
      .withConfig(b -> b.setEngineCredentialsId("sysdig-secure"))
      .build();

    var build = jenkins.buildAndAssertStatus(Result.FAILURE, project);
    jenkins.waitUntilNoActivity();

    jenkins.assertLogContains("Using new-scanning engine", build);
    jenkins.assertLogContains("Image Name: alpine", build);
    jenkins.assertLogContains("Downloading inlinescan v1.16.1", build);
    jenkins.assertLogContains("--apiurl=https://secure.sysdig.com", build);
    jenkins.assertLogContains("--loglevel=info --console-log alpine", build);
    jenkins.assertLogContains("Check that the API token is provided and is valid for the specified URL.", build);
  }

  @Test
  public void testFreestyleWithAllConfigs() throws Exception {
    var project = helpers.createFreestyleProjectWithImageScanningBuilder("nginx").withConfig(b -> {
      b.setEngineCredentialsId("sysdig-secure");
      b.setEngineURL("https://custom-engine-url.com");
      b.setEngineVerify(false);
      b.setInlineScanExtraParams("--severity high");
      b.setCustomCliVersion("2.0.0");
      b.setCliVersionToApply("custom");
      b.setPoliciesToApply("custom-policy-name");
      b.setBailOnFail(false);
      b.setBailOnPluginFail(false);
    }).build();

    var build = jenkins.buildAndAssertSuccess( project);

    jenkins.assertLogContains("Starting Sysdig Secure Container Image Scanner step, project: test0, job: 1", build);
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
  public void testFreestyleWithAllConfigsWithGlobalConfig() throws Exception {
    var project = helpers.createFreestyleProjectWithImageScanningBuilder("nginx").withGlobalConfig(b -> {
      b.setEngineCredentialsId("sysdig-secure");
      b.setEngineURL("https://custom-engine-url.com");
      b.setEngineVerify(false);
      b.setInlineScanExtraParams("--severity high");
      b.setCustomCliVersion("2.0.0");
      b.setCliVersionToApply("custom");
      b.setPoliciesToApply("custom-policy-name");
    }).build();

    var build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

    jenkins.assertLogContains("Starting Sysdig Secure Container Image Scanner step, project: test0, job: 1", build);
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
