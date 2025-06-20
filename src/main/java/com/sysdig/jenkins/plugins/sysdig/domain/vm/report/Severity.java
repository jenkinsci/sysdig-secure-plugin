package com.sysdig.jenkins.plugins.sysdig.domain.vm.report;

public class Severity {
  private final float cvssScore;

  public Severity(float cvssScore) {
    if (cvssScore < 0 || cvssScore > 10) {
      throw new IllegalArgumentException("CVSS out of range (0-10)");
    }

    this.cvssScore = cvssScore;
  }

  public String severityString() {
    if (cvssScore >= 9) return "Critical";
    else if (cvssScore >= 7) return "High";
    else if (cvssScore >= 5) return "Medium";
    else if (cvssScore >= 3) return "Low";
    else return "Negligible";
  }
}
