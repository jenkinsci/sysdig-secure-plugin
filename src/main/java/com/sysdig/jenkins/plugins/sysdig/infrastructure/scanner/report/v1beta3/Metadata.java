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

public class Metadata implements Serializable {
  private String pullString;
  private String imageId;

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

}
