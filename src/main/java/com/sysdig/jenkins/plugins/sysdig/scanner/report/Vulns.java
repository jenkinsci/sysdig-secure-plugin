package com.sysdig.jenkins.plugins.sysdig.scanner.report;

import java.io.Serializable;
import java.util.Optional;

public class Vulns implements Serializable {
  private Long critical;
  private Long high;
  private Long low;
  private Long medium;
  private Long negligible;

  public Optional<Long> getCritical() {
    return Optional.ofNullable(critical);
  }

  public void setCritical(Long critical) {
    this.critical = critical;
  }

  public Optional<Long> getHigh() {
    return Optional.ofNullable(high);
  }

  public void setHigh(Long high) {
    this.high = high;
  }

  public Optional<Long> getLow() {
    return Optional.ofNullable(low);
  }

  public void setLow(Long low) {
    this.low = low;
  }

  public Optional<Long> getMedium() {
    return Optional.ofNullable(medium);
  }

  public void setMedium(Long medium) {
    this.medium = medium;
  }

  public Optional<Long> getNegligible() {
    return Optional.ofNullable(negligible);
  }

  public void setNegligible(Long negligible) {
    this.negligible = negligible;
  }
}
