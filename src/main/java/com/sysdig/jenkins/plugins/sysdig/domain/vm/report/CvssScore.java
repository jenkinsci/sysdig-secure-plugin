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

public class CvssScore implements Serializable {
  private Value value;
  private String sourceName;

  public Optional<Value> getValue() {
    return Optional.ofNullable(value);
  }

  public void setValue(Value value) {
    this.value = value;
  }

  public Optional<String> getSourceName() {
    return Optional.ofNullable(sourceName);
  }

  public void setSourceName(String sourceName) {
    this.sourceName = sourceName;
  }
}
