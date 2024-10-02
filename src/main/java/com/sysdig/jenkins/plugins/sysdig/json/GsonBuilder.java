package com.sysdig.jenkins.plugins.sysdig.json;

import com.google.gson.Gson;
import com.sysdig.jenkins.plugins.sysdig.uireport.PolicyEvaluationReportSerializer;
import com.sysdig.jenkins.plugins.sysdig.uireport.PolicyEvaluationReport;
import com.sysdig.jenkins.plugins.sysdig.uireport.PolicyEvaluationSummary;
import com.sysdig.jenkins.plugins.sysdig.uireport.PolicyEvaluationSummarySerializer;

public class GsonBuilder {
  public static Gson build() {
    return new com.google.gson.GsonBuilder()
      .registerTypeAdapterFactory(OptionalTypeAdapter.FACTORY)
      .registerTypeAdapter(PolicyEvaluationReport.class, new PolicyEvaluationReportSerializer())
      .registerTypeAdapter(PolicyEvaluationSummary.class, new PolicyEvaluationSummarySerializer())
      .create();
  }
}
