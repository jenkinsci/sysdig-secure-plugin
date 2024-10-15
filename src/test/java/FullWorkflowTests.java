import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.sysdig.jenkins.plugins.sysdig.NewEngineBuilder;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;

public class FullWorkflowTests {
  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

  @Before
  public void setUp() throws Exception {
    var desc = new NewEngineBuilder("temp").getDescriptor();
    desc.save();
    configureSysdigCredentials();
  }

  private void configureSysdigCredentials() throws Descriptor.FormException {
    var creds = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "sysdig-secure", "sysdig-secure", "", "foo-token");
    SystemCredentialsProvider.getInstance().getCredentials().add(creds);
  }

  @Test
  public void testConfigurationPersistenceAfterRoundTrip() throws Exception {
    var project = createFreestyleProjectWithNewEngineBuilder("test");

    project = jenkins.configRoundtrip(project);

    jenkins.assertEqualDataBoundBeans(new NewEngineBuilder("test"), project.getBuildersList().get(0));
  }

  @Test
  public void testDefaultGlobalConfigurationValues() throws Exception {
    var globalConfig = jenkins.getInstance()
      .getDescriptorByType(NewEngineBuilder.GlobalConfiguration.class);

    assertEquals("https://secure.sysdig.com", globalConfig.getEngineURL());
    assertEquals("", globalConfig.getEngineCredentialsId());
    assertTrue(globalConfig.getEngineVerify());
    assertEquals("", globalConfig.getInlineScanExtraParams());
    assertEquals("", globalConfig.getScannerBinaryPath());
    assertEquals("", globalConfig.getPoliciesToApply());
    assertEquals("", globalConfig.getCliVersionToApply());
    assertEquals("", globalConfig.getCustomCliVersion());
    assertTrue(globalConfig.getBailOnFail());
    assertTrue(globalConfig.getBailOnPluginFail());
  }

  @Test
  public void testGlobalConfigurationRoundTrip() throws Exception {
    // Access global configuration using Jenkins.getDescriptorByType()
    var globalConfig = jenkins.getInstance()
      .getDescriptorByType(NewEngineBuilder.GlobalConfiguration.class);

    // Change configuration values
    globalConfig.setEngineURL("https://custom.sysdig.com");
    globalConfig.setEngineCredentialsId("sysdig-secure");
    globalConfig.setEngineVerify(false);
    globalConfig.setInlineScanExtraParams("--some-extra-param");
    globalConfig.setScannerBinaryPath("/path/to/scanner");
    globalConfig.setPoliciesToApply("policy1,policy2");
    globalConfig.setCliVersionToApply("custom");
    globalConfig.setCustomCliVersion("1.5.0");
    globalConfig.setBailOnFail(false);
    globalConfig.setBailOnPluginFail(false);

    // Save the new configuration
    globalConfig.save();

    // Perform a configuration round-trip to simulate saving through the UI
    jenkins.configRoundtrip();

    // Reload the configuration to ensure the changes persist after the round-trip
    var reloadedConfig = jenkins.getInstance()
      .getDescriptorByType(NewEngineBuilder.GlobalConfiguration.class);

    // Assert that the values were saved and reloaded correctly after the round-trip
    assertEquals("https://custom.sysdig.com", reloadedConfig.getEngineURL());
    assertEquals("sysdig-secure", reloadedConfig.getEngineCredentialsId());
    assertFalse(reloadedConfig.getEngineVerify());
    assertEquals("--some-extra-param", reloadedConfig.getInlineScanExtraParams());
    assertEquals("/path/to/scanner", reloadedConfig.getScannerBinaryPath());
    assertEquals("policy1,policy2", reloadedConfig.getPoliciesToApply());
    assertEquals("custom", reloadedConfig.getCliVersionToApply());
    assertEquals("1.5.0", reloadedConfig.getCustomCliVersion());
    assertFalse(reloadedConfig.getBailOnFail());
    assertFalse(reloadedConfig.getBailOnPluginFail());
  }


  @Test
  public void testBuildFailsWhenNoCredentialsSet() throws Exception {
    var project = createFreestyleProjectWithNewEngineBuilder("images_file");

    var build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

    jenkins.assertLogContains("API Credentials not defined", build);
  }

  @Test
  public void testBuildFailsWhenCredentialsNotFound() throws Exception {
    var project = createFreestyleProjectWithNewEngineBuilder("alpine", "non-existing");

    var build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

    jenkins.assertLogContains("Cannot find Jenkins credentials by ID", build);
  }

  @Test
  public void testPipelineFailsWhenNoImageSpecified() throws Exception {
    var job = createPipelineJobWithScript(
      "node {\n" +
        "  sysdigImageScan engineCredentialsId: 'sysdig-secure'\n"
        + "}"
    );

    var build = jenkins.buildAndAssertStatus(Result.FAILURE, job);

    jenkins.assertLogContains("Failed to perform inline-scan due to an unexpected error", build);
  }

  @Test
  public void testFreestyleScanJobLogOutput() throws Exception {
    var project = createFreestyleProjectWithNewEngineBuilder("alpine", "sysdig-secure");

    var build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

    // FIXME(fede): We should be using mocks that verify that everything is working properly, and the run succeeds.
    assertScanJobLogOutput(build);
  }

  @Test
  public void testPipelineScanJobLogOutput() throws Exception {
    var job = createPipelineJobWithScript("node {\n" + "  sysdigImageScan engineCredentialsId: 'sysdig-secure', imageName: 'alpine'\n" + "}");

    var build = jenkins.buildAndAssertStatus(Result.FAILURE, job);

    // FIXME(fede): We should be using mocks that verify that everything is working properly, and the run succeeds.
    assertScanJobLogOutput(build);
  }

  private void assertScanJobLogOutput(Run<?, ?> build) throws Exception {
    jenkins.assertLogContains("Using new-scanning engine", build);
    jenkins.assertLogContains("Image Name: alpine", build);
    jenkins.assertLogContains("Downloading inlinescan v1.16.1", build);
    jenkins.assertLogContains("Unable to retrieve MainDB", build);
    jenkins.assertLogContains("401 Unauthorized", build);
    jenkins.assertLogContains("Failed to perform inline-scan due to an unexpected error", build);
  }

  private FreeStyleProject createFreestyleProjectWithNewEngineBuilder(String imageName) throws Exception {
    var project = jenkins.createFreeStyleProject();
    var builder = new NewEngineBuilder(imageName);
    project.getBuildersList().add(builder);
    return project;
  }

  private FreeStyleProject createFreestyleProjectWithNewEngineBuilder(String imageName, String credentialsId) throws Exception {
    var project = createFreestyleProjectWithNewEngineBuilder(imageName);
    var builder = (NewEngineBuilder) project.getBuildersList().get(0);
    builder.setEngineCredentialsId(credentialsId);
    return project;
  }

  private WorkflowJob createPipelineJobWithScript(String script) throws Exception {
    var job = jenkins.createProject(WorkflowJob.class, "test-pipeline");
    job.setDefinition(new CpsFlowDefinition(script, true));
    return job;
  }
}
