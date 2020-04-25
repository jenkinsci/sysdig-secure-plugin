package com.sysdig.jenkins.plugins.sysdig.client;

public class ImageScanningException extends Exception {
  public ImageScanningException() {
    super();
  }

  public ImageScanningException(String s) {
    super(s);
  }

  public ImageScanningException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public ImageScanningException(Throwable throwable) {
    super(throwable);
  }

  protected ImageScanningException(String s, Throwable throwable, boolean b, boolean b1) {
    super(s, throwable, b, b1);
  }
}
