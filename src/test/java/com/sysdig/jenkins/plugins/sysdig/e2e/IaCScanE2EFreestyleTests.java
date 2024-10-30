package com.sysdig.jenkins.plugins.sysdig.e2e;

import hudson.model.Result;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class IaCScanE2EFreestyleTests {

  @Rule
  public JenkinsRule jenkins = new JenkinsRule();
  private final JenkinsTestHelpers helpers = new JenkinsTestHelpers(jenkins);

  @Before
  public void setUp() throws Exception {
    helpers.configureSysdigCredentials();
  }

  @Test
  public void testFreestyleWithDefaultConfig() throws Exception {
    var project = helpers.createFreestyleProjectWithIaCScanBuilder()
      .build();

    var build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

    jenkins.assertLogContains("Attempting to download CLI", build);
    jenkins.assertLogContains("Starting scan", build);
    jenkins.assertLogContains("API Credentials not defined. Make sure credentials are defined globally or in job.", build);
  }

  @Test
  public void testFreestyleWithNonExistingToken() throws Exception {
    var project = helpers.createFreestyleProjectWithIaCScanBuilder()
      .withConfig(c -> c.setEngineCredentialsId("non-existing-token"))
      .build();

    var build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

    jenkins.assertLogContains("Attempting to download CLI", build);
    jenkins.assertLogContains("Starting scan", build);
    jenkins.assertLogContains("Cannot find Jenkins credentials by ID: 'non-existing-token'. Ensure credentials are defined in Jenkins before using them", build);
  }

  @Test
  public void testFreestyleWithCredentialsAndAssertLogOutput() throws Exception {
    var project = helpers.createFreestyleProjectWithIaCScanBuilder()
      .withConfig(b -> b.setEngineCredentialsId("sysdig-secure"))
      .build();

    var build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

    jenkins.assertLogContains("Attempting to download CLI", build);
    jenkins.assertLogContains("Downloading https://download.sysdig.com/scanning/bin/sysdig-cli-scanner", build);
    jenkins.assertLogContains("Starting scan", build);
    jenkins.assertLogContains("--iac --apiurl=https://secure.sysdig.com --loglevel=info --recursive --severity-threshold=high", build);
    jenkins.assertLogContains("Process finished with status 3", build);
    jenkins.assertLogContains("(status 401):", build);
  }

  @Test
  public void testFreestyleWithAllConfigs() throws Exception {
    var project = helpers.createFreestyleProjectWithIaCScanBuilder().withConfig(b -> {
      b.setEngineCredentialsId("sysdig-secure");
      b.setPath("custom/path/to/scan");
      b.setListUnsupported(true);
      b.setIsRecursive(false);
      b.setSeverityThreshold("m");
      b.setSysdigEnv("https://us2.app.sysdig.com");
      b.setVersion("1.13.0");
    }).build();

    var build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

    jenkins.assertLogContains("Attempting to download CLI", build);
    jenkins.assertLogContains("Downloading https://download.sysdig.com/scanning/bin/sysdig-cli-scanner/1.13.0", build);
    jenkins.assertLogContains("Starting scan", build);
    jenkins.assertLogContains("--iac --apiurl=https://us2.app.sysdig.com --loglevel=info --list-unsupported-resources --severity-threshold=medium custom/path/to/scan", build);
    jenkins.assertLogContains("Process finished with status 3", build);
  }
}
