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
import com.sysdig.jenkins.plugins.sysdig.application.RunContext;
import com.sysdig.jenkins.plugins.sysdig.application.vm.ImageScanningBuilder;
import com.sysdig.jenkins.plugins.sysdig.domain.SysdigLogger;
import hudson.AbortException;
import hudson.PluginWrapper;
import jenkins.model.Jenkins;

import java.io.Serializable;
import java.util.List;

public class NewEngineBuildConfig implements Serializable {

  private final String imageName;
  private final boolean bailOnFail;
  private final boolean bailOnPluginFail;
  private final String engineurl;
  private final boolean engineverify;
  private final String inlineScanExtraParams;
  private final String sysdigToken;
  private final String policiesToApply;
  private final String scannerBinaryPath;
  private final String cliVersionToApply;
  private final String customCliVersion;

  public NewEngineBuildConfig(RunContext runContext, ImageScanningBuilder engineBuilder) throws AbortException {
    var globalConfig = engineBuilder.getDescriptor();

    String credID = firstNonEmpty(engineBuilder.getEngineCredentialsId(), globalConfig.getEngineCredentialsId());
    this.sysdigToken = runContext.getSysdigTokenFromCredentials(credID);


    imageName = engineBuilder.getImageName();
    bailOnFail = engineBuilder.getBailOnFail();
    bailOnPluginFail = engineBuilder.getBailOnPluginFail();

    if (!Strings.isNullOrEmpty(engineBuilder.getEngineURL())) {
      this.engineurl = engineBuilder.getEngineURL();
      this.engineverify = engineBuilder.getEngineVerify();
    } else {
      this.engineurl = globalConfig.getEngineURL();
      this.engineverify = globalConfig.getEngineVerify();
    }

    this.inlineScanExtraParams = firstNonEmpty(engineBuilder.getInlineScanExtraParams(), globalConfig.getInlineScanExtraParams());
    this.policiesToApply = firstNonEmpty(engineBuilder.getPoliciesToApply(), globalConfig.getPoliciesToApply());

    if (!Strings.isNullOrEmpty(engineBuilder.getCliVersionToApply())) {
      this.cliVersionToApply = engineBuilder.getCliVersionToApply();
    } else {
      this.cliVersionToApply = globalConfig.getCliVersionToApply();
    }

    if (!Strings.isNullOrEmpty(engineBuilder.getCustomCliVersion())) {
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

  private String firstNonEmpty(String... strings) {
    for (String s : strings) {
      if (!Strings.isNullOrEmpty(s)) {
        return s;
      }
    }
    return "";
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
  public void printWith(SysdigLogger logger) {
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
//    logger.logInfo(String.format("Debug: %s", this.getDebug())); //FIXME(fede): check how to reenable debug
    logger.logInfo(String.format("EngineURL: %s", this.getEngineurl()));
    logger.logInfo(String.format("EngineVerify: %s", this.getEngineverify()));
    logger.logInfo(String.format("Policies: %s", this.getPoliciesToApply()));
    logger.logInfo(String.format("CliVersion: %s", this.getCliVersionToApply()));
    logger.logInfo(String.format("CustomCliVersion: %s", this.getCustomCliVersion()));
    logger.logInfo(String.format("InlineScanExtraParams: %s", this.getInlineScanExtraParams()));
    logger.logInfo(String.format("BailOnFail: %s", this.getBailOnFail()));
    logger.logInfo(String.format("BailOnPluginFail: %s", this.getBailOnPluginFail()));
  }

  public boolean getDebug() {
    return false;
  }
}
