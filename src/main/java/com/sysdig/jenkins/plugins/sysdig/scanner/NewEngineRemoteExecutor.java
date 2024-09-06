/*
Copyright (C) 2016-2020 Sysdig

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
package com.sysdig.jenkins.plugins.sysdig.scanner;

import com.google.common.base.Strings;
import com.sysdig.jenkins.plugins.sysdig.NewEngineBuildConfig;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.remoting.Callable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.jenkinsci.remoting.RoleChecker;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class NewEngineRemoteExecutor implements Callable<String, Exception>, Serializable {

  private static final String FIXED_SCANNED_VERSION = "1.10.1";
  private final ScannerPaths scannerPaths;
  private final String imageName;
  private final NewEngineBuildConfig config;
  private final SysdigLogger logger;
  private final EnvVars envVars;
  private final String[] noProxy;
  public NewEngineRemoteExecutor(FilePath workspace, String imageName, NewEngineBuildConfig config, SysdigLogger logger, EnvVars envVars) {
    this.imageName = imageName;
    this.config = config;
    this.logger = logger;
    this.envVars = envVars;
    this.scannerPaths = new ScannerPaths(workspace);

    if (envVars.containsKey("no_proxy") || envVars.containsKey("NO_PROXY")) {
      String noProxy = envVars.getOrDefault("no_proxy", envVars.get("NO_PROXY"));
      this.noProxy = noProxy.split(",");
    } else {
      this.noProxy = new String[0];
    }
  }

  @Override
  public void checkRoles(RoleChecker checker) throws SecurityException {
  }

  @Override
  public String call() throws AbortException {
    try {
      // Create all the necessary folders to store execution temp files and such
      createExecutionWorkspace();
      // Retrieve the scanner bin file
      final File scannerBinaryFile = retrieveScannerBinFile();
      // Execute the scanner bin file and retrieves its json output
      return executeScan(scannerBinaryFile);
    } finally {
      purgeExecutionWorkspace();
    }
  }

  private File downloadInlineScan(String latestVersion) throws IOException, UnsupportedOperationException, InterruptedException {
    final File scannerBinFile = Files.createFile(Paths.get(this.scannerPaths.getBinFolder(), String.format("inlinescan-%s.bin", latestVersion))).toFile();
    logger.logInfo(System.getProperty("os.name"));

    String os = System.getProperty("os.name").toLowerCase().startsWith("mac") ? "darwin" : "linux";
    String arch = System.getProperty("os.arch").toLowerCase().startsWith("aarch64") ? "arm64" : "amd64";
    URL url = new URL("https://download.sysdig.com/scanning/bin/sysdig-cli-scanner/" + latestVersion + "/" + os + "/" + arch + "/sysdig-cli-scanner");
    Proxy proxy = getHttpProxy();
    boolean proxyException = Arrays.asList(noProxy).contains("sysdig.com") || Arrays.asList(noProxy).contains("download.sysdig.com");
    int downloadRetriesLeft = 5;
    while (true) {
      try {
        if (proxy != Proxy.NO_PROXY && proxy.type() != Proxy.Type.DIRECT && !proxyException) {
          FileUtils.copyInputStreamToFile(url.openConnection(proxy).getInputStream(), scannerBinFile);
        } else {
          FileUtils.copyURLToFile(url, scannerBinFile);
        }

        Files.setPosixFilePermissions(scannerBinFile.toPath(), EnumSet.of(PosixFilePermission.OWNER_EXECUTE));
        return scannerBinFile;
      } catch (Exception e) {
        downloadRetriesLeft--;
        if (downloadRetriesLeft > 0) {
          TimeUnit.SECONDS.sleep(2L);
        } else {
          throw e;
        }
      }
    }
  }

  private String getInlineScanLatestVersion() throws IOException {
    URL url = new URL("https://download.sysdig.com/scanning/sysdig-cli-scanner/latest_version.txt");
    Proxy proxy = getHttpProxy();
    boolean proxyException = Arrays.asList(noProxy).contains("sysdig.com") || Arrays.asList(noProxy).contains("download.sysdig.com");
    if (proxy != Proxy.NO_PROXY && proxy.type() != Proxy.Type.DIRECT && !proxyException) {
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection(proxy).getInputStream(), StandardCharsets.UTF_8))) {
        return reader.readLine();
      }
    } else {
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
        return reader.readLine();
      }
    }
  }

  private String getInlineScanPinnedVersion() {
    return FIXED_SCANNED_VERSION;
  }

  private String getInlineScanVersion() throws IOException {
    if (this.config.getCliVersionToApply().equals("custom")) {
      if (this.config.getCustomCliVersion().isEmpty()) {
        return getInlineScanPinnedVersion();
      }
      return this.config.getCustomCliVersion();
    }
    return getInlineScanPinnedVersion();
  }

  private Proxy getHttpProxy() throws IOException {
    Proxy proxy;
    String address = "";
    int port;
    URL proxyURL;

    if (envVars.containsKey("https_proxy") || envVars.containsKey("HTTPS_PROXY")) {
      address = envVars.getOrDefault("https_proxy", envVars.get("HTTPS_PROXY"));
    } else if (envVars.containsKey("http_proxy") || envVars.containsKey("HTTP_PROXY")) {
      address = envVars.getOrDefault("https_proxy", envVars.get("HTTPS_PROXY"));
    }

    if (!address.isEmpty()) {
      if (!address.startsWith("http://") && !address.startsWith("https://")) {
        address = "http://" + address;
      }
      proxyURL = new URL(address);
      port = proxyURL.getPort() != -1 ? proxyURL.getPort() : 80;
      proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyURL.getHost(), port));
    } else {
      proxy = Proxy.NO_PROXY;
    }
    logger.logDebug("Inline scan proxy: " + proxy);
    return proxy;
  }

  private void createExecutionWorkspace() throws AbortException {
    try {
      this.scannerPaths.create();
    } catch (Exception e) {
      logger.logError("Unable to create scanner execution workspace", e);
      throw new AbortException("Unable to create scanner execution workspace");
    }
  }

  private void purgeExecutionWorkspace() {
    try {
      this.scannerPaths.purge();
    } catch (IOException e) {
      logger.logError("Unable to delete scanner execution workspace", e);
    }
  }

  private File retrieveScannerBinFile() throws AbortException {
    File scannerBinaryPath;

    if (!config.getScannerBinaryPath().isEmpty()) {
      scannerBinaryPath = new File(config.getScannerBinaryPath());
      logger.logInfo("Inlinescan binary globally defined to* " + scannerBinaryPath.getPath());
    } else {
      try {
        String latestVersion = getInlineScanVersion();
        logger.logInfo("Downloading inlinescan v" + latestVersion);
        scannerBinaryPath = downloadInlineScan(latestVersion);
        logger.logInfo("Inlinescan binary downloaded to " + scannerBinaryPath.getPath());
      } catch (IOException | InterruptedException e) {
        throw new AbortException("Error downloading inlinescan binary: " + e);
      }
    }
    return scannerBinaryPath;
  }

  private String executeScan(final File scannerBinFile) throws AbortException {
    try {
      final long logsFileTrailerCheckingInterval = 200L;
      final long logsCollectionWaitingTime = 2000L;
      final File scannerJsonOutputFile = Files.createFile(Paths.get(this.scannerPaths.getBaseFolder(), "inlinescan.json")).toFile();
      final File scannerExecLogsFile = Files.createFile(Paths.get(this.scannerPaths.getBaseFolder(), "inlinescan-logs.log")).toFile();
      final Tailer logsFileTailer = Tailer.create(scannerExecLogsFile, new LogsFileToLoggerForwarder(this.logger), logsFileTrailerCheckingInterval);

      List<String> command = new ArrayList<>();
      command.add(scannerBinFile.getPath());
      command.add(String.format("--apiurl=%s", this.config.getEngineurl()));
      command.add(String.format("--dbpath=%s", this.scannerPaths.getDatabaseFolder()));
      command.add(String.format("--cachepath=%s", this.scannerPaths.getCacheFolder()));
      command.add(String.format("--output-json=%s", scannerJsonOutputFile.getAbsolutePath()));
      command.add("--console-log");

      if (this.config.getDebug()) {
        command.add("--loglevel=debug");
      }
      if (!this.config.getEngineverify()) {
        command.add("--skiptlsverify");
      }

      for (String extraParam : this.config.getInlineScanExtraParams().split(" ")) {
        if (!Strings.isNullOrEmpty(extraParam)) {
          command.add(extraParam);
        }
      }
      for (String policyId : this.config.getPoliciesToApply().split(" ")) {
        if (!Strings.isNullOrEmpty(policyId)) {
          command.add(String.format("--policy=%s", policyId));
        }
      }

      command.add(this.imageName);

      final ProcessBuilder processBuilder = new ProcessBuilder().command(command).redirectOutput(scannerExecLogsFile).redirectError(scannerExecLogsFile);
      final Map<String, String> processEnv = processBuilder.environment();
      processEnv.putAll(this.envVars);
      processEnv.put("TMPDIR", this.scannerPaths.getTmpFolder());
      processEnv.put("SECURE_API_TOKEN", this.config.getSysdigToken());

      logger.logInfo("Executing: " + String.join(" ", command));
      final Process scannerProcess = processBuilder.start();

      logger.logInfo("Waiting for scanner execution to be completed...");
      int scannerExitCode = scannerProcess.waitFor();
      Thread.sleep(logsCollectionWaitingTime); //just to be sure that all the logs have been written to the file for being successfully retrieved
      logsFileTailer.stop();
      logger.logInfo(String.format("Scanner exit code: %d", scannerExitCode));

      String jsonOutput = new String(Files.readAllBytes(Paths.get(scannerJsonOutputFile.getAbsolutePath())), Charset.defaultCharset());
      logger.logDebug("Inline scan JSON output:\n" + jsonOutput);

      if (scannerExitCode == 2) {
        jsonOutput = "{error:\"Wrong parameters in call to inline scanner\"}";
      } else if (scannerExitCode == 3) {
        jsonOutput = "{error:\"Unexpected error when executing scan\"}";
      } else if (scannerExitCode != 0 && scannerExitCode != 1) {
        throw new Exception("Cannot manage return code");
      }

      return jsonOutput;
    } catch (Exception e) {
      throw new AbortException("Error executing inlinescan binary: " + e);
    }
  }

  public static class LogsFileToLoggerForwarder extends TailerListenerAdapter {

    private final SysdigLogger logger;

    public LogsFileToLoggerForwarder(final SysdigLogger forwardTo) {
      this.logger = forwardTo;
    }

    public void handle(String line) {
      this.logger.logInfo(line);
    }
  }

  private static class ScannerPaths implements Serializable {
    private static final String SCANNER_EXEC_FOLDER_BASE_PATH_PATTERN = "sysdig-secure-scan-%d";
    private final String baseFolder;
    private final String binFolder;
    private final String databaseFolder;
    private final String cacheFolder;
    private final String tmpFolder;

    public ScannerPaths(final FilePath basePath) {
      this.baseFolder = Paths.get(basePath.getRemote(), String.format(SCANNER_EXEC_FOLDER_BASE_PATH_PATTERN, System.currentTimeMillis())).toString();
      this.binFolder = Paths.get(this.baseFolder, "bin").toString();
      this.databaseFolder = Paths.get(this.baseFolder, "db").toString();
      this.cacheFolder = Paths.get(this.baseFolder, "cache").toString();
      this.tmpFolder = Paths.get(this.baseFolder, "tmp").toString();
    }

    public String getBaseFolder() {
      return this.baseFolder;
    }

    public String getBinFolder() {
      return this.binFolder;
    }

    public String getDatabaseFolder() {
      return this.databaseFolder;
    }

    public String getCacheFolder() {
      return this.cacheFolder;
    }

    public String getTmpFolder() {
      return this.tmpFolder;
    }

    public void create() throws Exception {
      Files.createDirectories(Paths.get(this.baseFolder));
      Files.createDirectory(Paths.get(this.binFolder));
      Files.createDirectory(Paths.get(this.databaseFolder));
      Files.createDirectory(Paths.get(this.cacheFolder));
      Files.createDirectory(Paths.get(this.tmpFolder));
    }

    public void purge() throws IOException {
      FileUtils.deleteDirectory(new File(this.baseFolder));
    }
  }

}
