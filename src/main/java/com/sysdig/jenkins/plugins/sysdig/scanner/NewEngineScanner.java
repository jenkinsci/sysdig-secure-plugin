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
import com.sysdig.jenkins.plugins.sysdig.json.GsonBuilder;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.scanner.report.Package;
import com.sysdig.jenkins.plugins.sysdig.scanner.report.*;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.TaskListener;

import javax.annotation.Nonnull;
import java.util.List;

public class NewEngineScanner {

  protected final NewEngineBuildConfig config;
  protected final SysdigLogger logger;
  private final TaskListener listener;
  private final FilePath workspace;
  private final EnvVars envVars;

  public NewEngineScanner(@Nonnull TaskListener listener, @Nonnull NewEngineBuildConfig config, @Nonnull FilePath workspace, EnvVars envVars, SysdigLogger logger) {
    this.logger = logger;
    this.config = config;
    this.listener = listener;
    this.workspace = workspace;
    this.envVars = envVars;
  }

  public ImageScanningResult scanImage(String imageTag) throws InterruptedException {
    try {

      NewEngineRemoteExecutor task = new NewEngineRemoteExecutor(workspace, imageTag, config, logger, envVars);
      String scanRawOutput = workspace.act(task);
      JsonScanResult scanOutput = GsonBuilder.build().fromJson(scanRawOutput, JsonScanResult.class);

      return this.buildImageScanningResult(scanOutput.getResult().orElseThrow());

    } catch (ImageScanningException e) {
      logger.logError(e.getMessage());
      throw new InterruptedException("Failed to perform inline-scan due to an unexpected error. Please refer to above logs for more information");
    } catch (Exception e) {
      logger.logError("Failed to perform inline-scan due to an unexpected error", e);
      throw new InterruptedException("Failed to perform inline-scan due to an unexpected error. Please refer to above logs for more information");
    }
  }


  public ImageScanningResult buildImageScanningResult(Result result) {
    final String tag = result.getMetadata().orElseThrow().getPullString().orElseThrow();
    final String imageDigest = result.getMetadata().orElseThrow().getDigest().orElseThrow();
    final String evalStatus = result.getPolicyEvaluationsResult().orElseThrow();
    final List<Package> packages = result.getPackages().orElseThrow();
    final List<PolicyEvaluation> policies = result.getPolicyEvaluations().orElseThrow();

    return new ImageScanningResult(tag, imageDigest, evalStatus, packages, policies);
  }
}

