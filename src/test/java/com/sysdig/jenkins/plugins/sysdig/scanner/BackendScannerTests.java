package com.sysdig.jenkins.plugins.sysdig.scanner;

import com.sysdig.jenkins.plugins.sysdig.BuildConfig;
import com.sysdig.jenkins.plugins.sysdig.client.BackendScanningClientFactory;
import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningException;
import com.sysdig.jenkins.plugins.sysdig.client.SysdigSecureClient;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.AbortException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class BackendScannerTests {
  //TODO: Test error handling on API

  //TODO: Secure client is received at factory

  //TODO: Verify Token is received at factory

  //TODO: Verify URL is received at factory

  //TODO: Verify client is created with proxy if master is configured to use proxy

  @Rule
  public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  private final static String IMAGE_TO_SCAN = "foo:latest";
  private final static String IMAGE_DIGEST = "foo-digest";

  private OldEngineScanner scanner = null;
  private SysdigSecureClient client;

  @Before
  public void BeforeEach() throws ImageScanningException {
    BuildConfig config = mock(BuildConfig.class);
    BackendScanningClientFactory clientFactory = mock(BackendScanningClientFactory.class);
    this.client = mock(SysdigSecureClient.class);
    when(clientFactory.newInsecureClient(any(), any(), any())).thenReturn(client);

    SysdigLogger logger = mock(SysdigLogger.class);

    BackendScanner.setBackendScanningClientFactory(clientFactory);
    this.scanner = new BackendScanner(config, logger);
  }

  private void setupMocks() throws ImageScanningException {
    when(client.submitImageForScanning(eq(IMAGE_TO_SCAN), any(), any(), anyBoolean())).thenReturn(IMAGE_DIGEST);
  }

    @Test
  public void testImageIsScanned() throws ImageScanningException, AbortException,InterruptedException {
    setupMocks();

    // When
    ImageScanningSubmission submission = this.scanner.scanImage(IMAGE_TO_SCAN, null);

    // Then
    verify(client, times(1)).submitImageForScanning(eq(IMAGE_TO_SCAN), any(), any(), anyBoolean());
    assertEquals(IMAGE_TO_SCAN, submission.getTag());
    assertEquals(IMAGE_DIGEST, submission.getImageDigest());
  }


  @Test
  public void testNoDockerfilePosted() throws ImageScanningException, IOException, InterruptedException {
    setupMocks();

    // When
    this.scanner.scanImage(IMAGE_TO_SCAN, null);

    // Then
    verify(client, times(1)).submitImageForScanning(
      eq(IMAGE_TO_SCAN),
      isNull(),
      any(),
      anyBoolean());
  }

  @Test
  public void testDockerfilePosted() throws ImageScanningException, IOException, InterruptedException {
    setupMocks();

    //Given
    byte[] dockerfileBytes = "foo content of dockerfile".getBytes(StandardCharsets.UTF_8);
    //Given
    File f = File.createTempFile("test", "");
    FileUtils.writeByteArrayToFile(f, dockerfileBytes);

    // When
    this.scanner.scanImage(IMAGE_TO_SCAN, f.getAbsolutePath());

    // Then
    verify(client, times(1)).submitImageForScanning(
      eq(IMAGE_TO_SCAN),
      eq( new String(Base64.encodeBase64(dockerfileBytes), StandardCharsets.UTF_8)),
      any(),
      anyBoolean());

  }

  @Test
  public void testGetGateResults() throws ImageScanningException, AbortException, InterruptedException {
    setupMocks();

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
  public void testGetVulnsReport() throws ImageScanningException, AbortException, InterruptedException {
    setupMocks();

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

  @Test
  public void addedByAnnotationsAreIncluded() throws ImageScanningException, AbortException, InterruptedException {
    setupMocks();

    // When
    this.scanner.scanImage(IMAGE_TO_SCAN, null);

    // Then
    verify(client, times(1)).submitImageForScanning(
      any(),
      any(),
      argThat(annotations ->
        annotations.containsKey("added-by") && annotations.get("added-by").equals("cicd-scan-request")),
      anyBoolean());
  }

  @Test
  public void testNonExistingDockerfile() throws AbortException {
    // When
    AbortException thrown = assertThrows(
      AbortException.class,
      () -> this.scanner.scanImage(IMAGE_TO_SCAN, "non-existing-Dockerfile"));

    assertEquals("Dockerfile 'non-existing-Dockerfile' does not exist", thrown.getMessage());
  }

}
