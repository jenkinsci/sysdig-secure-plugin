package com.sysdig.jenkins.plugins.sysdig.scanner;

import com.sysdig.jenkins.plugins.sysdig.BuildConfig;
import com.sysdig.jenkins.plugins.sysdig.log.ConsoleLog;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

public abstract class Scanner {

  protected final Launcher launcher;
  protected final BuildConfig config;
  protected final SysdigLogger logger;

  public Scanner(Launcher launcher, TaskListener listener, BuildConfig config) {
    this.launcher = launcher;
    this.config = config;
    this.logger = new ConsoleLog(this.getClass().getSimpleName(), listener.getLogger(), config.getDebug());
  }

  public abstract ImageScanningSubmission scanImage(String imageTag, FilePath dockerfile) throws AbortException;
  public abstract JSONArray getGateResults(ImageScanningSubmission submission) throws AbortException;
  public abstract JSONObject getVulnsReport(ImageScanningSubmission submission) throws AbortException;

  public ArrayList<ImageScanningResult> scanImages(Map<String, FilePath> imagesAndDockerfiles) throws AbortException {
    if (imagesAndDockerfiles == null) {
      return new ArrayList<>();
    }

    ArrayList<ImageScanningResult> resultList = new ArrayList<>();

    for (Map.Entry<String, FilePath> entry : imagesAndDockerfiles.entrySet()) {
      ImageScanningSubmission submission = this.scanImage(entry.getKey(), entry.getValue());

      JSONArray scanReport = this.getGateResults(submission);
      JSONObject vulnsReport = this.getVulnsReport(submission);

      ImageScanningResult result = this.buildImageScanningResult(scanReport, vulnsReport, submission.getImageDigest(), submission.getTag());
      resultList.add(result);
    }

    return resultList;
  }

  private ImageScanningResult buildImageScanningResult(JSONArray scanReport, JSONObject vulnsReport, String imageDigest, String tag) throws AbortException {
    JSONObject tagEvalObj = JSONObject.fromObject(scanReport.get(0)).getJSONObject(imageDigest);
    JSONArray tagEvals = null;
    for (Object key : tagEvalObj.keySet()) {
      tagEvals = tagEvalObj.getJSONArray((String) key);
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
