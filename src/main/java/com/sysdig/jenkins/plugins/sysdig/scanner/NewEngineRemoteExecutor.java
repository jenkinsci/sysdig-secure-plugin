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
import org.jenkinsci.remoting.RoleChecker;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;


public class NewEngineRemoteExecutor implements Callable<String, Exception>, Serializable {

  private static class PrimeThread extends Thread {
    Process p;
    SysdigLogger logger;
    BufferedReader or = null;
    String output = "";

    PrimeThread(Process p, SysdigLogger logger) {
      this.p = p;
      this.logger = logger;
    }

    public void run() {
      try {
        SequenceInputStream message = new SequenceInputStream(p.getInputStream(), p.getErrorStream());

        or = new BufferedReader(new InputStreamReader(message, Charset.defaultCharset()));

        while ((output = or.readLine()) != null) {
          logger.logInfo(output);
        }
      } catch (IOException ioe) {
        logger.logError("Exception while reading input " + ioe);
      } finally {
        // close the streams using close method
        try {
          if (or != null) {
            or.close();
          }
        } catch (IOException ioe) {
          logger.logError("Error while closing stream: " + ioe);
        }
      }
    }
  }

  private static class ScannerPaths {
    private static final String SCANNER_EXEC_FOLDER_BASE_PATH_PATTERN = "sysdig-secure-scan-%d";
    private final Path baseFolder;
    private final Path binFolder;
    private final Path databaseFolder;
    private final Path cacheFolder;
    private final Path tmpFolder;

    public ScannerPaths(final FilePath basePath) {
      this.baseFolder = Paths.get(basePath.getRemote(), String.format(SCANNER_EXEC_FOLDER_BASE_PATH_PATTERN, System.currentTimeMillis()));
      this.binFolder = Paths.get(this.baseFolder.toString(), "bin");
      this.databaseFolder = Paths.get(this.baseFolder.toString(), "db");
      this.cacheFolder = Paths.get(this.baseFolder.toString(), "cache");
      this.tmpFolder = Paths.get(this.baseFolder.toString(), "tmp");
    }

    public Path getBaseFolder() {
      return this.baseFolder;
    }

    public Path getBinFolder() {
      return this.binFolder;
    }

    public Path getDatabaseFolder() {
      return this.databaseFolder;
    }

    public Path getCacheFolder() {
      return this.cacheFolder;
    }

    public Path getTmpFolder() {
      return this.tmpFolder;
    }

    public void create() throws Exception {
      Files.createDirectories(this.baseFolder);
      Files.createDirectory(this.binFolder);
      Files.createDirectory(this.databaseFolder);
      Files.createDirectory(this.cacheFolder);
      Files.createDirectory(this.tmpFolder);
    }

    public void purge() throws IOException {
      FileUtils.deleteDirectory(this.baseFolder.toFile());
    }
  }

  private final ScannerPaths scannerPaths;
  private final String imageName;
  private final String dockerFile;
  private final NewEngineBuildConfig config;
  private final SysdigLogger logger;
  private final EnvVars envVars;
  private final String[] noProxy;

