package com.sysdig.jenkins.plugins.sysdig.scanner;

import com.sysdig.jenkins.plugins.sysdig.BuildConfig;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.AbortException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class ScannerTests {

  //TODO: Test the parent Scanner class that contains the parsing of the JSON into ImageScanningResult

  //TODO: Test scan multiple images and collect the results (scanner.scanImages)

  private static class mockScanner extends Scanner {

    public mockScanner(BuildConfig config, SysdigLogger logger) {
      super(config, logger);
    }

    @Override
    public ImageScanningSubmission scanImage(String imageTag, String dockerfile) {
      return null;
    }

    @Override
    public JSONArray getGateResults(ImageScanningSubmission submission) {
      return null;
    }

    @Override
    public JSONObject getVulnsReport(ImageScanningSubmission submission) {
      return null;
    }
  }

  @Test
  public void errorIfDockerFileDoesNotExist() {
    HashMap<String,String> imagesAndDockerfiles = new HashMap<>();
    imagesAndDockerfiles.put("some-image", "non-existing-dockerfile");
    Scanner scanner = new mockScanner(null, null);

    // When
    AbortException thrown = assertThrows(
      AbortException.class,
      () -> scanner.scanImages(imagesAndDockerfiles));

    assertThat(thrown.getMessage(), CoreMatchers.containsString("Dockerfile"));
    assertThat(thrown.getMessage(), CoreMatchers.containsString("for image 'some-image'"));
    assertThat(thrown.getMessage(), CoreMatchers.containsString("does not exist"));
  }
}
