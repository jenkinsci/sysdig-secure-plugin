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

import com.google.common.base.Strings;
import com.sysdig.jenkins.plugins.sysdig.config.BuilderConfig;
import com.sysdig.jenkins.plugins.sysdig.config.GlobalConfig;
import com.sysdig.jenkins.plugins.sysdig.log.ConsoleLog;
import com.sysdig.jenkins.plugins.sysdig.scanner.*;
import hudson.AbortException;
import hudson.FilePath;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.util.logging.Logger;

public class SysdigBuilderExecutor {
  //  Log handler for logging above INFO level events to jenkins log
  private static final Logger LOG = Logger.getLogger(SysdigBuilderExecutor.class.getName());

  public SysdigBuilderExecutor(BuilderConfig builderConfig,
                               GlobalConfig globalConfig,
                               Run<?, ?> run,
                               FilePath workspace,
                               TaskListener listener) throws AbortException {

    LOG.warning(String.format("Starting Sysdig Secure Container Image Scanner step, project: %s, job: %d", run.getParent().getDisplayName(), run.getNumber()));

    /* Instantiate config and a new build worker */
    ConsoleLog logger = new ConsoleLog("SysdigSecurePlugin", listener, globalConfig.getDebug());

    if (workspace == null) {
      throw new AbortException("Workspace not available. This plugin must run inside a workspace.");
    }

    BuildConfig config = new BuildConfig(globalConfig, builderConfig, run);
    config.print(logger);

    // We are expecting that either the job credentials or global credentials will be set, otherwise, fail the build
    if (Strings.isNullOrEmpty(config.getCredentialsID())) {
      throw new AbortException("API Credentials not defined. Make sure credentials are defined globally or in job.");
    }

    if (Strings.isNullOrEmpty(config.getSysdigToken())) {
      throw new AbortException(String.format("Cannot find Jenkins credentials by ID: '%s'. Ensure credentials are defined in Jenkins before using them", config.getCredentialsID()));
    }

    BuildWorker worker = null;
    ReportConverter.GATE_ACTION finalAction = null;
    try {

      Scanner scanner = config.getInlineScanning() ?
        new InlineScanner(listener, config, workspace, logger) :
        new BackendScanner(config, logger);

      ReportConverter reporter = new ReportConverter(logger);

      worker = new BuildWorker(run, workspace, listener, logger, scanner, reporter);

      finalAction = worker.scanAndBuildReports(config);
    } catch (InterruptedException e) {
      logger.logWarn("Interrupted when executing Sysdig Secure Container Image Scanner Plugin", e);
      run.setResult(Result.ABORTED);
      return;
    } catch (AbortException e) {
      throw e;
    } catch (Exception e) {
      logger.logError("Error when executing Sysdig Secure Container Image Scanner Plugin", e);
      if (config.getBailOnPluginFail()) {
        throw new AbortException("Failing Sysdig Secure Container Image Scanner Plugin step due to errors in plugin execution");
      }

      logger.logWarn("Ignoring errors in plugin execution");
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
      LOG.warning("Completed Sysdig Secure Container Image Scanner step, project: "
        + run.getParent().getDisplayName() +
        ", job: " + run.getNumber());
    }

    //TODO(airadier): Option to mark as unstable build?
    /* Evaluate result of step based on gate action */
    if (null == finalAction) {
      logger.logWarn("Marking Sysdig Secure Container Image Scanner step as successful, no final result");
    } else if (config.getBailOnFail() && ReportConverter.GATE_ACTION.FAIL.equals(finalAction)) {
      logger.logError("Failing Sysdig Secure Container Image Scanner Plugin step due to final result " + finalAction);
      run.setResult(Result.FAILURE);
    } else {
      logger.logInfo("Marking Sysdig Secure Container Image Scanner step as successful, final result " + finalAction);
      run.setResult(Result.SUCCESS);
    }
  }


}
