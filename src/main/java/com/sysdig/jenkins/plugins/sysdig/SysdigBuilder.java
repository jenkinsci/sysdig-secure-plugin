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


import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.google.common.base.Strings;
import hudson.*;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;

/**
 * <p>Sysdig Secure Plugin  enables Jenkins users to scan container images, generate analysis, evaluate gate policy, and execute customizable
 * queries. The plugin can be used in a freestyle project as a step or invoked from a pipeline script</p>
 *
 * <p>Requirements:</p>
 *
 * <ol> <li>Jenkins installed and configured either as a single system, or with multiple configured jenkins worker nodes</li>
 *
 * <li>Each host on which jenkins jobs will run must have docker installed and the jenkins user (or whichever user you have configured
 * jenkins to run jobs as) must be allowed to interact with docker</li>
 * </ol>
 */
public class SysdigBuilder extends Builder implements SimpleBuildStep, SysdigScanStep {

  // Assigning the defaults here for pipeline builds
  private final String name;
  private boolean bailOnFail = DescriptorImpl.DEFAULT_BAIL_ON_FAIL;
  private boolean bailOnPluginFail = DescriptorImpl.DEFAULT_BAIL_ON_PLUGIN_FAIL;
  private boolean inlineScanning = DescriptorImpl.DEFAULT_INLINE_SCANNING;
  private boolean forceScan = DescriptorImpl.DEFAULT_FORCE_SCAN;

  // Override global config. Supported for sysdig-secure-engine mode config only
  private String engineurl = DescriptorImpl.EMPTY_STRING;
  private String engineCredentialsId = DescriptorImpl.EMPTY_STRING;
  private boolean engineverify = DescriptorImpl.DEFAULT_ENGINE_VERIFY;
  private String runAsUser = DescriptorImpl.EMPTY_STRING;
  private String inlineScanExtraParams = DescriptorImpl.EMPTY_STRING;

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean getBailOnFail() {
    return bailOnFail;
  }

  @Override
  public boolean getBailOnPluginFail() {
    return bailOnPluginFail;
  }

  @Override
  public String getEngineurl() {
    return engineurl;
  }

  @Override
  public String getEngineCredentialsId() {
    return engineCredentialsId;
  }

  @Override
  public boolean getEngineverify() {
    return engineverify;
  }

  @Override
  public String getRunAsUser() {
    return runAsUser;
  }

  @Override
  public String getInlineScanExtraParams() {
    return inlineScanExtraParams;
  }

  @Override
  public boolean isInlineScanning() {
    return inlineScanning;
  }

  @Override
  public boolean getForceScan() {
    return forceScan;
  }

  @DataBoundSetter
  @Override
  public void setBailOnFail(boolean bailOnFail) {
    this.bailOnFail = bailOnFail;
  }

  @DataBoundSetter
  @Override
  public void setBailOnPluginFail(boolean bailOnPluginFail) {
    this.bailOnPluginFail = bailOnPluginFail;
  }

  @DataBoundSetter
  @Override
  public void setEngineurl(String engineurl) {
    this.engineurl = engineurl;
  }

  @DataBoundSetter
  @Override
  public void setEngineCredentialsId(String engineCredentialsId) {
    this.engineCredentialsId = engineCredentialsId;
  }

  @DataBoundSetter
  @Override
  public void setEngineverify(boolean engineverify) {
    this.engineverify = engineverify;
  }

  @DataBoundSetter
  @Override
  public void setRunAsUser(String runAsUser) {
    this.runAsUser = runAsUser;
  }

  @DataBoundSetter
  @Override
  public void setInlineScanExtraParams(String inlineScanExtraParams) {
    this.inlineScanExtraParams = inlineScanExtraParams;
  }

  @DataBoundSetter
  @Override
  public void setInlineScanning(boolean inlineScanning) {
    this.inlineScanning = inlineScanning;
  }

  @DataBoundSetter
  @Override
  public void setForceScan(boolean forceScan) {
    this.forceScan = forceScan;
  }

  // Fields in config.jelly must match the parameter names in the "DataBoundConstructor" or "DataBoundSetter"
  @DataBoundConstructor
  public SysdigBuilder(String name) {
    this.name = name;
  }

  @Override
  public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws IOException, InterruptedException {
    Computer computer = workspace.toComputer();
    EnvVars envVars = new EnvVars();
    if (computer != null) {
      envVars.putAll(computer.buildEnvironment(listener));
    }

    EnvVars buildEnvVars = run.getEnvironment(listener);
    envVars.putAll(buildEnvVars);

    perform(run, workspace, launcher, listener, envVars);
  }

