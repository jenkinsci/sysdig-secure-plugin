package com.sysdig.jenkins.plugins.sysdig.domain.report;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

public class Vuln implements Serializable {
  private String name;
  private Severity severity;
  private CvssScore cvssScore;
  private String disclosureDate;
  private String solutionDate;
  private Boolean exploitable;
  private String fixedInVersion;
  private PublishDateByVendor publishDateByVendor;
  private Map<String, String> annotations;

  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  public void setName(String name) {
    this.name = name;
  }

  public Optional<Severity> getSeverity() {
    return Optional.ofNullable(severity);
  }

  public void setSeverity(Severity severity) {
    this.severity = severity;
  }

  public Optional<CvssScore> getCvssScore() {
    return Optional.ofNullable(cvssScore);
  }

  public void setCvssScore(CvssScore cvssScore) {
    this.cvssScore = cvssScore;
  }

  public Optional<String> getDisclosureDate() {
    return Optional.ofNullable(disclosureDate);
  }

  public void setDisclosureDate(String disclosureDate) {
    this.disclosureDate = disclosureDate;
  }

  public Optional<String> getSolutionDate() {
    return Optional.ofNullable(solutionDate);
  }

  public void setSolutionDate(String solutionDate) {
    this.solutionDate = solutionDate;
  }

  public Optional<Boolean> getExploitable() {
    return Optional.ofNullable(exploitable);
  }

  public void setExploitable(Boolean exploitable) {
    this.exploitable = exploitable;
  }

  public Optional<String> getFixedInVersion() {
    return Optional.ofNullable(fixedInVersion);
  }

  public void setFixedInVersion(String fixedInVersion) {
    this.fixedInVersion = fixedInVersion;
  }

  public Optional<PublishDateByVendor> getPublishDateByVendor() {
    return Optional.ofNullable(publishDateByVendor);
  }

  public void setPublishDateByVendor(PublishDateByVendor publishDateByVendor) {
    this.publishDateByVendor = publishDateByVendor;
  }

  public Optional<Map<String, String>> getAnnotations() {
    return Optional.ofNullable(annotations);
  }

  public void setAnnotations(Map<String, String> annotations) {
    this.annotations = annotations;
  }
}
