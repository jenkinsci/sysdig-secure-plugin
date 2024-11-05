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

public class Extra implements Serializable {
  private String level;
  private String key;
  private Long age;
  private String value;
  private String user;

  public Optional<String> getLevel() {
    return Optional.ofNullable(level);
  }

  public void setLevel(String level) {
    this.level = level;
  }

  public Optional<String> getKey() {
    return Optional.ofNullable(key);
  }

  public void setKey(String key) {
    this.key = key;
  }

  public Optional<Long> getAge() {
    return Optional.ofNullable(this.age);
  }

  public void setAge(Long age) {
    this.age = age;
  }

  public Optional<String> getValue() {
    return Optional.ofNullable(this.value);
  }

  public void setValue(String value) {
    this.value = value;
  }

  public Optional<String> getUser() {
    return Optional.ofNullable(this.user);
  }

  public void setUser(String user) {
    this.user = user;
  }
}
