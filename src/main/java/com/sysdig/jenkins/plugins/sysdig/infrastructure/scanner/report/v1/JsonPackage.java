package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1;

import java.util.List;
import java.util.Optional;

record JsonPackage(
  boolean isRemoved,
  boolean irRunning,
  String layerRef,
  String name,
  String path,
  String type,
  String version,
  List<String> vulnerabilitiesRefs
) {
  @Override
  public List<String> vulnerabilitiesRefs() {
    return Optional.ofNullable(vulnerabilitiesRefs).orElse(List.of());
  }
}
