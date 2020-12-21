package com.sysdig.jenkins.plugins.sysdig.log;

import java.io.PrintStream;

public interface SysdigLogger {
  void logDebug(String msg);

  void logDebug(String msg, Throwable t);

  void logInfo(String msg);

  void logInfo(String msg, Throwable t);

  void logWarn(String msg);

  void logWarn(String msg, Throwable t);

  void logError(String msg);

  void logError(String msg, Throwable t);
}
