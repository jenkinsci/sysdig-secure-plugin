package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.iac.v1;

record JsonIaCFindingsSummary(Integer critical, Integer high, Integer medium, Integer low, Integer negligible) {}
