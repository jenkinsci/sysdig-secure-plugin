package com.sysdig.jenkins.plugins.sysdig;

import com.google.common.base.Strings;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.scanner.ImageScanningResult;
import com.sysdig.jenkins.plugins.sysdig.scanner.report.Bundle;
import com.sysdig.jenkins.plugins.sysdig.scanner.report.PolicyEvaluation;
import com.sysdig.jenkins.plugins.sysdig.scanner.report.Predicate;
import com.sysdig.jenkins.plugins.sysdig.scanner.report.Rule;
import hudson.FilePath;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// FIXME(fede): Wow this is unmaintainable. We should not be handling jsons but report structures.
public class ReportConverter {
  private static final String LEGACY_PASSED_STATUS = "pass";
  private static final String PASSED_STATUS = "passed";
  private static final String ACCEPTED_STATUS = "accepted";
  private static final String NO_POLICY_STATUS = "noPolicy";

  protected final SysdigLogger logger;

  public ReportConverter(SysdigLogger logger) {
    this.logger = logger;
  }


  public Util.GATE_ACTION getFinalAction(ImageScanningResult result) {
    String evalStatus = result.getEvalStatus();
    logger.logDebug(String.format("Get policy evaluation status for image '%s': %s", result.getTag(), evalStatus));

    if (Stream.of(LEGACY_PASSED_STATUS, PASSED_STATUS, ACCEPTED_STATUS, NO_POLICY_STATUS).anyMatch(evalStatus::equalsIgnoreCase)) {
      return Util.GATE_ACTION.PASS;
    }

    return Util.GATE_ACTION.FAIL;
  }




}
