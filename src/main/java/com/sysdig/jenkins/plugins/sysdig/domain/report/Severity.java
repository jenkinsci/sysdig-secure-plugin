package com.sysdig.jenkins.plugins.sysdig.domain.report;

import java.io.Serializable;
import java.util.Optional;

public class Severity implements Serializable {
  private String value;
  private String sourceName;

  public Optional<String> getValue() {
    return Optional.ofNullable(value);
  }

  public void setValue(String value) {
    this.value = value;
  }

  public Optional<String> getSourceName() {
    return Optional.ofNullable(sourceName);
  }

  public void setSourceName(String sourceName) {
    this.sourceName = sourceName;
  }
}