  public void perform(Run run, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars envVars) throws AbortException {
    new SysdigBuilderExecutor(this, run, workspace, listener, envVars);
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  @Extension // This indicates to Jenkins that this is an implementation of an extension point.
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
    // Default job level config that may be used both by config.jelly and an instance of SysdigBuilder

    public static final String EMPTY_STRING = "";
    // Used in the jelly template
    public static final String DEFAULT_NAME = "sysdig_secure_images";
    public static final boolean DEFAULT_BAIL_ON_FAIL = true;
    public static final boolean DEFAULT_BAIL_ON_PLUGIN_FAIL = true;
    public static final boolean DEFAULT_INLINE_SCANNING = false;
    public static final boolean DEFAULT_FORCE_SCAN = false;
    public static final String DEFAULT_ENGINE_URL = "https://secure.sysdig.com";
    public static final boolean DEFAULT_ENGINE_VERIFY = true;

    // Global configuration
    private boolean debug = false;
    private String engineurl = DEFAULT_ENGINE_URL;
    private String engineCredentialsId;
    private boolean engineverify = DEFAULT_ENGINE_VERIFY;
    private String runAsUser = EMPTY_STRING;
    private String inlineScanExtraParams = EMPTY_STRING;
    private String inlinescanimage = "";
    private boolean forceinlinescan = false;
    private boolean forceNewEngine = false;
    private String cliVersionToApply = "";
    private String customCliVersion = "";


    private String scannerBinaryPath = EMPTY_STRING;

    // Upgrade case, you can never really remove these variables once they are introduced
    @Deprecated
    private boolean enabled;

    @Deprecated
    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public void setDebug(boolean debug) {
      this.debug = debug;
    }

    public void setCliVersionToApply(String cliVersionToApply) {
      this.cliVersionToApply = cliVersionToApply;
    }

    public void setCustomCliVersion(String customCliVersion) {
      this.customCliVersion = customCliVersion;
    }

    public void setEngineurl(String engineurl) {
      this.engineurl = engineurl;
    }

    public void setEngineCredentialsId(String engineCredentialsId) {
      this.engineCredentialsId = engineCredentialsId;
    }

    public void setEngineverify(boolean engineverify) {
      this.engineverify = engineverify;
    }

    public void setRunAsUser(String runAsUser) {
      this.runAsUser = runAsUser;
    }

    public void setinlineScanExtraParams(String inlineScanExtraParams) {
      this.inlineScanExtraParams = inlineScanExtraParams;
    }


    public void setInlinescanimage(String inlinescanimage) {
      this.inlinescanimage = inlinescanimage;
    }

    public void setForceinlinescan(boolean forceinlinescan) {
      this.forceinlinescan = forceinlinescan;
    }

    public void setForceNewEngine(boolean forceNewEngine) {
      this.forceNewEngine = forceNewEngine;
    }

    public void setScannerBinaryPath(String scannerBinaryPath) {
      this.scannerBinaryPath = scannerBinaryPath;
    }


    @Deprecated
    public boolean getEnabled() {
      return enabled;
    }

    public boolean getDebug() {
      return debug;
    }

    public String getCliVersionToApply() {
      return cliVersionToApply;
    }

    public String getCustomCliVersion() {
      return customCliVersion;
    }

    public String getEngineurl() {
      return engineurl;
    }

    public String getEngineCredentialsId() {
      return engineCredentialsId;
    }

    public boolean getEngineverify() {
      return engineverify;
    }

    public String getRunAsUser() {
      return runAsUser;
    }

    public String getInlineScanExtraParams() {
      return inlineScanExtraParams;
    }

    public String getInlinescanimage() {
      return inlinescanimage;
    }

    public boolean getForceinlinescan() {
      return forceinlinescan;
    }

    public boolean getForceNewEngine() {
      return forceNewEngine;
    }

    public String getScannerBinaryPath() {
      return scannerBinaryPath;
    }

    public DescriptorImpl() {
      load();
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return "Sysdig Secure Container Image Scanner (Legacy)";
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) {
      req.bindJSON(this, formData); // Use stapler request to bind
      save();
      return true;
    }

    /**
     * Performs on-the-fly validation of the form field 'name' (Image list file)
     *
     * @param value This parameter receives the value that the user has typed in the 'Image list file' box
     * @return Indicates the outcome of the validation. This is sent to the browser. <p> Note that returning {@link
     * FormValidation#error(String)} does not prevent the form from being saved. It just means that a message will be displayed to the
     * user
     */
    @SuppressWarnings("unused")
    public FormValidation doCheckName(@QueryParameter String value) {
      return Strings.isNullOrEmpty(value) ? FormValidation.error("Please enter a valid file name") : FormValidation.ok();
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
  }

}
