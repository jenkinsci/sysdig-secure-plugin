package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1;

record Failure(
  String remediation,
  String description
) {
}
