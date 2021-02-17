package com.sysdig.jenkins.plugins.sysdig.containerrunner;

import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.EnvVars;

import java.io.Serializable;

public class DockerClientContainerFactory implements ContainerRunnerFactory, Serializable {
  @Override
  public ContainerRunner getContainerRunner(SysdigLogger logger, EnvVars currentEnv) {
    return new DockerClientRunner(logger, currentEnv);
  }
}
