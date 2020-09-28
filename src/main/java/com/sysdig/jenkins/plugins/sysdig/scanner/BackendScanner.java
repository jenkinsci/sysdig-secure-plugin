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
import com.sysdig.jenkins.plugins.sysdig.client.*;
import hudson.AbortException;
import hudson.Launcher;
import hudson.model.TaskListener;

import java.util.ArrayList;
import java.util.Map;


public class BackendScanner extends Scanner {

  public BackendScanner(Launcher launcher, TaskListener listener, BuildConfig config) throws AbortException {
    super(launcher, listener, config);
  }

  @Override
  public ArrayList<ImageScanningResult> scanImages(Map<String, String> imagesAndDockerfiles) throws AbortException {
    String sysdigToken = config.getSysdigToken();
    SysdigSecureClient sysdigSecureClient = config.getEngineverify() ?
      SysdigSecureClientImpl.newClient(sysdigToken, config.getEngineurl()) :
      SysdigSecureClientImpl.newInsecureClient(sysdigToken, config.getEngineurl());
    sysdigSecureClient = new SysdigSecureClientImplWithRetries(sysdigSecureClient, 10);

    try {
      ArrayList<ImageScanningResult> resultList = new ArrayList<>();
      for (Map.Entry<String, String> entry : imagesAndDockerfiles.entrySet()) {
        String imageTag = entry.getKey();
        String dockerFile = entry.getValue();

        logger.logInfo(String.format("Submitting %s for analysis", imageTag));
        ImageScanningSubmission submission = sysdigSecureClient.submitImageForScanning(imageTag, dockerFile);
        logger.logInfo(String.format("Analysis request accepted, received image %s", submission.getImageDigest()));

        ImageScanningResult result = this.retrievePolicyEvaluation(submission);

        resultList.add(result);
      }
      return resultList;
    } catch (Exception e) {
      logger.logError("Failed to add image(s) to sysdig-secure-engine due to an unexpected error", e);
      throw new AbortException("Failed to add image(s) to sysdig-secure-engine due to an unexpected error. Please refer to above logs for more information");
    }
  }

  public ImageScanningResult retrievePolicyEvaluation(ImageScanningSubmission submission) throws AbortException {
    String sysdigToken = config.getSysdigToken();
    SysdigSecureClient sysdigSecureClient = config.getEngineverify() ?
      SysdigSecureClientImpl.newClient(sysdigToken, config.getEngineurl()) :
      SysdigSecureClientImpl.newInsecureClient(sysdigToken, config.getEngineurl());
    sysdigSecureClient = new SysdigSecureClientImplWithRetries(sysdigSecureClient, 10);

    ImageScanningResult result = null;
    try {
      String tag = submission.getTag();
      String imageDigest = submission.getImageDigest();

      logger.logInfo(String.format("Waiting for analysis of %s with digest %s, polling status periodically...", tag, imageDigest));

      for (int i = 0; i < Integer.parseInt(config.getEngineRetries()); i++) {
        Thread.sleep(i * 5000);

        result = sysdigSecureClient.retrieveImageScanningResults(tag, imageDigest);
        if (result != null) break;
      }

    } catch (InterruptedException | ImageScanningException e) {
      logger.logError("Failed to execute sysdig-secure-engine policy evaluation due to an unexpected error", e);
      throw new AbortException("Failed to execute sysdig-secure-engine policy evaluation due to an unexpected error. Please refer to above logs for more information");
    }
    return result;
  }
}
