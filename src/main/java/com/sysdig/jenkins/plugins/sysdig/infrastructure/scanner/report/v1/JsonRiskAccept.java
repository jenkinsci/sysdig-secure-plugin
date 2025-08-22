package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1;

import java.util.List;
import java.util.Optional;

record JsonRiskAccept(
        List<Object> context,
        String createdAt,
        String description,
        String entityType,
        String entityValue,
        String expirationDate,
        String id,
        String reason,
        String status,
        String updatedAt) {
    public Optional<String> expirationDateOpt() {
        return Optional.ofNullable(expirationDate);
    }
}
