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

import java.io.Serializable;
import java.util.Optional;

public class VulnTotalBySeverity implements Serializable {
  private Long critical;
  private Long high;
  private Long low;
  private Long medium;
  private Long negligible;

  public Optional<Long> getCritical() {
    return Optional.ofNullable(critical);
  }

  public void setCritical(Long critical) {
    this.critical = critical;
  }

  public Optional<Long> getHigh() {
    return Optional.ofNullable(high);
  }

  public void setHigh(Long high) {
    this.high = high;
  }

  public Optional<Long> getLow() {
    return Optional.ofNullable(low);
  }

  public void setLow(Long low) {
    this.low = low;
  }

  public Optional<Long> getMedium() {
    return Optional.ofNullable(medium);
  }

  public void setMedium(Long medium) {
    this.medium = medium;
  }

  public Optional<Long> getNegligible() {
    return Optional.ofNullable(negligible);
  }

  public void setNegligible(Long negligible) {
    this.negligible = negligible;
  }
}
