import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.sysdig.jenkins.plugins.sysdig.SysdigBuilder;
import com.sysdig.jenkins.plugins.sysdig.client.BackendScanningClientFactory;
import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningException;
import com.sysdig.jenkins.plugins.sysdig.client.SysdigSecureClient;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.Container;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.ContainerRunner;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.ContainerRunnerFactory;
import com.sysdig.jenkins.plugins.sysdig.scanner.BackendScanner;
import com.sysdig.jenkins.plugins.sysdig.scanner.InlineScannerRemoteExecutor;
import hudson.model.*;
import hudson.tasks.*;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.SystemUtils;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;

import static org.mockito.Mockito.*;

public class FullWorkflowTests {
  //TODO: Test (pipeline?) docker daemon not available

  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

  private static final String IMAGE_TO_SCAN = "my-image:tag";
  private static final String MOCK_GATES_REPORT_PASS = "[ {\"foo-digest\": { \"" + IMAGE_TO_SCAN + "\": [ { \"status\": \"pass\", \"detail\": { \"result\": { \"result\": {} } }} ] } } ]";
  private static final String MOCK_GATES_REPORT_FAIL = "[ {\"foo-digest\": { \"" + IMAGE_TO_SCAN + "\": [ { \"status\": \"fail\", \"detail\": { \"result\": { \"result\": {} } }} ] } } ]";
  private static final String MOCK_VULNS_REPORT = "{ \"vulnerabilities\": [] }";
  private SysdigSecureClient client;

  @Before
  public void BeforeEach() throws ImageScanningException, InterruptedException {
    client = mock(SysdigSecureClient.class);
    when(client.submitImageForScanning(any(), any(), any(), anyBoolean())).thenReturn("foo-digest");
    JSONArray gates = JSONArray.fromObject(MOCK_GATES_REPORT_PASS);
    when(client.retrieveImageScanningResults(any(), eq("foo-digest"))).thenReturn(gates);
    JSONObject vulns = JSONObject.fromObject(MOCK_VULNS_REPORT);
    when(client.retrieveImageScanningVulnerabilities(eq("foo-digest"))).thenReturn(vulns);

    BackendScanningClientFactory backendClientFactory = mock(BackendScanningClientFactory.class);
    when(backendClientFactory.newClient(any(), any(), any())).thenReturn(client);
    when(backendClientFactory.newInsecureClient(any(), any(), any())).thenReturn(client);

    ContainerRunner containerRunner = mock(ContainerRunner.class);
    ContainerRunnerFactory containerRunnerFactory = mock (ContainerRunnerFactory.class);
    when(containerRunnerFactory.getContainerRunner(any(), any())).thenReturn(containerRunner);

    InlineScannerRemoteExecutor.setContainerRunnerFactory(containerRunnerFactory);
    BackendScanner.setBackendScanningClientFactory(backendClientFactory);

    Container container = mock(Container.class);
    doReturn(container).when(containerRunner).createContainer(any(), any(), any(), any(), any());

    JSONObject output = new JSONObject();
    output.put("digest", "foo-digest");
    output.put("tag", IMAGE_TO_SCAN);
    output.put("scanReport", gates);
    output.put("vulnsReport", vulns);

    // Mock sync execution of the inline scan script. Mock the JSON output
    doNothing().when(container).exec(
      argThat(args -> args.get(0).equals("/sysdig-inline-scan.sh")),
      any(),
      argThat(matcher -> {
        matcher.accept(output.toString());
        return true;
      }),
      any()
    );

    SysdigBuilder.DescriptorImpl desc = new SysdigBuilder("temp").getDescriptor();
    desc.setDebug(true);
    desc.save();
  }

  @Test
  public void configurationRoundTrip() throws Exception {
    // Given
    FreeStyleProject project = jenkins.createFreeStyleProject();
    project.getBuildersList().add(new SysdigBuilder("test"));

    // When - this makes sure that the config is saved and retrieved again
    project = jenkins.configRoundtrip(project);

    // Then - the config should be preserved
    jenkins.assertEqualDataBoundBeans(new SysdigBuilder("test"), project.getBuildersList().get(0));
  }

