package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

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
