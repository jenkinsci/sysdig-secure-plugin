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
package com.sysdig.jenkins.plugins.sysdig;

import com.sysdig.jenkins.plugins.sysdig.domain.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.RunContext;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Vector;

public class SysdigIaCScanBuilder extends Builder implements SimpleBuildStep {

  private String secureAPIToken;
  private boolean listUnsupported = false; // --list-unsupported-resources
  private boolean isRecursive = true;
  private String path = "";
  private String severityThreshold = "h";
  private String sysdigEnv = "";
  private String version = "latest";

  @DataBoundConstructor
  public SysdigIaCScanBuilder(String secureAPIToken) {
    this.secureAPIToken = secureAPIToken;
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

  public String getSecureAPIToken() {
    return secureAPIToken;
  }

  // FIXME(fede): We are temporarily passing the actual token. We need to be passing the secretID in jenkins instead.
  public void setSecureAPIToken(String secureAPIToken) {
    this.secureAPIToken = secureAPIToken;
  }

  @DataBoundSetter
  public void setSeverityThreshold(String severityThreshold) {
    this.severityThreshold = severityThreshold;
  }

  private String getProcessOutput(Process p) throws IOException {

    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
    StringBuilder builder = new StringBuilder();
    String line = null;
    while ((line = reader.readLine()) != null) {
      builder.append(line);
      builder.append(System.getProperty("line.separator"));
    }
    String output = builder.toString();
    reader.close();
    return output;
  }

  private Vector<String> buildCommand(String exec) {
    Vector<String> cmd = new Vector<>();
    cmd.add(exec);
    cmd.add("--iac");
    cmd.add("-a");
    if (sysdigEnv.isEmpty()) {
      sysdigEnv = "https://secure-staging.sysdig.com";
    }
    cmd.add(sysdigEnv);

    if (isRecursive) {
      cmd.add("-r");
    }

    if (listUnsupported) {
      cmd.add("--list-unsupported-resources");
    }

    severity(cmd);

    cmd.add(path);

    return cmd;
  }

  private void severity(Vector<String> cmd) {
    cmd.add("-f");
    cmd.add(severityThreshold);
  }

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
    throws InterruptedException, IOException {
    RunContext runContext = new RunContext(run, workspace, listener, launcher);
    SysdigLogger logger = runContext.getLogger();

    logger.logInfo("Attempting to download CLI");
    String cwd = run.getRootDir().getAbsolutePath();
    CLIDownloadAction act;
    try {
      act = new CLIDownloadAction("IaC scanner", cwd, version);
    } catch (Exception e) {
      logger.logError(String.format("Failed to download CLI version: %s", version), e);
      run.setResult(Result.FAILURE);
      return;
    }
    run.addAction(act);
    logger.logInfo(String.format("CLI executable path: %s", act.cliExecPath()));

    logger.logInfo("Starting scan");
    try {
      if (act.cliExecPath().isEmpty()) {
        logger.logError("CLI executable path is empty");
        throw new Exception("empty path");
      }
      String exec = act.cliExecPath();
      ProcessBuilder pb = new ProcessBuilder(buildCommand(exec));
      Map<String, String> envv = pb.environment();
      envv.put("SECURE_API_TOKEN", secureAPIToken);

      logger.logDebug("Command to execute: " + pb.command());

      Process p = pb.start();
      logger.logInfo("Process started...");
      p.waitFor();

      String output = getProcessOutput(p);
      int exitCode = p.exitValue();
      logger.logInfo(String.format("Process finished with status %d", exitCode));
      logger.logInfo(output);

      switch (exitCode) {
        case 0:
          run.setResult(Result.SUCCESS);
          break;
        case 1:
          throw new FailedCLIScan(String.format("Scan failed%n%s", output));
        case 2:
          throw new BadParamCLIScan(String.format("Scan failed%n%s", output));
        case 3:
          throw new FailedCLIScan(String.format("Unable to complete scan, check if your token is valid%n%s", output));
        default:
          logger.logError(String.format("Unknown error: %s", output));
          run.setResult(Result.FAILURE);
          break;
      }
    } catch (FailedCLIScan e) {
      logger.logError(String.format("IaC scan failed (status 1): %s", e.getMessage()), e);
      run.setResult(Result.FAILURE);
    } catch (BadParamCLIScan e) {
      logger.logError(String.format("IaC scan failed due to missing parameters: %s", e.getMessage()), e);
      run.setResult(Result.FAILURE);
    } catch (Exception e) {
      logger.logError(String.format("Failed processing output: %s", e.getMessage()), e);
      run.setResult(Result.FAILURE);
    }
    logger.logInfo("Process completed");
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

  @Symbol("greet")
  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

    public static final boolean DEFAULT_IS_RECURSIVE = true;
    public static final String DEFAULT_CLI_VERSION = "latest";


    public FormValidation doCheckSysdigEnv(@QueryParameter String value, @QueryParameter boolean useFrench)
      throws IOException, ServletException {
      if (value.length() == 0)
        return FormValidation.error("missing field");
      if (value.length() < 4)
        return FormValidation.warning("too");

      return FormValidation.ok();
    }

    public FormValidation doCheckSecureAPIToken(@QueryParameter String value, @QueryParameter boolean useFrench)
      throws IOException, ServletException {
      if (value.length() == 0)
        return FormValidation.error("missing field");
      if (value.length() < 4)
        return FormValidation.warning("too");

      return FormValidation.ok();
    }

    public FormValidation doCheckPath(@QueryParameter String value, @QueryParameter boolean useFrench)
      throws IOException, ServletException {
      if (value.length() == 0)
        return FormValidation.error("missing field");

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
