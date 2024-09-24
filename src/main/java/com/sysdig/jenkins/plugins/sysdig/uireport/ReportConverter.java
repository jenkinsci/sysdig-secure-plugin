package com.sysdig.jenkins.plugins.sysdig.uireport;

import com.sysdig.jenkins.plugins.sysdig.Util;
import com.sysdig.jenkins.plugins.sysdig.scanner.ImageScanningResult;

import java.util.stream.Stream;

// FIXME(fede): Wow this is unmaintainable. We should not be handling jsons but report structures.
public class ReportConverter {
  private static final String LEGACY_PASSED_STATUS = "pass";
  private static final String PASSED_STATUS = "passed";
  private static final String ACCEPTED_STATUS = "accepted";
  private static final String NO_POLICY_STATUS = "noPolicy";

  public static Util.GATE_ACTION getFinalAction(ImageScanningResult result) {
    String evalStatus = result.getEvalStatus();

    if (Stream.of(LEGACY_PASSED_STATUS, PASSED_STATUS, ACCEPTED_STATUS, NO_POLICY_STATUS).anyMatch(evalStatus::equalsIgnoreCase)) {
      return Util.GATE_ACTION.PASS;
    }

    return Util.GATE_ACTION.FAIL;
  }
}
