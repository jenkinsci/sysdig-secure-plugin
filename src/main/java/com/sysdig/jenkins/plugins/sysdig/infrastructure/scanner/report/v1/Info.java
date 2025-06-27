package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1;

record Info(
  String scanTime,
  String scanDuration,
  String resultUrl,
  String resultId
) {
}
