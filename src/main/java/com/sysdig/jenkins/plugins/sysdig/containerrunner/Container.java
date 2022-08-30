package com.sysdig.jenkins.plugins.sysdig.containerrunner;

import java.util.List;
import java.util.function.Consumer;

public interface Container {
  void run(Consumer<String> stdoutCallback, Consumer<String> stderrCallback) throws InterruptedException;

  void runAsync(Consumer<String> stdoutCallback, Consumer<String> stderrCallback);

  void exec(List<String> args, List<String> envVars, Consumer<String> stdoutCallback, Consumer<String> stderrCallback) throws InterruptedException;

  void execAsync(List<String> args, List<String> envVars, Consumer<String> stdoutCallback, Consumer<String> stderrCallback);

  void stop(int timeout);

  void copy(String source, String destinationFolder);

  void ping();
}
