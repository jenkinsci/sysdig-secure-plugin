package com.sysdig.jenkins.plugins.sysdig;

import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.scanner.ImageScanningResult;
import hudson.AbortException;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReportConverterTests {

  private BuildWorker worker;
  private ReportConverter converter;

  @Before
  public void BeforeEach() throws Exception {
    TaskListener listener = mock(TaskListener.class);
    SysdigLogger logger = mock(SysdigLogger.class);
    Run<?,?> build = mock(Run.class);
    when(build.getNumber()).thenReturn(0);
    FilePath ws = mock(FilePath.class);
    worker = new BuildWorker(build, ws, listener, logger);

    converter = new ReportConverter(logger);
  }

  @Test
  public void testReportFinalActionPass() throws AbortException {
    // Given
    List<ImageScanningResult> results = new ArrayList<>();
    results.add(new ImageScanningResult("foo-tag1", "foo-digest1", "pass", new JSONObject(), new JSONObject()));
    results.add(new ImageScanningResult("foo-tag2", "foo-digest2", "pass", new JSONObject(), new JSONObject()));

    // Then
    assertEquals(Util.GATE_ACTION.PASS, converter.processPolicyEvaluation(results));
  }

  @Test
  public void testReportFinalActionFail() throws AbortException {
    // Given
    List<ImageScanningResult> results = new ArrayList<>();
    results.add(new ImageScanningResult("foo-tag1", "foo-digest1", "pass", new JSONObject(), new JSONObject()));
    results.add(new ImageScanningResult("foo-tag2", "foo-digest2", "fail", new JSONObject(), new JSONObject()));

    // Then
    assertEquals(Util.GATE_ACTION.FAIL, converter.processPolicyEvaluation(results));
  }

}
