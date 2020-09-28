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

import java.awt.*;
import java.io.File;
import java.security.InvalidParameterException;
import java.util.Optional;
import java.util.function.Function;

public class SysdigSecureClientImplWithRetries implements SysdigSecureClient {

  private SysdigSecureClient sysdigSecureClient;
  private int retries;

  public SysdigSecureClientImplWithRetries(SysdigSecureClient sysdigSecureClient, int retries) {
    if (retries <= 0) {
      throw new InvalidParameterException("the number of retries must be higher than 0");
    }
    this.sysdigSecureClient = sysdigSecureClient;
    this.retries = retries;
  }

  private interface RunnableFunction {
    public Object run() throws ImageScanningException;
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
  public ImageScanningSubmission submitImageForScanning(String tag, String dockerFile) throws ImageScanningException {
    return (ImageScanningSubmission)
      executeWithRetriesAndBackoff(() ->
        sysdigSecureClient.submitImageForScanning(tag, dockerFile)
      );
  }

  @Override
  public ImageScanningSubmission submitImageForScanning(String imageID, String imageName, String imageDigest, File scanningResult) throws ImageScanningException {
    return (ImageScanningSubmission)
      executeWithRetriesAndBackoff(() ->
        sysdigSecureClient.submitImageForScanning(imageID, imageName, imageDigest, scanningResult)
      );
  }

  @Override
  public ImageScanningResult retrieveImageScanningResults(String tag, String imageDigest) throws ImageScanningException {
    return (ImageScanningResult)
      executeWithRetriesAndBackoff(() ->
        sysdigSecureClient.retrieveImageScanningResults(tag, imageDigest)
      );
  }

  @Override
  public ImageScanningVulnerabilities retrieveImageScanningVulnerabilities(String tag, String imageDigest) throws ImageScanningException {
    return (ImageScanningVulnerabilities)
      executeWithRetriesAndBackoff(() ->
       sysdigSecureClient.retrieveImageScanningVulnerabilities(tag, imageDigest)
      );
  }

  @Override
  public String getScanningAccount() throws ImageScanningException {
    return (String)
    executeWithRetriesAndBackoff(() ->
      sysdigSecureClient.getScanningAccount()
    );
  }
}
