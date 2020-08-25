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
package com.sysdig.jenkins.plugins.sysdig;

import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningSubmission;
import com.sysdig.jenkins.plugins.sysdig.client.SysdigSecureClient;
import com.sysdig.jenkins.plugins.sysdig.client.SysdigSecureClientImpl;
import com.sysdig.jenkins.plugins.sysdig.client.SysdigSecureClientImplWithRetries;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.util.ArrayList;
import java.util.Map;


/**
 * A helper class to ensure concurrent jobs don't step on each other's toes. Sysdig Secure plugin instantiates a new instance of this class
 * for each individual job i.e. invocation of perform(). Global and project configuration at the time of execution is loaded into
 * worker instance via its constructor. That specific worker instance is responsible for the bulk of the plugin operations for a given
 * job.
 */
public class BuildWorkerBackend extends BuildWorker {

  public BuildWorkerBackend(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, BuildConfig config) throws AbortException {
    super(build, workspace, launcher, listener, config);
  }

  @Override
  public ArrayList<ImageScanningSubmission> scanImages(Map<String, String> imagesAndDockerfiles) throws AbortException {
    String sysdigToken = config.getSysdigToken();
    SysdigSecureClient sysdigSecureClient = config.getEngineverify() ?
      SysdigSecureClientImpl.newClient(sysdigToken, config.getEngineurl()) :
      SysdigSecureClientImpl.newInsecureClient(sysdigToken, config.getEngineurl());
    sysdigSecureClient = new SysdigSecureClientImplWithRetries(sysdigSecureClient, 10);

    try {
      ArrayList<ImageScanningSubmission> submissionList = new ArrayList<>();
      for (Map.Entry<String, String> entry : imagesAndDockerfiles.entrySet()) {
        String imageTag = entry.getKey();
        String dockerFile = entry.getValue();

        logger.logInfo(String.format("Submitting %s for analysis", imageTag));
        ImageScanningSubmission submission = sysdigSecureClient.submitImageForScanning(imageTag, dockerFile);
        logger.logInfo(String.format("Analysis request accepted, received image %s", submission.getImageDigest()));

        submissionList.add(submission);
      }
      return submissionList;
    } catch (Exception e) {
      logger.logError("Failed to add image(s) to sysdig-secure-engine due to an unexpected error", e);
      throw new AbortException("Failed to add image(s) to sysdig-secure-engine due to an unexpected error. Please refer to above logs for more information");
    }
  }
}
