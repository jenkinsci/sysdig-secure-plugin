package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.iac.v1;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Shared shape for the {@code unsupportedResources} and {@code errors} arrays: a source file plus a
 * human-readable {@code details} string and its structured {@code details_list} counterpart.
 */
record JsonIaCSourceDetails(
        String source,
        String details,
        @SerializedName("details_list") List<String> detailsList) {}
