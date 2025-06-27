package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1;

import java.util.List;

record Policies(
  String globalEvaluation,
  List<Policy> evaluations
) {
}
