package com.sysdig.jenkins.plugins.sysdig;

import com.sysdig.jenkins.plugins.sysdig.domain.vm.ImageScanningResult;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.report.Result;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.json.GsonBuilder;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Provides pre-configured objects for testing, following the Object Mother pattern.
 */
public class TestMother {

  /**
   * Returns a sample Result object for testing.
   *
   * @return a test Result object.
   * @throws IOException if an error occurs during object creation.
   */
  public static Result rawScanResult() throws IOException {
    var jsonContents = Objects.requireNonNull(TestMother.class.getResourceAsStream("gates1.json"));
    return GsonBuilder.build().fromJson(IOUtils.toString(jsonContents, StandardCharsets.UTF_8), Result.class);
  }

  /**
   * Returns a sample ImageScanningResult object for testing.
   *
   * @return a test ImageScanningResult object.
   * @throws IOException if an error occurs during object creation.
   */
  public static ImageScanningResult imageScanResult() throws IOException {
    return ImageScanningResult.fromReportResult(rawScanResult());
  }
}
