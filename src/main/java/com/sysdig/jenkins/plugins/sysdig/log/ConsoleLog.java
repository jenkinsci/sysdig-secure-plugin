package com.sysdig.jenkins.plugins.sysdig.log;

import java.io.PrintStream;
import java.util.Date;

/**
 * Logging mechanism for outputting messages to Jenkins build console
 */
public class ConsoleLog implements SysdigLogger {

  private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(ConsoleLog.class.getName());
  private static final String LOG_FORMAT = "%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tS.%1$tL %2$-6s %3$-15s %4$s";

  private final String name;
  private final PrintStream logger;
  private final boolean enableDebug;

  @Override
  public PrintStream getLogger() {
    return logger;
  }

  public ConsoleLog(String name, PrintStream logger, boolean enableDebug) {
    this.name = name;
    this.logger = logger;
    this.enableDebug = enableDebug;
  }

  @Override
  public void logDebug(String msg) {
    if (enableDebug) {
      logger.println(String.format(LOG_FORMAT, new Date(), "DEBUG", name, msg));
    }
  }

  @Override
  public void logDebug(String msg, Throwable t) {
    logDebug(msg);
    if (null != t) {
      t.printStackTrace(logger);
    }
  }

  @Override
  public void logInfo(String msg) {
    logger.println(String.format(LOG_FORMAT, new Date(), "INFO", name, msg));
  }

  @Override
  public void logInfo(String msg, Throwable t) {
    logInfo(msg);
    if (null != t) {
      t.printStackTrace(logger);
    }
  }

  @Override
  public void logWarn(String msg) {
    logger.println(String.format(LOG_FORMAT, new Date(), "WARN", name, msg));
  }

  @Override
  public void logWarn(String msg, Throwable t) {
    logWarn(msg);
    if (null != t) {
      t.printStackTrace(logger);
    }
  }

  @Override
  public void logError(String msg) {
    logger.println(String.format(LOG_FORMAT, new Date(), "ERROR", name, msg));
  }

  @Override
  public void logError(String msg, Throwable t) {
    logError(msg);
    if (null != t) {
      t.printStackTrace(logger);
    }
  }
}
