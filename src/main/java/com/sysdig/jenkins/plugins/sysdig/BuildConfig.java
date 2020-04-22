package com.sysdig.jenkins.plugins.sysdig;


import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;

/**
 * Holder for all Sysdig Secure configuration - includes global and project level attributes. A convenience class for capturing a snapshot of
 * the config at the beginning of plugin execution and caching it for use during that specific execution
 */
public class BuildConfig {

  // Build configuration
  private String name;
  private String engineRetries;
  private boolean bailOnFail;
  private boolean bailOnPluginFail;

  // Global configuration
  private boolean debug;
  private boolean inlineScanning;
  private String engineurl;
  private String engineuser;
  private String enginepass;
  private boolean engineverify;
  private String containerImageId;
  private String containerId;
  private String localVol;
  private String modulesVol;

  public BuildConfig(String name, String engineRetries, boolean bailOnFail, boolean bailOnPluginFail,
                     boolean debug, boolean inlineScanning, String engineurl, String engineuser, String enginepass,
                     boolean engineverify, String containerImageId, String containerId, String localVol, String modulesVol) {
    this.name = name;
    this.engineRetries = engineRetries;
    this.bailOnFail = bailOnFail;
    this.bailOnPluginFail = bailOnPluginFail;
    this.debug = debug;
    this.inlineScanning = inlineScanning;
    this.engineurl = engineurl;
    this.engineuser = engineuser;
    this.enginepass = enginepass;
    this.engineverify = engineverify;
    this.containerImageId = containerImageId;
    this.containerId = containerId;
    this.localVol = localVol;
    this.modulesVol = modulesVol;
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

  public boolean isInlineScanning() {
    return inlineScanning;
  }

  public String getEngineurl() {
    return engineurl;
  }

  public String getEngineuser() {
    return engineuser;
  }

  public String getEnginepass() {
    return enginepass;
  }

  public boolean getEngineverify() {
    return engineverify;
  }

  public String getContainerImageId() {
    return containerImageId;
  }

  public String getContainerId() {
    return containerId;
  }

  public String getLocalVol() {
    return localVol;
  }

  public String getModulesVol() {
    return modulesVol;
  }

  public void print(SysdigLogger consoleLog) {
    consoleLog.logInfo(String.format("[global] debug: %s", String.valueOf(debug)));
    consoleLog.logInfo(String.format("[global] inlineScanning: %s", inlineScanning));

    if (!inlineScanning) {
      // Global or build properties
      consoleLog.logInfo(String.format("[build] engineurl: %s", engineurl));
      consoleLog.logInfo("[build] engineverify: " + String.valueOf(engineverify));

      // Build properties
      consoleLog.logInfo(String.format("[build] name: %s", name));
      consoleLog.logInfo(String.format("[build] engineRetries: %s", engineRetries));
    } else {
      // Global properties
      consoleLog.logInfo(String.format("[global] containerImageId: %s", containerImageId));
      consoleLog.logInfo(String.format("[global] containerId: %s", containerId));
      consoleLog.logInfo(String.format("[global] localVol: %s", localVol));
      consoleLog.logInfo(String.format("[global] modulesVol: %s", modulesVol));

      // Build properties
      consoleLog.logInfo(String.format("[build] name: %s", name));
    }
    consoleLog.logInfo(String.format("[build] bailOnFail: %s", bailOnFail));
    consoleLog.logInfo(String.format("[build] bailOnPluginFail: %s", bailOnPluginFail));
  }
}
