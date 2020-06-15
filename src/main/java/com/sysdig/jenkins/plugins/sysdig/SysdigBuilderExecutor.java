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

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.google.common.base.Strings;
import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningSubmission;
import com.sysdig.jenkins.plugins.sysdig.log.ConsoleLog;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

public class SysdigBuilderExecutor {
  //  Log handler for logging above INFO level events to jenkins log
  private static final Logger LOG = Logger.getLogger(SysdigBuilderExecutor.class.getName());

  public SysdigBuilderExecutor(SysdigBuilder builder, Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, AbortException {

    LOG.warning(String.format("Starting Sysdig Secure Container Image Scanner step, project: %s, job: %d", run.getParent().getDisplayName(), run.getNumber()));

    boolean failedByGate = false;
    BuildConfig config = null;
    BuildWorker worker = null;
    SysdigBuilder.DescriptorImpl globalConfig = builder.getDescriptor();
    ConsoleLog console = new ConsoleLog("SysdigSecurePlugin", listener.getLogger(), globalConfig.getDebug());

    try {
      // We are expecting that either the job credentials or global credentials will be set, otherwise, fail the build
      if (Strings.isNullOrEmpty(builder.getEngineCredentialsId()) && Strings.isNullOrEmpty(globalConfig.getEngineCredentialsId())) {
        throw new AbortException(String.format("Cannot find Jenkins credentials by ID: '%s'. Ensure credentials are defined in Jenkins before using them", builder.getEngineCredentialsId()));
      }
      //Prefer the job credentials set by the user and fallback to the global ones


      /* Fetch Jenkins creds first, can't push this lower down the chain since it requires Jenkins instance object */
      String sysdigToken = getSysdigTokenFromCredentials(builder, globalConfig, run, console);

      String engineurl = getEngineurl(builder, globalConfig, console);

      boolean isInlineScanning = builder.isInlineScanning() || globalConfig.getInlineScanning();

      /* Instantiate config and a new build worker */
      config = new BuildConfig(builder.getName(), builder.getEngineRetries(), builder.getBailOnFail(), builder.getBailOnPluginFail(), globalConfig.getDebug(),
        isInlineScanning,
        engineurl,
        sysdigToken,
        builder.getEngineverify()
      );

      worker = isInlineScanning ?
        new BuildWorkerInline(run, workspace, launcher, listener, config) :
        new BuildWorkerBackend(run, workspace, launcher, listener, config);

      Map<String, String> imagesAndDockerfiles = worker.readImagesAndDockerfilesFromPath(workspace, config.getName());
      /* Run analysis */
      ArrayList<ImageScanningSubmission> submissionList = worker.scanImages(imagesAndDockerfiles);

      /* Run gates */
      Util.GATE_ACTION finalAction = worker.retrievePolicyEvaluation(submissionList);

      /* Run queries and continue even if it fails */
      try {
        worker.retrieveVulnerabilityEvaluation(submissionList);
      } catch (AbortException e) {
        console.logWarn("Recording failure to execute Sysdig Secure queries and moving on with plugin operation", e);
      }

      /* Setup reports */
      worker.setupBuildReports(finalAction);

      /* Evaluate result of step based on gate action */
      if (null != finalAction) {
        if ((config.getBailOnFail() && (Util.GATE_ACTION.STOP.equals(finalAction) || Util.GATE_ACTION.FAIL.equals(finalAction)))) {
          console.logWarn("Failing Sysdig Secure Container Image Scanner Plugin step due to final result " + finalAction);
          failedByGate = true;
          throw new AbortException("Failing Sysdig Secure Container Image Scanner Plugin step due to final result " + finalAction);
        } else {
          console.logInfo("Marking Sysdig Secure Container Image Scanner step as successful, final result " + finalAction);
        }
      } else {
        console.logInfo("Marking Sysdig Secure Container Image Scanner step as successful, no final result");
      }

    } catch (AbortException e) {
      if (failedByGate) {
        throw e;
      } else if ((null != config && config.getBailOnPluginFail()) || builder.getBailOnPluginFail()) {
        console.logError("Failing Sysdig Secure Container Image Scanner Plugin step due to errors in plugin execution", e);
        throw e;
      } else {
        console.logWarn("Marking Sysdig Secure Container Image Scanner step as successful despite errors in plugin execution");
      }
    } finally {
      // Wrap cleanup in try catch block to ensure this finally block does not throw an exception
      if (null != worker) {
        try {
          worker.cleanup();
        } catch (Exception e) {
          console.logDebug("Failed to cleanup after the plugin, ignoring the errors", e);
        }
      }
      console.logInfo("Completed Sysdig Secure Container Image Scanner step");
      LOG.warning("Completed Sysdig Secure Container Image Scanner step, project: " + run.getParent().getDisplayName() + ", job: " + run
        .getNumber());
    }
  }

  private String getEngineurl(SysdigBuilder builder, SysdigBuilder.DescriptorImpl globalConfig, ConsoleLog console) {
    String engineurl = globalConfig.getEngineurl();
    if (!Strings.isNullOrEmpty(builder.getEngineurl())) {
      console.logInfo("Build override set for Sysdig Secure Engine URL");
      engineurl = builder.getEngineurl();
    }
    return engineurl;
  }

  private String getSysdigTokenFromCredentials(SysdigBuilder builder, SysdigBuilder.DescriptorImpl globalConfig, Run<?, ?> run, ConsoleLog console) throws AbortException {
    String credID = !Strings.isNullOrEmpty(builder.getEngineCredentialsId()) ? builder.getEngineCredentialsId() : globalConfig.getEngineCredentialsId();
    console.logDebug("Processing Jenkins credential ID " + credID);

    String sysdigToken;
    try {
      StandardUsernamePasswordCredentials creds = CredentialsProvider.findCredentialById(credID, StandardUsernamePasswordCredentials.class, run, Collections.emptyList());
      if (null != creds) {
        //This is to maintain backward compatibility with how the API layer is fetching the information. This will be changed in the next version to use
        //the Authorization header instead.
        sysdigToken = creds.getPassword().getPlainText();
      } else {
        throw new AbortException(String.format("Cannot find Jenkins credentials by ID: '%s'. Ensure credentials are defined in Jenkins before using them", credID));
      }
    } catch (AbortException e) {
      throw e;
    } catch (Exception e) {
      console.logError(String.format("Error looking up Jenkins credentials by ID: '%s'", credID), e);
      throw new AbortException(String.format("Error looking up Jenkins credentials by ID: '%s", credID));
    }
    return sysdigToken;
  }
}
