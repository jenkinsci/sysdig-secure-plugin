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
    jenkins.assertLogContains("environment variable SECURE_API_TOKEN cannot be empty", build);
  }

  @Test
  public void testFreestyleWithCredentialsAndAssertLogOutput() throws Exception {
    var project = helpers.createFreestyleProjectWithIaCScanBuilder()
      .withConfig(b -> b.setSecureAPIToken("valid-token"))
      .build();

    var build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

    jenkins.assertLogContains("Attempting to download CLI", build);
    jenkins.assertLogContains("CLI executable path:", build);
    jenkins.assertLogContains("Starting scan", build);
    jenkins.assertLogContains("Process finished with status 3", build);
    jenkins.assertLogContains("(status 401):", build);
  }

  @Test
  public void testFreestyleWithAllConfigs() throws Exception {
    var project = helpers.createFreestyleProjectWithIaCScanBuilder().withConfig(b -> {
      b.setSecureAPIToken("valid-token");
      b.setPath("custom/path/to/scan");
      b.setListUnsupported(true);
      b.setIsRecursive(false);
      b.setSeverityThreshold("high");
      b.setSysdigEnv("https://us2.app.sysdig.com");
      b.setVersion("1.13.0");
    }).build();

    var build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

    jenkins.assertLogContains("Attempting to download CLI", build);
    jenkins.assertLogContains("CLI executable path:", build);
    jenkins.assertLogContains("1.13.0", build);
    jenkins.assertLogContains("Starting scan", build);
    jenkins.assertLogContains("Process finished with status 3", build);
  }
}
