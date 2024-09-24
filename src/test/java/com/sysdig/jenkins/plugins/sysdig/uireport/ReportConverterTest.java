package com.sysdig.jenkins.plugins.sysdig.uireport;

import com.sysdig.jenkins.plugins.sysdig.Util;
import com.sysdig.jenkins.plugins.sysdig.scanner.ImageScanningResult;
import hudson.FilePath;
import hudson.model.Run;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReportConverterTest {
  @Test
  public void reportFinalActionPass() {
    // Given
    var results = List.of(
      new ImageScanningResult("foo-tag1", "foo-digest1", "pass", List.of(), List.of()),
      new ImageScanningResult("foo-tag2", "foo-digest2", "passed", List.of(), List.of()),
      new ImageScanningResult("foo-tag3", "foo-digest2", "accepted", List.of(), List.of()),
      new ImageScanningResult("foo-tag4", "foo-digest2", "ACCEPTED", List.of(), List.of()),
      new ImageScanningResult("foo-tag5", "foo-digest2", "noPolicy", List.of(), List.of())
    );

    // Then
    results.forEach(result -> assertEquals(Util.GATE_ACTION.PASS, ReportConverter.getFinalAction(result)));
  }

  @Test
  public void reportFinalActionFail() {
    // Given
    var result = new ImageScanningResult("foo-tag2", "foo-digest2", "fail", List.of(), List.of());

    // Then
    assertEquals(Util.GATE_ACTION.FAIL, ReportConverter.getFinalAction(result));
  }
}
