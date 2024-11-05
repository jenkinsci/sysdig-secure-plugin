package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.List;

public class SysdigIaCScanningProcessBuilder extends SysdigProcessBuilderBase<SysdigIaCScanningProcessBuilder> {

  private boolean isRecursive;
  private boolean listUnsupportedResources;
  private Severity severity;
  private List<String> pathsToScan;

  public SysdigIaCScanningProcessBuilder(String sysdigCLIPath, String sysdigAPIToken) {
    super(sysdigCLIPath, sysdigAPIToken);
    this.isRecursive = false;
    this.listUnsupportedResources = false;
    this.severity = Severity.HIGH;
    this.pathsToScan = new ArrayList<>();
  }

  public SysdigIaCScanningProcessBuilder withRecursive(boolean isRecursive) {
    SysdigIaCScanningProcessBuilder clone = this.clone();
    clone.isRecursive = isRecursive;
    return clone;
  }

  public SysdigIaCScanningProcessBuilder withUnsupportedResources(boolean listUnsupportedResources) {
    SysdigIaCScanningProcessBuilder clone = this.clone();
    clone.listUnsupportedResources = listUnsupportedResources;
    return clone;
  }

  public SysdigIaCScanningProcessBuilder withSeverity(Severity severity) {
    SysdigIaCScanningProcessBuilder clone = this.clone();
    clone.severity = severity;
    return clone;
  }

  public SysdigIaCScanningProcessBuilder withPathsToScan(String... pathListToScan) {
    SysdigIaCScanningProcessBuilder clone = this.clone();
    clone.pathsToScan.addAll(List.of(pathListToScan));
    return clone;
  }

  @Override
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
    SysdigIaCScanningProcessBuilder cloned = (SysdigIaCScanningProcessBuilder) super.clone();
    cloned.pathsToScan = new ArrayList<>(this.pathsToScan);
    return cloned;
  }

  public enum Severity {
    HIGH,
    MEDIUM,
    LOW,
    NEVER;

    public static Severity fromString(String severity) {
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
        default:
          throw new IllegalArgumentException("Unsupported severity: " + severity);
      }
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
      throw new RuntimeException("Non-exhaustive switch in Severity");
    }
  }
}
