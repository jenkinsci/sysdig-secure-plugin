package com.sysdig.jenkins.plugins.sysdig.domain.vm.report;

public enum AcceptedRiskReason {
  RiskOwned,
  RiskTransferred,
  RiskAvoided,
  RiskMitigated,
  RiskNotRelevant,
  Custom,
  Unknown,
}
