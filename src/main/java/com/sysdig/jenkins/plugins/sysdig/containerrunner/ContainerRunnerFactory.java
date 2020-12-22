package com.sysdig.jenkins.plugins.sysdig.containerrunner;

import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;

public interface ContainerRunnerFactory {
  ContainerRunner getContainerRunner(SysdigLogger logger);
}
