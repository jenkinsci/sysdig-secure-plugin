import hudson.model.Result;
import hudson.model.Run;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class ImageScanningE2ETests {
  @Rule
  public JenkinsRule jenkins = new JenkinsRule();
  private final JenkinsTestHelpers helpers = new JenkinsTestHelpers(jenkins);

  @Before
  public void setUp() throws Exception {
    helpers.configureSysdigCredentials();
  }

  @Test
  public void testBuildFailsWhenNoCredentialsSet() throws Exception {
    var project = helpers.createFreestyleProjectWithNewEngineBuilder("images_file");

    var build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

    jenkins.assertLogContains("API Credentials not defined", build);
  }

  @Test
  public void testBuildFailsWhenCredentialsNotFound() throws Exception {
    var project = helpers.createFreestyleProjectWithNewEngineBuilder("alpine", "non-existing");

    var build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

    jenkins.assertLogContains("Cannot find Jenkins credentials by ID", build);
  }

  @Test
  public void testPipelineFailsWhenNoImageSpecified() throws Exception {
    var job = helpers.createPipelineJobWithScript(
      "node {\n" +
        "  sysdigImageScan engineCredentialsId: 'sysdig-secure'\n"
        + "}"
    );

    var build = jenkins.buildAndAssertStatus(Result.FAILURE, job);

    jenkins.assertLogContains("Failed to perform inline-scan due to an unexpected error", build);
  }

  @Test
  public void testFreestyleScanJobLogOutput() throws Exception {
    var project = helpers.createFreestyleProjectWithNewEngineBuilder("alpine", "sysdig-secure");

    var build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

    // FIXME(fede): We should be using mocks that verify that everything is working properly, and the run succeeds.
    assertScanJobLogOutput(build);
  }

  @Test
  public void testPipelineScanJobLogOutput() throws Exception {
    var job = helpers.createPipelineJobWithScript("node {\n" + "  sysdigImageScan engineCredentialsId: 'sysdig-secure', imageName: 'alpine'\n" + "}");

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
}
