package com.sysdig.jenkins.plugins.sysdig.domain.vm.report;

import java.io.Serializable;
import java.util.Optional;

public class Predicate implements Serializable {
  private String type;
  private Extra extra;

  public Optional<String> getType() {
    return Optional.ofNullable(type);
  }

  public void setType(String type) {
    this.type = type;
  }

  public Optional<Extra> getExtra() {
    return Optional.ofNullable(extra);
  }

  public void setExtra(Extra extra) {
    this.extra = extra;
  }
}
