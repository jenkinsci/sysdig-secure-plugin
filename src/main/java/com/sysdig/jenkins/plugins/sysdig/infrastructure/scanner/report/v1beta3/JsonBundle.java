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
import java.util.List;
import java.util.Optional;

public class JsonBundle implements Serializable {
  private String name;
  private String identifier;
  private String type;
  private List<JsonRule> rules;
  private String createdAt;
  private String updatedAt;

  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  public void setName(String name) {
    this.name = name;
  }

  public Optional<String> getIdentifier() {
    return Optional.ofNullable(identifier);
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public Optional<String> getType() {
    return Optional.ofNullable(type);
  }

  public void setType(String type) {
    this.type = type;
  }

  public Optional<List<JsonRule>> getRules() {
    return Optional.ofNullable(rules);
  }

  public void setRules(List<JsonRule> rules) {
    this.rules = rules;
  }

  public Optional<String> getCreatedAt() {
    return Optional.ofNullable(createdAt);
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public Optional<String> getUpdatedAt() {
    return Optional.ofNullable(updatedAt);
  }

  public void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
  }
}
