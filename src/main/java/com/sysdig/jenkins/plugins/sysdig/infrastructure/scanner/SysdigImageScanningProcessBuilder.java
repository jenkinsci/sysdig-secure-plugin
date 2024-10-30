package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner;

import com.google.common.base.Strings;
import com.sysdig.jenkins.plugins.sysdig.domain.SysdigLogger;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SysdigImageScanningProcessBuilder implements Cloneable {

  private final String sysdigCLIPath;
  private String engineURL;
  private String dbPath;
  private String cachePath;
  private String scanResultOutputPath;
  private boolean consoleLogEnabled;
  private LogLevel logLevel;
  private boolean verifyTLS;
  private List<String> extraParams;
  private List<String> policiesToApply;
  private String imageToScan;
  private FilePath workingDirectory;
  private EnvVars extraEnvVars;
  private OutputStream redirectedStdoutStream;
  private OutputStream redirectedStdErrStream;
  private boolean separateByLayer;


  SysdigImageScanningProcessBuilder(String sysdigCLIPath, String sysdigAPIToken) {
    this.sysdigCLIPath = sysdigCLIPath;
    this.engineURL = "";
    this.dbPath = "";
    this.cachePath = "";
    this.scanResultOutputPath = "";
    this.consoleLogEnabled = false;
    this.logLevel = LogLevel.INFO;
    this.verifyTLS = true;
    this.extraParams = new ArrayList<>();
    this.policiesToApply = new ArrayList<>();
    this.imageToScan = "";
    this.workingDirectory = null;
    this.extraEnvVars = new EnvVars();
    this.extraEnvVars.put("SECURE_API_TOKEN", sysdigAPIToken);
    this.separateByLayer = false;
  }

  public SysdigImageScanningProcessBuilder withEngineURL(String engineurl) {
    var clone = this.clone();
    clone.engineURL = engineurl;
    return clone;
  }

  public SysdigImageScanningProcessBuilder withDBPath(String dbPath) {
    var clone = this.clone();
    clone.dbPath = dbPath;
    return clone;
  }

  public SysdigImageScanningProcessBuilder withCachePath(String cacheFolder) {
    var clone = this.clone();
    clone.cachePath = cacheFolder;
    return clone;
  }

  public SysdigImageScanningProcessBuilder withScanResultOutputPath(String scanResultOutputPath) {
    var clone = this.clone();
    clone.scanResultOutputPath = scanResultOutputPath;
    return clone;
  }

  public SysdigImageScanningProcessBuilder withConsoleLog() {
    var clone = this.clone();
    clone.consoleLogEnabled = true;
    return clone;
  }

  public SysdigImageScanningProcessBuilder withLogLevel(LogLevel logLevel) {
    var clone = this.clone();
    clone.logLevel = logLevel;
    return clone;
  }

  public SysdigImageScanningProcessBuilder withTLSVerification(boolean verifyTLS) {
    var clone = this.clone();
    clone.verifyTLS = verifyTLS;
    return clone;
  }

  public SysdigImageScanningProcessBuilder withExtraParametersSeparatedBySpace(String inlineScanExtraParams) {
    var clone = this.clone();
    Arrays.stream(inlineScanExtraParams.split(" "))
      .map(String::trim)
      .filter(param -> !param.isEmpty())
      .forEach(clone.extraParams::add);
    return clone;
  }

  public SysdigImageScanningProcessBuilder withPoliciesToApplySeparatedBySpace(String policiesToApply) {
    var clone = this.clone();
    Arrays.stream(policiesToApply.split(" "))
      .map(String::trim)
      .filter(policy -> !policy.isEmpty())
      .forEach(clone.policiesToApply::add);
    return clone;
  }

  public SysdigImageScanningProcessBuilder withImageToScan(String image) {
    var clone = this.clone();
    clone.imageToScan = image;
    return clone;
  }

  public SysdigImageScanningProcessBuilder withWorkingDirectory(FilePath workingDirectory) {
    var clone = this.clone();
    clone.workingDirectory = workingDirectory;
    return clone;
  }

  public SysdigImageScanningProcessBuilder withExtraEnvVars(EnvVars envVars) {
    var clone = this.clone();
    clone.extraEnvVars.putAll(envVars);
    return clone;
  }

  public SysdigImageScanningProcessBuilder withStdoutRedirectedTo(OutputStream outputStream) {
    var clone = this.clone();
    clone.redirectedStdoutStream = outputStream;
    return clone;
  }

  public SysdigImageScanningProcessBuilder withStderrRedirectedTo(OutputStream outputStream) {
    var clone = this.clone();
    clone.redirectedStdErrStream = outputStream;
    return clone;
  }

  public SysdigImageScanningProcessBuilder withStdoutRedirectedTo(SysdigLogger sysdigLogger) {
    return withStdoutRedirectedTo(new LogOutputStreamAdapter(sysdigLogger));
  }

  public SysdigImageScanningProcessBuilder withStderrRedirectedTo(SysdigLogger sysdigLogger) {
    return withStderrRedirectedTo(new LogOutputStreamAdapter(sysdigLogger));
  }

  public SysdigImageScanningProcessBuilder withSeparateByLayer(boolean separateByLayer) {
    var clone = this.clone();
    clone.separateByLayer = separateByLayer;
    return clone;
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
    if (!Strings.isNullOrEmpty(engineURL)) arguments.add("--apiurl=" + engineURL);
    if (!Strings.isNullOrEmpty(scanResultOutputPath)) arguments.add("--json-scan-result=" + scanResultOutputPath);
    policiesToApply.stream().map(policy -> "--policy=" + policy).forEach(arguments::add);
    if (!Strings.isNullOrEmpty(dbPath)) arguments.add("--dbpath=" + dbPath);
    if (!Strings.isNullOrEmpty(cachePath)) arguments.add("--cachepath=" + cachePath);
    arguments.add("--loglevel=" + logLevel.toString());
    if (consoleLogEnabled) arguments.add("--console-log");
    if (!verifyTLS) arguments.add("--skiptlsverify");
    if (separateByLayer) arguments.add("--separate-by-layer");
    arguments.addAll(extraParams);
    arguments.add(imageToScan);
    return arguments;
  }

  @Override
  public SysdigImageScanningProcessBuilder clone() {
    try {
      SysdigImageScanningProcessBuilder cloned = (SysdigImageScanningProcessBuilder) super.clone();
      // Deep copy mutable fields
      cloned.extraParams = new ArrayList<>(this.extraParams);
      cloned.policiesToApply = new ArrayList<>(this.policiesToApply);
      cloned.extraEnvVars = new EnvVars(this.extraEnvVars);
      // Other fields are either immutable or primitives, so they can be copied as is
      return cloned;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError("Clone not supported", e);
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
