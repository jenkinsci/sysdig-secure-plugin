package com.sysdig.jenkins.plugins.sysdig.scanner;

import com.sysdig.jenkins.plugins.sysdig.BuildConfig;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.AbortException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

public abstract class OldEngineScanner implements ScannerInterface<JSONArray> {

  protected final BuildConfig config;

  protected final SysdigLogger logger;

  public OldEngineScanner(BuildConfig config, SysdigLogger logger) {
    this.logger = logger;
    this.config = config;
  }



  public ImageScanningResult buildImageScanningResult(JSONArray scanReport, JSONObject vulnsReport, String imageDigest, String tag) throws AbortException {
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
    JSONArray gatePolicies = tagEvals.getJSONObject(0).getJSONObject("detail").getJSONObject("policy").getJSONArray("policies");

    return new ImageScanningResult(tag, imageDigest, evalStatus, gateResult, vulnsReport,gatePolicies);
  }

  public ArrayList<ImageScanningResult> scanImages(Map<String, String> imagesAndDockerfiles) throws AbortException {
    if (imagesAndDockerfiles == null) {
      return new ArrayList<>();
    }

    ArrayList<ImageScanningResult> resultList = new ArrayList<>();

    for (Map.Entry<String, String> entry : imagesAndDockerfiles.entrySet()) {
      String dockerfile = entry.getValue();

      ImageScanningSubmission submission = this.scanImage(entry.getKey(), dockerfile);

      JSONArray scanReport = this.getGateResults(submission);
      JSONObject vulnsReport = this.getVulnsReport(submission);

      ImageScanningResult result = this.buildImageScanningResult(scanReport, vulnsReport, submission.getImageDigest(), submission.getTag());
      resultList.add(result);
    }

    return resultList;
  }
  
}
