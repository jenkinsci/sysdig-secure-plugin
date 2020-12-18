package com.sysdig.jenkins.plugins.sysdig.containerrunner;

import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;

public class DockerClientContainerFactory implements ContainerRunnerFactory {
  @Override
  public ContainerRunner getContainerRunner(SysdigLogger logger) {
    return new DockerClientRunner(logger);
  }
}
