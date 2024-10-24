package com.sysdig.jenkins.plugins.sysdig.domain.vm;

import java.io.IOException;

public interface ScanResultArchiver {
  void archiveScanResult(ImageScanningResult scanResult) throws IOException, InterruptedException;
}
