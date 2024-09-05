
import com.sysdig.jenkins.plugins.sysdig.NewEngineBuilder;
import org.apache.commons.lang.SystemUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.ContainerRunner;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.ContainerRunnerFactory;
import com.sysdig.jenkins.plugins.sysdig.scanner.InlineScannerRemoteExecutor;
import hudson.model.Descriptor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;


public class FullWorkflowTests {

  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

  @Before
  public void BeforeEach() {
    ContainerRunner containerRunner = mock(ContainerRunner.class);
    ContainerRunnerFactory containerRunnerFactory = mock(ContainerRunnerFactory.class);
    when(containerRunnerFactory.getContainerRunner(any(), any(), any())).thenReturn(containerRunner);

    InlineScannerRemoteExecutor.setContainerRunnerFactory(containerRunnerFactory);

    NewEngineBuilder.DescriptorImpl desc = new NewEngineBuilder("temp").getDescriptor();
    desc.save();
  }

  @Test
  public void configurationRoundTrip() throws Exception {
    // Given
    FreeStyleProject project = jenkins.createFreeStyleProject();
    project.getBuildersList().add(new NewEngineBuilder("test"));

    // When - this makes sure that the config is saved and retrieved again
    project = jenkins.configRoundtrip(project);

    // Then - the config should be preserved
    jenkins.assertEqualDataBoundBeans(new NewEngineBuilder("test"), project.getBuildersList().get(0));
  }

  @Test
  public void noCredentialsSetError() throws Exception {
    // Given
    FreeStyleProject project = jenkins.createFreeStyleProject();
    NewEngineBuilder builder = new NewEngineBuilder("images_file");
    project.getBuildersList().add(builder);

    // When
    FreeStyleBuild build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

    // Then
    jenkins.assertLogContains("API Credentials not defined", build);
  }

  @Test
  public void credentialsNotFoundError() throws Exception {
    // Given
    FreeStyleProject project = jenkins.createFreeStyleProject();
    NewEngineBuilder builder = new NewEngineBuilder("alpine");
    builder.setEngineCredentialsId("non-existing");
    project.getBuildersList().add(builder);

    // When
    FreeStyleBuild build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

    // Then
    jenkins.assertLogContains("Cannot find Jenkins credentials by ID", build);
  }

  @Test
  public void performFreestyleScanJob() throws Exception {
    // Given
    configureCredentials();

    FreeStyleProject project = jenkins.createFreeStyleProject();

    NewEngineBuilder builder = new NewEngineBuilder("alpine");
    builder.setEngineCredentialsId("sysdig-secure");
    project.getBuildersList().add(builder);

    // When
    // FIXME(fede): Adjust mocks to assert success
    FreeStyleBuild build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

    // Then
    jenkins.assertLogContains("Retrieving MainDB", build);
  }

  @Test
  public void scriptedPipelineScan() throws Exception {
    WorkflowJob job = performScriptedPipelineScanJob(true);
    WorkflowRun build = jenkins.buildAndAssertSuccess(job);
    // Then
    jenkins.assertLogContains("final result PASS", build);
  }

  @Test
  public void declarativePipelineBackendScan() throws Exception {
    performDeclarativePipelineScanJob(false);
  }

  @Test
  public void declarativePipelineInlineScan() throws Exception {
    performDeclarativePipelineScanJob(true);
  }

  @Test
  public void noImageSpecified() throws Exception {
    // Given
    configureCredentials();
    WorkflowJob job = jenkins.createProject(WorkflowJob.class, "no-image-specified");
    String pipelineScript
      = "node {\n"
      + "  sysdigImageScan engineCredentialsId: 'sysdig-secure'\n"
      + "}";
    job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
    WorkflowRun build = jenkins.buildAndAssertStatus(Result.FAILURE, job);

    // Then
    // FIXME(fede) we need to validate that the image has been provided, not just an error
    jenkins.assertLogContains("Failed to perform inline-scan due to an unexpected error", build);
  }

  private WorkflowJob performScriptedPipelineScanJob(boolean bailOnFail) throws Exception {
    // Given
    configureCredentials();
    WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
    String pipelineScript
      = "node {\n"
      + (SystemUtils.IS_OS_WINDOWS
      ? "  bat 'echo my-image:latest > images_file'\n"
      : "  sh 'echo my-image:latest > images_file'\n")
      + "  sysdig engineCredentialsId: 'sysdig-secure', name: 'images_file'"
      + (!bailOnFail ? ", bailOnFail: false" : "")
      + "\n"
      + "}";
    job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
    return job;
  }

  private void performDeclarativePipelineScanJob(boolean inline) throws Exception {
    // Given
    configureCredentials();
    WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-declarative-pipeline");
    String pipelineScript
      = "pipeline {\n"
      + "  agent any\n"
      + "  stages {\n"
      + "    stage('Test') {\n"
      + "      steps {\n"
      + (SystemUtils.IS_OS_WINDOWS
      ? "        bat 'echo my-image:latest > images_file'\n"
      : "        sh 'echo my-image:latest > images_file'\n")
      + "        sysdig engineCredentialsId: 'sysdig-secure', inlineScanning: " + inline + ", name: 'images_file'\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "}\n";
    job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
    WorkflowRun build = jenkins.buildAndAssertSuccess(job);

    // Then
    jenkins.assertLogContains("final result PASS", build);
  }

  private void configureCredentials() throws Descriptor.FormException {
    UsernamePasswordCredentials creds = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "sysdig-secure", "sysdig-secure", "", "foo-token");
    SystemCredentialsProvider.getInstance().getCredentials().add(creds);
  }

}
