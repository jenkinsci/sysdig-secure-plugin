package com.sysdig.jenkins.plugins.sysdig.scanner.report;

import java.io.Serializable;
import java.util.Optional;

public class Scanner implements Serializable {
  private String name;
  private String version;

  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  public void setName(String name) {
    this.name = name;
  }

  public Optional<String> getVersion() {
    return Optional.ofNullable(version);
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
