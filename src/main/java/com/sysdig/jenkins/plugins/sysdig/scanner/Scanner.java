package com.sysdig.jenkins.plugins.sysdig.scanner;

import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.AbortException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

public abstract class Scanner {

  protected final SysdigLogger logger;

  public Scanner(SysdigLogger logger) {
    this.logger = logger;
  }

  public abstract ImageScanningSubmission scanImage(String imageTag, String dockerfile) throws AbortException;
  public abstract JSONObject getGateResults(ImageScanningSubmission submission) throws AbortException;
  public abstract JSONObject getVulnsReport(ImageScanningSubmission submission) throws AbortException;
  protected abstract ImageScanningResult buildImageScanningResult(JSONArray scanReport, JSONObject vulnsReport, String imageDigest, String tag) throws AbortException;
  protected abstract ImageScanningResult buildImageScanningResult(JSONObject scanReport, JSONObject vulnsReport, String imageDigest, String tag) throws AbortException;
  public  abstract ArrayList<ImageScanningResult> scanImages(Map<String, String> imagesAndDockerfiles) throws AbortException;



}
