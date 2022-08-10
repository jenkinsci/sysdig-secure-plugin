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

/**
 * Holder for all Sysdig Secure configuration - includes global and project level attributes. A convenience class for capturing a snapshot of
 * the config at the beginning of plugin execution and caching it for use during that specific execution
 */
public class BuildConfig implements Serializable {

  private static final String DEFAULT_INLINE_SCAN_IMAGE = "quay.io/sysdig/secure-inline-scan:2";

  private final String imageListName;
  private final boolean bailOnFail;
  private final boolean bailOnPluginFail;
  private final boolean debug;
  private final String engineurl;
  private final boolean engineverify;
  private final String runAsUser;
  private final String inlineScanExtraParams;
  private final boolean inlineScanning;
  private final String sysdigToken;
  private final boolean forceScan;
  private final String inlineScanImage;

  public BuildConfig(SysdigBuilder.DescriptorImpl globalConfig, SysdigScanStep scanStep, String sysdigToken) {
    imageListName = scanStep.getName();
    bailOnFail = scanStep.getBailOnFail();
    bailOnPluginFail =  scanStep.getBailOnPluginFail();
    debug = globalConfig.getDebug();
    if (!Strings.isNullOrEmpty(scanStep.getEngineurl())) {
      engineurl = scanStep.getEngineurl();
      engineverify = scanStep.getEngineverify();
    } else {
      engineurl = globalConfig.getEngineurl();
      engineverify = globalConfig.getEngineverify();
    }

    if (!Strings.isNullOrEmpty(scanStep.getRunAsUser())) {
      runAsUser = scanStep.getRunAsUser();
    } else {
      runAsUser = globalConfig.getRunAsUser();
    }

    if (!Strings.isNullOrEmpty(scanStep.getInlineScanExtraParams())) {
      inlineScanExtraParams = scanStep.getInlineScanExtraParams();
    } else {
      inlineScanExtraParams = globalConfig.getInlineScanExtraParams();
    }

    if (globalConfig.getForceinlinescan()) {
      inlineScanning = true;
    } else {
      inlineScanning = scanStep.isInlineScanning();
    }

    forceScan = scanStep.getForceScan();
    this.sysdigToken = sysdigToken;
    if (!Strings.isNullOrEmpty(globalConfig.getInlinescanimage())) {
      inlineScanImage = globalConfig.getInlinescanimage();
    } else {
      inlineScanImage = DEFAULT_INLINE_SCAN_IMAGE;
    }
  }

  public String getImageListName() {
    return imageListName;
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

  public String getRunAsUser() {
    return runAsUser;
  }

  public String getInlineScanExtraParams() {
    return inlineScanExtraParams;
  }

  public boolean getInlineScanning() {
    return inlineScanning;
  }

  public boolean getForceScan() {
    return forceScan;
  }

  public String getInlineScanImage() {
    return inlineScanImage;
  }

  /**
   * Print versions info and configuration
   */
  public void print(SysdigLogger logger) {
    logger.logInfo("Jenkins version: " + Jenkins.VERSION);
    List<PluginWrapper> plugins;
    if (Jenkins.getInstance().getPluginManager() != null && (plugins = Jenkins.getInstance().getPluginManager().getPlugins()) != null) {
      for (PluginWrapper plugin : plugins) {
        if (plugin.getShortName().equals("sysdig-secure")) { // artifact ID of the plugin, TODO is there a better way to get this
          logger.logInfo(String.format("%s version: %s", plugin.getDisplayName(), plugin.getVersion()));
          break;
        }
      }
    }

    logger.logInfo(String.format("debug: %s", this.getDebug()));
    logger.logInfo(String.format("inlineScanning: %s", this.getInlineScanning()));
    logger.logInfo(String.format("engineurl: %s", this.getEngineurl()));
    logger.logInfo(String.format("engineverify: %s", this.getEngineverify()));
    logger.logInfo(String.format("runAsUser: %s", this.getRunAsUser()));
    logger.logInfo(String.format("inlineScanExtraParams: %s", this.getInlineScanExtraParams()));
    logger.logInfo(String.format("image list file: %s", this.getImageListName()));
    logger.logInfo(String.format("bailOnFail: %s", this.getBailOnFail()));
    logger.logInfo(String.format("bailOnPluginFail: %s", this.getBailOnPluginFail()));
    logger.logInfo(String.format("forceScan: %b", this.getForceScan()));
  }

}
