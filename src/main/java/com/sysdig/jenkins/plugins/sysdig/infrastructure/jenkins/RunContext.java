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
package com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.google.common.base.Strings;
import com.sysdig.jenkins.plugins.sysdig.domain.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.log.ConsoleLog;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import jenkins.tasks.SimpleBuildStep;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;

/**
 * Wraps the context of a Jenkins run.
 * A run represents the runtime execution of a Jenkins job.
 */
public class RunContext implements Serializable {
  private static final long serialVersionUID = 1;

  // The Jenkins run associated with this context
  private final transient Run<?, ?> run;

  // The workspace directory where the build is executed
  private final FilePath workspace;

  // Listener to capture and display logs
  private final TaskListener listener;

  // Environment variables available during the build
  private final EnvVars envVars;

  // Custom logger for Sysdig-specific logging
  private final SysdigLogger logger;

  // Launcher to execute processes
  private final transient Launcher launcher;

  /**
   * Constructs a new RunContext with the given parameters.
   *
   * @param run       The Jenkins run.
   * @param workspace The workspace directory.
   * @param listener  The task listener for logging.
   * @param envVars   The environment variables.
   * @throws IOException          If an I/O error occurs.
   * @throws InterruptedException If the thread is interrupted.
   */
  public RunContext(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull TaskListener listener, @Nonnull EnvVars envVars) throws IOException, InterruptedException {
    this.run = run;
    this.workspace = workspace;
    this.listener = listener;
    this.envVars = envVars;
    this.logger = new ConsoleLog("SysdigSecurePlugin", listener, false);
    this.launcher = workspace.createLauncher(listener);
  }

  /**
   * Retrieves the Jenkins run associated with this context.
   *
   * @return The Jenkins run.
   */
  public Run<?, ?> getRun() {
    return run;
  }

  /**
   * Constructs a FilePath within the workspace by appending the given subpaths.
   *
   * @param subpaths The subpaths to append.
   * @return The resulting FilePath.
   */
  public FilePath getPathFromWorkspace(@Nonnull String... subpaths) {
    var finalPath = workspace;
    for (String subpath : subpaths) {
      finalPath = new FilePath(finalPath, subpath);
    }
    return finalPath;
  }

  /**
   * Retrieves the environment variables of the build execution.
   *
   * @return The environment variables.
   */
  public EnvVars getEnvVars() {
    return envVars;
  }

  /**
   * Retrieves the custom Sysdig logger.
   * Useful for consistent log reporting within the plugin.
   *
   * @return The SysdigLogger instance.
   */
  public SysdigLogger getLogger() {
    return logger;
  }

  /**
   * Retrieves the Sysdig API token from Jenkins credentials using the provided credential ID.
   *
   * @param credID The ID of the Jenkins credential.
   * @return The plain text Sysdig API token.
   * @throws AbortException If the credential ID is invalid or not found.
   */
  public String getSysdigTokenFromCredentials(@Nonnull String credID) throws AbortException {
    logger.logDebug("Processing Jenkins credential ID " + credID);

    // Ensure that a credential ID is provided
    if (Strings.isNullOrEmpty(credID)) {
      throw new AbortException("API Credentials not defined. Make sure credentials are defined globally or in job.");
    }

    // Attempt to find the credentials by ID within the Jenkins context
    StandardUsernamePasswordCredentials creds = CredentialsProvider.findCredentialById(credID, StandardUsernamePasswordCredentials.class, run, Collections.emptyList());

    // If credentials are not found, abort the build with an error message
    if (creds == null) {
      throw new AbortException(String.format("Cannot find Jenkins credentials by ID: '%s'. Ensure credentials are defined in Jenkins before using them", credID));
    }

    // Return the plain text password (Sysdig API token)
    return creds.getPassword().getPlainText();
  }

  /**
   * Executes a SimpleBuildStep within the current run context.
   *
   * @param buildStep The build step to perform.
   * @throws IOException          If an I/O error occurs during execution.
   * @throws InterruptedException If the thread is interrupted.
   */
  public void perform(@Nonnull SimpleBuildStep buildStep) throws IOException, InterruptedException {
    buildStep.perform(run, workspace, envVars, launcher, listener);
  }

  /**
   * Retrieves the display name of the Jenkins project associated with this run.
   *
   * @return The project name.
   */
  public String getProjectName() {
    return run.getParent().getDisplayName();
  }

  /**
   * Retrieves the build number of the current run.
   *
   * @return The job number.
   */
  public int getJobNumber() {
    return run.getNumber();
  }

  /**
   * Executes a Callable task within the workspace context.
   * This allows for remote execution in Jenkins' distributed build environment.
   *
   * @param act The Callable task to execute.
   * @param <V> The return type of the Callable.
   * @param <E> The exception type that the Callable might throw.
   * @return The result of the Callable execution.
   * @throws IOException          If an I/O error occurs during execution.
   * @throws InterruptedException If the thread is interrupted.
   * @throws E                    If the Callable throws an exception.
   */
  public <V, E extends Throwable> V call(@Nonnull Callable<V, E> act) throws IOException, InterruptedException, E {
    return workspace.act(act);
  }
}