  public NewEngineRemoteExecutor(FilePath workspace, String imageName, String dockerFile, NewEngineBuildConfig config, SysdigLogger logger, EnvVars envVars) {
    this.imageName = imageName;
    this.dockerFile = dockerFile;
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
  public void checkRoles(RoleChecker checker) throws SecurityException {}

  @Override
  public String call() throws AbortException {
    if (!Strings.isNullOrEmpty(dockerFile)) {
      File f = new File(dockerFile);
      if (!f.exists()) {
        throw new AbortException("Dockerfile '" + dockerFile + "' does not exist");
      }
    }

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

  private File downloadInlineScan(String latestVersion) throws IOException, UnsupportedOperationException {
    final File scannerBinFile = Files.createFile(Paths.get(this.scannerPaths.getBinFolder().toString(), String.format("inlinescan-%s.bin", latestVersion))).toFile();
    logger.logInfo(System.getProperty("os.name"));

    String os = System.getProperty("os.name").toLowerCase().startsWith("mac") ? "darwin" : "linux";
    URL url = new URL("https://download.sysdig.com/scanning/bin/sysdig-cli-scanner/" + latestVersion + "/" + os + "/amd64/sysdig-cli-scanner");
    Proxy proxy = getHttpProxy();
    boolean proxyException = Arrays.asList(noProxy).contains("sysdig.com") || Arrays.asList(noProxy).contains("download.sysdig.com");
    if (proxy != Proxy.NO_PROXY && proxy.type() != Proxy.Type.DIRECT && !proxyException) {
      FileUtils.copyInputStreamToFile(url.openConnection(proxy).getInputStream(), scannerBinFile);
    } else {
      FileUtils.copyURLToFile(url, scannerBinFile);
    }

    Files.setPosixFilePermissions(scannerBinFile.toPath(), EnumSet.of(PosixFilePermission.OWNER_EXECUTE));
    return scannerBinFile;
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
        String latestVersion = getInlineScanLatestVersion();
        logger.logInfo("Downloading inlinescan v" + latestVersion);
        scannerBinaryPath = downloadInlineScan(latestVersion);
        logger.logInfo("Inlinescan binary downloaded to " + scannerBinaryPath.getPath());
      } catch (IOException e) {
        throw new AbortException("Error downloading inlinescan binary: " + e);
      }
    }
    return scannerBinaryPath;
  }

  private String executeScan(final File scannerBinFile) throws AbortException {
    try {
      final File scannerJsonOutputFile = Files.createFile(Paths.get(this.scannerPaths.getBaseFolder().toString(), "inlinescan.json")).toFile();

      List<String> command = new ArrayList<>();
      command.add(scannerBinFile.getPath());
      command.add(String.format("--apiurl=%s", this.config.getEngineurl()));
      command.add(String.format("--dbpath=%s", this.scannerPaths.getDatabaseFolder()));
      command.add(String.format("--cachepath=%s", this.scannerPaths.getCacheFolder()));
      command.add("--console-log");
      command.add(String.format("--output-json=%s", scannerJsonOutputFile.getAbsolutePath()));

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

      List<String> envVars = new ArrayList<>();
      envVars.add(String.format("TMPDIR=%s", this.scannerPaths.getTmpFolder()));
      envVars.add(String.format("SECURE_API_TOKEN=%s", this.config.getSysdigToken()));
      for (Map.Entry<String, String> entry : this.envVars.entrySet()) {
        envVars.add(entry.getKey() + "=" + entry.getValue());
      }

      logger.logInfo("Executing: " + String.join(" ", command));
      Process scanProcess = Runtime.getRuntime().exec(command.toArray(new String[0]), envVars.toArray(new String[0]));

      PrimeThread thread = new PrimeThread(scanProcess, logger);
      thread.start();

      int retCode = scanProcess.waitFor();
      thread.join();

      logger.logInfo("Inlinescan exit code: " + retCode);

      //TODO: For exit code 2 (wrong params), just show the output (should not happen, but just in case)
      String jsonOutput = new String(Files.readAllBytes(Paths.get(scannerJsonOutputFile.getAbsolutePath())), Charset.defaultCharset());
      logger.logDebug("Inline scan JSON output:\n" + jsonOutput);

      if (retCode == 2) {
        jsonOutput = "{error:\"Wrong parameters in call to inline scanner\"}";
      } else if (retCode == 3) {
        jsonOutput = "{error:\"Unexpected error when executing scan\"}";
      } else if (retCode != 0 && retCode != 1) {
        throw new Exception("Cannot manage return code");
      }

      return jsonOutput;
    } catch (Exception e) {
      throw new AbortException("Error executing inlinescan binary: " + e);
    }
  }

}
