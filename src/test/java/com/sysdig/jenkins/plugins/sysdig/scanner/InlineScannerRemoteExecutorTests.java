package com.sysdig.jenkins.plugins.sysdig.scanner;

import com.sysdig.jenkins.plugins.sysdig.BuildConfig;
import com.sysdig.jenkins.plugins.sysdig.SysdigBuilder;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.Container;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.ContainerRunner;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.ContainerRunnerFactory;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.EnvVars;
import net.sf.json.JSONObject;
import org.junit.*;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;

public class InlineScannerRemoteExecutorTests {
  private static final String SCAN_IMAGE = "quay.io/sysdig/test-secure-inline-scan:2";
  private static final String IMAGE_TO_SCAN = "foo:latest";
  private static final String SYSDIG_TOKEN = "foo-token";

  private InlineScannerRemoteExecutor scannerRemoteExecutor = null;

  //TODO: Throw exception on container run

  //TODO: Handle errors on plugin execution

  //TODO: Handle failed scan, arg errors, other errors

  @Rule
  public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  ContainerRunner containerRunner;
  private Container container;
  private JSONObject outputObject;
  private String logOutput;
  private SysdigLogger logger;
  private EnvVars nodeEnvVars;
  private BuildConfig config;

  @Before
  public void beforeEach() throws InterruptedException {
    config = mock(BuildConfig.class);
    when(config.getSysdigToken()).thenReturn(SYSDIG_TOKEN);
    when(config.getEngineverify()).thenReturn(true);
    when(config.getInlineScanImage()).thenReturn(SCAN_IMAGE);
    // new String in here is not redundant, as we want to make sure that internally we compare by value, not by ref
    when(config.getEngineurl()).thenReturn(new String(SysdigBuilder.DescriptorImpl.DEFAULT_ENGINE_URL));
    when(config.getDebug()).thenReturn(false);

    containerRunner = mock(ContainerRunner.class);
    ContainerRunnerFactory containerRunnerFactory = mock (ContainerRunnerFactory.class);
    when(containerRunnerFactory.getContainerRunner(any(), any())).thenReturn(containerRunner);

    nodeEnvVars = new EnvVars();
    logger = mock(SysdigLogger.class);
    InlineScannerRemoteExecutor.setContainerRunnerFactory(containerRunnerFactory);
    scannerRemoteExecutor = new InlineScannerRemoteExecutor(
      IMAGE_TO_SCAN,
      null,
      config,
      logger,
      nodeEnvVars);

    container = mock(Container.class);
    outputObject = new JSONObject();
    logOutput = "";

    // Mock container creation to return our mock
    doReturn(container).when(containerRunner).createContainer(any(), any(), any(), any(), any());

    // Mock async executions of "tail", to simulate some log output
    doNothing().when(container).execAsync(
      argThat(args -> args.get(0).equals("tail")),
      any(),
      argThat(matcher -> {
        matcher.accept(logOutput);
        return true;
      }),
      any()
    );

    // Mock sync execution of the inline scan script. Mock the JSON output
    doNothing().when(container).exec(
      argThat(args -> args.get(0).equals("/sysdig-inline-scan.sh")),
      any(),
      argThat(matcher -> {
        matcher.accept(outputObject.toString());
        return true;
      }),
      any()
    );

    // Mock execution of the touch or mkdir commands
    doNothing().when(container).exec(
      argThat(args -> args.get(0).equals("touch") || args.get(0).equals("mkdir")),
      any(),
      any(),
      any());
  }

  @Test
  public void containerIsCreatedAndExecuted() throws Exception {
    // When
    scannerRemoteExecutor.call();

    // Then
    verify(containerRunner, times(1)).createContainer(
      eq(SCAN_IMAGE),
      argThat(args -> args.contains("cat")),
      any(),
      any(),
      any());

    verify(container, times(1)).runAsync(any(), any());

    verify(container, times(1)).exec(
      argThat(args -> args.contains("/sysdig-inline-scan.sh")),
      isNull(),
      any(),
      any());
  }

  @Test
  public void containerDoesNotHaveAnyAdditionalParameters() throws Exception {
    // When
    scannerRemoteExecutor.call();

    // Then
    verify(containerRunner, times(1)).createContainer(
      eq(SCAN_IMAGE),
      argThat(args -> args.contains("cat")),
      any(),
      any(),
      any());

    verify(container, never()).exec(
      argThat(args -> args.stream().anyMatch(Pattern.compile("^(--verbose|-v|-s|--sysdig-url|-o|--on-prem|-f|--dockerfile|--sysdig-skip-tls)$").asPredicate()) ),
      isNull(),
      any(),
      any());
  }

