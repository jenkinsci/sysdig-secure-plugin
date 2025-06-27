/*
Copyright (C) 2016-2024 Sysdig

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner;

import com.google.common.base.Strings;
import com.sysdig.jenkins.plugins.sysdig.application.vm.ImageScanningConfig;
import com.sysdig.jenkins.plugins.sysdig.domain.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.ScanResult;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.http.RetriableRemoteDownloader;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.RunContext;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.json.GsonBuilder;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1beta3.JsonScanResult;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.FilePath;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;


public class RemoteSysdigImageScanner {
  private static final String FIXED_SCANNED_VERSION = "1.22.3";
  private final ScannerPaths scannerPaths;
  private final String imageName;
  private final RetriableRemoteDownloader retriableRemoteDownloader;
  private final ImageScanningConfig config;
  private final SysdigLogger logger;
  private final RunContext runContext;

  public RemoteSysdigImageScanner(@NonNull RunContext runContext, RetriableRemoteDownloader retriableRemoteDownloader, String imageName, ImageScanningConfig config) {
    this.runContext = runContext;
    this.imageName = imageName;
    this.retriableRemoteDownloader = retriableRemoteDownloader;
    this.config = config;
    this.scannerPaths = new ScannerPaths(runContext.getPathFromWorkspace());
    this.logger = runContext.getLogger();
  }

  public ScanResult performScan() throws AbortException {
    // Create all the necessary folders to store execution temp files and such
    createExecutionWorkspace();
    // Retrieve the scanner bin file
    final FilePath scannerBinaryFile = retrieveScannerBinFile();
    // Execute the scanner bin file and retrieves its json output
    String imageScanningResultJSON = executeScan(scannerBinaryFile);
    logger.logDebug("Raw scan result as JSON: ");
    logger.logDebug(imageScanningResultJSON);
    JsonScanResult jsonScanResult = GsonBuilder.build().fromJson(imageScanningResultJSON, JsonScanResult.class);

    return jsonScanResult
      .toDomain()
      .orElseThrow(() -> new AbortException(String.format("unable to obtain result from scan: %s", imageScanningResultJSON)));
  }

  private FilePath downloadInlineScan(String latestVersion) throws IOException, UnsupportedOperationException, InterruptedException {
    URL url = sysdigCLIScannerURLForVersion(latestVersion);
    return retriableRemoteDownloader.downloadExecutable(url, String.format("inlinescan-%s.bin", latestVersion));
  }

  private static URL sysdigCLIScannerURLForVersion(String latestVersion) throws MalformedURLException {
    String os = System.getProperty("os.name").toLowerCase().startsWith("mac") ? "darwin" : "linux";
    String arch = System.getProperty("os.arch").toLowerCase().startsWith("aarch64") ? "arm64" : "amd64";
    URL url = new URL("https://download.sysdig.com/scanning/bin/sysdig-cli-scanner/" + latestVersion + "/" + os + "/" + arch + "/sysdig-cli-scanner");
    return url;
  }

  private String getInlineScanPinnedVersion() {
    return FIXED_SCANNED_VERSION;
  }

  private String getInlineScanVersion() {
    if (Strings.isNullOrEmpty(this.config.getCliVersionToApply())) {
      return getInlineScanPinnedVersion();
    }

    if (!this.config.getCliVersionToApply().equals("custom")) {
      return getInlineScanPinnedVersion();
    }

    if (this.config.getCustomCliVersion().isEmpty()) {
      return getInlineScanPinnedVersion();
    }

    return this.config.getCustomCliVersion();
  }

  private void createExecutionWorkspace() throws AbortException {
    try {
      this.scannerPaths.create();
    } catch (Exception e) {
      logger.logError("Unable to create scanner execution workspace", e);
      throw new AbortException("Unable to create scanner execution workspace");
    }
  }

  private FilePath retrieveScannerBinFile() throws AbortException {
    if (!Strings.isNullOrEmpty(config.getScannerBinaryPath())) {
      FilePath scannerBinaryPath = runContext.getPathFromWorkspace(config.getScannerBinaryPath());
      logger.logInfo("Inlinescan binary globally defined to* " + scannerBinaryPath.getRemote());
      return scannerBinaryPath;
    }

    try {
      String latestVersion = getInlineScanVersion();
      logger.logInfo("Downloading inlinescan v" + latestVersion);
      FilePath scannerBinaryPath = downloadInlineScan(latestVersion);
      logger.logInfo("Inlinescan binary downloaded to " + scannerBinaryPath.getRemote());
      return scannerBinaryPath;
    } catch (IOException | InterruptedException e) {
      throw new AbortException("Error downloading inlinescan binary: " + e);
    }
  }


  private String executeScan(final FilePath scannerBinFile) throws AbortException {
    try {
      final FilePath scannerJsonOutputFile = this.scannerPaths.getBaseFolder().child("inlinescan.json");
      SysdigImageScanningProcessBuilder processBuilder = createProcessBuilder(scannerBinFile, scannerJsonOutputFile);

      logger.logInfo("Executing: " + String.join(" ", processBuilder.toCommandLineArguments()));
      logger.logInfo("Waiting for scanner execution to be completed...");
      int scannerExitCode = processBuilder.launchAndWait(this.runContext.getLauncher());
      logger.logInfo(String.format("Scanner exit code: %d", scannerExitCode));

      String jsonOutput = "";
      if (scannerJsonOutputFile.exists()) jsonOutput = scannerJsonOutputFile.readToString();
      logger.logDebug("Inline scan JSON output:\n" + jsonOutput);

      if (scannerExitCode == 2) {
        jsonOutput = "{error:\"Wrong parameters in call to inline scanner\"}";
      } else if (scannerExitCode == 3) {
        jsonOutput = "{error:\"Unexpected error when executing scan. Check that the API token is provided and is valid for the specified URL.\"}";
      } else if (scannerExitCode != 0 && scannerExitCode != 1) {
        throw new Exception("Cannot manage return code");
      }

      return jsonOutput;
    } catch (Exception e) {
      throw new AbortException("Error executing inlinescan binary: " + e);
    }
  }

  private SysdigImageScanningProcessBuilder createProcessBuilder(FilePath scannerBinFile, FilePath scannerJsonOutputFile) {
    SysdigImageScanningProcessBuilder processBuilder = new SysdigImageScanningProcessBuilder(scannerBinFile.getRemote(), this.config.getSysdigToken())
      .withExtraEnvVars(this.runContext.getEnvVars())
      .withEngineURL(this.config.getEngineurl())
      .withDBPath(this.scannerPaths.getDatabaseFolder().getRemote())
      .withCachePath(this.scannerPaths.getCacheFolder().getRemote())
      .withScanResultOutputPath(scannerJsonOutputFile.getRemote())
      .withConsoleLog()
      .withExtraParametersSeparatedBySpace(this.config.getInlineScanExtraParams())
      .withPoliciesToApplySeparatedBySpace(this.config.getPoliciesToApply())
      .withStdoutRedirectedTo(this.logger)
      .withStderrRedirectedTo(this.logger)
      .withLogLevel(config.getDebug() ? SysdigImageScanningProcessBuilder.LogLevel.DEBUG : SysdigImageScanningProcessBuilder.LogLevel.INFO)
      .withTLSVerification(config.getEngineverify());

    return processBuilder.withImageToScan(imageName);
  }

  private static class ScannerPaths implements Serializable {
    private static final String SCANNER_EXEC_FOLDER_BASE_PATH_PATTERN = "sysdig-secure-scan-%d";
    private final FilePath baseFolder;
    private final FilePath databaseFolder;
    private final FilePath cacheFolder;

    public ScannerPaths(final FilePath basePath) {
      this.baseFolder = basePath.child(String.format(SCANNER_EXEC_FOLDER_BASE_PATH_PATTERN, System.currentTimeMillis()));
      this.databaseFolder = baseFolder.child("db");
      this.cacheFolder = baseFolder.child("cache");
    }

    public FilePath getBaseFolder() {
      return this.baseFolder;
    }

    public FilePath getDatabaseFolder() {
      return this.databaseFolder;
    }

    public FilePath getCacheFolder() {
      return this.cacheFolder;
    }

    public void create() throws Exception {
      this.baseFolder.mkdirs();
      this.databaseFolder.mkdirs();
      this.cacheFolder.mkdirs();
    }
  }

}
