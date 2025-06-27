package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1;

record Layer(
  String command,
  String digest,
  Long index,
  Long size
) {
}
