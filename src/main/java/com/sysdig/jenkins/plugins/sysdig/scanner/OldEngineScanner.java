package com.sysdig.jenkins.plugins.sysdig.scanner;

import com.sysdig.jenkins.plugins.sysdig.BuildConfig;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.AbortException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public abstract class OldEngineScanner extends Scanner {

  protected final BuildConfig config;

  public OldEngineScanner(BuildConfig config, SysdigLogger logger) {
    super(logger);
    this.config = config;
  }

  public abstract ImageScanningSubmission scanImage(String imageTag, String dockerfile) throws AbortException;
  public abstract JSONArray getGateResults(ImageScanningSubmission submission) throws AbortException;
  public abstract JSONObject getVulnsReport(ImageScanningSubmission submission) throws AbortException;

  protected ImageScanningResult buildImageScanningResult(JSONArray scanReport, JSONObject vulnsReport, String imageDigest, String tag) throws AbortException {
    JSONObject reportDigests = JSONObject.fromObject(scanReport.get(0));
    JSONObject firstReport = null;
    for (Object key : reportDigests.keySet()) {
      firstReport = reportDigests.getJSONObject((String) key);
      break;
    }

    if (firstReport == null || firstReport.size() < 1) {
      throw new AbortException(String.format("Failed to analyze %s due to missing digest eval records in sysdig-secure-engine policy evaluation response", tag));
    }

    JSONArray tagEvals = null;
    for (Object key : firstReport.keySet()) {
      tagEvals = firstReport.getJSONArray((String) key);
      break;
    }

    if (tagEvals == null || tagEvals.size() < 1) {
      throw new AbortException(String.format("Failed to analyze %s due to missing tag eval records in sysdig-secure-engine policy evaluation response", tag));
    }

    String evalStatus = tagEvals.getJSONObject(0).getString("status");
    JSONObject gateResult = tagEvals.getJSONObject(0).getJSONObject("detail").getJSONObject("result").getJSONObject("result");

    return new ImageScanningResult(tag, imageDigest, evalStatus, gateResult, vulnsReport);
  }
  
}
