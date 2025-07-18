package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

import java.io.Serializable;

public interface PolicyBundleRuleFailure extends Serializable {
    enum PolicyBundleRuleFailureType {
        ImageConfigFailure,
        PkgVulnFailure,
    }

    PolicyBundleRuleFailureType type();

    String description();

    PolicyBundleRule parent();
}
