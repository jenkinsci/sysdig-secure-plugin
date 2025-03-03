package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner;

import com.sysdig.jenkins.plugins.sysdig.domain.SysdigLogger;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Using the Curiously Recurring Template Pattern (CRTP) and F-bounded quantification to enable fluent method chaining with type safety in subclasses.
public abstract class SysdigProcessBuilderBase<T extends SysdigProcessBuilderBase<T>> implements Cloneable {

  protected final String sysdigCLIPath;
  protected String engineURL;
  protected String scanResultOutputPath;
  protected boolean consoleLogEnabled;
  protected LogLevel logLevel;
  protected boolean verifyTLS;
  protected List<String> extraParams;
  protected List<String> policiesToApply;
  protected FilePath workingDirectory;
  protected EnvVars extraEnvVars;
  protected OutputStream redirectedStdoutStream;
  protected OutputStream redirectedStdErrStream;

  public SysdigProcessBuilderBase(String sysdigCLIPath, String sysdigAPIToken) {
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
  }

  public T withEngineURL(String engineURL) {
    T clone = this.clone();
    clone.engineURL = engineURL;
    return clone;
  }

  public T withScanResultOutputPath(String scanResultOutputPath) {
    T clone = this.clone();
    clone.scanResultOutputPath = scanResultOutputPath;
    return clone;
  }

  public T withConsoleLog() {
    T clone = this.clone();
    clone.consoleLogEnabled = true;
    return clone;
  }

  public T withLogLevel(LogLevel logLevel) {
    T clone = this.clone();
    clone.logLevel = logLevel;
    return clone;
  }

  public T withTLSVerification(boolean verifyTLS) {
    T clone = this.clone();
    clone.verifyTLS = verifyTLS;
    return clone;
  }

  public T withExtraParametersSeparatedBySpace(String inlineScanExtraParams) {
    T clone = this.clone();
    Arrays.stream(inlineScanExtraParams.split(" ")).map(String::trim).filter(param -> !param.isEmpty()).forEach(clone.extraParams::add);
    return clone;
  }

  public T withPoliciesToApplySeparatedBySpace(String policiesToApply) {
    T clone = this.clone();
    Arrays.stream(policiesToApply.split(" ")).map(String::trim).filter(policy -> !policy.isEmpty()).forEach(clone.policiesToApply::add);
    return clone;
  }

  public T withWorkingDirectory(FilePath workingDirectory) {
    T clone = this.clone();
    clone.workingDirectory = workingDirectory;
    return clone;
  }

  public T withExtraEnvVars(EnvVars envVars) {
    T clone = this.clone();
    clone.extraEnvVars.putAll(envVars);
    return clone;
  }

  public T withStdoutRedirectedTo(OutputStream outputStream) {
    T clone = this.clone();
    clone.redirectedStdoutStream = outputStream;
    return clone;
  }

  public T withStderrRedirectedTo(OutputStream outputStream) {
    T clone = this.clone();
    clone.redirectedStdErrStream = outputStream;
    return clone;
  }

  public T withStdoutRedirectedTo(SysdigLogger sysdigLogger) {
    return withStdoutRedirectedTo(new LogOutputStreamAdapter(sysdigLogger));
  }

  public T withStderrRedirectedTo(SysdigLogger sysdigLogger) {
    return withStderrRedirectedTo(new LogOutputStreamAdapter(sysdigLogger));
  }

  public int launchAndWait(@Nonnull  Launcher launcher) throws IOException, InterruptedException {
    Launcher.ProcStarter procStarter = launcher.launch().cmds(this.toCommandLineArguments()).envs(this.extraEnvVars);

    if (workingDirectory != null) procStarter.pwd(workingDirectory);
    if (redirectedStdoutStream != null) procStarter.stdout(redirectedStdoutStream);
    if (redirectedStdErrStream != null) procStarter.stderr(redirectedStdErrStream);

    return procStarter.join();
  }

  public abstract List<String> toCommandLineArguments();

  @Override
  public T clone() {
    try {
      @SuppressWarnings("unchecked") T cloned = (T) super.clone();
      // Deep copy mutable fields
      cloned.extraParams = new ArrayList<>(this.extraParams);
      cloned.policiesToApply = new ArrayList<>(this.policiesToApply);
      cloned.extraEnvVars = new EnvVars(this.extraEnvVars);
      return cloned;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError("Clone not supported", e);
    }
  }

  public enum LogLevel {
    DEBUG, INFO;

    @Override
    public String toString() {
      return switch (this) {
        case INFO -> "info";
        case DEBUG -> "debug";
      };
    }
  }
}
