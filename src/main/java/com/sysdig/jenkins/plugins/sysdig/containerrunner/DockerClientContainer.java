package com.sysdig.jenkins.plugins.sysdig.containerrunner;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmd;
import com.github.dockerjava.api.model.Frame;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

public class DockerClientContainer implements Container {

  private final DockerClient dockerClient;
  private final String containerId;

  public DockerClientContainer(DockerClient dockerClient, String containerId) {
    this.dockerClient = dockerClient;
    this.containerId = containerId;
  }

  @Override
  public void run(Consumer<String> logFrameCallback) throws InterruptedException {
    runAsyncWithAdapter(logFrameCallback).awaitCompletion();
  }

  @Override
  public void runAsync(Consumer<String> logFrameCallback) {
    runAsyncWithAdapter(logFrameCallback);
  }

  private ResultCallback.Adapter<Frame> runAsyncWithAdapter(Consumer<String> logFrameCallback) {
    dockerClient.startContainerCmd(this.containerId)
      .exec();

    return dockerClient.logContainerCmd(this.containerId)
      .withStdOut(true)
      .withStdErr(true)
      .withFollowStream(true)
      .withTailAll()
      .exec(new ResultCallback.Adapter<Frame>() {
        @Override
        public void onNext(Frame item) {
          if (logFrameCallback != null){
            logFrameCallback.accept(new String(item.getPayload(), StandardCharsets.UTF_8));
          }
          super.onNext(item);
        }
      });
  }

  @Override
  public void exec(List<String> cmd, List<String> envVars, Consumer<String> logFrameCallback) throws InterruptedException {
     execAsyncWithAdapter(cmd, envVars, logFrameCallback).awaitCompletion();
  }

  @Override
  public void execAsync(List<String> cmd, List<String> envVars, Consumer<String> logFrameCallback) {
    execAsyncWithAdapter(cmd, envVars, logFrameCallback);
  }

  private ResultCallback.Adapter<Frame>  execAsyncWithAdapter(List<String> cmd, List<String> envVars, Consumer<String> logFrameCallback) {
    ExecCreateCmd execCmd = dockerClient.execCreateCmd(this.containerId)
      .withAttachStderr(true)
      .withAttachStdin(true)
      .withAttachStdout(true);

    if (cmd != null) {
      execCmd = execCmd.withCmd(cmd.toArray(new String[0]));
    }

    if (envVars != null) {
      execCmd = execCmd.withEnv(envVars);
    }

    return dockerClient.execStartCmd(execCmd.exec().getId())
      .exec(new ResultCallback.Adapter<Frame>() {
        @Override
        public void onNext(Frame item) {
          if (logFrameCallback != null){
            logFrameCallback.accept(new String(item.getPayload(), StandardCharsets.UTF_8));
          }
          super.onNext(item);
        }
      });
  }

  @Override
  public void stop(int timeout) {
    dockerClient.stopContainerCmd(this.containerId)
      .withTimeout(timeout)
      .exec();

    dockerClient.removeContainerCmd(this.containerId)
    .withForce(true)
    .exec();
  }
}
