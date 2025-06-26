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
package com.sysdig.jenkins.plugins.sysdig.domain.vm;

import com.sysdig.jenkins.plugins.sysdig.domain.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.report.EvaluationResult;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.report.ScanResult;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A helper class to ensure concurrent jobs don't step on each other's toes. Sysdig Secure plugin instantiates a new instance of this class
 * for each individual job i.e. invocation of perform(). Global and project configuration at the time of execution is loaded into
 * worker instance via its constructor. That specific worker instance is responsible for the bulk of the plugin operations for a given
 * job.
 */
public class ImageScanningService {
  private final ImageScanner scanner;
  private final ScanResultArchiver imageScanningArchiverService;
  protected final SysdigLogger logger;

  public ImageScanningService(@NonNull ImageScanner scanner, @NonNull ScanResultArchiver imageScanningArchiverService, @NonNull SysdigLogger logger) {
    this.scanner = scanner;
    this.imageScanningArchiverService = imageScanningArchiverService;
    this.logger = logger;
  }

  public EvaluationResult scanAndArchiveResult(String imageName) throws InterruptedException {
    if (imageName.trim().isEmpty()) {
      throw new IllegalArgumentException("the image name to scan must not be empty");
    }
    ScanResult scanResult = scanner.scanImage(imageName);

    EvaluationResult finalAction = scanResult.evaluationResult();
    logger.logInfo("Sysdig Secure Container Image Scanner Plugin step result - " + finalAction);

    try {
      imageScanningArchiverService.archiveScanResult(scanResult);
    } catch (Exception e) {
      logger.logError("Recording failure to build reports and moving on with plugin operation", e);
    }

    return finalAction;
  }

}
