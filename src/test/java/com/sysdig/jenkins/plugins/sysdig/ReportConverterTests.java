package com.sysdig.jenkins.plugins.sysdig;

import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.scanner.ImageScanningResult;
import hudson.AbortException;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReportConverterTests {

  private ReportConverter converter;

  @Before
  public void BeforeEach() {
    SysdigLogger logger = mock(SysdigLogger.class);
    Run<?,?> build = mock(Run.class);
    when(build.getNumber()).thenReturn(0);
    FilePath ws = mock(FilePath.class);
    converter = new ReportConverter(logger);
  }

  @Test
  public void reportFinalActionPass() throws AbortException {
    // Given
    List<ImageScanningResult> results = new ArrayList<>();
    results.add(new ImageScanningResult("foo-tag1", "foo-digest1", "pass", new JSONObject(), new JSONObject()));
    results.add(new ImageScanningResult("foo-tag2", "foo-digest2", "pass", new JSONObject(), new JSONObject()));

    // Then
    assertEquals(Util.GATE_ACTION.PASS, converter.getFinalAction(results));
  }

  @Test
  public void reportFinalActionFail() throws AbortException {
    // Given
    List<ImageScanningResult> results = new ArrayList<>();
    results.add(new ImageScanningResult("foo-tag1", "foo-digest1", "pass", new JSONObject(), new JSONObject()));
    results.add(new ImageScanningResult("foo-tag2", "foo-digest2", "fail", new JSONObject(), new JSONObject()));

    // Then
    assertEquals(Util.GATE_ACTION.FAIL, converter.getFinalAction(results));
  }

  @Test
  public void generateGatesArtifact() throws IOException, InterruptedException {
    // Given
    List<ImageScanningResult> results = new ArrayList<>();
    ClassLoader classLoader = getClass().getClassLoader();
    byte[] data = Files.readAllBytes(Paths.get(classLoader.getResource("com/sysdig/jenkins/plugins/sysdig/ReportConverterTests/gates.json").getFile()));
    JSONObject gatesReport = (JSONObject) JSONSerializer.toJSON(new String(data, StandardCharsets.UTF_8));
    data = Files.readAllBytes(Paths.get(classLoader.getResource("com/sysdig/jenkins/plugins/sysdig/ReportConverterTests/gates2.json").getFile()));
    JSONObject gatesReport2 = (JSONObject) JSONSerializer.toJSON(new String(data, StandardCharsets.UTF_8));

    results.add(new ImageScanningResult("foo-tag1", "foo-digest1", "pass", gatesReport, new JSONObject()));
    results.add(new ImageScanningResult("foo-tag2", "foo-digest2", "pass", gatesReport2, new JSONObject()));

    File tmp = File.createTempFile("gatesreport", "");
    tmp.deleteOnExit();

    // When
    converter.processPolicyEvaluation(results, new FilePath(tmp));

    // Then
    byte[] reportData = Files.readAllBytes(Paths.get(tmp.getAbsolutePath()));
    JSONObject processedReport = (JSONObject) JSONSerializer.toJSON(new String(reportData, StandardCharsets.UTF_8));
    assertEquals(gatesReport.get("foodigest1"), processedReport.get("foodigest1"));
    assertEquals(gatesReport2.get("foodigest2"), processedReport.get("foodigest2"));
  }

  @Test
  public void generateVulnerabilitiesArtifact() throws IOException, InterruptedException {
    byte[] data;

    // Given
    List<ImageScanningResult> results = new ArrayList<>();
    ClassLoader classLoader = getClass().getClassLoader();

    data = Files.readAllBytes(Paths.get(classLoader.getResource("com/sysdig/jenkins/plugins/sysdig/ReportConverterTests/vulns1.json").getFile()));
    JSONObject vulnsReport = (JSONObject) JSONSerializer.toJSON(new String(data, StandardCharsets.UTF_8));
    results.add(new ImageScanningResult("foo-tag1", "foo-digest1", "pass", new JSONObject(), vulnsReport));

    data = Files.readAllBytes(Paths.get(classLoader.getResource("com/sysdig/jenkins/plugins/sysdig/ReportConverterTests/vulns2.json").getFile()));
    JSONObject vulnsReport2 = (JSONObject) JSONSerializer.toJSON(new String(data, StandardCharsets.UTF_8));
    results.add(new ImageScanningResult("foo-tag2", "foo-digest2", "pass", new JSONObject(), vulnsReport2));

    File tmp = File.createTempFile("vulnerabilitiesreport", "");
    tmp.deleteOnExit();

    // When
    converter.processVulnerabilities(results, new FilePath(tmp));

    // Then
    byte[] reportData = Files.readAllBytes(Paths.get(tmp.getAbsolutePath()));
    JSONObject processedReport = (JSONObject) JSONSerializer.toJSON(new String(reportData, StandardCharsets.UTF_8));
    assertEquals("Vulnerability Package", processedReport.getJSONArray("columns").getJSONObject(3).get("title"));
    assertEquals(vulnsReport.getJSONArray("vulnerabilities").getJSONObject(0).get("package"), processedReport.getJSONArray("data").getJSONArray(0).get(3));
    assertEquals(vulnsReport2.getJSONArray("vulnerabilities").getJSONObject(0).get("package"), processedReport.getJSONArray("data").getJSONArray(1).get(3));
  }

}
