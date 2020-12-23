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
import com.sysdig.jenkins.plugins.sysdig.config.BuilderConfig;
import com.sysdig.jenkins.plugins.sysdig.config.GlobalConfig;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.PluginWrapper;
import hudson.model.Run;
import jenkins.model.Jenkins;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Holder for all Sysdig Secure configuration - includes global and project level attributes. A convenience class for capturing a snapshot of
 * the config at the beginning of plugin execution and caching it for use during that specific execution
 */
public class BuildConfig implements Serializable {

  private final String imagesFile;
  private final boolean debug;
  private final boolean bailOnFail;
  private final boolean bailOnPluginFail;
  private final boolean inlineScanning;
  private final String engineURL;
  private final boolean engineTLSVerify;
  private final String credentialsID;
  private final String sysdigToken;

  public BuildConfig(GlobalConfig globalConfig, BuilderConfig builder, Run<?, ?> run) {
    imagesFile = builder.getImagesFile();
    debug = globalConfig.getDebug();
    bailOnFail = builder.getBailOnFail();
    bailOnPluginFail =  builder.getBailOnPluginFail();
    inlineScanning = builder.isInlineScanning();

    if (!Strings.isNullOrEmpty(builder.getEngineurl())) {
      engineURL = builder.getEngineurl();
      engineTLSVerify = builder.getEngineverify();
    } else {
      engineURL = globalConfig.getEngineurl();
      engineTLSVerify = globalConfig.getEngineverify();
    }

    if (!Strings.isNullOrEmpty(builder.getEngineCredentialsId())) {
      this.credentialsID = builder.getEngineCredentialsId();
    } else {
      this.credentialsID = globalConfig.getEngineCredentialsId();
    }

    if (!Strings.isNullOrEmpty(this.credentialsID)) {
      this.sysdigToken = getSysdigTokenFromCredentials(run, this.credentialsID);
    } else {
      this.sysdigToken = null;
    }
  }

  public String getImagesFile() {
    return imagesFile;
  }

  public boolean getDebug() {
    return debug;
  }

  public boolean getBailOnFail() {
    return bailOnFail;
  }

  public boolean getBailOnPluginFail() {
    return bailOnPluginFail;
  }

  public boolean getInlineScanning() {
    return inlineScanning;
  }

  public String getEngineURL() {
    return engineURL;
  }

  public boolean getEngineTLSVerify() {
    return engineTLSVerify;
  }

  public String getCredentialsID() {
    return credentialsID;
  }

  public String getSysdigToken() {
    return sysdigToken;
  }

  /**
   * Print versions info and configuration
   */
  public void print(SysdigLogger logger) {
    logger.logInfo("Jenkins version: " + Jenkins.VERSION);
    List<PluginWrapper> plugins;
    if (Jenkins.get().getPluginManager() != null && (plugins = Jenkins.get().getPluginManager().getPlugins()) != null) {
      for (PluginWrapper plugin : plugins) {
        if (plugin.getShortName().equals("sysdig-secure")) { // artifact ID of the plugin, TODO is there a better way to get this
          logger.logInfo(String.format("%s version: %s", plugin.getDisplayName(), plugin.getVersion()));
          break;
        }
      }
    }

    logger.logInfo(String.format("imagesFile: %s", this.getImagesFile()));
    logger.logInfo(String.format("debug: %s", this.getDebug()));
    logger.logInfo(String.format("bailOnFail: %s", this.getBailOnFail()));
    logger.logInfo(String.format("bailOnPluginFail: %s", this.getBailOnPluginFail()));
    logger.logInfo(String.format("inlineScanning: %s", this.getInlineScanning()));
    logger.logInfo(String.format("engineurl: %s", this.getEngineURL()));
    logger.logInfo(String.format("engineverify: %s", this.getEngineTLSVerify()));
    logger.logInfo(String.format("credentialsID: %s", this.getCredentialsID()));
  }

  public static String getSysdigTokenFromCredentials(Run<?, ?> run, String credentialsID) {

    StandardUsernamePasswordCredentials credentials = CredentialsProvider.findCredentialById(
      credentialsID,
      StandardUsernamePasswordCredentials.class,
      run,
      Collections.emptyList());

    if (null == credentials) {
      return null;
    }

    return credentials.getPassword().getPlainText();
  }

}
