package com.sysdig.jenkins.plugins.sysdig;

import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningSubmission;
import hudson.AbortException;
import hudson.FilePath;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface BuildWorker {
  void runQueries(List<ImageScanningSubmission> submissionList) throws AbortException;

  void setupBuildReports() throws AbortException;

  void cleanup();

  Map<String, String> readImagesAndDockerfilesFromPath(FilePath workspace, String manifestFile) throws AbortException;

  ArrayList<ImageScanningSubmission> scanImages(Map<String, String> imagesAndDockerfiles) throws AbortException;

  Util.GATE_ACTION runGates(List<ImageScanningSubmission> submissionList) throws AbortException;
}
