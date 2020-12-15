package com.sysdig.jenkins.plugins.sysdig.scanner;

import com.sysdig.jenkins.plugins.sysdig.BuildConfig;
import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningException;
import com.sysdig.jenkins.plugins.sysdig.containerrunner.ContainerRunner;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import net.sf.json.JSONObject;
import org.junit.*;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.*;

public class InlineScannerRemoteExecutorTests {
  private final String SCAN_IMAGE = "quay.io/sysdig/secure-inline-scan:2";
  private final String IMAGE_TO_SCAN = "foo:latest";
  private final String SYSDIG_TOKEN = "foo-token";

  private final BuildConfig config = new BuildConfig("name", true, true, false, true, "", SYSDIG_TOKEN, false);

  private InlineScannerRemoteExecutor scannerRemoteExecutor = new InlineScannerRemoteExecutor(IMAGE_TO_SCAN, null,null, config);

  //TODO: Sysdig URL

  //TODO: Dockerfile

  //TODO: Skip TLS

  //TODO: Proxy

  //TODO: Handle errors, failed scan, arg errors, other errors

  @Rule public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Test
  public void testContainerIsExecuted() throws ImageScanningException, InterruptedException {

    // Given
    ContainerRunner runner = mock(ContainerRunner.class);
    SysdigLogger logger = mock(SysdigLogger.class);

    JSONObject outputObject = new JSONObject();

    // When
    when(runner.runContainer(any(), any(), any())).thenReturn(outputObject.toString());

    // Do
    scannerRemoteExecutor.scanImage(runner, logger);

    // Then
    verify(runner, atLeastOnce()).runContainer(eq(SCAN_IMAGE), any(), any());
  }


  @Test
  public void testOutputIsLogged() throws ImageScanningException, InterruptedException {

    // Given
    ContainerRunner runner = mock(ContainerRunner.class);
    SysdigLogger logger = mock(SysdigLogger.class);

    JSONObject outputObject = new JSONObject();
    outputObject.put("log", "foo-output");

    // When
    when(runner.runContainer(any(), any(), any())).thenReturn(outputObject.toString());

    // Do
    scannerRemoteExecutor.scanImage(runner, logger);

    // Then
    verify(logger, atLeastOnce()).logInfo(argThat(msg -> msg.contains("foo-output")));
  }

  @Test
  public void testDefaultContainerParameters() throws ImageScanningException, InterruptedException {

    // Given
    ContainerRunner runner = mock(ContainerRunner.class);
    SysdigLogger logger = mock(SysdigLogger.class);

    JSONObject outputObject = new JSONObject();
    // When
    when(runner.runContainer(any(), any(), any())).thenReturn(outputObject.toString());

    // Do
    scannerRemoteExecutor.scanImage(runner, logger);

    // Then
    verify(runner, atLeastOnce()).runContainer(any(), argThat(args -> args.contains("--format=JSON")), any());
    verify(runner, atLeastOnce()).runContainer(any(), argThat(args -> args.contains(IMAGE_TO_SCAN)), any());
  }

  @Test
  public void testSysdigTokenAsEnvVar() throws ImageScanningException, InterruptedException {

    // Given
    ContainerRunner runner = mock(ContainerRunner.class);
    SysdigLogger logger = mock(SysdigLogger.class);

    JSONObject outputObject = new JSONObject();
    // When
    when(runner.runContainer(any(), any(), any())).thenReturn(outputObject.toString());

    // Do
    scannerRemoteExecutor.scanImage(runner, logger);

    // Then
    verify(runner, atLeastOnce()).runContainer(any(), any(), argThat(env -> env.contains("SYSDIG_API_TOKEN=" + SYSDIG_TOKEN)));
    verify(runner, atLeastOnce()).runContainer(any(), argThat(args -> args.contains(IMAGE_TO_SCAN)), any());
  }

//  @Test
//  public void failTest() throws ImageScanningException, InterruptedException {
//    SysdigLogger mockedLogger = mock(SysdigLogger.class);
//
//    DockerClient x = DockerClientImpl.getInstance();
//    DockerClient spy = spy(x);)
//
//
//    DockerClient mockedDockerClient = mock(DockerClient.class);
//    PullImageCmd mockerPullImageCmd = mock(PullImageCmd.class);
//    ResultCallback.Adapter<PullResponseItem> mockedCallback = mock(ResultCallback.Adapter.class);
//    PullImageResultCallback pullCallback = new PullImageResultCallback();
//
//    CreateContainerCmd createContainerCmd = mock(CreateContainerCmd.class);
//    CreateContainerResponse createContainerResponse = new CreateContainerResponse();
//
//    StartContainerCmd startContainerCmd = mock(StartContainerCmd.class);
//
//    LogContainerCmd logContainerCmd = mock (LogContainerCmd.class);
//    ResultCallback.Adapter<Frame> logContainerResultCallback = mock(ResultCallback.Adapter.class);
//
//    when(mockedDockerClient.pullImageCmd(SCAN_IMAGE)).thenReturn(mockerPullImageCmd);
//    when(mockerPullImageCmd.start()).thenReturn(mockedCallback);
//    when(mockedCallback.awaitCompletion()).thenReturn(pullCallback);
//
//    when(mockedDockerClient.createContainerCmd(SCAN_IMAGE)).thenReturn(createContainerCmd);
//    when(createContainerCmd.withCmd((String[])any())).thenReturn(createContainerCmd);
//    when(createContainerCmd.withEnv((String)any())).thenReturn(createContainerCmd);
//    when(createContainerCmd.withHostConfig(any())).thenReturn(createContainerCmd);
//    when(createContainerCmd.exec()).thenReturn(createContainerResponse);
//
//    when(mockedDockerClient.startContainerCmd(any())).thenReturn(startContainerCmd);
//
//
//    when(mockedDockerClient.logContainerCmd(any())).thenReturn(logContainerCmd);
//    when(logContainerCmd.withStdOut(any())).thenReturn(logContainerCmd);
//    when(logContainerCmd.withStdErr(any())).thenReturn(logContainerCmd);
//    when(logContainerCmd.withFollowStream(any())).thenReturn(logContainerCmd);
//    when(logContainerCmd.withTailAll()).thenReturn(logContainerCmd);
//    when(logContainerCmd.exec(any())).thenReturn(logContainerResultCallback);
//
//    scannerRemoteExecutor.scanImage(mockedDockerClient, mockedLogger);
//
//    assertEquals(0,0);
//  }
}
