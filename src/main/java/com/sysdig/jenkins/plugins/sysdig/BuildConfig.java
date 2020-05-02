package com.sysdig.jenkins.plugins.sysdig;


import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;

/**
 * Holder for all Sysdig Secure configuration - includes global and project level attributes. A convenience class for capturing a snapshot of
 * the config at the beginning of plugin execution and caching it for use during that specific execution
 */
public class BuildConfig {

  // Build configuration
  private final String name;
  private final String engineRetries;
  private final boolean bailOnFail;
  private final boolean bailOnPluginFail;

  // Global configuration
  private final boolean debug;
  private final boolean inlineScanning;
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
