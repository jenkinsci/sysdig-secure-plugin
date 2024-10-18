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
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.scanner.ImageScanningResult;
import com.sysdig.jenkins.plugins.sysdig.scanner.NewEngineScanner;
import hudson.AbortException;
import hudson.model.Run;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.logging.Logger;

public class NewEngineBuilderExecutor {
  //  Log handler for logging above INFO level events to jenkins log
  private static final Logger LOG = Logger.getLogger(NewEngineBuilderExecutor.class.getName());

  private final SysdigLogger logger;

  public NewEngineBuilderExecutor(@Nonnull NewEngineBuilder builder,
                                @Nonnull RunContext runContext) throws AbortException {

    LOG.warning(String.format("Starting Sysdig Secure Container Image Scanner step, project: %s, job: %d", runContext.getProjectName(), runContext.getJobNumber()));


    /* Instantiate config and a new build worker */
    logger = runContext.getSysdigLogger();

    /* Fetch Jenkins creds first, can't push this lower down the chain since it requires Jenkins instance object */
    //Prefer the job credentials set by the user and fallback to the global ones
    String credID = !Strings.isNullOrEmpty(builder.getEngineCredentialsId()) ? builder.getEngineCredentialsId() : builder.getDescriptor().getEngineCredentialsId();
    final String sysdigToken = runContext.getSysdigTokenFromCredentials(credID);

    NewEngineBuildConfig config = new NewEngineBuildConfig(builder, sysdigToken);
    config.printWith(logger);

    BuildWorker worker = null;
    ImageScanningResult.FinalAction finalAction = null;
    try {

      NewEngineScanner scanner = new NewEngineScanner(config, runContext);
      worker = new BuildWorker(runContext, scanner);
      finalAction = worker.scanAndBuildReports(config.getImageName());

    } catch (Exception e) {
      if (config.getBailOnPluginFail() || builder.getBailOnPluginFail()) {
        logger.logError("Failing Sysdig Secure Container Image Scanner Plugin step due to errors in plugin execution", e);
        throw new AbortException("Failing Sysdig Secure Container Image Scanner Plugin step due to errors in plugin execution");
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
      LOG.warning("Completed Sysdig Secure Container Image Scanner step, project: "
        + runContext.getProjectName() +
        ", job: " + runContext.getJobNumber());
    }

    /* Evaluate result of step based on gate action */
    if (null == finalAction) {
      logger.logInfo("Marking Sysdig Secure Container Image Scanner step as successful, no final result");
    } else if ((config.getBailOnFail() || builder.getBailOnFail()) && ImageScanningResult.FinalAction.ActionFail.equals(finalAction)) {
      logger.logWarn("Failing Sysdig Secure Container Image Scanner Plugin step due to final result " + finalAction);
      throw new AbortException("Failing Sysdig Secure Container Image Scanner Plugin step due to final result " + finalAction);
    } else {
      logger.logInfo("Marking Sysdig Secure Container Image Scanner step as successful, final result " + finalAction);
    }
  }

}
