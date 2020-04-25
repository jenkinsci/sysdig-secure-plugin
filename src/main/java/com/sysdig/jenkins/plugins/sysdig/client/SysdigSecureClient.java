package com.sysdig.jenkins.plugins.sysdig.client;

public interface SysdigSecureClient {
  ImageScanningSubmission submitImageForScanning(String tag, String dockerFile) throws ImageScanningException;
  ImageScanningResult retrieveImageScanningResults(String tag, String dockerFile) throws ImageScanningException;
}
