package com.sysdig.jenkins.plugins.sysdig.domain.vm.report;

import com.sysdig.jenkins.plugins.sysdig.domain.AggregateChild;

import java.net.URL;
import java.time.Duration;
import java.util.Date;
import java.util.Objects;

public class ScanInfo implements AggregateChild<ScanResult> {
  private final Date scanStart;
  private final Duration scanDuration;
  private final URL remoteResult;
  private final String resultID;
  private final ScanResult root;

  public ScanInfo(
    Date scanStart,
    Duration scanDuration,
    URL remoteResult,
    String resultID,
    ScanResult root
  ) {
    this.scanStart = scanStart;
    this.scanDuration = scanDuration;
    this.remoteResult = remoteResult;
    this.resultID = resultID;
    this.root = root;
  }

  public Date scanStart() {
    return scanStart;
  }

  public Duration scanDuration() {
    return scanDuration;
  }

  public URL remoteResult() {
    return remoteResult;
  }

  public String resultID() {
    return resultID;
  }

  @Override
  public ScanResult root() {
    return root;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (ScanInfo) obj;
    return Objects.equals(this.scanStart, that.scanStart) &&
      Objects.equals(this.scanDuration, that.scanDuration) &&
      Objects.equals(this.remoteResult, that.remoteResult) &&
      Objects.equals(this.resultID, that.resultID);
  }

  @Override
  public int hashCode() {
    return Objects.hash(scanStart, scanDuration, remoteResult, resultID);
  }
}
