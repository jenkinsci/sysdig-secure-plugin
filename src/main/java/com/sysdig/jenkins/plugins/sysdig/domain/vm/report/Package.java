package com.sysdig.jenkins.plugins.sysdig.domain.vm.report;


import com.sysdig.jenkins.plugins.sysdig.domain.AggregateChild;

import java.util.*;

public class Package implements AggregateChild<ScanResult> {
  private final PackageType type;
  private final String name;
  private final String version;
  private final String path;
  private final Layer foundInLayer;
  private final HashMap<String, Vulnerability> vulnerabilities;
  private final Set<AcceptedRisk> acceptedRisks;
  private final ScanResult root;

  Package(PackageType type, String name, String version, String path, Layer foundInLayer, ScanResult root) {
    this.type = type;
    this.name = name;
    this.version = version;
    this.path = path;
    this.foundInLayer = foundInLayer;
    this.root = root;
    this.vulnerabilities = new HashMap<>();
    this.acceptedRisks = new HashSet<>();
  }

  public PackageType type() {
    return type;
  }

  public String name() {
    return name;
  }

  public String version() {
    return version;
  }

  public String path() {
    return path;
  }

  public Layer foundInLayer() {
    return foundInLayer;
  }

  public Collection<Vulnerability> vulnerabilities() {
    return Collections.unmodifiableCollection(vulnerabilities.values());
  }

  public void addAcceptedRisk(AcceptedRisk acceptedRisk) {
    if (this.acceptedRisks.add(acceptedRisk)) {
      acceptedRisk.addForPackage(this);
    }
  }

  public Set<AcceptedRisk> acceptedRisks() {
    return Collections.unmodifiableSet(acceptedRisks);
  }

  @Override
  public ScanResult root() {
    return root;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Package aPackage = (Package) o;
    return type == aPackage.type && Objects.equals(name, aPackage.name) && Objects.equals(version, aPackage.version) && Objects.equals(path, aPackage.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, name, version, path);
  }
}
