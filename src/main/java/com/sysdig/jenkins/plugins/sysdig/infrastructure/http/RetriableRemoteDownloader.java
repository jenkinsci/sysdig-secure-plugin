package com.sysdig.jenkins.plugins.sysdig.infrastructure.http;

import com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.RunContext;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.FilePath;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class RetriableRemoteDownloader extends RemoteDownloader {
  private final int maxRetries;
  private final long sleepSeconds;

  public RetriableRemoteDownloader(@NonNull RunContext runContext) {
    super(runContext);
    this.maxRetries = 5;
    this.sleepSeconds = 2;
  }

  @Override
  public FilePath downloadFile(URL url, String fileName) throws IOException, InterruptedException {
    int downloadRetriesLeft = maxRetries;
    while (true) {
      try {
        return super.downloadFile(url, fileName);
      } catch (Exception e) {
        downloadRetriesLeft--;
        if (downloadRetriesLeft > 0) {
          logger.logWarn(String.format("Error downloading the file, retrying in %d seconds... (%d retries left)", sleepSeconds, downloadRetriesLeft));
          TimeUnit.SECONDS.sleep(sleepSeconds);
        } else {
          throw new IOException("Error downloading the executable file", e);
        }
      }
    }
  }
}
