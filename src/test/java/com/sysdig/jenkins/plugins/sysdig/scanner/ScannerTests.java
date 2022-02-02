package com.sysdig.jenkins.plugins.sysdig.scanner;

import com.sysdig.jenkins.plugins.sysdig.BuildConfig;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.AbortException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;

import static org.junit.Assert.*;

public class ScannerTests {

  class MockScanner extends Scanner {
    private Map<String, ImageScanningSubmission> submissions;
    private Map<String, JSONArray> gateResults;
    private Map<String, JSONObject> vulnsReport;

    public MockScanner(BuildConfig config, SysdigLogger logger) {
      super(config, logger);
      this.submissions = new HashMap<>();
      this.gateResults = new HashMap<>();
      this.vulnsReport = new HashMap<>();
    }

    @Override
    public ImageScanningSubmission scanImage(String imageTag, String dockerfile) throws AbortException {
      return this.submissions.get(imageTag);
    }

    @Override
    public JSONArray getGateResults(ImageScanningSubmission submission) throws AbortException {
      return this.gateResults.get(submission.getTag());
    }

    @Override
    public JSONObject getVulnsReport(ImageScanningSubmission submission) throws AbortException {
      return this.vulnsReport.get(submission.getTag());
    }

    public void setSubmission(String imageTag, ImageScanningSubmission submission) {
      this.submissions.put(imageTag, submission);
    }

    public void setGateResults(String imageTag, JSONArray gateResults) {
      this.gateResults.put(imageTag, gateResults);
    }

    public void setVulnsReport(String imageTag, JSONObject vulnsReport) {
      this.vulnsReport.put(imageTag, vulnsReport);
    }

  }

  MockScanner scanner;

  @Before
  public void BeforeEach() {
    BuildConfig config = mock(BuildConfig.class);
    PrintStream logger = mock(PrintStream.class);
    this.scanner = new MockScanner(config, mock(SysdigLogger.class));
  }

  @Test
  public void testParsingReports() throws AbortException {
    Map<String, String> imagesAndDockerfiles = new HashMap<>();
    imagesAndDockerfiles.put("image:tag", null);

    scanner.setSubmission("image:tag", new ImageScanningSubmission("image:tag", "some-digest"));
    scanner.setGateResults("image:tag", JSONArray.fromObject("[{\"some-digest\": {\"random:image-tag\" : [{\"status\": \"foo\", \"detail\" : {\"result\": {\"result\": { \"foo\": \"bar\"}},  \"policy\": { \"policies\": [{ \"policyName\": \"default\",\"policyId\": \"default\"}] }}}]}}]"));
    scanner.setVulnsReport("image:tag", JSONObject.fromObject("{\"vulns\": \"blahblah\"}"));

    ArrayList<ImageScanningResult> results = scanner.scanImages(imagesAndDockerfiles);

    assertEquals("foo", results.get(0).getEvalStatus());
    assertEquals(JSONObject.fromObject("{\"foo\": \"bar\"}"), results.get(0).getGateResult());
    assertEquals(JSONArray.fromObject("[{ \"policyName\": \"default\",\"policyId\": \"default\"}]"), results.get(0).getGatePolicies());
    assertEquals(JSONObject.fromObject("{\"vulns\": \"blahblah\"}"), results.get(0).getVulnerabilityReport());
  }

  @Test
  public void testDigestMismatchInReportIsIgnored() throws AbortException {
    Map<String, String> imagesAndDockerfiles = new HashMap<>();
    imagesAndDockerfiles.put("image:tag", null);

    scanner.setSubmission("image:tag", new ImageScanningSubmission("image:tag", "other-digest"));
    scanner.setGateResults("image:tag", JSONArray.fromObject("[{\"some-digest\": {\"random:image-tag\" : [{\"status\": \"foo\", \"detail\" : {\"result\": {\"result\": { \"foo\": \"bar\"}},  \"policy\": { \"policies\": [{ \"policyName\": \"default\",\"policyId\": \"default\"}] }}}]}}]"));
    scanner.setVulnsReport("image:tag", JSONObject.fromObject("{\"vulns\": \"blahblah\"}"));

    ArrayList<ImageScanningResult> results = scanner.scanImages(imagesAndDockerfiles);

    assertEquals("foo", results.get(0).getEvalStatus());
  }

  @Test
  public void testMultipleImages() throws AbortException {
    Map<String, String> imagesAndDockerfiles = new HashMap<>();
    imagesAndDockerfiles.put("image1:tag1", null);
    imagesAndDockerfiles.put("image2:tag2", null);

    scanner.setSubmission("image1:tag1", new ImageScanningSubmission("image1:tag1", "some-digest"));
    scanner.setGateResults("image1:tag1", JSONArray.fromObject("[{\"some-digest\": {\"random:image-tag\" : [{\"status\": \"foo1\", \"detail\" : {\"result\": {\"result\": { \"foo\": \"bar\"}},  \"policy\": { \"policies\": [{ \"policyName\": \"default\",\"policyId\": \"default\"}] }}}]}}]"));
    scanner.setVulnsReport("image1:tag1", JSONObject.fromObject("{\"vulns\": \"blahblah1\"}"));

    scanner.setSubmission("image2:tag2", new ImageScanningSubmission("image2:tag2", "some-digest"));
    scanner.setGateResults("image2:tag2", JSONArray.fromObject("[{\"some-digest\": {\"random:image-tag\" : [{\"status\": \"foo2\", \"detail\" : {\"result\": {\"result\": { \"foo\": \"bar\"}},  \"policy\": { \"policies\": [{ \"policyName\": \"default\",\"policyId\": \"default\"}] }}}]}}]"));
    scanner.setVulnsReport("image2:tag2", JSONObject.fromObject("{\"vulns\": \"blahblah2\"}"));

    ArrayList<ImageScanningResult> results = scanner.scanImages(imagesAndDockerfiles);

    assertEquals(2, results.size());
    ImageScanningResult one = results.get(0);
    ImageScanningResult two = results.get(1);
    assertNotEquals(one, two);
    results.forEach(entry -> {
      if (entry.getTag() == "image1:tag1") {
        assertEquals("foo1", entry.getEvalStatus());
      } else {
        assertEquals("foo2", entry.getEvalStatus());
      }
    });
  }

}
