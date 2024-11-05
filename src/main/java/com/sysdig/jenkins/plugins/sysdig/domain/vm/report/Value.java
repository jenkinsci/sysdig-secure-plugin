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

public class Value implements Serializable {
  private String version;
  private Double score;
  private String vector;

  public Optional<String> getVersion() {
    return Optional.ofNullable(version);
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public Optional<Double> getScore() {
    return Optional.ofNullable(score);
  }

  public void setScore(Double score) {
    this.score = score;
  }

  public Optional<String> getVector() {
    return Optional.ofNullable(vector);
  }

  public void setVector(String vector) {
    this.vector = vector;
  }
}
