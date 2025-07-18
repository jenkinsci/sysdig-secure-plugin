package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1;

import java.util.Map;

record JsonResult(
  String assetType,
  Map<String, JsonLayer> layers,
  JsonMetadata metadata,
  Map<String, JsonPackage> packages,
  JsonPolicies policies,
  JsonProducer producer,
  Map<String, JsonRiskAccept> riskAccepts,
  String stage,
  Map<String, JsonVulnerability> vulnerabilities
) {
}
