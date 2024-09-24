package com.sysdig.jenkins.plugins.sysdig.scanner.report;

import java.io.Serializable;
import java.util.Optional;

public class JsonScanResult implements Serializable {
  private ScanInfo scanInfo;
  private Scanner scanner;
  private Result result;

  public Optional<ScanInfo> getScanInfo() {
    return Optional.ofNullable(scanInfo);
  }

  public void setScanInfo(ScanInfo scanInfo) {
    this.scanInfo = scanInfo;
  }

  public Optional<Scanner> getScanner() {
    return Optional.ofNullable(scanner);
  }

  public void setScanner(Scanner scanner) {
    this.scanner = scanner;
  }

  public Optional<Result> getResult() {
    return Optional.ofNullable(result);
  }

  public void setResult(Result result) {
    this.result = result;
  }
}
