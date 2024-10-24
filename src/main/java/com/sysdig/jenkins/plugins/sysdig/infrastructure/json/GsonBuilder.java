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
package com.sysdig.jenkins.plugins.sysdig.infrastructure.json;

import com.google.gson.Gson;
import com.sysdig.jenkins.plugins.sysdig.application.vm.report.PolicyEvaluationReport;
import com.sysdig.jenkins.plugins.sysdig.application.vm.report.PolicyEvaluationSummary;

public class GsonBuilder {
  public static Gson build() {
    return new com.google.gson.GsonBuilder()
      .registerTypeAdapter(PolicyEvaluationReport.class, new PolicyEvaluationReportSerializer())
      .registerTypeAdapter(PolicyEvaluationSummary.class, new PolicyEvaluationSummarySerializer())
      .create();
  }
}
