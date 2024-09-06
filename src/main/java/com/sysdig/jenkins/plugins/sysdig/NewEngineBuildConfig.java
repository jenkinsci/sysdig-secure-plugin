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
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.PluginWrapper;
import jenkins.model.Jenkins;

import java.io.Serializable;
import java.util.List;

public class NewEngineBuildConfig implements Serializable {

  private final String imageName;
  private final boolean bailOnFail;
  private final boolean bailOnPluginFail;
  private final boolean debug;
  private final String engineurl;
  private final boolean engineverify;
  private final String inlineScanExtraParams;
  private final String sysdigToken;
  private final String policiesToApply;
  private final String scannerBinaryPath;
  private final String cliVersionToApply;
  private final String customCliVersion;

  public NewEngineBuildConfig(SysdigBuilder.DescriptorImpl globalConfig, NewEngineBuilder engineBuilder, String sysdigToken) {
    imageName = engineBuilder.getImageName();
    bailOnFail = engineBuilder.getBailOnFail();
    bailOnPluginFail = engineBuilder.getBailOnPluginFail();
    debug = globalConfig.getDebug();
    if (!Strings.isNullOrEmpty(engineBuilder.getEngineURL())) {
      engineurl = engineBuilder.getEngineURL();
      engineverify = engineBuilder.getEngineVerify();
    } else {
      engineurl = globalConfig.getEngineurl();
      engineverify = globalConfig.getEngineverify();
    }

    if (!Strings.isNullOrEmpty(engineBuilder.getInlineScanExtraParams())) {
      inlineScanExtraParams = engineBuilder.getInlineScanExtraParams();
    } else {
      inlineScanExtraParams = globalConfig.getInlineScanExtraParams();
    }

    this.sysdigToken = sysdigToken;

    this.policiesToApply = engineBuilder.getPoliciesToApply();

    if (!Strings.isNullOrEmpty(engineBuilder.getCliVersionToApply())
        && !engineBuilder.getCliVersionToApply().equals("global_default")) {
      this.cliVersionToApply = engineBuilder.getCliVersionToApply();
    } else {
      this.cliVersionToApply = globalConfig.getCliVersionToApply();
    }

    if (!Strings.isNullOrEmpty(engineBuilder.getCustomCliVersion())
        && !engineBuilder.getCliVersionToApply().equals("global_default")) {
      this.customCliVersion = engineBuilder.getCustomCliVersion();
    } else {
      this.customCliVersion = globalConfig.getCustomCliVersion();
    }

    if (!Strings.isNullOrEmpty(engineBuilder.getScannerBinaryPath())) {
      this.scannerBinaryPath = engineBuilder.getScannerBinaryPath();
    } else {
      this.scannerBinaryPath = globalConfig.getScannerBinaryPath();
    }

  }

  public String getImageName() {
    return imageName;
  }

  public boolean getBailOnFail() {
    return bailOnFail;
  }

  public boolean getBailOnPluginFail() {
    return bailOnPluginFail;
  }

  public boolean getDebug() {
    return debug;
  }

  public String getEngineurl() {
    return engineurl;
  }

  public String getSysdigToken() {
    return sysdigToken;
  }

  public boolean getEngineverify() {
    return engineverify;
  }

  public String getInlineScanExtraParams() {
    return inlineScanExtraParams;
  }

  public String getPoliciesToApply() {
    return policiesToApply;
  }

  public String getCliVersionToApply() {
    return cliVersionToApply;
  }

  public String getCustomCliVersion() {
    return customCliVersion;
  }

  public String getScannerBinaryPath() {
    return scannerBinaryPath;
  }

  /**
   * Print versions info and configuration
   */
  public void print(SysdigLogger logger) {
    logger.logInfo("Jenkins version: " + Jenkins.VERSION);
    List<PluginWrapper> plugins;
    if (Jenkins.get().getPluginManager() != null && (plugins = Jenkins.get().getPluginManager().getPlugins()) != null) {
      for (PluginWrapper plugin : plugins) {
        if (plugin.getShortName().equals("sysdig-secure")) { // artifact ID of the plugin,
          logger.logInfo(String.format("%s version: %s", plugin.getDisplayName(), plugin.getVersion()));
          break;
        }
      }
    }

    logger.logInfo("Using new-scanning engine");
    logger.logInfo(String.format("Image Name: %s", this.getImageName()));
    logger.logInfo(String.format("Debug: %s", this.getDebug()));
    logger.logInfo(String.format("EngineURL: %s", this.getEngineurl()));
    logger.logInfo(String.format("EngineVerify: %s", this.getEngineverify()));
    logger.logInfo(String.format("Policies: %s", this.getPoliciesToApply()));
    logger.logInfo(String.format("CliVersion: %s", this.getCliVersionToApply()));
    logger.logInfo(String.format("CustomCliVersion: %s", this.getCustomCliVersion()));
    logger.logInfo(String.format("InlineScanExtraParams: %s", this.getInlineScanExtraParams()));
    logger.logInfo(String.format("BailOnFail: %s", this.getBailOnFail()));
    logger.logInfo(String.format("BailOnPluginFail: %s", this.getBailOnPluginFail()));
  }
}
