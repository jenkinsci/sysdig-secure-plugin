package com.sysdig.jenkins.plugins.sysdig;

import org.kohsuke.stapler.DataBoundSetter;

public interface SysdigScanStep {

  // Getters are used by config.jelly
  public String getName();

  public boolean getBailOnFail();

  public boolean getBailOnPluginFail();

  public String getEngineurl();

  public String getEngineCredentialsId();

  public boolean getEngineverify();

  public String getRunAsUser();

  public String getInlineScanExtraParams();

  public boolean isInlineScanning();

  public boolean getForceScan();

  @DataBoundSetter
  public void setBailOnFail(boolean bailOnFail);

  @DataBoundSetter
  public void setBailOnPluginFail(boolean bailOnPluginFail);

  @DataBoundSetter
  public void setEngineurl(String engineurl);

  @DataBoundSetter
  public void setEngineCredentialsId(String engineCredentialsId);

  @DataBoundSetter
  public void setEngineverify(boolean engineverify);

  @DataBoundSetter
  public void setRunAsUser(String runAsUser);

  @DataBoundSetter
  public void setInlineScanExtraParams(String inlineScanExtraParams);

  @DataBoundSetter
  public void setInlineScanning(boolean inlineScanning);

  @DataBoundSetter
  public void setForceScan(boolean forceScan);

}
