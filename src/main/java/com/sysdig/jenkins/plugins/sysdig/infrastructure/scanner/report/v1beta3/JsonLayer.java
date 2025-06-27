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

import java.util.List;
import java.util.Map;
import java.util.Optional;

class JsonLayer {
  private String digest;
  private Long size;
  private String command;
  private Map<String, Object> runningVulns;
  private List<?> baseImages;

  public Optional<String> getDigest() {
    return Optional.ofNullable(digest);
  }

  public void setDigest(String digest) {
    this.digest = digest;
  }

  public Optional<Long> getSize() {
    return Optional.ofNullable(size);
  }

  public void setSize(Long size) {
    this.size = size;
  }

  public Optional<String> getCommand() {
    return Optional.ofNullable(command);
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public Optional<Map<String, Object>> getRunningVulns() {
    return Optional.ofNullable(runningVulns);
  }

  public void setRunningVulns(Map<String, Object> runningVulns) {
    this.runningVulns = runningVulns;
  }

  public Optional<List<?>> getBaseImages() {
    return Optional.ofNullable(baseImages);
  }

  public void setBaseImages(List<?> baseImages) {
    this.baseImages = baseImages;
  }
}
