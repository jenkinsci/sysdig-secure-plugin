package com.sysdig.jenkins.plugins.sysdig.domain.report;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

public class Failure implements Serializable {
  private String remediation;
  @SerializedName("Arguments")
  private Map<String, String> arguments;
  private Long pkgIndex;
  private Long vulnInPkgIndex;
  private String ref;
  private String description;

  public Optional<String> getRemediation() {
    return Optional.ofNullable(remediation);
  }

  public void setRemediation(String remediation) {
    this.remediation = remediation;
  }

  public Optional<Map<String, String>> getArguments() {
    return Optional.ofNullable(arguments);
  }

  public void setArguments(Map<String, String> arguments) {
    this.arguments = arguments;
  }

  public Optional<Long> getPkgIndex() {
    return Optional.ofNullable(pkgIndex);
  }

  public void setPkgIndex(Long pkgIndex) {
    this.pkgIndex = pkgIndex;
  }

  public Optional<Long> getVulnInPkgIndex() {
    return Optional.ofNullable(vulnInPkgIndex);
  }

  public void setVulnInPkgIndex(Long vulnInPkgIndex) {
    this.vulnInPkgIndex = vulnInPkgIndex;
  }

  public Optional<String> getRef() {
    return Optional.ofNullable(ref);
  }

  public void setRef(String ref) {
    this.ref = ref;
  }

  public Optional<String> getDescription() {
    return Optional.ofNullable(description);
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
