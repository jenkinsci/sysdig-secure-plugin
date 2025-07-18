package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1;

import java.util.Optional;

record JsonLayer(
  String command,
  String digest,
  Long index,
  Long size
) {
  @Override
  public Long size() {
    return Optional.ofNullable(size).orElse(0L);
  }
}
