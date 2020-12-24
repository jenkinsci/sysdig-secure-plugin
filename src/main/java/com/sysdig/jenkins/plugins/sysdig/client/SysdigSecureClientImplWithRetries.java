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

import com.sysdig.jenkins.plugins.sysdig.ImageScanningException;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.security.InvalidParameterException;
import java.util.Map;

public class SysdigSecureClientImplWithRetries implements SysdigSecureClient {

  private final SysdigSecureClient sysdigSecureClient;
  private final int retries;
  private final int sleepSeconds;
  private final SysdigLogger logger;

  public SysdigSecureClientImplWithRetries(SysdigSecureClient sysdigSecureClient, SysdigLogger logger, int retries, int sleepSeconds) {
    if (retries <= 0) {
      throw new InvalidParameterException("the number of retries must be higher than 0");
    }
    this.sysdigSecureClient = sysdigSecureClient;
    this.retries = retries;
    this.sleepSeconds = sleepSeconds;
    this.logger = logger;
  }

  private interface RunnableFunction<T> {
    T run() throws ImageScanningException, InterruptedException;
  }

  private <T> T executeWithRetriesAndBackoff(RunnableFunction<T> function) throws ImageScanningException, InterruptedException {
    ImageScanningException lastException = new ImageScanningException("the number of retries is negative or 0");
    long sleepTime = 0;
    for (int i = 0; i < retries; i++) {
      try {
        Thread.sleep(sleepTime);
        sleepTime += 1000L * sleepSeconds;
        return function.run();
      } catch (ImageScanningException e) {
        logger.logDebug("SysdigClient error in retry number " + i, e);
        lastException = e;
      }
      logger.logDebug("SysdigClient retrying in " + sleepTime + "ms");
    }
    throw lastException;
  }

  @Override
  public String submitImageForScanning(String tag, String dockerFileContents, Map<String, String> annotations) throws ImageScanningException, InterruptedException {
    return executeWithRetriesAndBackoff(() ->
        sysdigSecureClient.submitImageForScanning(tag, dockerFileContents, annotations)
      );
  }

  @Override
  public JSONArray retrieveImageScanningResults(String tag, String imageDigest) throws ImageScanningException, InterruptedException {
    return executeWithRetriesAndBackoff(() ->
        sysdigSecureClient.retrieveImageScanningResults(tag, imageDigest)
      );
  }

  @Override
  public JSONObject retrieveImageScanningVulnerabilities(String imageDigest) throws ImageScanningException, InterruptedException {
    return executeWithRetriesAndBackoff(() ->
        sysdigSecureClient.retrieveImageScanningVulnerabilities(imageDigest)
      );
  }

}
