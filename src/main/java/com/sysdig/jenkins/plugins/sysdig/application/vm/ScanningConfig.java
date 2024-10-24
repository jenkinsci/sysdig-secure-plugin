package com.sysdig.jenkins.plugins.sysdig.application.vm;

import com.sysdig.jenkins.plugins.sysdig.domain.SysdigLogger;

public interface ScanningConfig {
  String getImageName();

  boolean getBailOnFail();

  boolean getBailOnPluginFail();

  String getEngineurl();

  String getSysdigToken();

  boolean getEngineverify();

  String getInlineScanExtraParams();

  String getPoliciesToApply();

  String getCliVersionToApply();

  String getCustomCliVersion();

  String getScannerBinaryPath();

  void printWith(SysdigLogger logger);

  boolean getDebug();
}
