package com.sysdig.jenkins.plugins.sysdig.uireport;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.sysdig.jenkins.plugins.sysdig.json.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PolicyEvaluationReportSerializerTest {
  private final Gson gson = GsonBuilder.build();

  @Test
  public void testSerializeEmptyReport() {
    // Given
    PolicyEvaluationReport report = new PolicyEvaluationReport(false);

    // When
    JsonElement jsonElement = gson.toJsonTree(report);

    // Then
    assertEquals(JsonParser.parseString("{}"), jsonElement, "Expected serialized JSON to be empty for an empty report");
  }

  @Test
  public void testSerializeReportWithResults() {
    // Given
    PolicyEvaluationReport report = new PolicyEvaluationReport(true);
    Stream.of(
      new PolicyEvaluationReportLine("image-1", "repo:tag", "trigger-1", "gate-1", "trigger-1-description", "output-1-check", "action-1", false, "policy-1", "policy-name-1"),
      new PolicyEvaluationReportLine("image-1", "repo:tag", "trigger-4", "gate-1", "trigger-4-description", "output-4-check", "action-4", true, "policy-4", "policy-name-4"),
      new PolicyEvaluationReportLine("image-1", "repo:tag", "trigger-5", "gate-2", "trigger-5-description", "output-5-check", "action-5", false, "policy-5", "policy-name-5"),
      new PolicyEvaluationReportLine("image-2", "repo:anothertag", "trigger-2", "gate-2", "trigger-2-description", "output-2-check", "action-2", true, "policy-2", "policy-name-2"),
      new PolicyEvaluationReportLine("image-3", "repo:thirdtag", "trigger-3", "gate-3", "trigger-3-description", "output-3-check", "action-3", false, "policy-3", "policy-name-3")
    ).forEach(report::addResult);

    // When
    JsonElement jsonElement = gson.toJsonTree(report);

    // Then
    JsonElement expectedJson = JsonParser.parseString("{" +
      "\"image-1\":{\"result\":{\"header\":[\"Image_Id\",\"Repo_Tag\",\"Trigger_Id\",\"Gate\",\"Trigger\",\"Check_Output\",\"Gate_Action\",\"Whitelisted\",\"Policy_Id\",\"Policy_Name\"],\"final_action\":\"STOP\",\"rows\":[" +
      "[\"image-1\",\"repo:tag\",\"trigger-1\",\"gate-1\",\"trigger-1-description\",\"output-1-check\",\"action-1\",false,\"policy-1\",\"policy-name-1\"]," +
      "[\"image-1\",\"repo:tag\",\"trigger-4\",\"gate-1\",\"trigger-4-description\",\"output-4-check\",\"action-4\",true,\"policy-4\",\"policy-name-4\"]," +
      "[\"image-1\",\"repo:tag\",\"trigger-5\",\"gate-2\",\"trigger-5-description\",\"output-5-check\",\"action-5\",false,\"policy-5\",\"policy-name-5\"]" +
      "]}}," +
      "\"image-2\":{\"result\":{\"header\":[\"Image_Id\",\"Repo_Tag\",\"Trigger_Id\",\"Gate\",\"Trigger\",\"Check_Output\",\"Gate_Action\",\"Whitelisted\",\"Policy_Id\",\"Policy_Name\"],\"final_action\":\"STOP\",\"rows\":[[\"image-2\",\"repo:anothertag\",\"trigger-2\",\"gate-2\",\"trigger-2-description\",\"output-2-check\",\"action-2\",true,\"policy-2\",\"policy-name-2\"]]}}," +
      "\"image-3\":{\"result\":{\"header\":[\"Image_Id\",\"Repo_Tag\",\"Trigger_Id\",\"Gate\",\"Trigger\",\"Check_Output\",\"Gate_Action\",\"Whitelisted\",\"Policy_Id\",\"Policy_Name\"],\"final_action\":\"STOP\",\"rows\":[[\"image-3\",\"repo:thirdtag\",\"trigger-3\",\"gate-3\",\"trigger-3-description\",\"output-3-check\",\"action-3\",false,\"policy-3\",\"policy-name-3\"]]}}}");

    assertEquals(expectedJson, jsonElement, "Serialized JSON does not match expected output.");
  }
}
