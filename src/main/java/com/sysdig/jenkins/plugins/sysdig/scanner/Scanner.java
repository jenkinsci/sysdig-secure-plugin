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

package com.sysdig.jenkins.plugins.sysdig.scanner;

import com.google.common.base.Strings;
import com.sysdig.jenkins.plugins.sysdig.BuildConfig;
import com.sysdig.jenkins.plugins.sysdig.ImageScanningException;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.AbortException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

public abstract class Scanner {

  protected final BuildConfig config;
  protected final SysdigLogger logger;

  public Scanner(BuildConfig config, SysdigLogger logger) {
    this.config = config;
    this.logger = logger;
  }

  public abstract ImageScanningSubmission scanImage(String imageTag, String dockerfile) throws ImageScanningException, InterruptedException;
  public abstract JSONArray getGateResults(ImageScanningSubmission submission) throws ImageScanningException;
  public abstract JSONObject getVulnsReport(ImageScanningSubmission submission) throws ImageScanningException;

  public ArrayList<ImageScanningResult> scanImages(Map<String, String> imagesAndDockerfiles) throws AbortException, ImageScanningException, InterruptedException {
    if (imagesAndDockerfiles == null) {
      return new ArrayList<>();
    }

    ArrayList<ImageScanningResult> resultList = new ArrayList<>();

    //TODO(airadier): We could run this in parallel
    for (Map.Entry<String, String> entry : imagesAndDockerfiles.entrySet()) {
      String dockerfile = entry.getValue();
      if (!Strings.isNullOrEmpty(dockerfile)) {
        File f = new File(dockerfile);
        if (!f.exists()) {
          throw new AbortException("Dockerfile '" + dockerfile + "' for image '" + entry.getKey() + "' does not exist");
        }
      }

      ImageScanningSubmission submission = this.scanImage(entry.getKey(), dockerfile);

      JSONArray scanReport = this.getGateResults(submission);
      JSONObject vulnsReport = this.getVulnsReport(submission);

      ImageScanningResult result = this.buildImageScanningResult(scanReport, vulnsReport, submission.getImageDigest(), submission.getTag());
      resultList.add(result);
    }

    return resultList;
  }

  private ImageScanningResult buildImageScanningResult(JSONArray scanReport, JSONObject vulnsReport, String imageDigest, String tag) {
    JSONObject tagEvalObj = JSONObject.fromObject(scanReport.get(0)).getJSONObject(imageDigest);
    JSONArray tagEvals = null;
    for (Object key : tagEvalObj.keySet()) {
      tagEvals = tagEvalObj.getJSONArray((String) key);
      break;
    }

    String evalStatus = null;
    JSONObject gateResult;
    if (tagEvals == null || tagEvals.size() < 1) {
      gateResult = new JSONObject();
      logger.logError("Failed to analyze image '" + tag + "' due to missing tag eval records in sysdig-secure-engine policy evaluation response");
    } else {
      gateResult = tagEvals.getJSONObject(0).getJSONObject("detail").getJSONObject("result").getJSONObject("result");
      evalStatus = tagEvals.getJSONObject(0).getString("status");
    }

    return new ImageScanningResult(tag, imageDigest, evalStatus, gateResult, vulnsReport);
  }

}
