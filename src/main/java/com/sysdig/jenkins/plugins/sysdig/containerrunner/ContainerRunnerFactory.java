package com.sysdig.jenkins.plugins.sysdig.containerrunner;

import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.EnvVars;

public interface ContainerRunnerFactory {
  ContainerRunner getContainerRunner(SysdigLogger logger, EnvVars currentEnv, String dockerHost);
}
