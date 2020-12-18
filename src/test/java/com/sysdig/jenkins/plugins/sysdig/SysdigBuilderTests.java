package com.sysdig.jenkins.plugins.sysdig;

import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.sysdig.jenkins.plugins.sysdig.client.BackendScanningClientFactory;
import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningException;
import com.sysdig.jenkins.plugins.sysdig.client.SysdigSecureClient;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.Container;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.ContainerRunner;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.ContainerRunnerFactory;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.Shell;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;

import static org.mockito.Mockito.*;

public class SysdigBuilderTests {

  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

  //TODO: Test declarative pipeline backend scan
  //TODO: Test declarative pipeline inline scan
  //TODO: Test scripted pipeline backend scan
  //TODO: Test scripted pipeline  inline scan

  //TODO: Test (pipeline?) missing image_file
  //TODO: Test (pipeline?) docker daemon not available

  private BackendScanningClientFactory backendClientFactory;
  private SysdigSecureClient client;
  private ContainerRunnerFactory containerRunnerFactory;
  private ContainerRunner containerRunner;

  private static final String IMAGE_TO_SCAN = "my-image:tag";
  private static final String MOCK_GATES_REPORT = "[ {\"foo-digest\": { \"" + IMAGE_TO_SCAN + "\": [ { \"status\": \"pass\", \"detail\": { \"result\": { \"result\": {} } }} ] } } ]";
  private static final String MOCK_VULNS_REPORT = "{ \"vulnerabilities\": [] }";

  @Before
  public void BeforeEach() throws ImageScanningException, InterruptedException {
    client = mock(SysdigSecureClient.class);
    when(client.submitImageForScanning(any(), any(), any())).thenReturn("foo-digest");
    JSONArray gates = JSONArray.fromObject(MOCK_GATES_REPORT);
    when(client.retrieveImageScanningResults(any(), eq("foo-digest"))).thenReturn(gates);
    JSONObject vulns = JSONObject.fromObject(MOCK_VULNS_REPORT);
    when(client.retrieveImageScanningVulnerabilities(eq("foo-digest"))).thenReturn(vulns);

    backendClientFactory = mock(BackendScanningClientFactory.class);
    when(backendClientFactory.newClient(any(), any(), any())).thenReturn(client);
    when(backendClientFactory.newInsecureClient(any(), any(), any())).thenReturn(client);

    containerRunner = mock(ContainerRunner.class);
    containerRunnerFactory = mock (ContainerRunnerFactory.class);
    when(containerRunnerFactory.getContainerRunner(any())).thenReturn(containerRunner);

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
  public void noCredentialsError() throws Exception {
    // Given
    FreeStyleProject project = jenkins.createFreeStyleProject();
    SysdigBuilder builder = new SysdigBuilder("images_file");
    project.getBuildersList().add(builder);

    // When
    FreeStyleBuild build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

    // Then
    jenkins.assertLogContains("Cannot find Jenkins credentials by ID", build);
  }

  private void performFreestyleScanJob(boolean inline) throws Exception {
    // Given
    UsernamePasswordCredentials creds = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "sysdig-secure", "sysdig-secure", "", "foo-token");
    SystemCredentialsProvider.getInstance().getCredentials().add(creds);

    FreeStyleProject project = jenkins.createFreeStyleProject();

    Shell shell = new Shell("echo my-image:latest > images_file");
    project.getBuildersList().add(shell);

    SysdigBuilder builder = new SysdigBuilder("images_file");
    builder.setBackendClientFactory(backendClientFactory);
    builder.setContainerRunnerFactory(containerRunnerFactory);
    builder.setEngineCredentialsId("sysdig-secure");
    builder.setInlineScanning(inline);
    project.getBuildersList().add(builder);

    // When
    FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

    // Then
    jenkins.assertLogContains("final result PASS", build);
  }

  @Test
  public void freestyleBackendScan() throws Exception {
    performFreestyleScanJob(false);
  }

  @Test
  public void freestyleInlineScan() throws Exception {
    performFreestyleScanJob(true);
  }


}
