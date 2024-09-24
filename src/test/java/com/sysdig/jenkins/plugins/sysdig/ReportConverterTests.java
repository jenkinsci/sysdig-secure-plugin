package com.sysdig.jenkins.plugins.sysdig;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.sysdig.jenkins.plugins.sysdig.json.GsonBuilder;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.scanner.ImageScanningResult;
import com.sysdig.jenkins.plugins.sysdig.scanner.report.Package;
import com.sysdig.jenkins.plugins.sysdig.scanner.report.Result;
import com.sysdig.jenkins.plugins.sysdig.uireport.VulnerabilityReport;
import hudson.FilePath;
import hudson.model.Run;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReportConverterTests {

  private ReportConverter converter;

  @Before
  public void BeforeEach() {
    SysdigLogger logger = mock(SysdigLogger.class);
    Run<?, ?> build = mock(Run.class);
    when(build.getNumber()).thenReturn(0);
    FilePath ws = mock(FilePath.class);
    converter = new ReportConverter(logger);
  }

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
    results.forEach(result -> assertEquals(Util.GATE_ACTION.PASS, converter.getFinalAction(result)));
  }

  @Test
  public void reportFinalActionFail() {
    // Given
    var result = new ImageScanningResult("foo-tag2", "foo-digest2", "fail", List.of(), List.of());

    // Then
    assertEquals(Util.GATE_ACTION.FAIL, converter.getFinalAction(result));
  }

  @Test
  public void generateGatesArtifact() throws IOException, InterruptedException {
    // Need getAbsolutePath to fix issue in Windows path starting with a / (like "/C:/..." )
    byte[] data = IOUtils.toByteArray(getClass().getResourceAsStream("ReportConverterTests/gates1.json"));
    Result result = GsonBuilder.build().fromJson(new String(data, StandardCharsets.UTF_8), Result.class);

    // Given
    var imageScanningResult = new ImageScanningResult("foo-tag1", "foo-digest1", "pass", result.getPackages().orElseThrow(), result.getPolicyEvaluations().orElseThrow());

    File tmp = File.createTempFile("gatesreport", "");
    tmp.deleteOnExit();

    // When
    converter.processPolicyEvaluation(imageScanningResult, new FilePath(tmp));

    // Then
    byte[] reportData = Files.readAllBytes(Paths.get(tmp.getAbsolutePath()));
    var reportFromDisk = GsonBuilder.build().fromJson(new String(reportData, StandardCharsets.UTF_8), JsonObject.class);
    assertEquals(reportFromDisk.getAsJsonObject("foo-digest1").getAsJsonObject("result").getAsJsonArray("rows").asList().size(), 45);
  }

  @Test
  public void generateVulnerabilitiesArtifact() throws IOException, InterruptedException {
    // Need getAbsolutePath to fix issue in Windows path starting with a / (like "/C:/..." )
    byte[] data = IOUtils.toByteArray(getClass().getResourceAsStream("ReportConverterTests/vulns1.json"));

    Type listType = new TypeToken<List<Package>>() {}.getType();
    List<Package> vulnsReport = GsonBuilder.build().fromJson(new String(data, StandardCharsets.UTF_8), listType);
    File tmp = File.createTempFile("vulnerabilitiesreport", "");
    tmp.deleteOnExit();

    // Given
    ImageScanningResult imageScanningResult = new ImageScanningResult("foo-tag1", "foo-digest1", "pass", vulnsReport, List.of());

    // When
    VulnerabilityReport.processVulnerabilities(imageScanningResult, new FilePath(tmp));

    // Then
    byte[] reportData = Files.readAllBytes(Paths.get(tmp.getAbsolutePath()));
    JSONObject processedReport = (JSONObject) JSONSerializer.toJSON(new String(reportData, StandardCharsets.UTF_8));
    assertEquals("Vulnerability Package", processedReport.getJSONArray("columns").getJSONObject(3).get("title"));
    assertEquals(vulnsReport.get(1).getName().orElseThrow(), processedReport.getJSONArray("data").getJSONArray(0).get(3));
  }
}
