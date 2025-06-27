package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1;

public record JsonScanResult(
  Info info,
  Scanner scanner,
  Result result
) {
}
