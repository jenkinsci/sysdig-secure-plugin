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
package com.sysdig.jenkins.plugins.sysdig.domain.vm.report;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public class Package implements Serializable {
  private String type;
  private String name;
  private String version;
  private String path;
  private String layerDigest;
  private List<Vuln> vulns;
  private String suggestedFix;

  public Optional<String> getType() {
    return Optional.ofNullable(type);
  }

  public void setType(String type) {
    this.type = type;
  }

  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  public void setName(String name) {
    this.name = name;
  }

  public Optional<String> getVersion() {
    return Optional.ofNullable(version);
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public Optional<String> getPath() {
    return Optional.ofNullable(path);
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Optional<String> getLayerDigest() {
    return Optional.ofNullable(layerDigest);
  }

  public void setLayerDigest(String layerDigest) {
    this.layerDigest = layerDigest;
  }

  public Optional<List<Vuln>> getVulns() {
    return Optional.ofNullable(vulns);
  }

  public void setVulns(List<Vuln> vulns) {
    this.vulns = vulns;
  }

  public Optional<String> getSuggestedFix() {
    return Optional.ofNullable(suggestedFix);
  }

  public void setSuggestedFix(String suggestedFix) {
    this.suggestedFix = suggestedFix;
  }
}
