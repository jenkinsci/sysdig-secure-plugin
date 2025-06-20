package com.sysdig.jenkins.plugins.sysdig.domain.vm.report;

import com.sysdig.jenkins.plugins.sysdig.domain.AggregateChild;

import java.util.Objects;

public class ScannerInfo implements AggregateChild<ScanResult> {
  private final String name;
  private final String version;
  private final ScanResult root;

  public ScannerInfo(String name, String version, ScanResult root) {
    this.name = name;
    this.version = version;
    this.root = root;
  }

  public String name() {
    return name;
  }

  public String version() {
    return version;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (ScannerInfo) obj;
    return Objects.equals(this.name, that.name) &&
      Objects.equals(this.version, that.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, version);
  }

  @Override
  public String toString() {
    return "ScannerInfo[" +
      "name=" + name + ", " +
      "version=" + version + ']';
  }


  @Override
  public ScanResult root() {
    return root;
  }
}
