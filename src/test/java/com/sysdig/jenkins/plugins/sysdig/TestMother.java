package com.sysdig.jenkins.plugins.sysdig;

import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.ScanResult;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.json.GsonBuilder;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1beta3.JsonScanResult;
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
  public static JsonScanResult rawScanResult() throws IOException {
    var jsonContents = Objects.requireNonNull(TestMother.class.getResourceAsStream("gates1.json"));
    return GsonBuilder.build().fromJson(IOUtils.toString(jsonContents, StandardCharsets.UTF_8), JsonScanResult.class);
  }

  /**
   * Returns a sample ImageScanningResult object for testing.
   *
   * @return a test ImageScanningResult object.
   * @throws IOException if an error occurs during object creation.
   */
  public static ScanResult imageScanResult() throws IOException {
    return rawScanResult().toDomain().get();
  }
}
