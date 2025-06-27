package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1beta3;

import java.io.Serializable;
import java.util.Optional;

public class JsonAcceptedRisk implements Serializable {
  private String id;
  private String status;
  private String reason;
  private String description;
  private String expirationDate;
  private String createdAt;
  private String updatedAt;

  public Optional<String> getId() {
    return Optional.ofNullable(id);
  }

  public void setId(String id) {
    this.id = id;
  }

  public Optional<String> getStatus() {
    return Optional.ofNullable(status);
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Optional<String> getReason() {
    return Optional.ofNullable(reason);
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public Optional<String> getDescription() {
    return Optional.ofNullable(description);
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Optional<String> getExpirationDate() {
    return Optional.ofNullable(expirationDate);
  }

  public void setExpirationDate(String expirationDate) {
    this.expirationDate = expirationDate;
  }

  public Optional<String> getCreatedAt() {
    return Optional.ofNullable(createdAt);
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public Optional<String> getUpdatedAt() {
    return Optional.ofNullable(updatedAt);
  }

  public void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
  }
}
