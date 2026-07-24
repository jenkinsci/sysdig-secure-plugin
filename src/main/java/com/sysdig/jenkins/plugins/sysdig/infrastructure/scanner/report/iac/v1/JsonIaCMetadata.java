package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.iac.v1;

import java.util.List;

record JsonIaCMetadata(List<String> sources, Integer totalModules, Integer totalResource, Integer totalFolders) {}
