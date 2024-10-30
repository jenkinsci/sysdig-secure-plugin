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
package com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.iac.entrypoint;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.sysdig.jenkins.plugins.sysdig.domain.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.http.RetriableRemoteDownloader;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.RunContext;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.SysdigIaCScanningProcessBuilder;
import hudson.*;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Vector;

public class IaCScanningBuilder extends Builder implements SimpleBuildStep {
  private static final String FIXED_SCANNED_VERSION = "1.16.1";

  private String engineCredentialsId;
  private boolean listUnsupported = false;
  private boolean isRecursive = true;
  private String path = "";
  private String severityThreshold = "h";
  private String sysdigEnv = "";
  private String version = FIXED_SCANNED_VERSION;

  @DataBoundConstructor
  public IaCScanningBuilder(String engineCredentialsId) {
    this.engineCredentialsId = engineCredentialsId;
  }

  public boolean isListUnsupported() {
    return listUnsupported;
  }

  @DataBoundSetter
  public void setListUnsupported(boolean listUnsupported) {
    this.listUnsupported = listUnsupported;
  }

  public String getPath() {
    return path;
  }

  @DataBoundSetter
  public void setPath(String path) {
    this.path = path;
  }

  public boolean getIsRecursive() {
    return isRecursive;
  }

  @DataBoundSetter
  public void setIsRecursive(boolean isRecursive) {
    this.isRecursive = isRecursive;
  }

  public String getVersion() {
    return this.version;
  }

  @DataBoundSetter
  public void setVersion(String ver) {
    this.version = ver;
  }

  public String getSysdigEnv() {
    return sysdigEnv;
  }

  @DataBoundSetter
  public void setSysdigEnv(String env) {
    this.sysdigEnv = env;
  }

  @DataBoundSetter
  public void setSeverityThreshold(String severityThreshold) {
    this.severityThreshold = severityThreshold;
  }

  public String getEngineCredentialsId() {
    return engineCredentialsId;
  }

  @DataBoundSetter
  public void setEngineCredentialsId(String engineCredentialsId) {
    this.engineCredentialsId = engineCredentialsId;
  }


  private SysdigIaCScanningProcessBuilder buildCommand(RunContext runContext, String exec) throws AbortException {
    SysdigIaCScanningProcessBuilder processBuilder = new SysdigIaCScanningProcessBuilder(exec, runContext.getSysdigTokenFromCredentials(engineCredentialsId))
      .withRecursive(getIsRecursive())
      .withUnsupportedResources(isListUnsupported())
      .withSeverity(SysdigIaCScanningProcessBuilder.Severity.fromString(severityThreshold))
      .withPathsToScan(path)
      .withStdoutRedirectedTo(runContext.getLogger())
      .withStderrRedirectedTo(runContext.getLogger());

    if (!sysdigEnv.isEmpty()) processBuilder = processBuilder.withEngineURL(sysdigEnv);

    return processBuilder;
  }

  private void severity(Vector<String> cmd) {
    cmd.add("-f");
    cmd.add(severityThreshold);
  }

