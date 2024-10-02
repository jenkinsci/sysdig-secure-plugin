package com.sysdig.jenkins.plugins.sysdig.uireport;

import com.google.gson.*;

import java.lang.reflect.Type;

public class PolicyEvaluationSummarySerializer implements JsonSerializer<PolicyEvaluationSummary> {
  private enum GateSummaryColumn {Repo_Tag, Stop_Actions, Warn_Actions, Go_Actions, Final_Action}

  @Override
  public JsonElement serialize(PolicyEvaluationSummary summary, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject result = new JsonObject();

    result.add("header", generateDataTablesColumnsForGateSummary());
    result.add("rows", generateRowsForSummary(summary));

    return result;
  }

  private static JsonArray generateRowsForSummary(PolicyEvaluationSummary summary) {
    JsonArray rows = new JsonArray();

    summary.getLines().forEach(line -> {
      var summaryRow = new JsonObject();

      summaryRow.addProperty(GateSummaryColumn.Repo_Tag.toString(), line.getImageTag());
      summaryRow.addProperty(GateSummaryColumn.Stop_Actions.toString(), line.getNonWhitelistedStopActions());
      summaryRow.addProperty(GateSummaryColumn.Warn_Actions.toString(), line.getNonWhitelistedWarnActions());
      summaryRow.addProperty(GateSummaryColumn.Go_Actions.toString(), line.getNonWhitelistedGoActions());
      summaryRow.addProperty(GateSummaryColumn.Final_Action.toString(), line.getFinalAction());

      rows.add(summaryRow);
    });

    return rows;
  }


  protected static JsonArray generateDataTablesColumnsForGateSummary() {
    JsonArray headers = new JsonArray();

    for (GateSummaryColumn column : GateSummaryColumn.values()) {
      String columnName = column.toString();

      JsonObject header = new JsonObject();
      header.addProperty("data", columnName);
      header.addProperty("title", columnName.replaceAll("_", " "));
      headers.add(header);
    }

    return headers;
  }
}
