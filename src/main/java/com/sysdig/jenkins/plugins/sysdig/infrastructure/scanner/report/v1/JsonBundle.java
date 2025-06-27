package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1;

import java.util.List;

record JsonBundle(
  String identifier,
  String name,
  List<JsonRule> rules,
  String type
) {
}
