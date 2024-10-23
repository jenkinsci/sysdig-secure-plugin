package com.sysdig.jenkins.plugins.sysdig.infrastructure.json;

import com.google.gson.Gson;
import com.sysdig.jenkins.plugins.sysdig.application.ui.report.PolicyEvaluationReport;
import com.sysdig.jenkins.plugins.sysdig.application.ui.report.PolicyEvaluationSummary;

public class GsonBuilder {
  public static Gson build() {
    return new com.google.gson.GsonBuilder()
      .registerTypeAdapter(PolicyEvaluationReport.class, new PolicyEvaluationReportSerializer())
      .registerTypeAdapter(PolicyEvaluationSummary.class, new PolicyEvaluationSummarySerializer())
      .create();
  }
}
