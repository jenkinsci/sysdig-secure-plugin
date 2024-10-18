package com.sysdig.jenkins.plugins.sysdig.log;

import java.io.Serializable;

public interface SysdigLogger extends Serializable {
  void logDebug(String msg);

  void logDebug(String msg, Throwable t);

  void logInfo(String msg);

  void logInfo(String msg, Throwable t);

  void logWarn(String msg);

  void logWarn(String msg, Throwable t);

  void logError(String msg);

  void logError(String msg, Throwable t);

  boolean isDebugEnabled();
}
