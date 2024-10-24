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

public class JsonScanResult implements Serializable {
  private ScanInfo scanInfo;
  private Scanner scanner;
  private Result result;

  public Optional<ScanInfo> getScanInfo() {
    return Optional.ofNullable(scanInfo);
  }

  public void setScanInfo(ScanInfo scanInfo) {
    this.scanInfo = scanInfo;
  }

  public Optional<Scanner> getScanner() {
    return Optional.ofNullable(scanner);
  }

  public void setScanner(Scanner scanner) {
    this.scanner = scanner;
  }

  public Optional<Result> getResult() {
    return Optional.ofNullable(result);
  }

  public void setResult(Result result) {
    this.result = result;
  }
}