  @Test
  public void dockerSocketIsMounted() throws Exception {
    // When
    scannerRemoteExecutor.call();

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
    scannerRemoteExecutor.call();

    // Then
    verify(logger, atLeastOnce()).logInfo(argThat(msg -> msg.contains("foo-output")));
  }

  @Test
  public void scanJSONOutputIsReturned() throws Exception {
    // Given
    outputObject.put("foo-key", "foo-value");

    // When
    String scanOutput = scannerRemoteExecutor.call();

    // Then
    assertEquals(scanOutput, outputObject.toString());
  }

  @Test
  public void addedByAnnotationsAreIncluded() throws Exception {
    // When
    scannerRemoteExecutor.call();

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
    scannerRemoteExecutor.call();

    // Then
    verify(container, times(1)).exec(argThat(args -> args.contains("--format=JSON")), isNull(), any(), any());
    verify(container, times(1)).exec(argThat(args -> args.contains(IMAGE_TO_SCAN)), isNull(), any(), any());
  }

  @Test
  public void customURLIsProvidedAsParameter() throws Exception {
    when(config.getEngineurl()).thenReturn("https://my-foo-url");

    // When
    scannerRemoteExecutor.call();

    // Then
    verify(container, times(1)).exec(argThat(args -> args.contains("--sysdig-url=https://my-foo-url")), isNull(), any(), any());
    verify(container, times(1)).exec(argThat(args -> args.contains("--on-prem")), isNull(), any(), any());
  }

  @Test
  public void verboseIsEnabledWhenDebug() throws Exception {
    when(config.getDebug()).thenReturn(true);

    // When
    scannerRemoteExecutor.call();

    // Then
    verify(container, times(1)).exec(argThat(args -> args.contains("--verbose")), isNull(), any(), any());
  }

  @Test
  public void skipTLSFlagWhenInsecure() throws Exception {
    when(config.getEngineverify()).thenReturn(false);

    // When
    scannerRemoteExecutor.call();

    // Then
    verify(container, times(1)).exec(argThat(args -> args.contains("--sysdig-skip-tls")), isNull(), any(), any());
  }

  @Test
  public void dockerfileIsProvidedAsParameter() throws Exception {
    scannerRemoteExecutor = new InlineScannerRemoteExecutor(IMAGE_TO_SCAN, "/tmp/foo-dockerfile", config, logger, nodeEnvVars);

    // When
    scannerRemoteExecutor.call();

    // Then
    verify(container, times(1)).exec(argThat(args -> args.contains("--dockerfile=/tmp/foo-dockerfile")), isNull(), any(), any());
  }

  @Test
  public void dockerfileIsCopiedInsideContainer() throws Exception {
    scannerRemoteExecutor = new InlineScannerRemoteExecutor(IMAGE_TO_SCAN, "/some/path/foo-dockerfile", config, logger, nodeEnvVars);

    // When
    scannerRemoteExecutor.call();

    verify(container, times(1)).copy("/some/path/foo-dockerfile", "/tmp/");

    verify(containerRunner, times(1)).createContainer(
      eq(SCAN_IMAGE),
      any(),
      any(),
      any(),
      any());
  }


  @Test
  public void setSysdigTokenIsProvidedAsEnvironmentVariable() throws Exception {
    // When
    scannerRemoteExecutor.call();

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
    scannerRemoteExecutor.call();

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
    scannerRemoteExecutor.call();

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
    scannerRemoteExecutor.call();

    // Then
    verify(containerRunner, times(1)).createContainer(
      any(),
      any(),
      any(),
      argThat(env -> env.contains("no_proxy=1.2.3.4,5.6.7.8")),
      any());
  }

  @Test
  public void inlineScanImageCanBeOverriddenWithNodeVars() throws Exception {
    // Given
    nodeEnvVars.put("SYSDIG_OVERRIDE_INLINE_SCAN_IMAGE", "my-repo/my-custom-image:foo");

    // When
    scannerRemoteExecutor.call();

    // Then
    verify(containerRunner, times(1)).createContainer(
      eq("my-repo/my-custom-image:foo"),
      argThat(args -> args.contains("cat")),
      any(),
      any(),
      any());
  }

  @Test
  public void inlineScanImageCanBeOverriddenWithSystemVars() throws Exception {

    // When
    withEnvironmentVariable("SYSDIG_OVERRIDE_INLINE_SCAN_IMAGE", "my-repo/my-custom-image:foo")
      .execute(() -> scannerRemoteExecutor.call());

    // Then
    verify(containerRunner, times(1)).createContainer(
      eq("my-repo/my-custom-image:foo"),
      argThat(args -> args.contains("cat")),
      any(),
      any(),
      any());
  }

}
