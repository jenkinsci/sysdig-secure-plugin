package com.sysdig.jenkins.plugins.sysdig.domain;

import com.sysdig.jenkins.plugins.sysdig.domain.vm.ImageScanningResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageScanningResultTest {

  @Test
  void reportFinalActionPassForDifferentEvalStatuses() {
    // Given
    var expectedEvalStatusToSucceed = Stream.of("pass", "passed", "accepted", "ACCEPTED", "noPolicy");
    var results = expectedEvalStatusToSucceed.map(evalStatus -> new ImageScanningResult("foo-tag", "foo-digest", evalStatus, List.of(), List.of()));

    // Then
    results.forEach(result -> assertEquals(ImageScanningResult.FinalAction.ActionPass, result.getFinalAction(), String.format("%s eval status doesn't match", result.getEvalStatus())));
  }

  @Test
  void reportFinalActionFailForOtherEvalStatuses() {
    // Given
    var expectedEvalStatusToFail = Stream.of("fail", "random", "", "123423");
    var results = expectedEvalStatusToFail.map(evalStatus -> new ImageScanningResult("foo-tag", "foo-digest", evalStatus, List.of(), List.of()));

    // Then
    results.forEach(result -> assertEquals(ImageScanningResult.FinalAction.ActionFail, result.getFinalAction(), String.format("%s eval status doesn't match", result.getEvalStatus())));
  }
}