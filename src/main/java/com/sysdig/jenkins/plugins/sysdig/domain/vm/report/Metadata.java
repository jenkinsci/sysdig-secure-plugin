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
import java.util.Optional;

public class Metadata implements Serializable {
  private String pullString;
  private String imageId;
  private String digest;
  private String baseOs;
  private Long size;
  private String os;
  private String architecture;
  private Long layersCount;
  private String createdAt;

  public Optional<String> getPullString() {
    return Optional.ofNullable(pullString);
  }

  public void setPullString(String pullString) {
    this.pullString = pullString;
  }

  public Optional<String> getImageId() {
    return Optional.ofNullable(imageId);
  }

  public void setImageId(String imageId) {
    this.imageId = imageId;
  }

  public Optional<String> getDigest() {
    return Optional.ofNullable(digest);
  }

  public void setDigest(String digest) {
    this.digest = digest;
  }

  public Optional<String> getBaseOs() {
    return Optional.ofNullable(baseOs);
  }

  public void setBaseOs(String baseOs) {
    this.baseOs = baseOs;
  }

  public Optional<Long> getSize() {
    return Optional.ofNullable(size);
  }

  public void setSize(Long size) {
    this.size = size;
  }

  public Optional<String> getOs() {
    return Optional.ofNullable(os);
  }

  public void setOs(String os) {
    this.os = os;
  }

  public Optional<String> getArchitecture() {
    return Optional.ofNullable(architecture);
  }

  public void setArchitecture(String architecture) {
    this.architecture = architecture;
  }

  public Optional<Long> getLayersCount() {
    return Optional.ofNullable(layersCount);
  }

  public void setLayersCount(Long layersCount) {
    this.layersCount = layersCount;
  }

  public Optional<String> getCreatedAt() {
    return Optional.ofNullable(createdAt);
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }
}
