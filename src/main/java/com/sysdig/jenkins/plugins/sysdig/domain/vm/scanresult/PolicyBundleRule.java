package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

import java.io.Serializable;
import java.util.List;

public interface PolicyBundleRule extends Serializable {
    String id();

    String description();

    EvaluationResult evaluationResult();

    PolicyBundle parent();

    List<? extends PolicyBundleRuleFailure> failures();
}
