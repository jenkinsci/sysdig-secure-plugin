package com.sysdig.jenkins.plugins.sysdig.domain.report;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public class Result implements Serializable {
  private String type;
  private Metadata metadata;
  private VulnTotalBySeverity vulnTotalBySeverity;
  private FixableVulnTotalBySeverity fixableVulnTotalBySeverity;
  private Long exploitsCount;
  private List<Package> packages;
  private List<Layer> layers;
  private List<PolicyEvaluation> policyEvaluations;
  private String policyEvaluationsResult;

  public Optional<String> getType() {
    return Optional.ofNullable(type);
  }

  public void setType(String type) {
    this.type = type;
  }

  public Optional<Metadata> getMetadata() {
    return Optional.ofNullable(metadata);
  }

  public void setMetadata(Metadata metadata) {
    this.metadata = metadata;
  }

  public Optional<VulnTotalBySeverity> getVulnTotalBySeverity() {
    return Optional.ofNullable(vulnTotalBySeverity);
  }

  public void setVulnTotalBySeverity(VulnTotalBySeverity vulnTotalBySeverity) {
    this.vulnTotalBySeverity = vulnTotalBySeverity;
  }

  public Optional<FixableVulnTotalBySeverity> getFixableVulnTotalBySeverity() {
    return Optional.ofNullable(fixableVulnTotalBySeverity);
  }

  public void setFixableVulnTotalBySeverity(FixableVulnTotalBySeverity fixableVulnTotalBySeverity) {
    this.fixableVulnTotalBySeverity = fixableVulnTotalBySeverity;
  }

  public Optional<Long> getExploitsCount() {
    return Optional.ofNullable(exploitsCount);
  }

  public void setExploitsCount(Long exploitsCount) {
    this.exploitsCount = exploitsCount;
  }

  public Optional<List<Package>> getPackages() {
    return Optional.ofNullable(packages);
  }

  public void setPackages(List<Package> packages) {
    this.packages = packages;
  }

  public Optional<List<Layer>> getLayers() {
    return Optional.ofNullable(layers);
  }

  public void setLayers(List<Layer> layers) {
    this.layers = layers;
  }

  public Optional<List<PolicyEvaluation>> getPolicyEvaluations() {
    return Optional.ofNullable(policyEvaluations);
  }

  public void setPolicyEvaluations(List<PolicyEvaluation> policyEvaluations) {
    this.policyEvaluations = policyEvaluations;
  }

  public Optional<String> getPolicyEvaluationsResult() {
    return Optional.ofNullable(policyEvaluationsResult);
  }

  public void setPolicyEvaluationsResult(String policyEvaluationsResult) {
    this.policyEvaluationsResult = policyEvaluationsResult;
  }
}
