package com.sysdig.jenkins.plugins.sysdig.containerrunner;

import java.util.List;

public interface ContainerRunner {
  String runContainer(String imageName, List<String> args, List<String> envVars) throws InterruptedException;
}
