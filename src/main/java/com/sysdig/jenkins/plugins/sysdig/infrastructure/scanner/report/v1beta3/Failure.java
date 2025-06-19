/*
Copyright (C) 2016-2024 Sysdig

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1beta3;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

public class Failure implements Serializable {
  private String remediation;
  @SerializedName("Arguments")
  private Map<String, Object> arguments;
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

  public Optional<Map<String, Object>> getArguments() {
    return Optional.ofNullable(arguments);
  }

  public void setArguments(Map<String, Object> arguments) {
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
