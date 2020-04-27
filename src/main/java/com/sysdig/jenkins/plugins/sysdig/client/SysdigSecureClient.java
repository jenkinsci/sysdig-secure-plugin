package com.sysdig.jenkins.plugins.sysdig.client;

import java.util.Optional;

public interface SysdigSecureClient {
  ImageScanningSubmission submitImageForScanning(String tag, String dockerFile) throws ImageScanningException;
  Optional<ImageScanningResult> retrieveImageScanningResults(String tag, String imageDigest) throws ImageScanningException;
  ImageScanningVulnerabilities retrieveImageScanningVulnerabilities(String tag, String imageDigest) throws ImageScanningException;
}
