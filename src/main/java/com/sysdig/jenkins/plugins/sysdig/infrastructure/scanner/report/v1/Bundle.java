package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1;

import java.util.List;

record Bundle(
  String identifier,
  String name,
  List<Rule> rules,
  String type
) {
}
