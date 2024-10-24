package com.sysdig.jenkins.plugins.sysdig.domain.vm.report;

import java.io.Serializable;
import java.util.Optional;

public class ScanInfo  implements Serializable {
  private String scanTime;
  private String scanDuration;
  private String resultUrl;
  private String resultId;

  public Optional<String> getScanTime() {
    return Optional.ofNullable(scanTime);
  }

  public void setScanTime(String scanTime) {
    this.scanTime = scanTime;
  }

  public Optional<String> getScanDuration() {
    return Optional.ofNullable(scanDuration);
  }

  public void setScanDuration(String scanDuration) {
    this.scanDuration = scanDuration;
  }

  public Optional<String> getResultUrl() {
    return Optional.ofNullable(resultUrl);
  }

  public void setResultUrl(String resultUrl) {
    this.resultUrl = resultUrl;
  }

  public Optional<String> getResultId() {
    return Optional.ofNullable(resultId);
  }

  public void setResultId(String resultId) {
    this.resultId = resultId;
  }
}
