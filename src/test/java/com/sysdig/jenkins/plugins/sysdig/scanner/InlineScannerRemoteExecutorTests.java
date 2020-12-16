package com.sysdig.jenkins.plugins.sysdig.scanner;

import com.sysdig.jenkins.plugins.sysdig.BuildConfig;
import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningException;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.Container;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.ContainerRunner;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
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

  private final BuildConfig config = new BuildConfig("name", true, true, false, true, "", SYSDIG_TOKEN, false);

  private InlineScannerRemoteExecutor scannerRemoteExecutor = null;

  //TODO: Sysdig URL

  //TODO: Dockerfile

  //TODO: Skip TLS

  //TODO: Proxy

  //TODO: Throw exception on container run

  //TODO: Handle errors on plugin execution

  //TODO: Handle failed scan, arg errors, other errors

  @Rule public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  private ContainerRunner runner;
  private Container container;
  private SysdigLogger logger;
  private JSONObject outputObject;
  private String logOutput;

  @Before
  public void beforeEach() throws InterruptedException {
    scannerRemoteExecutor = new InlineScannerRemoteExecutor(IMAGE_TO_SCAN, null,null, config);

    runner = mock(ContainerRunner.class);
    container = mock(Container.class);
    logger = mock(SysdigLogger.class);
    outputObject = new JSONObject();
    logOutput = "";

    // Mock container creation to return our mock
    doReturn(container).when(runner).createContainer(any(), any(), any(), any());

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
  public void containerIsCreatedAndExecuted() throws ImageScanningException, InterruptedException {
    // When
    scannerRemoteExecutor.scanImage(runner, logger);

    // Then
    verify(runner, times(1)).createContainer(
      eq(SCAN_IMAGE),
      argThat(args -> args.contains("cat")),
      any(),
      any());

    verify(container, times(1)).runAsync(any());

    verify(container, times(1)).exec(
      argThat( args -> args.contains("/sysdig-inline-scan.sh")),
      any(),
      any());
  }

  @Test
  public void logOutputIsSentToTheLogger() throws ImageScanningException, InterruptedException {
    // Given
    logOutput = "foo-output";

    // When
    scannerRemoteExecutor.scanImage(runner, logger);

    // Then
    verify(logger, atLeastOnce()).logInfo(argThat(msg -> msg.contains("foo-output")));
  }

  @Test
  public void scanJSONOutputIsReturned() throws ImageScanningException, InterruptedException {
    // Given
    outputObject.put("foo-key", "foo-value");

    // When
    JSONObject scanOutput = scannerRemoteExecutor.scanImage(runner, logger);

    // Then
    assertEquals(scanOutput.toString(), outputObject.toString());
  }

  @Test
  public void addedByAnnotationsAreIncluded() throws ImageScanningException, InterruptedException {
    // When
    scannerRemoteExecutor.scanImage(runner, logger);

    // Then
    verify(runner, times(1)).createContainer(any(), any(), any(), argThat(env -> env.contains("SYSDIG_ADDED_BY=cicd-inline-scan")));
  }

  @Test
  public void containerExecutionContainsExpectedParameters() throws ImageScanningException, InterruptedException {
    // When
    scannerRemoteExecutor.scanImage(runner, logger);

    // Then
    verify(container, times(1)).exec(argThat(args -> args.contains("--format=JSON")), isNull(), any());
    verify(container, times(1)).exec(argThat(args -> args.contains(IMAGE_TO_SCAN)), isNull(), any());
  }

  @Test
  public void setSysdigTokenIsProvidedAsEnvironmentVariable() throws ImageScanningException, InterruptedException {
    // When
    scannerRemoteExecutor.scanImage(runner, logger);

    // Then
    verify(runner, times(1)).createContainer(any(), any(), any(), argThat(env -> env.contains("SYSDIG_API_TOKEN=" + SYSDIG_TOKEN)));
  }

}
