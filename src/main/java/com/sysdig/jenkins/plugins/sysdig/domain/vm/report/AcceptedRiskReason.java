package com.sysdig.jenkins.plugins.sysdig.domain.vm.report;

import java.io.Serializable;

public enum AcceptedRiskReason implements Serializable {
  RiskOwned,
  RiskTransferred,
  RiskAvoided,
  RiskMitigated,
  RiskNotRelevant,
  Custom,
  Unknown,
}
