package com.sysdig.jenkins.plugins.sysdig.scanner;

import com.sysdig.jenkins.plugins.sysdig.BuildConfig;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.Container;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.ContainerRunner;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.EnvVars;
import net.sf.json.JSONObject;
import org.junit.*;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class InlineScannerRemoteExecutorTests {
  private static final String SCAN_IMAGE = "quay.io/sysdig/secure-inline-scan:2";
  private static final String IMAGE_TO_SCAN = "foo:latest";
  private static final String SYSDIG_TOKEN = "foo-token";

  private InlineScannerRemoteExecutor scannerRemoteExecutor = null;

  //TODO: Sysdig URL

  //TODO: Skip TLS

  //TODO: Throw exception on container run

  //TODO: Handle errors on plugin execution

  //TODO: Handle failed scan, arg errors, other errors

  @Rule
  public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  private ContainerRunner containerRunner;
  private Container container;
  private SysdigLogger logger;
  private JSONObject outputObject;
  private String logOutput;
  private EnvVars nodeEnvVars;
  private BuildConfig config;

  @Before
  public void beforeEach() throws InterruptedException {
    config = mock(BuildConfig.class);
    when(config.getSysdigToken()).thenReturn(SYSDIG_TOKEN);

    scannerRemoteExecutor = new InlineScannerRemoteExecutor(IMAGE_TO_SCAN, null, null, config, null);

    containerRunner = mock(ContainerRunner.class);
    container = mock(Container.class);
    logger = mock(SysdigLogger.class);
    nodeEnvVars = new EnvVars();
    outputObject = new JSONObject();
    logOutput = "";

    // Mock container creation to return our mock
    doReturn(container).when(containerRunner).createContainer(any(), any(), any(), any(), any());

    // Mock async executions of "tail", to simulate some log output
    doNothing().when(container).execAsync(argThat(args -> args.get(0).equals("tail")), any(), argThat(matcher -> {
      matcher.accept(logOutput);
      return true;
    }));

    // Mock sync execution of the inline scan script. Mock the JSON output
    doNothing().when(container).exec(argThat(args -> args.get(0).equals("/sysdig-inline-scan.sh")), any(), argThat(matcher -> {
      matcher.accept(outputObject.toString());
      return true;
    }));

    // Mock execution of the touch or mkdir commands
    doNothing().when(container).exec(argThat(args -> args.get(0).equals("touch") || args.get(0).equals("mkdir")), any(), any());
  }

  @Test
  public void containerIsCreatedAndExecuted() throws Exception {
    // When
    scannerRemoteExecutor.scanImage(containerRunner, logger, nodeEnvVars);

    // Then
    verify(containerRunner, times(1)).createContainer(
      eq(SCAN_IMAGE),
      argThat(args -> args.contains("cat")),
      any(),
      any(),
      any());

    verify(container, times(1)).runAsync(any());

    verify(container, times(1)).exec(
      argThat(args -> args.contains("/sysdig-inline-scan.sh")),
      any(),
      any());
  }

  @Test
  public void dockerSocketIsMounted() throws Exception {
    // When
    scannerRemoteExecutor.scanImage(containerRunner, logger, nodeEnvVars);

    // Then
    verify(containerRunner, times(1)).createContainer(
      eq(SCAN_IMAGE),
      argThat(args -> args.contains("cat")),
      any(),
      any(),
      argThat(args -> args.contains("/var/run/docker.sock:/var/run/docker.sock")));
  }

  @Test
  public void logOutputIsSentToTheLogger() throws Exception {
    // Given
    logOutput = "foo-output";

    // When
    scannerRemoteExecutor.scanImage(containerRunner, logger, nodeEnvVars);

    // Then
    verify(logger, atLeastOnce()).logInfo(argThat(msg -> msg.contains("foo-output")));
  }

  @Test
  public void scanJSONOutputIsReturned() throws Exception {
    // Given
    outputObject.put("foo-key", "foo-value");

    // When
    String scanOutput = scannerRemoteExecutor.scanImage(containerRunner, logger, nodeEnvVars);

    // Then
    assertEquals(scanOutput, outputObject.toString());
  }

  @Test
  public void addedByAnnotationsAreIncluded() throws Exception {
    // When
    scannerRemoteExecutor.scanImage(containerRunner, logger, nodeEnvVars);

    // Then
    verify(containerRunner, times(1)).createContainer(
      any(),
      any(),
      any(),
      argThat(env -> env.contains("SYSDIG_ADDED_BY=cicd-inline-scan")),
      any());
  }

  @Test
  public void containerExecutionContainsExpectedParameters() throws Exception {
    // When
    scannerRemoteExecutor.scanImage(containerRunner, logger, nodeEnvVars);

    // Then
    verify(container, times(1)).exec(argThat(args -> args.contains("--format=JSON")), isNull(), any());
    verify(container, times(1)).exec(argThat(args -> args.contains(IMAGE_TO_SCAN)), isNull(), any());
  }

  @Test
  public void dockerfileIsProvidedAsParameter() throws Exception {
    scannerRemoteExecutor = new InlineScannerRemoteExecutor(IMAGE_TO_SCAN, "/tmp/foo-dockerfile", null, config, null);

    // When
    scannerRemoteExecutor.scanImage(containerRunner, logger, nodeEnvVars);

    // Then
    verify(container, times(1)).exec(argThat(args -> args.contains("--dockerfile=/tmp/Dockerfile")), isNull(), any());
  }

  @Test
  public void dockerfileIsMountedAtTmp() throws Exception {
    scannerRemoteExecutor = new InlineScannerRemoteExecutor(IMAGE_TO_SCAN, "/tmp/foo-dockerfile", null, config, null);

    // When
    scannerRemoteExecutor.scanImage(containerRunner, logger, nodeEnvVars);

    verify(containerRunner, times(1)).createContainer(
      eq(SCAN_IMAGE),
      any(),
      any(),
      any(),
      argThat(arg -> arg.contains("/tmp/foo-dockerfile:/tmp/Dockerfile")));
  }


  @Test
  public void setSysdigTokenIsProvidedAsEnvironmentVariable() throws Exception {
    // When
    scannerRemoteExecutor.scanImage(containerRunner, logger, nodeEnvVars);

    // Then
    verify(containerRunner, times(1)).createContainer(
      any(),
      any(),
      any(),
      argThat(env -> env.contains("SYSDIG_API_TOKEN=" + SYSDIG_TOKEN)),
      any());
  }

  @Test
  public void applyProxyEnvVarsFrom_http_proxy() throws Exception {
    // Given
    nodeEnvVars.put("http_proxy", "http://httpproxy:1234");

    // When
    scannerRemoteExecutor.scanImage(containerRunner, logger, nodeEnvVars);

    // Then
    verify(containerRunner, times(1)).createContainer(
      any(),
      any(),
      any(),
      argThat(env -> env.contains("http_proxy=http://httpproxy:1234")),
      any());
    verify(containerRunner, times(1)).createContainer(
      any(),
      any(),
      any(),
      argThat(env -> env.contains("https_proxy=http://httpproxy:1234")),
      any());
  }

  @Test
  public void applyProxyEnvVarsFrom_https_proxy() throws Exception {
    // Given
    nodeEnvVars.put("http_proxy", "http://httpproxy:1234");
    nodeEnvVars.put("https_proxy", "http://httpsproxy:1234");

    // When
    scannerRemoteExecutor.scanImage(containerRunner, logger, nodeEnvVars);

    // Then
    verify(containerRunner, times(1)).createContainer(
      any(),
      any(),
      any(),
      argThat(env -> env.contains("http_proxy=http://httpproxy:1234")),
      any());
    verify(containerRunner, times(1)).createContainer(
      any(),
      any(),
      any(),
      argThat(env -> env.contains("https_proxy=http://httpsproxy:1234")),
      any());
  }

  @Test
  public void applyProxyEnvVarsFrom_no_proxy() throws Exception {
    // Given
    nodeEnvVars.put("no_proxy", "1.2.3.4,5.6.7.8");

    // When
    scannerRemoteExecutor.scanImage(containerRunner, logger, nodeEnvVars);

    // Then
    verify(containerRunner, times(1)).createContainer(
      any(),
      any(),
      any(),
      argThat(env -> env.contains("no_proxy=1.2.3.4,5.6.7.8")),
      any());
  }
}
