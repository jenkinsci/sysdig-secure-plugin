package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1;

import java.util.List;

record JsonRule(
        String description,
        String evaluationResult,
        String failureType,
        List<JsonFailure> failures,
        String ruleId,
        String ruleType) {}
