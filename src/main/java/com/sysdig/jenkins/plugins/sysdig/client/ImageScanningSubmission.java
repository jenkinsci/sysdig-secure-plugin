package com.sysdig.jenkins.plugins.sysdig.client;

public class ImageScanningSubmission {
  private final String tag;
  private final String imageDigest;

  public ImageScanningSubmission(String tag, String imageDigest) {
    this.tag = tag;
    this.imageDigest = imageDigest;
  }

  public String getTag() {
    return tag;
  }

  public String getImageDigest() {
    return imageDigest;
  }
}
