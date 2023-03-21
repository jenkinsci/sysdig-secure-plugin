package com.sysdig.jenkins.plugins.sysdig;

import org.kohsuke.stapler.DataBoundSetter;

public interface NewEngineScanStep {

  // Getters are used by config.jelly
  String getImageName();

  boolean getBailOnFail();

  boolean getBailOnPluginFail();

  String getPoliciesToApply();

  String getCliVersionToApply();

  String getCustomCliVersion();

  String getEngineURL();

  String getEngineCredentialsId();

  boolean getEngineVerify();

  String getInlineScanExtraParams();

  String getScannerBinaryPath();

  @DataBoundSetter
  void setBailOnFail(boolean bailOnFail);

  @DataBoundSetter
  void setBailOnPluginFail(boolean bailOnPluginFail);

  @DataBoundSetter
  void setPoliciesToApply(String policiesToApply);

  @DataBoundSetter
  void setCliVersionToApply(String cliVersionToApply);

  @DataBoundSetter
  void setCustomCliVersion(String customCliVersion);

  @DataBoundSetter
  void setEngineURL(String engineurl);

  @DataBoundSetter
  void setEngineCredentialsId(String engineCredentialsId);

  @DataBoundSetter
  void setEngineVerify(boolean engineverify);

  @DataBoundSetter
  void setInlineScanExtraParams(String inlineScanExtraParams);

  @DataBoundSetter
  void setScannerBinaryPath(String scannerBinaryPath);

}
