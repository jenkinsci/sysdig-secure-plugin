/*
Copyright (C) 2016-2024 Sysdig

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

import com.sysdig.jenkins.plugins.sysdig.application.vm.ImageScanningConfig;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.ImageScanner;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.ImageScanningResult;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.http.RetriableRemoteDownloader;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.RunContext;

import javax.annotation.Nonnull;

public class SysdigImageScanner implements ImageScanner {
  protected final ImageScanningConfig config;
  private final RunContext runContext;

  public SysdigImageScanner(@Nonnull RunContext runContext, @Nonnull ImageScanningConfig config) {
    this.runContext = runContext;
    this.config = config;
  }

  @Override
  public ImageScanningResult scanImage(String imageTag) throws InterruptedException {
    try {
      RetriableRemoteDownloader downloader = new RetriableRemoteDownloader(this.runContext);
      RemoteSysdigImageScanner task = new RemoteSysdigImageScanner(runContext, downloader, imageTag, config);
      return task.performScan();
    } catch (Exception e) {
      runContext.getLogger().logError("Failed to perform inline-scan due to an unexpected error", e);
      throw new InterruptedException("Failed to perform inline-scan due to an unexpected error. Please refer to above logs for more information");
    }
  }
}
