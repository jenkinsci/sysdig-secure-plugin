package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1beta3;

import java.util.Optional;

record JsonAcceptedRisk(
  String id,
  String status,
  String reason,
  String description,
  String expirationDate,
  String createdAt,
  String updatedAt
) {
  public Optional<String> optId() {
    return Optional.ofNullable(id);
  }
}
