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
package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner;

import com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.RunContext;
import com.sysdig.jenkins.plugins.sysdig.application.vm.ScanningConfig;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.ImageScanner;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.ImageScanningException;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.ImageScanningResult;

import javax.annotation.Nonnull;

public class SysdigImageScanner implements ImageScanner {
  protected final ScanningConfig config;
  private final RunContext runContext;

  public SysdigImageScanner(@Nonnull RunContext runContext, @Nonnull ScanningConfig config) {
    this.runContext = runContext;
    this.config = config;
  }

  @Override
  public ImageScanningResult scanImage(String imageTag) throws InterruptedException {
    try {
      RemoteSysdigImageScanner task = new RemoteSysdigImageScanner(runContext, imageTag, config);
      return runContext.call(task);
    } catch (ImageScanningException e) {
      runContext.getLogger().logError(e.getMessage());
      throw new InterruptedException("Failed to perform inline-scan due to an unexpected error. Please refer to above logs for more information");
    } catch (Exception e) {
      runContext.getLogger().logError("Failed to perform inline-scan due to an unexpected error", e);
      throw new InterruptedException("Failed to perform inline-scan due to an unexpected error. Please refer to above logs for more information");
    }
  }
}

