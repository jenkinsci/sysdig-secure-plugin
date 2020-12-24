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
import com.sysdig.jenkins.plugins.sysdig.ImageScanningException;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.TaskListener;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class InlineScanner extends Scanner {

  private final Map<String, JSONObject> scanOutputs;
  private final TaskListener listener;
  private final FilePath workspace;

  public InlineScanner(@Nonnull TaskListener listener, @Nonnull BuildConfig config, FilePath workspace, SysdigLogger logger) {
    super(config, logger);

    this.scanOutputs = new HashMap<>();
    this.listener = listener;
    this.workspace = workspace;
  }

  @Override
  public ImageScanningSubmission scanImage(String imageTag, String dockerFile) throws ImageScanningException, InterruptedException {

    try {
      final EnvVars nodeEnvVars = new EnvVars(System.getenv());

      Computer computer = this.workspace.toComputer();
      if (computer != null) {
        nodeEnvVars.putAll(computer.buildEnvironment(listener));
      }

      InlineScannerRemoteExecutor task = new InlineScannerRemoteExecutor(imageTag,
        dockerFile,
        config,
        logger,
        nodeEnvVars);

      String scanRawOutput = workspace.act(task);

      JSONObject scanOutput = JSONObject.fromObject(scanRawOutput);

      //TODO: Get exit code, and get "error" from JSON only if exit code 0 or 1 or 3.
      //TODO: If exit code 2, show the standard output and error (should be already in the logs)
      if (scanOutput.has("error")) {
        throw new ImageScanningException(scanOutput.getString("error"));
      }

      String digest = scanOutput.getString("digest");
      String tag = scanOutput.getString("tag");

      this.scanOutputs.put(digest, scanOutput);

      return new ImageScanningSubmission(tag, digest);
    } catch (InterruptedException e) {
      throw e;
    } catch (ImageScanningException e) {
      throw e;
    } catch (Exception e) {
      throw new ImageScanningException("Failed to perform inline-scan due to an unexpected error", e);
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

