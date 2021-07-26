package com.sysdig.jenkins.plugins.sysdig.containerrunner;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.google.common.base.Strings;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.EnvVars;

import java.util.ArrayList;
import java.util.List;

public class DockerClientRunner implements ContainerRunner {

  private final DockerClient dockerClient;
  private final SysdigLogger logger;

  public DockerClientRunner(SysdigLogger logger, EnvVars currentEnv) {


    DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();
    if (currentEnv.get("DOCKER_HOST") != null) {
      configBuilder.withDockerHost(currentEnv.get("DOCKER_HOST"));
    }

    if (currentEnv.get("DOCKER_TLS_VERIFY") != null) {
      configBuilder.withDockerTlsVerify(currentEnv.get("DOCKER_TLS_VERIFY"));
    }

    if (currentEnv.get("DOCKER_CERT_PATH") != null) {
      configBuilder.withDockerCertPath(currentEnv.get("DOCKER_CERT_PATH"));
    }

    DockerClientConfig config = configBuilder.build();

    ApacheDockerHttpClient.Builder clientBuilder = new ApacheDockerHttpClient.Builder();

    if (config.getDockerHost() != null) {
      clientBuilder.dockerHost(config.getDockerHost());
    }

    if (config.getSSLConfig() != null) {
      clientBuilder.sslConfig(config.getSSLConfig());
    }

    this.dockerClient = DockerClientBuilder
      .getInstance(config)
      .withDockerHttpClient(clientBuilder.build())
      .build();

    logger.logInfo(String.format("DOCKER_HOST=%s", config.getDockerHost()));

    this.logger = logger;
  }

  @Override
  public Container createContainer(String imageName, List<String> entryPoint,  List<String> cmd, List<String> envVars, String user, List<String> volumeBinds) throws InterruptedException {

    logger.logInfo(String.format("Pulling image: %s", imageName));
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

    if (!Strings.isNullOrEmpty(user)) {
      createContainerCmd = createContainerCmd.withUser(user);
    }

    CreateContainerResponse createContainerResponse  = createContainerCmd.exec();
    final String containerId = createContainerResponse.getId();

    logger.logInfo(String.format("Container ID %s", containerId));

    return new DockerClientContainer(this.dockerClient, containerId);
  }

}
