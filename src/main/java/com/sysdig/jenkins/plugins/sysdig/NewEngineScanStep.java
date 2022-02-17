package com.sysdig.jenkins.plugins.sysdig;

import org.kohsuke.stapler.DataBoundSetter;

public interface NewEngineScanStep {

  // Getters are used by config.jelly
  public String getImageName();

  public boolean getBailOnFail();

  public boolean getBailOnPluginFail();

  public String getPoliciesToApply();


  public String getEngineURL();

  public String getEngineCredentialsId();

  public boolean getEngineVerify();

  public String getInlineScanExtraParams();

  @DataBoundSetter
  public void setBailOnFail(boolean bailOnFail);

  @DataBoundSetter
  public void setBailOnPluginFail(boolean bailOnPluginFail);

  @DataBoundSetter
  public void setPoliciesToApply(String policiesToApply);

  @DataBoundSetter
  public void setEngineURL(String engineurl);

  @DataBoundSetter
  public void setEngineCredentialsId(String engineCredentialsId);

  @DataBoundSetter
  public void setEngineVerify(boolean engineverify);

  @DataBoundSetter
  public void setInlineScanExtraParams(String inlineScanExtraParams);
}
