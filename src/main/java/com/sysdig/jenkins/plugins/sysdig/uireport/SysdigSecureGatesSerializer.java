package com.sysdig.jenkins.plugins.sysdig.uireport;


import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Stream;

public class SysdigSecureGatesSerializer implements JsonSerializer<SysdigSecureGates> {
  @Override
  public JsonElement serialize(SysdigSecureGates sysdigSecureGates, Type type, JsonSerializationContext jsonSerializationContext) {
    JsonObject jsonObject = new JsonObject();

    sysdigSecureGates.getResultsForEachImage().entrySet().forEach(keyPair -> {
      jsonObject.add(keyPair.getKey(), serializeTopLevelResultsList(sysdigSecureGates, keyPair.getValue()));
    });

    return jsonObject;
  }

  private JsonElement serializeTopLevelResultsList(SysdigSecureGates sysdigSecureGates, List<SysdigSecureGateResult> results) {
    JsonObject resultObject = new JsonObject();
    resultObject.add("header", header());
    resultObject.add("final_action", new JsonPrimitive(sysdigSecureGates.isFailed() ? "STOP" : "GO"));
    resultObject.add("rows", serializeRows(results));

    JsonObject jsonObject = new JsonObject();
    jsonObject.add("result", resultObject);

    return jsonObject;
  }

  private JsonArray serializeRows(List<SysdigSecureGateResult> results) {
    JsonArray array = new JsonArray();

    results.stream().map(this::serializeRow).forEach(array::add);

    return array;
  }

  private JsonArray serializeRow(SysdigSecureGateResult result) {
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
