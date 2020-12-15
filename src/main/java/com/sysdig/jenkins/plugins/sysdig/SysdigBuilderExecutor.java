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
import com.sysdig.jenkins.plugins.sysdig.log.ConsoleLog;
import com.sysdig.jenkins.plugins.sysdig.scanner.*;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.util.Collections;
import java.util.logging.Logger;

public class SysdigBuilderExecutor {
  //  Log handler for logging above INFO level events to jenkins log
  private static final Logger LOG = Logger.getLogger(SysdigBuilderExecutor.class.getName());

  private final ConsoleLog logger;

  public SysdigBuilderExecutor(SysdigBuilder builder, Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws AbortException {

    LOG.warning(String.format("Starting Sysdig Secure Container Image Scanner step, project: %s, job: %d", run.getParent().getDisplayName(), run.getNumber()));

    boolean failedByGate = false;
    BuildConfig config = null;
    BuildWorker worker = null;
    SysdigBuilder.DescriptorImpl globalConfig = builder.getDescriptor();

    logger = new ConsoleLog("SysdigSecurePlugin", listener.getLogger(), globalConfig.getDebug());

    try {
      // We are expecting that either the job credentials or global credentials will be set, otherwise, fail the build
      if (Strings.isNullOrEmpty(builder.getEngineCredentialsId()) && Strings.isNullOrEmpty(globalConfig.getEngineCredentialsId())) {
        throw new AbortException(String.format("Cannot find Jenkins credentials by ID: '%s'. Ensure credentials are defined in Jenkins before using them", builder.getEngineCredentialsId()));
      }
      //Prefer the job credentials set by the user and fallback to the global ones


      /* Fetch Jenkins creds first, can't push this lower down the chain since it requires Jenkins instance object */
      String sysdigToken = getSysdigTokenFromCredentials(builder, globalConfig, run);

      String engineurl = getEngineurl(builder, globalConfig);

      boolean isInlineScanning = builder.isInlineScanning() || globalConfig.getInlineScanning();

      /* Instantiate config and a new build worker */
      config = new BuildConfig(builder.getName(), builder.getBailOnFail(), builder.getBailOnPluginFail(), globalConfig.getDebug(),
        isInlineScanning,
        engineurl,
        sysdigToken,
        builder.getEngineverify()
      );

      worker = new BuildWorker(run, workspace, listener, config);
      Scanner scanner = isInlineScanning ?
        new InlineScanner(launcher, listener, config) :
        new BackendScanner(launcher, listener, config);

      Util.GATE_ACTION finalAction = worker.scanAndBuildReports(scanner);

      /* Evaluate result of step based on gate action */
      if (null != finalAction) {
        if ((config.getBailOnFail() && Util.GATE_ACTION.FAIL.equals(finalAction))) {
          logger.logWarn("Failing Sysdig Secure Container Image Scanner Plugin step due to final result " + finalAction);
          failedByGate = true;
          throw new AbortException("Failing Sysdig Secure Container Image Scanner Plugin step due to final result " + finalAction);
        } else {
          logger.logInfo("Marking Sysdig Secure Container Image Scanner step as successful, final result " + finalAction);
        }
      } else {
        logger.logInfo("Marking Sysdig Secure Container Image Scanner step as successful, no final result");
      }

    } catch (AbortException e) {
      if (failedByGate) {
        throw e;
      } else if ((null != config && config.getBailOnPluginFail()) || builder.getBailOnPluginFail()) {
        logger.logError("Failing Sysdig Secure Container Image Scanner Plugin step due to errors in plugin execution", e);
        throw e;
      } else {
        logger.logWarn("Marking Sysdig Secure Container Image Scanner step as successful despite errors in plugin execution");
      }
    } finally {
      // Wrap cleanup in try catch block to ensure this finally block does not throw an exception
      if (null != worker) {
        try {
          worker.cleanup();
        } catch (Exception e) {
          logger.logDebug("Failed to cleanup after the plugin, ignoring the errors", e);
        }
      }
      logger.logInfo("Completed Sysdig Secure Container Image Scanner step");
      LOG.warning("Completed Sysdig Secure Container Image Scanner step, project: " + run.getParent().getDisplayName() + ", job: " + run
        .getNumber());
    }
  }

  private String getEngineurl(SysdigBuilder builder, SysdigBuilder.DescriptorImpl globalConfig) {
    String engineurl = globalConfig.getEngineurl();
    if (!Strings.isNullOrEmpty(builder.getEngineurl())) {
      logger.logInfo("Build override set for Sysdig Secure Engine URL");
      engineurl = builder.getEngineurl();
    }
    return engineurl;
  }

  private String getSysdigTokenFromCredentials(SysdigBuilder builder, SysdigBuilder.DescriptorImpl globalConfig, Run<?, ?> run) throws AbortException {
    String credID = !Strings.isNullOrEmpty(builder.getEngineCredentialsId()) ? builder.getEngineCredentialsId() : globalConfig.getEngineCredentialsId();
    logger.logDebug("Processing Jenkins credential ID " + credID);

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
      logger.logError(String.format("Error looking up Jenkins credentials by ID: '%s'", credID), e);
      throw new AbortException(String.format("Error looking up Jenkins credentials by ID: '%s", credID));
    }
    return sysdigToken;
  }
}
