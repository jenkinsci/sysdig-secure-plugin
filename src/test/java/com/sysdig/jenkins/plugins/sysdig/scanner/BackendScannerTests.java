package com.sysdig.jenkins.plugins.sysdig.scanner;

import com.sysdig.jenkins.plugins.sysdig.BuildConfig;
import com.sysdig.jenkins.plugins.sysdig.client.BackendScanningClientFactory;
import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningException;
import com.sysdig.jenkins.plugins.sysdig.client.SysdigSecureClient;
import hudson.AbortException;
import hudson.Launcher;
import hudson.model.TaskListener;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.io.PrintStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class BackendScannerTests {
  //TODO: Mock the client (inject as dependency?) and check it posts image, checks results, retrieves reports
  //TODO: Test error handling on API
  //TODO: Secure client

  @Rule
  public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  private final static String IMAGE_TO_SCAN = "foo:latest";
  private final static String IMAGE_DIGEST = "foo-digest";

  private final BuildConfig config = new BuildConfig("name", true, true, false, true, "", "foo", true);
  private Scanner scanner = null;
  private SysdigSecureClient client;

  @Before
  public void BeforeEach() throws ImageScanningException {
    Launcher launcher = mock(Launcher.class);
    TaskListener listener = mock(TaskListener.class);
    BackendScanningClientFactory clientFactory = mock(BackendScanningClientFactory.class);
    this.client = mock(SysdigSecureClient.class);
    when(clientFactory.newClient(any(), any(), any())).thenReturn(client);

    PrintStream logger = mock(PrintStream.class);
    when(listener.getLogger()).thenReturn((logger));

    when(client.submitImageForScanning(eq(IMAGE_TO_SCAN), any(), any())).thenReturn(IMAGE_DIGEST);

    this.scanner = new BackendScanner(launcher, listener, config, clientFactory);
  }

  @Test
  public void testImageIsScanned() throws ImageScanningException, AbortException {
    //Given

    // When
    ImageScanningSubmission submission = this.scanner.scanImage(IMAGE_TO_SCAN, null);

    // Then
    verify(client, times(1)).submitImageForScanning(eq(IMAGE_TO_SCAN), any(), any());
    assertEquals(IMAGE_TO_SCAN, submission.getTag());
    assertEquals(IMAGE_DIGEST, submission.getImageDigest());
  }

  @Test
  public void testGetGateResults() throws ImageScanningException, AbortException {
    //Given
    JSONArray returnedGateResults = new JSONArray();
    JSONObject someJSON = new JSONObject();
    someJSON.put("foo-key", "foo-value");
    returnedGateResults.add(someJSON);
    when(client.retrieveImageScanningResults(IMAGE_TO_SCAN, IMAGE_DIGEST)).thenReturn(returnedGateResults);

    // When
    ImageScanningSubmission submission = this.scanner.scanImage(IMAGE_TO_SCAN, null);
    JSONArray gateResults = this.scanner.getGateResults(submission);

    // Then
    assertEquals(returnedGateResults.toString(), gateResults.toString());
  }

  @Test
  public void testGetVulnsReport() throws ImageScanningException, AbortException {
    // Given
    JSONObject returnedVulnsReport = new JSONObject();
    returnedVulnsReport.put("foo-key", "foo-value");
    when(client.retrieveImageScanningVulnerabilities(IMAGE_DIGEST)).thenReturn(returnedVulnsReport);

    // When
    ImageScanningSubmission submission = this.scanner.scanImage(IMAGE_TO_SCAN, null);
    JSONObject vulnsReport = this.scanner.getVulnsReport(submission);

    // Then
    assertEquals(returnedVulnsReport.toString(), vulnsReport.toString());
  }


  //TODO: Annotation added-by=cicd-scan-request is added

  @Test
  public void addedByAnnotationsAreIncluded() throws ImageScanningException, AbortException {
    // When
    this.scanner.scanImage(IMAGE_TO_SCAN, null);

    // Then
    verify(client, times(1)).submitImageForScanning(
      any(),
      any(),
      argThat(annotations ->
        annotations.containsKey("added-by") && annotations.get("added-by").equals("cicd-scan-request")));
  }

}
