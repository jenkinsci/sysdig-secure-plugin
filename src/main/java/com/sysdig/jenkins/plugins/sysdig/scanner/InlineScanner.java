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
package com.sysdig.jenkins.plugins.sysdig.scanner;

import com.sysdig.jenkins.plugins.sysdig.BuildConfig;
import com.sysdig.jenkins.plugins.sysdig.log.ConsoleLog;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.remoting.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class InlineScanner extends Scanner {

  private final Map<String, JSONObject> scanOutputs;
  private final SysdigLogger logger;
  private final TaskListener listener;

  public InlineScanner(Launcher launcher, TaskListener listener, BuildConfig config) {
    super(launcher, listener, config);
    this.scanOutputs = new HashMap<>();
    this.listener = listener;
    this.logger = new ConsoleLog(this.getClass().getSimpleName(), listener.getLogger(), false);
  }

  @Override
  public ImageScanningSubmission scanImage(String imageTag, FilePath dockerFile) throws AbortException {
    VirtualChannel channel = launcher.getChannel();
    if (channel == null) {
      throw new AbortException("There's no channel to communicate with the worker");
    }

    try {
      InlineScannerRemoteExecutor task = new InlineScannerRemoteExecutor(imageTag, dockerFile, listener, config);
      JSONObject scanOutput = channel.call(task);

      String digest = scanOutput.getString("digest");
      String tag = scanOutput.getString("tag");
      this.scanOutputs.put(digest, scanOutput);

      return new ImageScanningSubmission(tag, digest);

    } catch (Exception e) {
      logger.logError("Failed to perform inline-scan due to an unexpected error", e);
      throw new AbortException("Failed to perform inline-scan due to an unexpected error. Please refer to above logs for more information");
    }

  }

  @Override
  public JSONArray getGateResults(ImageScanningSubmission submission) {
    if (this.scanOutputs.containsKey(submission.getImageDigest())) {
      return this.scanOutputs.get(submission.getImageDigest()).getJSONArray("scanReport");
    }

    return null;
  }

  @Override
  public JSONObject getVulnsReport(ImageScanningSubmission submission) {
    if (this.scanOutputs.containsKey(submission.getImageDigest())) {
      return this.scanOutputs.get(submission.getImageDigest()).getJSONObject("vulnsReport");
    }

    return null;
  }
}

