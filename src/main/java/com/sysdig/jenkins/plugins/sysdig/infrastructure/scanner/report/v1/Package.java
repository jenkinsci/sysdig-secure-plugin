package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1;

import java.util.List;

record Package(
  boolean isRemoved,
  boolean irRunning,
  String layerRef,
  String name,
  String path,
  String type,
  String version,
  List<String> vulnerabilitiesRefs
) {
}
