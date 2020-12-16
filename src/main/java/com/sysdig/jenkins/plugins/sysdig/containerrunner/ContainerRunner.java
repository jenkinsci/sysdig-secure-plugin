package com.sysdig.jenkins.plugins.sysdig.containerrunner;

import java.util.List;

public interface ContainerRunner {
  public Container createContainer(String imageName, List<String> entryPoint,  List<String> cmd, List<String> envVars) throws InterruptedException;
}
