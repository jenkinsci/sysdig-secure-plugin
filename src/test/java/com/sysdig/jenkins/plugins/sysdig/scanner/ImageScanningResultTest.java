package com.sysdig.jenkins.plugins.sysdig.scanner;

import org.junit.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class ImageScanningResultTest {
  @Test
  public void reportFinalActionPassForDifferentEvalStatuses() {
    // Given
    var expectedEvalStatusToSucceed = Stream.of("pass", "passed", "accepted", "ACCEPTED", "noPolicy");
    var results = expectedEvalStatusToSucceed.map(evalStatus -> new ImageScanningResult("foo-tag", "foo-digest", evalStatus, List.of(), List.of()));

    // Then
    results.forEach(result -> assertEquals(String.format("%s eval status doesn't match", result.getEvalStatus()), ImageScanningResult.FinalAction.ActionPass, result.getFinalAction()));
  }

  @Test
  public void reportFinalActionFailForOtherEvalStatuses() {
    // Given
    var expectedEvalStatusToFail = Stream.of("fail", "random", "", "123423");
    var results = expectedEvalStatusToFail.map(evalStatus -> new ImageScanningResult("foo-tag", "foo-digest", evalStatus, List.of(), List.of()));

    // Then
    results.forEach(result -> assertEquals(String.format("%s eval status doesn't match", result.getEvalStatus()), ImageScanningResult.FinalAction.ActionFail, result.getFinalAction()));
  }
}