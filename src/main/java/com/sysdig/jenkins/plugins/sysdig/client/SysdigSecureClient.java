package com.sysdig.jenkins.plugins.sysdig.client;

import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;

import java.io.File;
import java.io.Serializable;
import java.util.Optional;

public interface SysdigSecureClient extends Serializable {
  ImageScanningSubmission submitImageForScanning(String tag, String dockerFile) throws ImageScanningException;

  ImageScanningSubmission submitImageForScanning(String imageID, String imageName, String imageDigest, File scanningResult) throws ImageScanningException;

  Optional<ImageScanningResult> retrieveImageScanningResults(String tag, String imageDigest) throws ImageScanningException;
  ImageScanningVulnerabilities retrieveImageScanningVulnerabilities(String tag, String imageDigest) throws ImageScanningException;
  String getScanningAccount() throws ImageScanningException;
}
