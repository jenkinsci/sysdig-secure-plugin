package com.sysdig.jenkins.plugins.sysdig.domain.vm.report;


import java.math.BigInteger;
import java.net.URL;
import java.time.Duration;
import java.util.Date;
import java.util.Map;

public class ScanResult {
  private ScanInfo scanInfo;
  private ScannerInfo scanner;
  private Result result;

  private ScanResult() {
  }

  public ScanInfo scanInfo() {
    return scanInfo;
  }

  public ScannerInfo scanner() {
    return scanner;
  }

  public Result result() {
    return result;
  }

  public static ScanResultBuilder createScanResult() {
    return new ScanResultBuilder();
  }

  public static class ScanResultBuilder {
    private ScanInfo scanInfo;
    private ScannerInfo scanner;
    private Result result;
    private final ScanResult scanResult;

    ScanResultBuilder() {
      this.scanResult = new ScanResult();
    }

    public ScanResultBuilder withScanInfo(Date scanStart, Duration scanDuration, URL remoteResult, String resultID) {
      this.scanInfo = new ScanInfo(scanStart, scanDuration, remoteResult, resultID, this.scanResult);
      return this;
    }

    public ScanResultBuilder withScannerInfo(String name, String version) {
      this.scanner = new ScannerInfo(name, version, this.scanResult);
      return this;
    }

    public ScanResultBuilder withResult(ScanType type, String pullString, String imageID, String digest, OperatingSystem baseOS, BigInteger sizeInBytes, Architecture architecture, Map<String, String> labels, Date createdAt) {
      Metadata metadata = new Metadata(pullString, imageID, digest, baseOS, sizeInBytes, architecture, labels, createdAt, this.scanResult);
      this.result = new Result(type, metadata, this.scanResult);
      return this;
    }

    public ScanResult build() {
      if (this.scanInfo == null) {
        throw new IllegalStateException("scan info has not been specified");
      }

      if (this.scanner == null) {
        throw new IllegalStateException("scanner info has not been specified");
      }

      if (this.result == null) {
        throw new IllegalStateException("result info has not been specified");
      }

      scanResult.scanInfo = scanInfo;
      scanResult.scanner = scanner;
      scanResult.result = result;
      return scanResult;
    }
  }
}
