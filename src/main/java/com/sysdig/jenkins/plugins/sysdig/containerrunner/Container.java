package com.sysdig.jenkins.plugins.sysdig.containerrunner;

import java.util.List;
import java.util.function.Consumer;

public interface Container {
  void run(Consumer<String> logFrameCallback) throws InterruptedException;
  void runAsync(Consumer<String> logFrameCallback);
  void exec(List<String> args, List<String> envVars, Consumer<String> logFrameCallback) throws InterruptedException;
  void execAsync(List<String> args, List<String> envVars, Consumer<String> logFrameCallback);
  void stop(int timeout);
}
