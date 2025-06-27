package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1;

import java.util.List;

record Policy(
  List<Bundle> bundles,
  String createdAt,
  String description,
  String evaluation,
  String identifier,
  String name,
  String updatedAt
) {
}
