package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1;

import com.google.gson.annotations.SerializedName;

record JsonProviderCvssScore(
        Float score,
        String vector,
        String version,
        @SerializedName("temporal_score") Float temporalScore) {}
