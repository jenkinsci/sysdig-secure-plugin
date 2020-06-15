/*
Copyright (C) 2016-2020 Sysdig

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
package com.sysdig.jenkins.plugins.sysdig.client;

import net.sf.json.JSONArray;

public class ImageScanningVulnerabilities {
  private JSONArray dataJson;

  public ImageScanningVulnerabilities(JSONArray dataJson) {

    this.dataJson = dataJson;
  }

  public JSONArray getDataJson() {
    return dataJson;
  }

  public void setDataJson(JSONArray dataJson) {
    this.dataJson = dataJson;
  }
}
