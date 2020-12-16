package com.sysdig.jenkins.plugins.sysdig.scanner;

import com.sysdig.jenkins.plugins.sysdig.BuildConfig;
import hudson.AbortException;
import hudson.model.TaskListener;
import hudson.Launcher;
import hudson.remoting.VirtualChannel;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintStream;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class InlineScannerTests {

  private final String IMAGE_TO_SCAN = "foo:latest";

  private BuildConfig config;
  private Launcher launcher = null;
  private Scanner scanner = null;

  //TODO: Handle exception in channel.call
  //TODO: Check that tag in ImageSubmission result should match the requested tag, otherwise fail

  @Before
  public void BeforeEach() {
    this.launcher = mock(Launcher.class);
    this.config = mock(BuildConfig.class);
    TaskListener listener = mock(TaskListener.class);
    PrintStream logger = mock(PrintStream.class);
    when(listener.getLogger()).thenReturn((logger));
    this.scanner = new InlineScanner(launcher, listener, config);
  }

  @Test
  public void testImageIsScanned() throws IOException, InterruptedException, Throwable {
    // Given
    JSONObject output = new JSONObject();
    output.put("digest", "foo-digest");
    output.put("tag", IMAGE_TO_SCAN);

    VirtualChannel channel = mock(VirtualChannel.class);
    when(this.launcher.getChannel()).thenReturn(channel);
    when(channel.call(any())).thenReturn(output);

    // When
    ImageScanningSubmission submission = this.scanner.scanImage(IMAGE_TO_SCAN, null);

    // Then
    assertEquals(IMAGE_TO_SCAN, submission.getTag());
  }

  @Test
  public void testAbortIfNoChannel() {
    // When
    AbortException thrown = assertThrows(
      AbortException.class,
      () -> this.scanner.scanImage(IMAGE_TO_SCAN, null));

    // Then
    assertTrue(thrown.getMessage().contains("channel"));
    //TODO: Check exception registered in log
  }

  @Test
  public void testNoTagInOutput() throws Throwable {
    // Given
    JSONObject output = new JSONObject();
    output.put("digest", "foo-digest");

    VirtualChannel channel = mock(VirtualChannel.class);
    when(this.launcher.getChannel()).thenReturn(channel);
    when(channel.call(any())).thenReturn(output);

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

    VirtualChannel channel = mock(VirtualChannel.class);
    when(this.launcher.getChannel()).thenReturn(channel);
    when(channel.call(any())).thenReturn(output);

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

    VirtualChannel channel = mock(VirtualChannel.class);
    when(this.launcher.getChannel()).thenReturn(channel);
    when(channel.call(any())).thenReturn(output);

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

    VirtualChannel channel = mock(VirtualChannel.class);
    when(this.launcher.getChannel()).thenReturn(channel);
    when(channel.call(any())).thenReturn(output);

    // Do
    ImageScanningSubmission submission = this.scanner.scanImage(IMAGE_TO_SCAN, null);
    JSONObject vulnsReport = this.scanner.getVulnsReport(submission);

    // Then
    assertEquals(returnedVulnsReport.toString(), vulnsReport.toString());
  }

}
