package com.sysdig.jenkins.plugins.sysdig.domain;

public interface ImageScanner {
  ImageScanningResult scanImage(String imageTag) throws InterruptedException;
}
