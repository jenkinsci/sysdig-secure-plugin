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
import com.sysdig.jenkins.plugins.sysdig.config.GlobalConfig;
import hudson.model.AbstractProject;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.util.Collections;

public abstract class SysdigBuilderDescriptor extends BuildStepDescriptor<Builder> implements GlobalConfig {
  // Default job level config that may be used both by config.jelly and an instance of SysdigBuilder

  public static final String EMPTY_STRING = "";
  // Used in the jelly template
  @SuppressWarnings("unused")
  public static final String DEFAULT_NAME = "sysdig_secure_images";
  public static final boolean DEFAULT_BAIL_ON_FAIL = true;
  public static final boolean DEFAULT_BAIL_ON_PLUGIN_FAIL = true;
  public static final boolean DEFAULT_INLINE_SCANNING = false;
  public static final String DEFAULT_ENGINE_URL = "https://secure.sysdig.com";
  public static final boolean DEFAULT_ENGINE_VERIFY = true;

  // Global configuration. Don't rename this fields, for backwards compatibility in serialization
  private boolean debug = false;
  private String engineurl = DEFAULT_ENGINE_URL;
  private String engineCredentialsId;
  private boolean engineverify = DEFAULT_ENGINE_VERIFY;

  // Upgrade case, you can never really remove these variables once they are introduced
  @Deprecated
  private boolean enabled;

  @Deprecated
  public boolean getEnabled() {
    return enabled;
  }

  @Override
  public boolean getDebug() {
    return debug;
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
  public boolean getEngineverify() { return engineverify; }

  @Deprecated
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @SuppressWarnings("unused")
  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  @SuppressWarnings("unused")
  public void setEngineurl(String engineURL) {
    this.engineurl = engineURL;
  }

  @SuppressWarnings("unused")
  public void setEngineCredentialsId(String engineCredentialsId) {
    this.engineCredentialsId = engineCredentialsId;
  }

  @SuppressWarnings("unused")
  public void setEngineverify(boolean engineTLSVerify) {
    this.engineverify = engineTLSVerify;
  }

  public SysdigBuilderDescriptor() {
    load();
  }

  @Override
  public boolean isApplicable(Class<? extends AbstractProject> aClass) {
    return true;
  }

  @Nonnull
  public abstract String getDisplayName();

  @Override
  public boolean configure(StaplerRequest req, JSONObject formData) {
    req.bindJSON(this, formData); // Use stapler request to bind
    save();
    return true;
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