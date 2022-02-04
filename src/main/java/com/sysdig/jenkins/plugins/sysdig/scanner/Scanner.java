package com.sysdig.jenkins.plugins.sysdig.scanner;

import com.sysdig.jenkins.plugins.sysdig.BuildConfig;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.AbortException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

public abstract class Scanner implements ScannerInterface {

  protected final SysdigLogger logger;

  public Scanner(SysdigLogger logger) {
    this.logger = logger;
  }

  public abstract ImageScanningSubmission scanImage(String imageTag, String dockerfile) throws AbortException;
  public abstract JSONArray getGateResults(ImageScanningSubmission submission) throws AbortException;
  public abstract JSONObject getVulnsReport(ImageScanningSubmission submission) throws AbortException;
  protected abstract ImageScanningResult buildImageScanningResult(JSONArray scanReport, JSONObject vulnsReport, String imageDigest, String tag) throws AbortException;

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
