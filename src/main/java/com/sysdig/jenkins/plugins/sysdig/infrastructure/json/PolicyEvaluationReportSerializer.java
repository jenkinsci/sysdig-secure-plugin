package com.sysdig.jenkins.plugins.sysdig.infrastructure.json;


import com.google.gson.*;
import com.sysdig.jenkins.plugins.sysdig.application.vm.report.PolicyEvaluationReport;
import com.sysdig.jenkins.plugins.sysdig.application.vm.report.PolicyEvaluationReportLine;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Stream;

public class PolicyEvaluationReportSerializer implements JsonSerializer<PolicyEvaluationReport> {
  @Override
  public JsonElement serialize(PolicyEvaluationReport policyEvaluationReport, Type type, JsonSerializationContext jsonSerializationContext) {
    JsonObject jsonObject = new JsonObject();

    policyEvaluationReport.getResultsForEachImage().entrySet().forEach(keyPair -> {
      jsonObject.add(keyPair.getKey(), serializeTopLevelResultsList(policyEvaluationReport, keyPair.getValue()));
    });

    return jsonObject;
  }

  private JsonElement serializeTopLevelResultsList(PolicyEvaluationReport policyEvaluationReport, List<PolicyEvaluationReportLine> results) {
    JsonObject resultObject = new JsonObject();
    resultObject.add("header", header());
    resultObject.add("final_action", new JsonPrimitive(policyEvaluationReport.isFailed() ? "STOP" : "GO"));
    resultObject.add("rows", serializeRows(results));

    JsonObject jsonObject = new JsonObject();
    jsonObject.add("result", resultObject);

    return jsonObject;
  }

  private JsonArray serializeRows(List<PolicyEvaluationReportLine> results) {
    JsonArray array = new JsonArray();

    results.stream().map(this::serializeRow).forEach(array::add);

    return array;
  }

  private JsonArray serializeRow(PolicyEvaluationReportLine result) {
    JsonArray array = new JsonArray();

    array.add(result.getImageID());
    array.add(result.getRepoTag());
    array.add(result.getTriggerID());
    array.add(result.getGate());
    array.add(result.getTrigger());
    array.add(result.getCheckOutput());
    array.add(result.getGateAction());
    array.add(result.getWhitelisted());
    array.add(result.getPolicyID());
    array.add(result.getPolicyName());

    return array;
  }

  private JsonArray header() {
    JsonArray array = new JsonArray();

    Stream.of(
      "Image_Id",
      "Repo_Tag",
      "Trigger_Id",
      "Gate",
      "Trigger",
      "Check_Output",
      "Gate_Action",
      "Whitelisted",
      "Policy_Id",
      "Policy_Name"
    ).forEach(array::add);

    return array;
  }
}
