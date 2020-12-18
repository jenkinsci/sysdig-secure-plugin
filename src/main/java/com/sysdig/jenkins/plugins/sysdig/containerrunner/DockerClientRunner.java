package com.sysdig.jenkins.plugins.sysdig.containerrunner;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.netty.NettyDockerCmdExecFactory;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;

import java.util.ArrayList;
import java.util.List;

public class DockerClientRunner implements ContainerRunner {

  private final DockerClient dockerClient;
  private final SysdigLogger logger;

  public DockerClientRunner(SysdigLogger logger) {
    this.dockerClient = DockerClientBuilder
      .getInstance()
      .withDockerCmdExecFactory(new NettyDockerCmdExecFactory())
      .build();

    this.logger = logger;
  }

  @Override
  public Container createContainer(String imageName, List<String> entryPoint,  List<String> cmd, List<String> envVars, List<String> volumeBinds) throws InterruptedException {

    logger.logInfo(String.format("Pulling image %s", imageName));
    dockerClient.pullImageCmd(imageName).start().awaitCompletion();

    logger.logInfo(String.format("Creating container for image: %s", imageName));

    HostConfig hostConfig = HostConfig.newHostConfig();

    if (volumeBinds != null) {
      List<Bind> binds = new ArrayList<>();
      volumeBinds.forEach(rawBind -> binds.add(Bind.parse(rawBind)));
      hostConfig = hostConfig.withBinds(binds);
    }

    CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(imageName)
      .withStdinOpen(true)
      .withHostConfig(hostConfig);

    if (entryPoint != null) {
      createContainerCmd = createContainerCmd.withEntrypoint(entryPoint);
    }

    if (cmd != null) {
      createContainerCmd = createContainerCmd.withCmd(cmd);
    }

    if (envVars != null) {
      createContainerCmd = createContainerCmd.withEnv(envVars);
    }

    CreateContainerResponse createContainerResponse  = createContainerCmd.exec();
    final String containerId = createContainerResponse.getId();

    logger.logInfo(String.format("Container ID %s", containerId));

    return new DockerClientContainer(this.dockerClient, containerId);
  }

}
