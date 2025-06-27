package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1;

import java.util.List;

record Rule(
  String description,
  String evaluationResult,
  String failureType,
  List<Failure> failures,
  String ruleId,
  String ruleType
) {
}
