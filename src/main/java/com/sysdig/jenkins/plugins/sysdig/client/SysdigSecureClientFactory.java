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

public class SysdigSecureClientFactory implements BackendScanningClientFactory {
  private final static int CLIENT_RETRIES = 10;
  private final static int SLEEP_SECONDS = 5;

  public SysdigSecureClient newClient(String token, String apiURL, SysdigLogger logger) {
    SysdigSecureClientImpl client = new SysdigSecureClientImpl(token, apiURL, true, logger);
    return new SysdigSecureClientImplWithRetries(client, logger, CLIENT_RETRIES, SLEEP_SECONDS);
  }

  public SysdigSecureClient newInsecureClient(String token, String apiURL, SysdigLogger logger) {
    SysdigSecureClientImpl client = new SysdigSecureClientImpl(token, apiURL, false, logger);
    return new SysdigSecureClientImplWithRetries(client, logger, CLIENT_RETRIES, SLEEP_SECONDS);
  }
}