  @Override
  public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull EnvVars envVars, @Nonnull Launcher launcher, @Nonnull TaskListener listener)
    throws InterruptedException, IOException {
    RunContext runContext = new RunContext(run, workspace, envVars, listener);
    SysdigLogger logger = runContext.getLogger();

    logger.logInfo("Attempting to download CLI");
    RetriableRemoteDownloader downloader = new RetriableRemoteDownloader(runContext);

    FilePath filePath;
    try {
      filePath = downloader.downloadExecutable(sysdigCLIScannerURLForVersion(getVersion()), "sysdig-cli-scanner");
    } catch (Exception e) {
      logger.logError(String.format("Failed to download CLI version: %s", getVersion()), e);
      run.setResult(Result.FAILURE);
      return;
    }

    logger.logInfo("Starting scan");
    try {
      String exec = filePath.getRemote();
      SysdigIaCScanningProcessBuilder processBuilder = buildCommand(runContext, exec);
      logger.logDebug("Command to execute: " + String.join(" ", processBuilder.toCommandLineArguments()));

      int exitCode = processBuilder.launchAndWait(runContext.getLauncher());
      logger.logInfo(String.format("Process finished with status %d", exitCode));

      switch (exitCode) {
        case 0:
          run.setResult(Result.SUCCESS);
          break;
        case 1:
          throw new FailedCLIScan("Scan failed");
        case 2:
          throw new BadParamCLIScan("Scan failed");
        case 3:
          throw new FailedCLIScan("Unable to complete scan, check if your token is valid");
        default:
          logger.logError("Unknown error");
          run.setResult(Result.FAILURE);
          break;
      }
    } catch (FailedCLIScan e) {
      logger.logError(String.format("IaC scan failed (status 1): %s", e.getMessage()));
      run.setResult(Result.FAILURE);
    } catch (BadParamCLIScan e) {
      logger.logError(String.format("IaC scan failed due to missing parameters: %s", e.getMessage()));
      run.setResult(Result.FAILURE);
    } catch (Exception e) {
      logger.logError(String.format("Failed processing output: %s", e.getMessage()), e);
      run.setResult(Result.FAILURE);
    }
    logger.logInfo("Process completed");
  }


  // FIXME(fede): Remove this duplicate method
  private static URL sysdigCLIScannerURLForVersion(String version) throws MalformedURLException {
    if (version.trim().equalsIgnoreCase("latest")) version = FIXED_SCANNED_VERSION;

    String os = System.getProperty("os.name").toLowerCase().startsWith("mac") ? "darwin" : "linux";
    String arch = System.getProperty("os.arch").toLowerCase().startsWith("aarch64") ? "arm64" : "amd64";
    URL url = new URL(String.format("https://download.sysdig.com/scanning/bin/sysdig-cli-scanner/%s/%s/%s/sysdig-cli-scanner", version, os, arch));
    return url;
  }


  public static class FailedCLIScan extends Exception {
    public FailedCLIScan(String errorMessage) {
      super(errorMessage);
    }
  }

  public static class BadParamCLIScan extends Exception {
    public BadParamCLIScan(String errorMessage) {
      super(errorMessage);
    }
  }

  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
    public static final boolean DEFAULT_IS_RECURSIVE = true;
    public static final String DEFAULT_CLI_VERSION = "latest";

    @SuppressWarnings("unused")
    public FormValidation doCheckSysdigEnv(@QueryParameter String value, @QueryParameter boolean useFrench)
      throws IOException, ServletException {
      if (value.isEmpty())
        return FormValidation.error("missing field");
      if (value.length() < 4)
        return FormValidation.warning("too");

      return FormValidation.ok();
    }

    @SuppressWarnings("unused")
    public FormValidation doCheckSecureAPIToken(@QueryParameter String value, @QueryParameter boolean useFrench)
      throws IOException, ServletException {
      if (value.isEmpty())
        return FormValidation.error("missing field");
      if (value.length() < 4)
        return FormValidation.warning("too");

      return FormValidation.ok();
    }

    @SuppressWarnings("unused")
    public FormValidation doCheckPath(@QueryParameter String value, @QueryParameter boolean useFrench)
      throws IOException, ServletException {
      if (value.isEmpty())
        return FormValidation.error("missing field");

      return FormValidation.ok();
    }

    @SuppressWarnings("unused")
    public ListBoxModel doFillEngineCredentialsIdItems(@QueryParameter String credentialsId) {
      StandardListBoxModel result = new StandardListBoxModel();

      if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
        return result.includeCurrentValue(credentialsId);
      }

      return result.includeEmptyValue().includeMatchingAs(ACL.SYSTEM,
        Jenkins.get(),
        StandardUsernamePasswordCredentials.class,
        Collections.emptyList(),
        CredentialsMatchers.always());
    }

    public FormValidation doCheckCredentialsId(@QueryParameter String value) throws IOException, ServletException {
      if (value == null || value.trim().isEmpty()) {
        return FormValidation.error("Credentials ID must be provided.");
      }

      // Optionally, verify that the credentials exist
      StandardCredentials credentials = CredentialsMatchers.firstOrNull(
        CredentialsProvider.lookupCredentials(
          StandardCredentials.class,
          Jenkins.get(),
          ACL.SYSTEM,
          Collections.emptyList()
        ),
        CredentialsMatchers.withId(value)
      );

      if (credentials == null) {
        return FormValidation.error("No credentials found with the given ID.");
      }

      return FormValidation.ok();
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return "Sysdig Secure Code Scan";
    }
  }

}
