package com.sysdig.jenkins.plugins.sysdig.domain.vm.report;

public record PolicyBundleRule(
  String id,
  String description,
  EvaluationResult evaluationResult
) {
}
