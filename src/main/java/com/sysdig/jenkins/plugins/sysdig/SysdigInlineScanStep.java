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
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;

/**
 * <p>Sysdig Secure Plugin enables Jenkins users to scan container images, generate analysis, evaluate gate policy, and execute customizable
 * queries. The plugin can be used in a freestyle project as a step or invoked from a pipeline script</p>
 */
public class SysdigInlineScanStep extends Builder implements SimpleBuildStep, BuilderConfig {

  // Assigning the defaults here for pipeline builds
  private final String imagesFile;
  private boolean bailOnFail = SysdigBuilder.DescriptorImpl.DEFAULT_BAIL_ON_FAIL;
  private boolean bailOnPluginFail = SysdigBuilder.DescriptorImpl.DEFAULT_BAIL_ON_PLUGIN_FAIL;

  // Override global config. Supported for sysdig-secure-engine mode config only
  private String engineURL = SysdigBuilder.DescriptorImpl.EMPTY_STRING;
  private String engineCredentialsId = SysdigBuilder.DescriptorImpl.EMPTY_STRING;
  private boolean engineTLSVerify = SysdigBuilder.DescriptorImpl.DEFAULT_ENGINE_VERIFY;

  // Getters are used by config.jelly
  @Override
  public String getImagesFile() {
    return imagesFile;
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
  public boolean isInlineScanning() {
    return true;
  }

  @Override
  public String getEngineurl() {
    return getEngineURL();
  }

  public String getEngineURL() {
    return engineURL;
  }

  @Override
  public boolean getEngineverify() {
    return getEngineTLSVerify();
  }

  public boolean getEngineTLSVerify() {
    return engineTLSVerify;
  }

  @Override
  public String getEngineCredentialsId() {
    return engineCredentialsId;
  }

  @DataBoundSetter
  @SuppressWarnings("unused")
  public void setBailOnFail(boolean bailOnFail) {
    this.bailOnFail = bailOnFail;
  }

  @DataBoundSetter
  @SuppressWarnings("unused")
  public void setBailOnPluginFail(boolean bailOnPluginFail) {
    this.bailOnPluginFail = bailOnPluginFail;
  }

  @DataBoundSetter
  @SuppressWarnings("unused")
  public void setEngineURL(String engineURL) {
    this.engineURL = engineURL;
  }

  @DataBoundSetter
  @SuppressWarnings("unused")
  public void setEngineTLSVerify(boolean engineTLSVerify) {
    this.engineTLSVerify = engineTLSVerify;
  }

  @DataBoundSetter
  @SuppressWarnings("unused")
  public void setEngineCredentialsId(String engineCredentialsId) {
    this.engineCredentialsId = engineCredentialsId;
  }

  // Fields in config.jelly must match the parameter names in the "DataBoundConstructor" or "DataBoundSetter"
  @DataBoundConstructor
  @SuppressWarnings("unused")
  public SysdigInlineScanStep(String imagesFile) {
    this.imagesFile = imagesFile;
  }

  @Override
  public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws AbortException {
    GlobalConfig globalConfig = (SysdigBuilder.DescriptorImpl)Jenkins.get().getDescriptorOrDie(SysdigBuilder.class);
    new SysdigBuilderExecutor(this, globalConfig, run, workspace, listener);
  }

  @Override
  public DescriptorImpl getDescriptor() {
      return (DescriptorImpl)super.getDescriptor();
  }

  @Symbol("sysdigInlineScan") // For Jenkins pipeline workflow. This lets pipeline refer to step using the defined identifier
  @Extension // This indicates to Jenkins that this is an implementation of an extension point.
  public static final class DescriptorImpl extends SysdigBuilderDescriptor {
    @Nonnull
    @Override
    public String getDisplayName() {
      return "Sysdig Secure Inline Scan";
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
    public FormValidation doCheckImagesFile(@QueryParameter String value) {
      return Strings.isNullOrEmpty(value) ? FormValidation.error("Please enter a valid file name") : FormValidation.ok();
    }
  }
}

