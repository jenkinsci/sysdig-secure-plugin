package com.sysdig.jenkins.plugins.sysdig;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.google.common.base.Strings;
import com.sysdig.jenkins.plugins.sysdig.log.ConsoleLog;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Action;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import jenkins.tasks.SimpleBuildStep;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;

/**
 * Wraps the context of a run. You can think of a run as the job runtime execution of a project.
 */
public class RunContext implements Serializable {
  private static final long serialVersionUID = 1;

  private final transient Run<?, ?> run;
  private final FilePath workspace;
  private final TaskListener listener;
  private final EnvVars envVars;
  private final SysdigLogger logger;
  private final transient Launcher launcher;

  public RunContext(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull TaskListener listener, @Nonnull EnvVars envVars) throws IOException, InterruptedException {
    this.run = run;
    this.workspace = workspace;
    this.listener = listener;
    this.envVars = envVars;
    this.logger = new ConsoleLog("SysdigSecurePlugin", listener, false);
    this.launcher = workspace.createLauncher(listener);
  }

  // The actual run from jenkins.
  public Run<?, ?> getRun() {
    return run;
  }

  public FilePath getPathFromWorkspace(@Nonnull String... subpaths) {
    var finalPath = workspace;
    for (String subpath : subpaths) {
      finalPath = new FilePath(finalPath, subpath);
    }
    return finalPath;
  }

  // The env vars of the execution.
  public EnvVars getEnvVars() {
    return envVars;
  }

  // Returns the logger from the task listener. Useful for common log reporting.
  public SysdigLogger getSysdigLogger() {
    return logger;
  }

  public String getSysdigTokenFromCredentials(@Nonnull String credID) throws AbortException {
    logger.logDebug("Processing Jenkins credential ID " + credID);

    // We are expecting that either the job credentials or global credentials will be set, otherwise, fail the build
    if (Strings.isNullOrEmpty(credID)) {
      throw new AbortException("API Credentials not defined. Make sure credentials are defined globally or in job.");
    }

    StandardUsernamePasswordCredentials creds = CredentialsProvider.findCredentialById(credID, StandardUsernamePasswordCredentials.class, run, Collections.emptyList());
    if (null == creds) {
      throw new AbortException(String.format("Cannot find Jenkins credentials by ID: '%s'. Ensure credentials are defined in Jenkins before using them", credID));
    }

    return creds.getPassword().getPlainText();
  }

  public Launcher getLauncher() {
    return launcher;
  }


  public void perform(@Nonnull SimpleBuildStep buildStep) throws IOException, InterruptedException {
    buildStep.perform(run, workspace, envVars, launcher, listener);
  }

  public String getProjectName() {
    return run.getParent().getDisplayName();
  }

  public int getJobNumber() {
    return run.getNumber();
  }

  public <V, E extends Throwable> V call(@Nonnull Callable<V, E> act) throws IOException, InterruptedException, E {
    return workspace.act(act);
  }
}
