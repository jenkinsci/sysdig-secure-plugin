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

public class NewEngineBuilder extends Builder implements SimpleBuildStep, NewEngineScanStep {

  // Assigning the defaults here for pipeline builds
  private final String imageName;
  private boolean bailOnFail = SysdigBuilder.DescriptorImpl.DEFAULT_BAIL_ON_FAIL;
  private boolean bailOnPluginFail = SysdigBuilder.DescriptorImpl.DEFAULT_BAIL_ON_PLUGIN_FAIL;

  // Override global config. Supported for sysdig-secure-engine mode config only
  private String engineURL = SysdigBuilder.DescriptorImpl.EMPTY_STRING;
  private String engineCredentialsId = SysdigBuilder.DescriptorImpl.EMPTY_STRING;
  private boolean engineVerify = SysdigBuilder.DescriptorImpl.DEFAULT_ENGINE_VERIFY;
  private String inlineScanExtraParams = SysdigBuilder.DescriptorImpl.EMPTY_STRING;
  private String policiesToApply = "";
  private String scannerBinaryPath = SysdigBuilder.DescriptorImpl.EMPTY_STRING;


  @Override
  public String getImageName() {
    return imageName;
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
  public String getPoliciesToApply() {
    return policiesToApply;
  }

  @Override
  public String getEngineURL() {
    return engineURL;
  }

  @Override
  public String getEngineCredentialsId() {
    return engineCredentialsId;
  }

  @Override
  public boolean getEngineVerify() {
    return engineVerify;
  }

  @Override
  public String getInlineScanExtraParams() { return inlineScanExtraParams; }

  @Override
  public String getScannerBinaryPath() { return scannerBinaryPath; }

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
  public void setPoliciesToApply(String policiesToApply) {
    this.policiesToApply = policiesToApply;
  }

  @DataBoundSetter
  @Override
  public void setEngineURL(String engineURL) {
    this.engineURL = engineURL;
  }

  @DataBoundSetter
  @Override
  public void setEngineCredentialsId(String engineCredentialsId) {
    this.engineCredentialsId = engineCredentialsId;
  }

  @DataBoundSetter
  @Override
  public void setEngineVerify(boolean engineVerify) {
    this.engineVerify = engineVerify;
  }

  @DataBoundSetter
  @Override
  public void setScannerBinaryPath(String scannerBinaryPath) { this.scannerBinaryPath = scannerBinaryPath; }


  @DataBoundSetter
  @Override
  public void setInlineScanExtraParams(String inlineScanExtraParams) {
    this.inlineScanExtraParams = inlineScanExtraParams;
  }

  // Fields in config.jelly must match the parameter names in the "DataBoundConstructor" or "DataBoundSetter"
  @DataBoundConstructor
  public NewEngineBuilder(String imageName) {
    this.imageName = imageName;
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
    new NewEngineBuilderExecutor(this, run, workspace, listener, envVars);
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  @Extension // This indicates to Jenkins that this is an implementation of an extension point.
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
    public static final String DEFAULT_NAME = "sysdig_secure_images";
    public static final boolean DEFAULT_BAIL_ON_FAIL = true;
    public static final boolean DEFAULT_BAIL_ON_PLUGIN_FAIL = true;
    public static final String DEFAULT_ENGINE_URL = "https://secure.sysdig.com";
    public static final boolean DEFAULT_ENGINE_VERIFY = true;

    public DescriptorImpl() {
      load();
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return "Sysdig Image Scanning";
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) {
      req.bindJSON(this, formData); // Use stapler request to bind
      save();
      return true;
    }

    /**
     * Performs on-the-fly validation of the form field 'imageName'
     *
     * @param value This parameter receives the value that the user has typed in the 'Image name' box
     * @return Indicates the outcome of the validation. This is sent to the browser. <p> Note that returning {@link
     * FormValidation#error(String)} does not prevent the form from being saved. It just means that a message will be displayed to the
     * user
     */
    @SuppressWarnings("unused")
    public FormValidation doCheckImageName(@QueryParameter String value) {
      return Strings.isNullOrEmpty(value) ? FormValidation.error("Please enter a valid image name") : FormValidation.ok();
    }

    //TODO: Add image list file support

    @SuppressWarnings("unused")
    public ListBoxModel doFillEngineCredentialsIdItems(@QueryParameter String credentialsId) {
      StandardListBoxModel result = new StandardListBoxModel();

      if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
        return result.includeCurrentValue(credentialsId);
      }

      return result.includeEmptyValue().includeMatchingAs(ACL.SYSTEM,
        Jenkins.getInstance(),
        //TODO: Move to another kind of credentials
        StandardUsernamePasswordCredentials.class,
        Collections.emptyList(),
        CredentialsMatchers.always());
    }

  }
}

