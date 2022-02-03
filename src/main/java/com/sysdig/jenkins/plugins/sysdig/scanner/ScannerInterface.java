package com.sysdig.jenkins.plugins.sysdig.scanner;

import hudson.AbortException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

public interface ScannerInterface {
  public ImageScanningSubmission scanImage(String imageTag, String dockerfile) throws AbortException;
  public JSONArray getGateResults(ImageScanningSubmission submission) throws AbortException;
  public JSONObject getVulnsReport(ImageScanningSubmission submission) throws AbortException;
  public ArrayList<ImageScanningResult> scanImages(Map<String, String> imagesAndDockerfiles) throws AbortException;
}
