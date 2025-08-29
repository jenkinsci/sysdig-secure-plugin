package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.diff;

import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.ScanResult;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.Vulnerability;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ScanResultDiff {

  private final List<Vulnerability> vulnerabilitiesAdded;
  private final List<Vulnerability> vulnerabilitiesFixed;

  private ScanResultDiff(ScanResult oldScanResult, ScanResult newScanResult) {
    this.vulnerabilitiesAdded = newScanResult.vulnerabilities().stream()
      .filter(vuln -> !oldScanResult.vulnerabilities().contains(vuln))
      .collect(Collectors.toList());

    this.vulnerabilitiesFixed = oldScanResult.vulnerabilities().stream()
      .filter(vuln -> !newScanResult.vulnerabilities().contains(vuln))
      .collect(Collectors.toList());
  }

  public static ScanResultDiff betweenPreviousAndNew(ScanResult oldScanResult, ScanResult newScanResult) {
      return new ScanResultDiff(oldScanResult, newScanResult);
  }

  public List<Vulnerability> getVulnerabilitiesAdded() {
    return vulnerabilitiesAdded;
  }

  public List<Vulnerability> getVulnerabilitiesFixed() {
    return vulnerabilitiesFixed;
  }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ScanResultDiff that = (ScanResultDiff) o;
        return Objects.equals(vulnerabilitiesAdded, that.vulnerabilitiesAdded) && Objects.equals(vulnerabilitiesFixed, that.vulnerabilitiesFixed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vulnerabilitiesAdded, vulnerabilitiesFixed);
    }
}