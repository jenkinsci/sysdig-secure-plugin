package com.sysdig.jenkins.plugins.sysdig.uireport;

import com.google.gson.JsonObject;
import com.sysdig.jenkins.plugins.sysdig.json.GsonBuilder;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.scanner.ImageScanningResult;
import com.sysdig.jenkins.plugins.sysdig.scanner.report.Result;
import hudson.FilePath;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class PolicyReportTest {
  private PolicyEvaluationReportProcessor policyReport;

  @Before
  public void BeforeEach() {
    SysdigLogger logger = mock(SysdigLogger.class);
    policyReport = new PolicyEvaluationReportProcessor(logger);
  }

  @Test
  public void generateGatesArtifact() throws IOException, InterruptedException {
    // Need getAbsolutePath to fix issue in Windows path starting with a / (like "/C:/..." )
    byte[] data = IOUtils.toByteArray(getClass().getResourceAsStream("gates1.json"));
    Result result = GsonBuilder.build().fromJson(new String(data, StandardCharsets.UTF_8), Result.class);

    // Given
    var imageScanningResult = new ImageScanningResult("foo-tag1", "foo-digest1", "pass", result.getPackages().orElseThrow(), result.getPolicyEvaluations().orElseThrow());

    File tmp = File.createTempFile("gatesreport", "");
    tmp.deleteOnExit();

    // When
    policyReport.processPolicyEvaluation(imageScanningResult, new FilePath(tmp));

    // Then
    byte[] reportData = Files.readAllBytes(Paths.get(tmp.getAbsolutePath()));
    var reportFromDisk = GsonBuilder.build().fromJson(new String(reportData, StandardCharsets.UTF_8), JsonObject.class);
    assertEquals(reportFromDisk.getAsJsonObject("foo-digest1").getAsJsonObject("result").getAsJsonArray("rows").asList().size(), 45);
  }
}
