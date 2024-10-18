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
import com.sysdig.jenkins.plugins.sysdig.RunContext;
import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningException;
import com.sysdig.jenkins.plugins.sysdig.json.GsonBuilder;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.scanner.report.*;
import hudson.EnvVars;
import hudson.FilePath;

import javax.annotation.Nonnull;

public class NewEngineScanner {
  protected final NewEngineBuildConfig config;
  private final RunContext runContext;

  public NewEngineScanner(@Nonnull NewEngineBuildConfig config, @Nonnull RunContext runContext) {
    this.config = config;
    this.runContext = runContext;
  }

  public ImageScanningResult scanImage(String imageTag) throws InterruptedException {
    try {
      NewEngineRemoteExecutor task = new NewEngineRemoteExecutor(imageTag, config, runContext);
      String scanRawOutput = runContext.call(task);
      JsonScanResult scanOutput = GsonBuilder.build().fromJson(scanRawOutput, JsonScanResult.class);
      return ImageScanningResult.fromReportResult(scanOutput.getResult().orElseThrow());
    } catch (ImageScanningException e) {
      runContext.getSysdigLogger().logError(e.getMessage());
      throw new InterruptedException("Failed to perform inline-scan due to an unexpected error. Please refer to above logs for more information");
    } catch (Exception e) {
      runContext.getSysdigLogger().logError("Failed to perform inline-scan due to an unexpected error", e);
      throw new InterruptedException("Failed to perform inline-scan due to an unexpected error. Please refer to above logs for more information");
    }
  }
}

