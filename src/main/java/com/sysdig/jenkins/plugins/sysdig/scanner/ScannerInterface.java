package com.sysdig.jenkins.plugins.sysdig.scanner;

import hudson.AbortException;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

public interface ScannerInterface {
  ImageScanningSubmission scanImage(String imageTag, String dockerfile) throws AbortException, InterruptedException;

  JSONObject getGateResults(ImageScanningSubmission submission) throws AbortException;

  JSONObject getVulnsReport(ImageScanningSubmission submission) throws AbortException;

  ImageScanningResult buildImageScanningResult(JSONObject scanReport, JSONObject vulnsReport, String imageDigest, String tag) throws AbortException;

  ArrayList<ImageScanningResult> scanImages(Map<String, String> imagesAndDockerfiles) throws AbortException, InterruptedException;

}
