package com.sysdig.jenkins.plugins.sysdig.client;

import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;

public interface BackendScanningClientFactory {

  SysdigSecureClient newClient(String token, String apiURL, SysdigLogger logger);

  SysdigSecureClient newInsecureClient(String token, String apiURL, SysdigLogger logger);
}