  @Test
  public void noCredentialsSetError() throws Exception {
    // Given
    FreeStyleProject project = jenkins.createFreeStyleProject();
    SysdigBuilder builder = new SysdigBuilder("images_file");
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
    SysdigBuilder builder = new SysdigBuilder("images_file");
    builder.setEngineCredentialsId("non-existing");
    project.getBuildersList().add(builder);

    // When
    FreeStyleBuild build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

    // Then
    jenkins.assertLogContains("Cannot find Jenkins credentials by ID", build);
  }

  @Test
  public void freestyleBackendScan() throws Exception {
    performFreestyleScanJob(false);
  }

  @Test
  public void freestyleInlineScan() throws Exception {
    performFreestyleScanJob(true);
  }

  @Test
  public void scriptedPipelineBackendScan() throws Exception {
    WorkflowJob job = performScriptedPipelineScanJob(false, true);
    WorkflowRun build = jenkins.buildAndAssertSuccess(job);
    // Then
    jenkins.assertLogContains("final result PASS", build);
  }

  @Test
  public void scriptedPipelineInlineScanPass() throws Exception {
    WorkflowJob job = performScriptedPipelineScanJob(true, true);
    WorkflowRun build = jenkins.buildAndAssertSuccess(job);
    // Then
    jenkins.assertLogContains("final result PASS", build);
  }

  @Test
  public void scriptedPipelineBackendScanFail() throws Exception {
    when(client.retrieveImageScanningResults(any(), eq("foo-digest"))).thenReturn(JSONArray.fromObject(MOCK_GATES_REPORT_FAIL));
    WorkflowJob job = performScriptedPipelineScanJob(false, true);
    WorkflowRun build = jenkins.buildAndAssertStatus(Result.FAILURE, job);
    // Then
    jenkins.assertLogContains("final result FAIL", build);
  }

  @Test
  public void scriptedPipelineBackendScanFailButIgnore() throws Exception {
    when(client.retrieveImageScanningResults(any(), eq("foo-digest"))).thenReturn(JSONArray.fromObject(MOCK_GATES_REPORT_FAIL));
    WorkflowJob job = performScriptedPipelineScanJob(false, false);
    WorkflowRun build = jenkins.buildAndAssertStatus(Result.SUCCESS, job);
    // Then
    jenkins.assertLogContains("final result FAIL", build);
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
  public void missingImagesFile() throws Exception {
    // Given
    configureCredentials();
    WorkflowJob job = jenkins.createProject(WorkflowJob.class, "no-images-file");
    String pipelineScript
      = "node {\n"
      + "  sysdig engineCredentialsId: 'sysdig-secure', name: 'images_file'\n"
      + "}";
    job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
    WorkflowRun build = jenkins.buildAndAssertStatus(Result.FAILURE, job);

    // Then
    jenkins.assertLogContains("Image list file 'images_file' not found", build);
  }

  private void performFreestyleScanJob(boolean inline) throws Exception {
    // Given
    configureCredentials();

    FreeStyleProject project = jenkins.createFreeStyleProject();

    if (SystemUtils.IS_OS_WINDOWS) {
      BatchFile batch = new BatchFile("echo my-image:latest > images_file");
      project.getBuildersList().add(batch);
    } else {
      Shell shell = new Shell("echo my-image:latest > images_file");
      project.getBuildersList().add(shell);
    }

    SysdigBuilder builder = new SysdigBuilder("images_file");
    builder.setEngineCredentialsId("sysdig-secure");
    builder.setInlineScanning(inline);
    project.getBuildersList().add(builder);

    // When
    FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

    // Then
    jenkins.assertLogContains("final result PASS", build);
  }

  private WorkflowJob performScriptedPipelineScanJob(boolean inline, boolean bailOnFail) throws Exception {
    // Given
    configureCredentials();
    WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
    String pipelineScript
      = "node {\n"
      + (SystemUtils.IS_OS_WINDOWS
      ? "  bat 'echo my-image:latest > images_file'\n"
      : "  sh 'echo my-image:latest > images_file'\n")
      + "  sysdig engineCredentialsId: 'sysdig-secure', inlineScanning: " + inline + ", name: 'images_file'"
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

  private void configureCredentials() {
    UsernamePasswordCredentials creds = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "sysdig-secure", "sysdig-secure", "", "foo-token");
    SystemCredentialsProvider.getInstance().getCredentials().add(creds);
  }

}
