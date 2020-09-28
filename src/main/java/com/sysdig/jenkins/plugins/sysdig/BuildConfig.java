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


import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;

import java.io.Serializable;

/**
 * Holder for all Sysdig Secure configuration - includes global and project level attributes. A convenience class for capturing a snapshot of
 * the config at the beginning of plugin execution and caching it for use during that specific execution
 */
public class BuildConfig implements Serializable {

  // Build configuration
  private final String name;
  private final String engineRetries;
  private final boolean bailOnFail;
  private final boolean bailOnPluginFail;

  // Global configuration
  private final boolean debug;
  private final boolean inlineScanning;
  private final String containerRunMethod;
  private final String engineurl;
  private final String sysdigToken;
  private final boolean engineverify;

  public BuildConfig(String name, String engineRetries, boolean bailOnFail, boolean bailOnPluginFail,
                     boolean debug, boolean inlineScanning, String engineurl, String sysdigToken,
                     boolean engineverify) {
    this.name = name;
    this.engineRetries = engineRetries;
    this.bailOnFail = bailOnFail;
    this.bailOnPluginFail = bailOnPluginFail;
    this.debug = debug;
    this.inlineScanning = inlineScanning;
    this.containerRunMethod = "TO-DO";
    this.engineurl = engineurl;
    this.sysdigToken = sysdigToken;
    this.engineverify = engineverify;
  }

  public String getName() {
    return name;
  }

  public String getEngineRetries() {
    return engineRetries;
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

  public void print(SysdigLogger consoleLog) {
    consoleLog.logInfo(String.format("[global] debug: %s", debug));
    consoleLog.logInfo(String.format("[global] inlineScanning: %s", inlineScanning));
    consoleLog.logInfo(String.format("[build] engineurl: %s", engineurl));
    consoleLog.logInfo(String.format("[build] engineverify: %s", engineverify));
    consoleLog.logInfo(String.format("[build] name: %s", name));
    consoleLog.logInfo(String.format("[build] bailOnFail: %s", bailOnFail));
    consoleLog.logInfo(String.format("[build] engineRetries: %s", engineRetries));
    consoleLog.logInfo(String.format("[build] bailOnPluginFail: %s", bailOnPluginFail));
  }
}
