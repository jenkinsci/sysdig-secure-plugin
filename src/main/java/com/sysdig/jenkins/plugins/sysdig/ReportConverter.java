package com.sysdig.jenkins.plugins.sysdig;

import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.scanner.ImageScanningResult;
import hudson.AbortException;
import hudson.FilePath;
import net.sf.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ReportConverter {
  private final SysdigLogger logger;

  public ReportConverter(SysdigLogger logger) {
    this.logger = logger;
  }

  public Util.GATE_ACTION processPolicyEvaluation(List<ImageScanningResult> results) throws AbortException {
    Util.GATE_ACTION finalAction = Util.GATE_ACTION.PASS;

    for (ImageScanningResult result : results) {
      JSONObject gateResult = result.getGateResult();
      String evalStatus = result.getEvalStatus();

      logger.logDebug(String.format("Get policy evaluation status for image 's': %s", result.getTag(), evalStatus));

      if (!"pass".equals(evalStatus)) {
        finalAction = Util.GATE_ACTION.FAIL;
      }
    }

    return finalAction;
  }
}
