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

public class ScanInfo  implements Serializable {
  private String scanTime;
  private String scanDuration;
  private String resultUrl;
  private String resultId;

  public Optional<String> getScanTime() {
    return Optional.ofNullable(scanTime);
  }

  public void setScanTime(String scanTime) {
    this.scanTime = scanTime;
  }

  public Optional<String> getScanDuration() {
    return Optional.ofNullable(scanDuration);
  }

  public void setScanDuration(String scanDuration) {
    this.scanDuration = scanDuration;
  }

  public Optional<String> getResultUrl() {
    return Optional.ofNullable(resultUrl);
  }

  public void setResultUrl(String resultUrl) {
    this.resultUrl = resultUrl;
  }

  public Optional<String> getResultId() {
    return Optional.ofNullable(resultId);
  }

  public void setResultId(String resultId) {
    this.resultId = resultId;
  }
}
