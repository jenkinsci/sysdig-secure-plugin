package com.sysdig.jenkins.plugins.sysdig.infrastructure.json;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.sysdig.jenkins.plugins.sysdig.application.ui.report.PolicyEvaluationSummary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PolicyEvaluationSummarySerializerTest {

  private final Gson gson = GsonBuilder.build();

  @Test
  void testSerializeEmptySummary() {
    // Given
    PolicyEvaluationSummary summary = new PolicyEvaluationSummary();

    // When
    JsonElement jsonElement = gson.toJsonTree(summary);

    // Then
    JsonElement expectedJson = JsonParser.parseString("{" +
      "\"header\":[{" +
      "\"data\":\"Repo_Tag\",\"title\":\"Repo Tag\"},{\"data\":\"Stop_Actions\",\"title\":\"Stop Actions\"},{\"data\":\"Warn_Actions\",\"title\":\"Warn Actions\"},{\"data\":\"Go_Actions\",\"title\":\"Go Actions\"},{\"data\":\"Final_Action\",\"title\":\"Final Action\"}]," +
      "\"rows\":[]}");

    assertEquals(expectedJson, jsonElement, "Serialized JSON does not match expected output for an empty summary");
  }

  @Test
  void testSerializeSummaryWithLines() {
    // Given
    PolicyEvaluationSummary summary = new PolicyEvaluationSummary();
    summary.addSummaryLine("repo:tag1", 2, 3, 1, "STOP");
    summary.addSummaryLine("repo:tag2", 1, 2, 0, "WARN");

    // When
    JsonElement jsonElement = gson.toJsonTree(summary);

    // Then
    JsonElement expectedJson = JsonParser.parseString("{" +
      "\"header\":[{" +
      "\"data\":\"Repo_Tag\",\"title\":\"Repo Tag\"},{\"data\":\"Stop_Actions\",\"title\":\"Stop Actions\"},{\"data\":\"Warn_Actions\",\"title\":\"Warn Actions\"},{\"data\":\"Go_Actions\",\"title\":\"Go Actions\"},{\"data\":\"Final_Action\",\"title\":\"Final Action\"}]," +
      "\"rows\":[" +
      "{\"Repo_Tag\":\"repo:tag1\",\"Stop_Actions\":2,\"Warn_Actions\":3,\"Go_Actions\":1,\"Final_Action\":\"STOP\"}," +
      "{\"Repo_Tag\":\"repo:tag2\",\"Stop_Actions\":1,\"Warn_Actions\":2,\"Go_Actions\":0,\"Final_Action\":\"WARN\"}" +
      "]}"
    );

    assertEquals(expectedJson, jsonElement, "Serialized JSON does not match expected output for a summary with lines");
  }
}
