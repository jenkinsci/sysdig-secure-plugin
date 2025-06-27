package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1;

import java.util.Map;

record Result(
  String assetType,
  Map<String, Layer> layers,
  Metadata metadata,
  Map<String, Package> packages,
  Policies policies,
  Producer producer,
  Map<String, RiskAccept> riskAccepts,
  String stage,
  Map<String, Vulnerability> vulnerabilities
) {
}
