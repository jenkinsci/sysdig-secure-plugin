package com.sysdig.jenkins.plugins.sysdig.domain.report;

import java.io.Serializable;
import java.util.Optional;

public class Value implements Serializable {
  private String version;
  private Double score;
  private String vector;

  public Optional<String> getVersion() {
    return Optional.ofNullable(version);
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public Optional<Double> getScore() {
    return Optional.ofNullable(score);
  }

  public void setScore(Double score) {
    this.score = score;
  }

  public Optional<String> getVector() {
    return Optional.ofNullable(vector);
  }

  public void setVector(String vector) {
    this.vector = vector;
  }
}
