package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1;

record CvssScore(
  Float score,
  String vector,
  String version
) {
}
