package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1;

record JsonCvssScore(Float score, String vector, String version) {}
