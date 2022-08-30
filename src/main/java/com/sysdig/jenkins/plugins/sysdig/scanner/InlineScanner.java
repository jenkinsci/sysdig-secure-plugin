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
import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningException;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.TaskListener;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class InlineScanner extends OldEngineScanner {

  private final Map<String, JSONObject> scanOutputs;
  private final TaskListener listener;
  private final FilePath workspace;
  private final EnvVars envVars;

  public InlineScanner(@Nonnull TaskListener listener, @Nonnull BuildConfig config, FilePath workspace, EnvVars envVars, SysdigLogger logger) {
    super(config, logger);

    this.scanOutputs = new HashMap<>();
    this.listener = listener;
    this.workspace = workspace;
    this.envVars = envVars;
  }

  @Override
  public ImageScanningSubmission scanImage(String imageTag, String dockerFile) throws AbortException {

    if (this.workspace == null) {
      throw new AbortException("Inline-scan failed. No workspace available");
    }

    try {

      InlineScannerRemoteExecutor task = new InlineScannerRemoteExecutor(imageTag,
        dockerFile,
        config,
        logger,
        envVars);

      String scanRawOutput = workspace.act(task);

      JSONObject scanOutput = JSONObject.fromObject(scanRawOutput);

      if (scanOutput.has("error")) {
        throw new ImageScanningException(scanOutput.getString("error"));
      }

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
