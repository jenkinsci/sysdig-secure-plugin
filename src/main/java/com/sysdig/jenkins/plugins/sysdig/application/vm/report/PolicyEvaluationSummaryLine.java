/*
Copyright (C) 2016-2024 Sysdig

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.sysdig.jenkins.plugins.sysdig.application.vm.report;

import java.util.Objects;

public final class PolicyEvaluationSummaryLine {
    private final String imageTag;
    private final int nonWhitelistedStopActions;
    private final int nonWhitelistedWarnActions;
    private final int nonWhitelistedGoActions;
    private final String finalAction;

    public PolicyEvaluationSummaryLine(
        String imageTag,
        int nonWhitelistedStopActions,
        int nonWhitelistedWarnActions,
        int nonWhitelistedGoActions,
        String finalAction) {
        this.imageTag = imageTag;
        this.nonWhitelistedStopActions = nonWhitelistedStopActions;
        this.nonWhitelistedWarnActions = nonWhitelistedWarnActions;
        this.nonWhitelistedGoActions = nonWhitelistedGoActions;
        this.finalAction = finalAction;
    }

    public String imageTag() {
        return imageTag;
    }

    public int nonWhitelistedStopActions() {
        return nonWhitelistedStopActions;
    }

    public int nonWhitelistedWarnActions() {
        return nonWhitelistedWarnActions;
    }

    public int nonWhitelistedGoActions() {
        return nonWhitelistedGoActions;
    }

    public String finalAction() {
        return finalAction;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PolicyEvaluationSummaryLine) obj;
        return Objects.equals(this.imageTag, that.imageTag) &&
            this.nonWhitelistedStopActions == that.nonWhitelistedStopActions &&
            this.nonWhitelistedWarnActions == that.nonWhitelistedWarnActions &&
            this.nonWhitelistedGoActions == that.nonWhitelistedGoActions &&
            Objects.equals(this.finalAction, that.finalAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(imageTag, nonWhitelistedStopActions, nonWhitelistedWarnActions, nonWhitelistedGoActions, finalAction);
    }

    @Override
    public String toString() {
        return "PolicyEvaluationSummaryLine[" +
            "imageTag=" + imageTag + ", " +
            "nonWhitelistedStopActions=" + nonWhitelistedStopActions + ", " +
            "nonWhitelistedWarnActions=" + nonWhitelistedWarnActions + ", " +
            "nonWhitelistedGoActions=" + nonWhitelistedGoActions + ", " +
            "finalAction=" + finalAction + ']';
    }
}
