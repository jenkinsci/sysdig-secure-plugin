package com.sysdig.jenkins.plugins.sysdig;

import org.kohsuke.stapler.DataBoundSetter;

public interface SysdigScanStep {

  // Getters are used by config.jelly
  String getName();

  boolean getBailOnFail();

  boolean getBailOnPluginFail();

  String getEngineurl();

  String getEngineCredentialsId();

  boolean getEngineverify();

  String getRunAsUser();

  String getInlineScanExtraParams();

  boolean isInlineScanning();

  boolean getForceScan();

  @DataBoundSetter
  void setBailOnFail(boolean bailOnFail);

  @DataBoundSetter
  void setBailOnPluginFail(boolean bailOnPluginFail);

  @DataBoundSetter
  void setEngineurl(String engineurl);

  @DataBoundSetter
  void setEngineCredentialsId(String engineCredentialsId);

  @DataBoundSetter
  void setEngineverify(boolean engineverify);

  @DataBoundSetter
  void setRunAsUser(String runAsUser);

  @DataBoundSetter
  void setInlineScanExtraParams(String inlineScanExtraParams);

  @DataBoundSetter
  void setInlineScanning(boolean inlineScanning);

  @DataBoundSetter
  void setForceScan(boolean forceScan);

}
