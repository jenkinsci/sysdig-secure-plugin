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

import net.sf.json.JSONObject;

import java.io.Serializable;

public class ImageScanningResult extends ImageScanningSubmission implements Serializable {
  private final String evalStatus;
  private final JSONObject gateResult;

  public ImageScanningResult(String tag, String imageDigest, String evalStatus, JSONObject gateResult) {
    super(tag, imageDigest);
    this.evalStatus = evalStatus;
    this.gateResult = gateResult;
  }

  public String getEvalStatus() {
    return evalStatus;
  }

  public JSONObject getGateResult() {
    return gateResult;
  }
}
