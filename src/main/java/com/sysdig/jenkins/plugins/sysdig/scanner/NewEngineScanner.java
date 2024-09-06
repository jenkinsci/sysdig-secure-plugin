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

import com.sysdig.jenkins.plugins.sysdig.NewEngineBuildConfig;
import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningException;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.TaskListener;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NewEngineScanner implements ScannerInterface {

  protected final NewEngineBuildConfig config;
  private final Map<String, JSONObject> scanOutputs;
  private final TaskListener listener;
  private final FilePath workspace;
  private final EnvVars envVars;
  protected final SysdigLogger logger;

  public NewEngineScanner(@Nonnull TaskListener listener, @Nonnull NewEngineBuildConfig config, FilePath workspace, EnvVars envVars, SysdigLogger logger) {
    this.logger = logger;
    this.config = config;
    this.scanOutputs = new HashMap<>();
    this.listener = listener;
    this.workspace = workspace;
    this.envVars = envVars;
  }

  @Override
  public ImageScanningSubmission scanImage(String imageTag, String dockerFile) throws AbortException, InterruptedException {

    if (this.workspace == null) {
      throw new AbortException("Inline-scan failed. No workspace available");
    }

    try {

      NewEngineRemoteExecutor task = new NewEngineRemoteExecutor(workspace, imageTag, dockerFile, config, logger, envVars);

      String scanRawOutput = workspace.act(task);

      JSONObject scanOutput = JSONObject.fromObject(scanRawOutput);

      if (scanOutput.has("error")) {
        throw new ImageScanningException(scanOutput.getString("error"));
      }

      String digest = scanOutput.getJSONObject("metadata").getString("digest");
      String tag = scanOutput.getJSONObject("metadata").getString("pullString");

      this.scanOutputs.put(digest, scanOutput);

      return new ImageScanningSubmission(tag, digest);

    } catch (ImageScanningException e) {
      logger.logError(e.getMessage());
      throw new InterruptedException("Failed to perform inline-scan due to an unexpected error. Please refer to above logs for more information");
    } catch (Exception e) {
      logger.logError("Failed to perform inline-scan due to an unexpected error", e);
      throw new InterruptedException("Failed to perform inline-scan due to an unexpected error. Please refer to above logs for more information");
    }
  }


  public JSONObject getGateResults(ImageScanningSubmission submission) {
    if (this.scanOutputs.containsKey(submission.getImageDigest())) {
      return this.scanOutputs.get(submission.getImageDigest()).getJSONObject("policies");
    }

    return null;
  }

  @Override
  public JSONObject getVulnsReport(ImageScanningSubmission submission) {
    if (this.scanOutputs.containsKey(submission.getImageDigest())) {
      return this.scanOutputs.get(submission.getImageDigest()).getJSONObject("packages");
    }

    return null;
  }


  @Override
  public ImageScanningResult buildImageScanningResult(JSONObject scanReport, JSONObject vulnsReport, String imageDigest, String tag) {
    final String evalStatus = scanReport.getString("status");
    final JSONArray gatePolicies = scanReport.optJSONArray("list") != null ? scanReport.getJSONArray("list") : new JSONArray();

    return new ImageScanningResult(tag, imageDigest, evalStatus, scanReport, vulnsReport, gatePolicies);
  }

  @Override
  public ArrayList<ImageScanningResult> scanImages(Map<String, String> imagesAndDockerfiles) throws AbortException, InterruptedException {
    if (imagesAndDockerfiles == null) {
      return new ArrayList<>();
    }

    ArrayList<ImageScanningResult> resultList = new ArrayList<>();

    for (Map.Entry<String, String> entry : imagesAndDockerfiles.entrySet()) {
      String dockerfile = entry.getValue();

      ImageScanningSubmission submission = this.scanImage(entry.getKey(), dockerfile);

      JSONObject scanReport = this.getGateResults(submission);
      JSONObject vulnsReport = this.getVulnsReport(submission);

      ImageScanningResult result = this.buildImageScanningResult(scanReport, vulnsReport, submission.getImageDigest(), submission.getTag());
      resultList.add(result);
    }

    return resultList;
  }

}

