package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1beta3;

import java.io.Serializable;
import java.util.Optional;

public class JsonAcceptedRiskReference implements Serializable {
  private String id;

  public Optional<String> getId() {
    return Optional.ofNullable(id);
  }

  public void setId(String id) {
    this.id = id;
  }
}
