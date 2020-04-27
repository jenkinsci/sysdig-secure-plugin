package com.sysdig.jenkins.plugins.sysdig.client;

import net.sf.json.JSONArray;

public class ImageScanningVulnerabilities {
  private JSONArray dataJson;

  public ImageScanningVulnerabilities(JSONArray dataJson) {

    this.dataJson = dataJson;
  }

  public JSONArray getDataJson() {
    return dataJson;
  }

  public void setDataJson(JSONArray dataJson) {
    this.dataJson = dataJson;
  }
}
