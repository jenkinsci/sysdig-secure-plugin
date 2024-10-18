package com.sysdig.jenkins.plugins.sysdig.log;

public class NopLogger implements SysdigLogger {
  @Override
  public void logDebug(String msg) {

  }

  @Override
  public void logDebug(String msg, Throwable t) {

  }

  @Override
  public void logInfo(String msg) {

  }

  @Override
  public void logInfo(String msg, Throwable t) {

  }

  @Override
  public void logWarn(String msg) {

  }

  @Override
  public void logWarn(String msg, Throwable t) {

  }

  @Override
  public void logError(String msg) {

  }

  @Override
  public void logError(String msg, Throwable t) {

  }

  @Override
  public boolean isDebugEnabled() {
    return false;
  }
}