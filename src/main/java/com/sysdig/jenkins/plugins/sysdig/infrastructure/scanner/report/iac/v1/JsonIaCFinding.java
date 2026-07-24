package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.iac.v1;

import java.util.List;

record JsonIaCFinding(
        Integer controlId,
        String name,
        String severity,
        List<JsonIaCResource> resources,
        List<String> policies,
        List<String> requirements) {}
