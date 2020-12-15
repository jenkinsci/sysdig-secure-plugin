package com.sysdig.jenkins.plugins.sysdig.containerrunner;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.netty.NettyDockerCmdExecFactory;
import com.sysdig.jenkins.plugins.sysdig.log.ConsoleLog;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.model.TaskListener;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class DockerClientRunner implements ContainerRunner {

  private final DockerClient dockerClient;
  private final SysdigLogger logger;

  public DockerClientRunner(TaskListener listener, boolean enableDebug) {
    this.dockerClient = DockerClientBuilder
      .getInstance()
      .withDockerCmdExecFactory(new NettyDockerCmdExecFactory())
      .build();

    this.logger = new ConsoleLog(
      this.getClass().getSimpleName(),
      listener.getLogger(),
      enableDebug);
  }

  @Override
  public String runContainer(String imageName, List<String> args, List<String> envVars) throws InterruptedException {
    logger.logInfo(String.format("Pulling image %s", imageName));
    dockerClient.pullImageCmd(imageName).start().awaitCompletion();

    logger.logInfo(String.format("Creating container for image: %s", imageName));
    String scanningContainerID = this.createContainer(imageName, args, envVars);

    logger.logInfo(String.format("Executing container for image: %s", imageName));
    return this.executeContainer(scanningContainerID);
  }

  private String createContainer(String imageName, List<String> args, List<String> envVars) {
    HostConfig hostConfig = HostConfig.newHostConfig()
      .withAutoRemove(true)
      .withBinds(
        Bind.parse("/var/run/docker.sock:/var/run/docker.sock"));

    CreateContainerResponse createdScanningContainer = dockerClient.createContainerCmd(imageName)
      .withCmd(args.toArray(new String[0]))
      .withEnv(envVars)
      .withHostConfig(hostConfig)
      .exec();

    return createdScanningContainer.getId();
  }

  private String executeContainer(String scanningContainerID) throws InterruptedException {
    ResultCallbackTemplate<?, Frame> logCallback = new ResultCallback.Adapter<Frame>() {
      final StringBuilder builder = new StringBuilder();

      @Override
      public void onNext(Frame item) {
        builder.append(new String(item.getPayload(), StandardCharsets.UTF_8).replaceAll("\\s+$", ""));
        super.onNext(item);
      }

      @Override
      public String toString() {
        return builder.toString();
      }
    };

    dockerClient.startContainerCmd(scanningContainerID)
      .exec();

    dockerClient.logContainerCmd(scanningContainerID)
      .withStdOut(true)
      .withStdErr(true)
      .withFollowStream(true)
      .withTailAll()
      .exec(logCallback)
      .awaitCompletion();

    return logCallback.toString();
  }


}
