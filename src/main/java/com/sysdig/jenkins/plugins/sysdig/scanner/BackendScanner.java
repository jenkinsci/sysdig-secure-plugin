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
import com.sysdig.jenkins.plugins.sysdig.client.*;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.codec.binary.Base64;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;


public class BackendScanner extends Scanner {

  private static final Map<String, String> annotations = Collections.singletonMap("added-by", "cicd-scan-request");
  private final SysdigSecureClient sysdigSecureClient;

  // Use a default container runner factory, but allow overriding for mocks in tests
  private static BackendScanningClientFactory backendScanningClientFactory = new SysdigSecureClientFactory();
  public static void setBackendScanningClientFactory(BackendScanningClientFactory backendScanningClientFactory) {
    BackendScanner.backendScanningClientFactory = backendScanningClientFactory;
  }

  public BackendScanner(BuildConfig config, SysdigLogger logger) {
    super(config, logger);

    String sysdigToken = config.getSysdigToken();
    this.sysdigSecureClient = config.getEngineTLSVerify() ?
      backendScanningClientFactory.newClient(sysdigToken, config.getEngineURL(), logger) :
      backendScanningClientFactory.newInsecureClient(sysdigToken, config.getEngineURL(), logger);
  }

  @Override
  public ImageScanningSubmission scanImage(String imageTag, String dockerfile) throws ImageScanningException {

    try {
      logger.logInfo(String.format("Submitting %s for analysis", imageTag));
      String dockerFileContents = null;
      if (!Strings.isNullOrEmpty(dockerfile)) {
        byte[] dockerfileBytes = Files.readAllBytes(Paths.get(dockerfile));
        dockerFileContents = new String(Base64.encodeBase64(dockerfileBytes), StandardCharsets.UTF_8);
      }

      String imageDigest = sysdigSecureClient.submitImageForScanning(imageTag, dockerFileContents, annotations);
      logger.logInfo(String.format("Analysis request accepted, received image %s", imageDigest));
      return new ImageScanningSubmission(imageTag, imageDigest);
    } catch (Exception e) {
      throw new ImageScanningException("Failed to add image '" + imageTag + "' due to an unexpected error", e);
    }
  }

  @Override
  public JSONArray getGateResults(ImageScanningSubmission submission) throws ImageScanningException {
    String tag = submission.getTag();
    String imageDigest = submission.getImageDigest();

    try {
      logger.logInfo(String.format("Waiting for analysis of %s with digest %s", tag, imageDigest));
      return sysdigSecureClient.retrieveImageScanningResults(tag, imageDigest);
    } catch (ImageScanningException e) {
      throw new ImageScanningException("Failed to retrieve policy evaluation for image '" + tag + "' digest '" + imageDigest + "' due to an unexpected error", e);
    }
  }

  @Override
  public JSONObject getVulnsReport(ImageScanningSubmission submission) throws ImageScanningException {
    String tag = submission.getTag();
    String imageDigest = submission.getImageDigest();

    try {
      logger.logInfo(String.format("Querying vulnerability listing of %s width digest %s", tag, imageDigest));
      return sysdigSecureClient.retrieveImageScanningVulnerabilities(imageDigest);
    } catch (ImageScanningException e) {
      throw new ImageScanningException("Unable to retrieve vulnerabilities report for tag " + tag + " digest " + imageDigest, e);
    }
  }


}
