package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1;

import java.util.List;

record JsonPolicies(
  String globalEvaluation,
  List<JsonPolicy> evaluations
) {
}
