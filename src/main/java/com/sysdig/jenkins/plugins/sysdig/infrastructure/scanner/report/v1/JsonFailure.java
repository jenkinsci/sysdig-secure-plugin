package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1;

record JsonFailure(
  String remediation,
  String packageRef,
  String vulnerabilityRef
) {
}
