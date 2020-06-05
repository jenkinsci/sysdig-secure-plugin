package com.sysdig.jenkins.plugins.sysdig;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.netty.NettyDockerCmdExecFactory;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningException;
import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningSubmission;
import com.sysdig.jenkins.plugins.sysdig.client.SysdigSecureClient;
import com.sysdig.jenkins.plugins.sysdig.client.SysdigSecureClientImpl;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.*;
import org.antlr.v4.runtime.misc.NotNull;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.VFS;
import org.jenkinsci.remoting.RoleChecker;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * A helper class to ensure concurrent jobs don't step on each other's toes. Sysdig Secure plugin instantiates a new instance of this class
 * for each individual job i.e. invocation of perform(). Global and project configuration at the time of execution is loaded into
 * worker instance via its constructor. That specific worker instance is responsible for the bulk of the plugin operations for a given
 * job.
 */
public class BuildWorkerInline extends BuildWorker {


  public BuildWorkerInline(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, BuildConfig config) throws AbortException {
    super(build, workspace, launcher, listener, config);
  }

  @Override
  public @NotNull
  ArrayList<ImageScanningSubmission> scanImages(Map<String, String> imagesAndDockerfiles) throws AbortException, InterruptedException {
    if (imagesAndDockerfiles == null) {
      return new ArrayList<>();
    }

    SysdigSecureClient sysdigSecureClient = config.getEngineverify() ?
      SysdigSecureClientImpl.newClient(config.getSysdigToken(), config.getEngineurl()) :
      SysdigSecureClientImpl.newInsecureClient(config.getSysdigToken(), config.getEngineurl());

    ArrayList<ImageScanningSubmission> imageScanningSubmissions = new ArrayList<>();
    try {
      VirtualChannel channel = launcher.getChannel();
      if (channel == null) {
        throw new AbortException("There's no channel to communicate with the worker");
      }

      for (Map.Entry<String, String> entry : imagesAndDockerfiles.entrySet()) {
        RemoteInlineScanningExecution task = new RemoteInlineScanningExecution(entry.getKey(), entry.getValue(), listener, sysdigSecureClient);

        ImageScanningSubmission submission = channel.call(task);
        imageScanningSubmissions.add(submission);
      }
    } catch (Exception e) {
      throw new AbortException(e.toString());
    }

    return imageScanningSubmissions;
  }

}
