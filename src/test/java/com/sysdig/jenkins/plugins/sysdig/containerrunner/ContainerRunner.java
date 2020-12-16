package com.sysdig.jenkins.plugins.sysdig.containerrunner;

public class ContainerRunner {

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
