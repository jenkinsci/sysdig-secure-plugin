package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner;

import com.google.common.base.Strings;
import com.sysdig.jenkins.plugins.sysdig.domain.SysdigLogger;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SysdigIaCScanningProcessBuilder implements Cloneable {

  private final String sysdigCLIPath;
  private String engineURL;
  private String scanResultOutputPath;
  private boolean consoleLogEnabled;
  private LogLevel logLevel;
  private boolean verifyTLS;
  private List<String> extraParams;
  private List<String> policiesToApply;
  private FilePath workingDirectory;
  private EnvVars extraEnvVars;
  private OutputStream redirectedStdoutStream;
  private OutputStream redirectedStdErrStream;
  private boolean isRecursive;
  private boolean listUnsupportedResources;
  private Severity severity;
  private List<String> pathsToScan;


  public SysdigIaCScanningProcessBuilder(String sysdigCLIPath, String sysdigAPIToken) {
    this.sysdigCLIPath = sysdigCLIPath;
    this.engineURL = "https://secure.sysdig.com";
    this.scanResultOutputPath = "";
    this.consoleLogEnabled = false;
    this.logLevel = LogLevel.INFO;
    this.verifyTLS = true;
    this.extraParams = new ArrayList<>();
    this.policiesToApply = new ArrayList<>();
    this.workingDirectory = null;
    this.extraEnvVars = new EnvVars();
    this.extraEnvVars.put("SECURE_API_TOKEN", sysdigAPIToken);
    this.isRecursive = false;
    this.listUnsupportedResources = false;
    this.severity = Severity.HIGH;
    this.pathsToScan = new ArrayList<>();
  }

  public SysdigIaCScanningProcessBuilder withEngineURL(String engineurl) {
    var clone = this.clone();
    clone.engineURL = engineurl;
    return clone;
  }

  public SysdigIaCScanningProcessBuilder withScanResultOutputPath(String scanResultOutputPath) {
    var clone = this.clone();
    clone.scanResultOutputPath = scanResultOutputPath;
    return clone;
  }

  public SysdigIaCScanningProcessBuilder withConsoleLog() {
    var clone = this.clone();
    clone.consoleLogEnabled = true;
    return clone;
  }

  public SysdigIaCScanningProcessBuilder withLogLevel(LogLevel logLevel) {
    var clone = this.clone();
    clone.logLevel = logLevel;
    return clone;
  }

  public SysdigIaCScanningProcessBuilder withTLSVerification(boolean verifyTLS) {
    var clone = this.clone();
    clone.verifyTLS = verifyTLS;
    return clone;
  }

  public SysdigIaCScanningProcessBuilder withExtraParametersSeparatedBySpace(String inlineScanExtraParams) {
    var clone = this.clone();
    Arrays.stream(inlineScanExtraParams.split(" "))
      .map(String::trim)
      .filter(param -> !param.isEmpty())
      .forEach(clone.extraParams::add);
    return clone;
  }

  public SysdigIaCScanningProcessBuilder withPoliciesToApplySeparatedBySpace(String policiesToApply) {
    var clone = this.clone();
    Arrays.stream(policiesToApply.split(" "))
      .map(String::trim)
      .filter(policy -> !policy.isEmpty())
      .forEach(clone.policiesToApply::add);
    return clone;
  }

  public SysdigIaCScanningProcessBuilder withWorkingDirectory(FilePath workingDirectory) {
    var clone = this.clone();
    clone.workingDirectory = workingDirectory;
    return clone;
  }

  public SysdigIaCScanningProcessBuilder withExtraEnvVars(EnvVars envVars) {
    var clone = this.clone();
    clone.extraEnvVars.putAll(envVars);
    return clone;
  }

  public SysdigIaCScanningProcessBuilder withStdoutRedirectedTo(OutputStream outputStream) {
    var clone = this.clone();
    clone.redirectedStdoutStream = outputStream;
    return clone;
  }

  public SysdigIaCScanningProcessBuilder withStderrRedirectedTo(OutputStream outputStream) {
    var clone = this.clone();
    clone.redirectedStdErrStream = outputStream;
    return clone;
  }

  public SysdigIaCScanningProcessBuilder withStdoutRedirectedTo(SysdigLogger sysdigLogger) {
    return withStdoutRedirectedTo(new LogOutputStreamAdapter(sysdigLogger));
  }

  public SysdigIaCScanningProcessBuilder withStderrRedirectedTo(SysdigLogger sysdigLogger) {
    return withStderrRedirectedTo(new LogOutputStreamAdapter(sysdigLogger));
  }

  public int launchAndWait(Launcher launcher) throws IOException, InterruptedException {
    Launcher.ProcStarter procStarter = launcher
      .launch()
      .cmds(this.toCommandLineArguments())
      .envs(this.extraEnvVars);

    if (workingDirectory != null) procStarter.pwd(workingDirectory);
    if (redirectedStdoutStream != null) procStarter.stdout(redirectedStdoutStream);
    if (redirectedStdErrStream != null) procStarter.stderr(redirectedStdErrStream);

    return procStarter.join();
  }

  public List<String> toCommandLineArguments() {
    var arguments = new ArrayList<String>();
    arguments.add(this.sysdigCLIPath);
    arguments.add("--iac");

    if (!Strings.isNullOrEmpty(engineURL)) arguments.add("--apiurl=" + engineURL);
    if (consoleLogEnabled) arguments.add("--console-log");
    arguments.add("--loglevel=" + logLevel.toString());
    if (!Strings.isNullOrEmpty(scanResultOutputPath)) arguments.add("--output-json=" + scanResultOutputPath);
    if (!verifyTLS) arguments.add("--skiptlsverify");

    if (listUnsupportedResources) arguments.add("--list-unsupported-resources");
    policiesToApply.stream().map(policy -> "--policy=" + policy).forEach(arguments::add);
    if (isRecursive) arguments.add("--recursive");
    arguments.add("--severity-threshold=" + severity.toString());

    arguments.addAll(extraParams);
    arguments.addAll(!pathsToScan.isEmpty() ? pathsToScan : List.of("."));

    return arguments;
  }

  @Override
  public SysdigIaCScanningProcessBuilder clone() {
    try {
      SysdigIaCScanningProcessBuilder cloned = (SysdigIaCScanningProcessBuilder) super.clone();
      // Deep copy mutable fields
      cloned.extraParams = new ArrayList<>(this.extraParams);
      cloned.policiesToApply = new ArrayList<>(this.policiesToApply);
      cloned.extraEnvVars = new EnvVars(this.extraEnvVars);
      cloned.pathsToScan = new ArrayList<>(this.pathsToScan);
      // Other fields are either immutable or primitives, so they can be copied as is
      return cloned;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError("Clone not supported", e);
    }
  }

  public SysdigIaCScanningProcessBuilder withRecursive(boolean isRecursive) {
    var clone = this.clone();
    clone.isRecursive = isRecursive;
    return clone;
  }

  public SysdigIaCScanningProcessBuilder withUnsupportedResources(boolean listUnsupportedResources) {
    var clone = this.clone();
    clone.listUnsupportedResources = listUnsupportedResources;
    return clone;
  }

  public SysdigIaCScanningProcessBuilder withSeverity(@Nonnull Severity severity) {
    var clone = this.clone();
    clone.severity = severity;
    return clone;
  }

  public SysdigIaCScanningProcessBuilder withPathsToScan(String ...pathListToScan) {
    var clone = this.clone();
    clone.pathsToScan.addAll(List.of(pathListToScan));
    return clone;
  }


  public enum Severity {
    HIGH,
    MEDIUM,
    LOW,
    NEVER;

    public static Severity fromString(@Nonnull String severity) {
      switch (severity.toLowerCase().trim()) {
        case "high":
        case "h":
          return HIGH;
        case "medium":
        case "m":
          return MEDIUM;
        case "low":
        case "l":
          return LOW;
        case "never":
        case "n":
          return NEVER;
      }
      throw new InvalidParameterException("unsupported severity: " + severity);
    }

    @Override
    public String toString() {
      switch (this) {
        case HIGH:
          return "high";
        case MEDIUM:
          return "medium";
        case LOW:
          return "low";
        case NEVER:
          return "never";
      }
      throw new RuntimeException("non-exhaustive switch in Severity");
    }
  }


  public enum LogLevel {
    DEBUG,
    INFO;

    @Override
    public String toString() {
      switch (this) {
        case INFO:
          return "info";
        case DEBUG:
          return "debug";
      }

      throw new RuntimeException("LogLevel not covered exhaustively");
    }
  }
}
