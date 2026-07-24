package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.iac.v1;

import java.util.List;

record JsonIaCResult(
        String type,
        JsonIaCMetadata metadata,
        JsonIaCFindingsSummary findingsSummaryBySeverity,
        List<JsonIaCSourceDetails> unsupportedResources,
        List<JsonIaCSourceDetails> errors,
        List<JsonIaCFinding> findings) {}
