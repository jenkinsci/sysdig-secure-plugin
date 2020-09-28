/*
Copyright (C) 2016-2020 Sysdig

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.sysdig.jenkins.plugins.sysdig;

import com.sysdig.jenkins.plugins.sysdig.client.*;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.*;
import org.antlr.v4.runtime.misc.NotNull;

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
  ArrayList<ImageScanningSubmission> scanImages(Map<String, String> imagesAndDockerfiles) throws AbortException {
    if (imagesAndDockerfiles == null) {
      return new ArrayList<>();
    }

    ArrayList<ImageScanningSubmission> imageScanningSubmissions = new ArrayList<>();
    try {
      VirtualChannel channel = launcher.getChannel();
      if (channel == null) {
        throw new AbortException("There's no channel to communicate with the worker");
      }

      for (Map.Entry<String, String> entry : imagesAndDockerfiles.entrySet()) {
        RemoteInlineScanningExecution task = new RemoteInlineScanningExecution(entry.getKey(), entry.getValue(), listener, config);

        ImageScanningSubmission submission = channel.call(task);
        imageScanningSubmissions.add(submission);
      }
    } catch (Exception e) {
      throw new AbortException(e.toString());
    }

    return imageScanningSubmissions;
  }

}
