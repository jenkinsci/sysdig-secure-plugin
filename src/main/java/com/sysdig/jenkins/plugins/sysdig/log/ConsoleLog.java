package com.sysdig.jenkins.plugins.sysdig.log;

import hudson.model.TaskListener;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Date;

/**
 * Logging mechanism for outputting messages to Jenkins build console
 */
public class ConsoleLog implements SysdigLogger, Serializable {

  private static final long serialVersionUID = 1;

  private static final String LOG_FORMAT = "%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tS.%1$tL %2$-6s %3$-15s %4$s%n";

  private final String name;
  private final boolean enableDebug;
  private final TaskListener listener;
  // Not serializable, so make it transient and create a new one if required
  private transient PrintStream _logger;

  // Wrap access to non-serializable PrintStream logger
  private PrintStream getLogger() {
    if (_logger == null) {
      _logger = listener.getLogger();
    }
    return _logger;
  }

  public ConsoleLog(String name, TaskListener listener, boolean enableDebug) {
    this.name = name;
    this.listener = listener;
    this.enableDebug = enableDebug;
  }

  @Override
  public void logDebug(String msg) {
    if (enableDebug) {
      getLogger().printf(LOG_FORMAT, new Date(), "DEBUG", name, msg);
    }
  }

  @Override
  public void logDebug(String msg, Throwable t) {
    logDebug(msg);
    if (null != t) {
      t.printStackTrace(getLogger());
    }
  }

  @Override
  public void logInfo(String msg) {
    getLogger().printf(LOG_FORMAT, new Date(), "INFO", name, msg);
  }

  @Override
  public void logInfo(String msg, Throwable t) {
    logInfo(msg);
    if (null != t) {
      t.printStackTrace(getLogger());
    }
  }

  @Override
  public void logWarn(String msg) {
    getLogger().printf(LOG_FORMAT, new Date(), "WARN", name, msg);
  }

  @Override
  public void logWarn(String msg, Throwable t) {
    logWarn(msg);
    if (null != t) {
      t.printStackTrace(getLogger());
    }
  }

  @Override
  public void logError(String msg) {
    getLogger().printf(LOG_FORMAT, new Date(), "ERROR", name, msg);
  }

  @Override
  public void logError(String msg, Throwable t) {
    logError(msg);
    if (null != t) {
      t.printStackTrace(getLogger());
    }
  }
}
