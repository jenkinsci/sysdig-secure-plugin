package com.sysdig.jenkins.plugins.sysdig.domain.vm;

public interface ImageScanner {
  ImageScanningResult scanImage(String imageTag) throws InterruptedException;
}
