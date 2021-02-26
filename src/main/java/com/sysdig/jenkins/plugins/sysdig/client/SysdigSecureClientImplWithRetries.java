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
import net.sf.json.JSONObject;

import java.security.InvalidParameterException;
import java.util.Map;

public class SysdigSecureClientImplWithRetries implements SysdigSecureClient {

  private final SysdigSecureClient sysdigSecureClient;
  private final int retries;

  public SysdigSecureClientImplWithRetries(SysdigSecureClient sysdigSecureClient, int retries) {
    if (retries <= 0) {
      throw new InvalidParameterException("the number of retries must be higher than 0");
    }
    this.sysdigSecureClient = sysdigSecureClient;
    this.retries = retries;
  }

  private interface RunnableFunction {
    Object run() throws ImageScanningException;
  }

  private Object executeWithRetriesAndBackoff(RunnableFunction function) throws ImageScanningException {
    ImageScanningException lastException = new ImageScanningException("the number of retries is negative or 0");
    long sleepTime = 0;
    for (int i = 0; i < retries; i++) {
      try {
        Thread.sleep(sleepTime);
        sleepTime += 5000;
        return function.run();
      } catch (ImageScanningException e) {
        lastException = e;
      } catch (InterruptedException e) {
        lastException = new ImageScanningException(e);
      }
    }
    throw lastException;
  }

  @Override
  public String submitImageForScanning(String tag, String dockerFileContents, Map<String, String> annotations, boolean forceDockerImage) throws ImageScanningException {
    return (String)
      executeWithRetriesAndBackoff(() ->
        sysdigSecureClient.submitImageForScanning(tag, dockerFileContents, annotations, forceDockerImage)
      );
  }

  @Override
  public JSONArray retrieveImageScanningResults(String tag, String imageDigest) throws ImageScanningException {
    return (JSONArray)
      executeWithRetriesAndBackoff(() ->
        sysdigSecureClient.retrieveImageScanningResults(tag, imageDigest)
      );
  }

  @Override
  public JSONObject retrieveImageScanningVulnerabilities(String imageDigest) throws ImageScanningException {
    return (JSONObject)
      executeWithRetriesAndBackoff(() ->
        sysdigSecureClient.retrieveImageScanningVulnerabilities(imageDigest)
      );
  }

}
