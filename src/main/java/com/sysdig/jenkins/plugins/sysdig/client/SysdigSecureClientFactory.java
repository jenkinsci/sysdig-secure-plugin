package com.sysdig.jenkins.plugins.sysdig.client;

import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;

public class SysdigSecureClientFactory implements BackendScanningClientFactory {
  private final static int CLIENT_RETRIES = 10;

  public SysdigSecureClient newClient(String token, String apiURL, SysdigLogger logger) {
    SysdigSecureClientImpl client = new SysdigSecureClientImpl(token, apiURL, true, logger);
    return new SysdigSecureClientImplWithRetries(client, CLIENT_RETRIES);
  }

  public SysdigSecureClient newInsecureClient(String token, String apiURL, SysdigLogger logger) {
    SysdigSecureClientImpl client = new SysdigSecureClientImpl(token, apiURL, false, logger);
    return new SysdigSecureClientImplWithRetries(client, CLIENT_RETRIES);
  }
}
