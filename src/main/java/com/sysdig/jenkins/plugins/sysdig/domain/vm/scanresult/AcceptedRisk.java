package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

import com.sysdig.jenkins.plugins.sysdig.domain.AggregateChild;

import java.io.Serializable;
import java.util.*;

public class AcceptedRisk implements AggregateChild<ScanResult>, Serializable {
  private final String id;
  private final AcceptedRiskReason reason;
  private final String description;
  private final Date expirationDate;
  private final boolean isActive;
  private final Date createdAt;
  private final Date updatedAt;
  private final Set<Vulnerability> assignedToVulnerabilities;
  private final Set<Package> assignedToPackages;
  private final ScanResult root;

  public AcceptedRisk(String id, AcceptedRiskReason reason, String description, Date expirationDate, boolean isActive, Date createdAt, Date updatedAt, ScanResult root) {
    this.id = id;
    this.reason = reason;
    this.description = description;
    this.expirationDate = expirationDate;
    this.isActive = isActive;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.assignedToVulnerabilities = new HashSet<>();
    this.assignedToPackages = new HashSet<>();
    this.root = root;
  }

  public String id() {
    return id;
  }

  public AcceptedRiskReason reason() {
    return reason;
  }

  public String description() {
    return description;
  }

  public Date expirationDate() {
    return expirationDate;
  }

  public boolean isActive() {
    return isActive;
  }

  public Date createdAt() {
    return createdAt;
  }

  public Date updatedAt() {
    return updatedAt;
  }

  public void addForVulnerability(Vulnerability vulnerability) {
    if (assignedToVulnerabilities.add(vulnerability)) {
      vulnerability.addAcceptedRisk(this);
    }
  }

  public Set<Vulnerability> assignedToVulnerabilities() {
    return Collections.unmodifiableSet(assignedToVulnerabilities);
  }

  public void addForPackage(Package aPackage) {
    if (assignedToPackages.add(aPackage)) {
      aPackage.addAcceptedRisk(this);
    }
  }

  public Set<Package> assignedToPackages() {
    return Collections.unmodifiableSet(assignedToPackages);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    AcceptedRisk that = (AcceptedRisk) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public ScanResult root() {
    return root;
  }
}
