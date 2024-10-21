package com.sysdig.jenkins.plugins.sysdig.domain.report;

import java.io.Serializable;
import java.util.Optional;

public class CvssScore implements Serializable {
  private Value value;
  private String sourceName;

  public Optional<Value> getValue() {
    return Optional.ofNullable(value);
  }

  public void setValue(Value value) {
    this.value = value;
  }

  public Optional<String> getSourceName() {
    return Optional.ofNullable(sourceName);
  }

  public void setSourceName(String sourceName) {
    this.sourceName = sourceName;
  }
}
