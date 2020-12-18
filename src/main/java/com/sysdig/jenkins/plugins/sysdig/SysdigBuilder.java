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
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
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
public class SysdigBuilder extends Builder implements SimpleBuildStep {

  // Assigning the defaults here for pipeline builds
  private final String name;
  private boolean bailOnFail = DescriptorImpl.DEFAULT_BAIL_ON_FAIL;
  private boolean bailOnPluginFail = DescriptorImpl.DEFAULT_BAIL_ON_PLUGIN_FAIL;
  private boolean inlineScanning = DescriptorImpl.DEFAULT_INLINE_SCANNING;

  // Override global config. Supported for sysdig-secure-engine mode config only
  private String engineurl = DescriptorImpl.EMPTY_STRING;
  private String engineCredentialsId = DescriptorImpl.EMPTY_STRING;
  private boolean engineverify = DescriptorImpl.DEFAULT_ENGINE_VERIFY;
  // More flags to indicate boolean override, ugh!

  // Getters are used by config.jelly
  public String getName() {
    return name;
  }

  public boolean getBailOnFail() {
    return bailOnFail;
  }

  public boolean getBailOnPluginFail() {
    return bailOnPluginFail;
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

  public boolean isInlineScanning() {
    return inlineScanning;
  }

  @DataBoundSetter
  public void setBailOnFail(boolean bailOnFail) {
    this.bailOnFail = bailOnFail;
  }

  @DataBoundSetter
  public void setBailOnPluginFail(boolean bailOnPluginFail) {
    this.bailOnPluginFail = bailOnPluginFail;
  }

  @DataBoundSetter
  public void setEngineurl(String engineurl) {
    this.engineurl = engineurl;
  }

  @DataBoundSetter
  public void setEngineCredentialsId(String engineCredentialsId) {
    this.engineCredentialsId = engineCredentialsId;
  }

  @DataBoundSetter
  public void setEngineverify(boolean engineverify) {
    this.engineverify = engineverify;
  }

  @DataBoundSetter
  public void setInlineScanning(boolean inlineScanning) {
    this.inlineScanning = inlineScanning;
  }

  // Fields in config.jelly must match the parameter names in the "DataBoundConstructor" or "DataBoundSetter"
  @DataBoundConstructor
  public SysdigBuilder(String name) {
    this.name = name;
  }

  @Override
  public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws AbortException {
    new SysdigBuilderExecutor(this, run, workspace, listener);
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  @Symbol("sysdig") // For Jenkins pipeline workflow. This lets pipeline refer to step using the defined identifier
  @Extension // This indicates to Jenkins that this is an implementation of an extension point.
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
    // Default job level config that may be used both by config.jelly and an instance of SysdigBuilder

    public static final String EMPTY_STRING = "";
    // Used in the jelly template
    public static final String DEFAULT_NAME = "sysdig_secure_images";
    public static final boolean DEFAULT_BAIL_ON_FAIL = true;
    public static final boolean DEFAULT_BAIL_ON_PLUGIN_FAIL = true;
    public static final boolean DEFAULT_INLINE_SCANNING = true;
    public static final String DEFAULT_ENGINE_URL = "https://secure.sysdig.com";
    public static final boolean DEFAULT_ENGINE_VERIFY = true;

    // Global configuration
    private boolean debug = false;
    private String engineurl = DEFAULT_ENGINE_URL;
    private String engineCredentialsId;
    private boolean engineverify = DEFAULT_ENGINE_VERIFY;

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

    public void setEngineurl(String engineurl) {
      this.engineurl = engineurl;
    }

    public void setEngineCredentialsId(String engineCredentialsId) {
      this.engineCredentialsId = engineCredentialsId;
    }

    public void setEngineverify(boolean engineverify) {
      this.engineverify = engineverify;
    }

    @Deprecated
    public boolean getEnabled() {
      return enabled;
    }

    public boolean getDebug() {
      return debug;
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

    public DescriptorImpl() {
      load();
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return "Sysdig Secure Container Image Scanner";
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

