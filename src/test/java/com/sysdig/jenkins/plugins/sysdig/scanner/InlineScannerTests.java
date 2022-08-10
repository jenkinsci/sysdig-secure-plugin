package com.sysdig.jenkins.plugins.sysdig.scanner;

import com.sysdig.jenkins.plugins.sysdig.BuildConfig;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import java.io.PrintStream;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class InlineScannerTests {

  private final String IMAGE_TO_SCAN = "foo:latest";

  private FilePath workspace = null;
  private OldEngineScanner scanner = null;

  //TODO: Handle exception in channel.call

  TaskListener listener;

  @Before
  public void BeforeEach() {
    this.workspace = mock(FilePath.class);
    BuildConfig config = mock(BuildConfig.class);
    listener = mock(TaskListener.class);
    PrintStream logger = mock(PrintStream.class);
    when(listener.getLogger()).thenReturn((logger));
    this.scanner = new InlineScanner(listener, config, workspace, new EnvVars(), mock(SysdigLogger.class));
  }

  @Test
  public void testImageIsScanned() throws Throwable {
    // Given
    JSONObject output = new JSONObject();
    output.put("digest", "foo-digest");
    output.put("tag", IMAGE_TO_SCAN);

    when(workspace.act(ArgumentMatchers.<Callable<String, Exception>>any())).thenReturn(output.toString());

    // When
    ImageScanningSubmission submission = this.scanner.scanImage(IMAGE_TO_SCAN, null);

    // Then
    assertEquals(IMAGE_TO_SCAN, submission.getTag());
  }

  @Test
  public void testAbortIfNoWorkspace() {
    //Given
    this.scanner = new InlineScanner(listener, mock(BuildConfig.class), null, new EnvVars(), mock(SysdigLogger.class));

    // When
    AbortException thrown = assertThrows(
      AbortException.class,
      () -> this.scanner.scanImage(IMAGE_TO_SCAN, null));

    // Then
    assertTrue(thrown.getMessage().contains("workspace"));
    //TODO: Check exception registered in log
  }

  @Test
  public void testNoTagInOutput() throws Throwable {
    // Given
    JSONObject output = new JSONObject();
    output.put("digest", "foo-digest");

    when(workspace.act(ArgumentMatchers.<Callable<String, Exception>>any())).thenReturn(output.toString());

    // When
    AbortException thrown = assertThrows(
      AbortException.class,
      () -> this.scanner.scanImage(IMAGE_TO_SCAN, null));

    // Then
    assertTrue(thrown.getMessage().contains("Failed to perform"));
    //TODO: Check exception registered in log
  }

  @Test
  public void testNoDigestInOutput() throws Throwable {
    // Given
    JSONObject output = new JSONObject();
    output.put("tag", IMAGE_TO_SCAN);

    when(workspace.act(ArgumentMatchers.<Callable<String, Exception>>any())).thenReturn(output.toString());

    // When
    AbortException thrown = assertThrows(
      AbortException.class,
      () -> this.scanner.scanImage(IMAGE_TO_SCAN, null));

    // Then
    assertTrue(thrown.getMessage().contains("Failed to perform"));
    //TODO: Check exception registered in log
  }

  @Test
  public void testGetGateResults() throws Throwable {
    // Given
    JSONArray returnedGateResults = new JSONArray();
    JSONObject someJSON = new JSONObject();
    someJSON.put("foo-key", "foo-value");
    returnedGateResults.add(someJSON);

    JSONObject output = new JSONObject();
    output.put("digest", "foo-digest");
    output.put("tag", IMAGE_TO_SCAN);
    output.put("scanReport", returnedGateResults);

    when(workspace.act(ArgumentMatchers.<Callable<String, Exception>>any())).thenReturn(output.toString());

    // When
    ImageScanningSubmission submission = this.scanner.scanImage(IMAGE_TO_SCAN, null);
    JSONArray gateResults = this.scanner.getGateResults(submission);

    // Then
    assertEquals(returnedGateResults.toString(), gateResults.toString());
  }

  @Test
  public void testGetVulnsReport() throws Throwable {
    // Given
    JSONObject returnedVulnsReport = new JSONObject();
    returnedVulnsReport.put("foo-key", "foo-value");

    JSONObject output = new JSONObject();
    output.put("digest", "foo-digest");
    output.put("tag", IMAGE_TO_SCAN);
    output.put("vulnsReport", returnedVulnsReport);

    when(workspace.act(ArgumentMatchers.<Callable<String, Exception>>any())).thenReturn(output.toString());

    // Do
    ImageScanningSubmission submission = this.scanner.scanImage(IMAGE_TO_SCAN, null);
    JSONObject vulnsReport = this.scanner.getVulnsReport(submission);

    // Then
    assertEquals(returnedVulnsReport.toString(), vulnsReport.toString());
  }

}
