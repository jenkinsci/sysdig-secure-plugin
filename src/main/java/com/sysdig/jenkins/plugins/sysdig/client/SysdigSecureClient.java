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

import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;

import java.io.File;
import java.io.Serializable;
import java.util.Optional;

public interface SysdigSecureClient extends Serializable {
  ImageScanningSubmission submitImageForScanning(String tag, String dockerFile) throws ImageScanningException;

  ImageScanningSubmission submitImageForScanning(String imageID, String imageName, String imageDigest, File scanningResult) throws ImageScanningException;

  ImageScanningResult retrieveImageScanningResults(String tag, String imageDigest) throws ImageScanningException;
  ImageScanningVulnerabilities retrieveImageScanningVulnerabilities(String tag, String imageDigest) throws ImageScanningException;
  String getScanningAccount() throws ImageScanningException;
}
