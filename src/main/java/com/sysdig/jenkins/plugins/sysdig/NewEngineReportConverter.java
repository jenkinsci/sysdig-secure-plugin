package com.sysdig.jenkins.plugins.sysdig;

import com.google.common.base.Strings;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.scanner.ImageScanningResult;
import hudson.AbortException;
import hudson.FilePath;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class NewEngineReportConverter extends ReportConverter{


  public NewEngineReportConverter(SysdigLogger logger) {
    super(logger);

  }


  @Override
  protected JSONArray getVulnerabilitiesArray(String tag, JSONObject vulnsReport) {
    JSONArray dataJson = new JSONArray();
    JSONArray vulList = vulnsReport.getJSONArray("list");
    for (int i = 0; i < vulList.size(); i++) {
      JSONObject vulnJson = vulList.getJSONObject(i);
      vulnJson.getJSONArray("vulnerabilities").forEach(item -> {
        JSONObject obj = (JSONObject) item;
        JSONArray vulnArray = new JSONArray();
        vulnArray.addAll(Arrays.asList(
          tag,
          ((JSONObject) item).getString("name"),
          ((JSONObject) item).getJSONObject("severity").getString("label"),
          vulnJson.getString("name"),
          vulnJson.getString("suggestedFix"),
          vulnJson.has("url") ? vulnJson.getString("url") : "",
          vulnJson.getString("type"),
          ((JSONObject) item).getString("disclosureDate"),
          ((JSONObject) item).getString("solutionDate")
        ));
        dataJson.add(vulnArray);
      });
    }

    return dataJson;
  }
}
