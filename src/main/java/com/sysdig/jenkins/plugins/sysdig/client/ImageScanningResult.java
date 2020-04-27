package com.sysdig.jenkins.plugins.sysdig.client;

import net.sf.json.JSONObject;

public class ImageScanningResult {
  private String evalStatus;
  private JSONObject gateResult;

  public ImageScanningResult(String evalStatus, JSONObject gateResult) {

    this.evalStatus = evalStatus;
    this.gateResult = gateResult;
  }

  public String getEvalStatus() {
    return evalStatus;
  }

  public void setEvalStatus(String evalStatus) {
    this.evalStatus = evalStatus;
  }

  public JSONObject getGateResult() {
    return gateResult;
  }

  public void setGateResult(JSONObject gateResult) {
    this.gateResult = gateResult;
  }
}
