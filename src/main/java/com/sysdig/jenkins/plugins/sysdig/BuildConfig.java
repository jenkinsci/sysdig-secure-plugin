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

  private final SysdigBuilder builder;
  private final SysdigBuilder.DescriptorImpl globalConfig;
  private final String sysdigToken;


  public BuildConfig(SysdigBuilder.DescriptorImpl globalConfig, SysdigBuilder builder, String sysdigToken) {
    this.builder = builder;
    this.globalConfig = globalConfig;
    this.sysdigToken = sysdigToken;
  }

  public String getName() {
    return builder.getName();
  }

  public boolean getBailOnFail() {
    return builder.getBailOnFail();
  }

  public boolean getBailOnPluginFail() {
    return builder.getBailOnPluginFail();
  }

  public boolean getDebug() {
    return globalConfig.getDebug();
  }

  public String getEngineurl() {
    if (!Strings.isNullOrEmpty(builder.getEngineurl())) {
      return builder.getEngineurl();
    }
    return globalConfig.getEngineurl();
  }

  public String getSysdigToken() {
    return sysdigToken;
  }

  public boolean getEngineverify() {
    return builder.getEngineverify();
  }

  public boolean isInlineScanning() {
    return builder.isInlineScanning() || globalConfig.getInlineScanning();
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

    logger.logInfo(String.format("[global] debug: %s", globalConfig.getDebug()));
    logger.logInfo(String.format("[global] inlineScanning: %s", globalConfig.getInlineScanning()));
    logger.logInfo(String.format("[build] inlineScanning: %s", builder.isInlineScanning()));
    logger.logInfo(String.format("[build] engineurl: %s", this.getEngineurl()));
    logger.logInfo(String.format("[build] engineverify: %s", this.getEngineverify()));
    logger.logInfo(String.format("[build] name: %s", this.getName()));
    logger.logInfo(String.format("[build] bailOnFail: %s", this.getBailOnFail()));
    logger.logInfo(String.format("[build] bailOnPluginFail: %s", this.getBailOnPluginFail()));
  }

}
