package com.sysdig.jenkins.plugins.sysdig.domain.iac.scanresult;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * A policy control that failed against one or more IaC resources.
 */
public record Finding(
        int controlId,
        String name,
        Severity severity,
        List<Resource> resources,
        List<String> policies,
        List<String> requirements)
        implements Serializable {

    public Finding {
        resources = resources == null ? List.of() : List.copyOf(resources);
        policies = policies == null ? List.of() : List.copyOf(policies);
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
    }

    @Override
    public List<Resource> resources() {
        return Collections.unmodifiableList(resources);
    }

    @Override
    public List<String> policies() {
        return Collections.unmodifiableList(policies);
    }

    @Override
    public List<String> requirements() {
        return Collections.unmodifiableList(requirements);
    }
}
