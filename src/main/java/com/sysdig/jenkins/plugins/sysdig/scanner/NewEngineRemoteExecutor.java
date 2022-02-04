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
import hudson.remoting.Callable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.remoting.RoleChecker;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

public class NewEngineRemoteExecutor implements Callable<String, Exception>, Serializable {

  //TODO: ...
  private final String imageName;
  private final String dockerFile;
  private final NewEngineBuildConfig config;
  private final SysdigLogger logger;
  private final EnvVars envVars;

  public NewEngineRemoteExecutor(String imageName, String dockerFile, NewEngineBuildConfig config, SysdigLogger logger, EnvVars envVars) {
    this.imageName = imageName;
    this.dockerFile = dockerFile;
    this.config = config;
    this.logger = logger;
    this.envVars = envVars;
  }

  @Override
  public void checkRoles(RoleChecker checker) throws SecurityException { }
  @Override

  public String call() throws InterruptedException, AbortException {

    if (!Strings.isNullOrEmpty(dockerFile)) {
      File f = new File(dockerFile);
      if (!f.exists()) {
        throw new AbortException("Dockerfile '" + dockerFile + "' does not exist");
      }
    }

    //Download
    File tmpBinary;
    try {
      String latestVersion = getInlineScanLatestVersion();
      logger.logInfo("Downloading inlinescan v" + latestVersion);
      tmpBinary = downloadInlineScan(latestVersion);
      logger.logInfo("Inlinescan binary downloaded to " + tmpBinary.getPath());
      Files.setPosixFilePermissions(tmpBinary.toPath(), EnumSet.of(PosixFilePermission.OWNER_EXECUTE));
    } catch (IOException e) {
      throw new AbortException("Error downloading inlinescan binary: " + e);
    }

    //Prepare args and execute
    try {
      File scanLog  = File.createTempFile("inlinescan", ".log");
      File scanResult  = File.createTempFile("inlinescan", ".json");
      List<String> command = new ArrayList<>();
      command.add(tmpBinary.getPath());
      command.add("--apiurl");
      command.add(config.getEngineurl());
      command.add("--logfile");
      command.add(scanLog.getAbsolutePath());
      command.add("--output-json");
      command.add(scanResult.getAbsolutePath());

      for (String extraParam: config.getInlineScanExtraParams().split(" ")) {
        if (!Strings.isNullOrEmpty(extraParam)) {
          command.add(extraParam);
        }
      }

      for (String policyId: config.getPoliciesToApply().split(" ")) {
        if (!Strings.isNullOrEmpty(policyId)) {
          command.add("--policy");
          command.add(policyId);
        }
      }

      if (!config.getEngineverify()) {
        command.add("--skiptlsverify");
      }

      if (!Strings.isNullOrEmpty(config.getInlineScanExtraParams())) {
        new ArrayList<String>();
        command.addAll(Arrays.asList(config.getInlineScanExtraParams().split(" ")));
      }

      command.add(this.imageName);

      List<String> env = new ArrayList<>();
      env.add("SECURE_API_TOKEN=" + config.getSysdigToken());
      for (String key: envVars.keySet()) {
        env.add(key + "=" + envVars.get(key));
      }

      logger.logInfo("Executing: " + String.join(" ", command));
      Process p = Runtime.getRuntime().exec(command.toArray(new String[0]), env.toArray(new String[0]));

      String stderr = IOUtils.toString(p.getErrorStream(), Charset.defaultCharset());
      String stdout = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());

      int retCode = p.waitFor();

      logger.logInfo("Inlinescan exit code: " + retCode);

      logger.logInfo("Inline scan output:\n" + stdout);
      logger.logInfo("Inline scan error:\n" + stderr);

      logger.logDebug("Inline scan logs:\n" + new String(Files.readAllBytes(Paths.get(scanLog.getAbsolutePath()))));

      //TODO: For exit code 2 (wrong params), just show the output (should not happen, but just in case)
      String jsonOutput = new String(Files.readAllBytes(Paths.get(scanResult.getAbsolutePath())));
      logger.logDebug("Inline scan JSON output:\n" + jsonOutput);

      return jsonOutput;

    } catch (IOException e) {
      throw new AbortException("Error executing inlinescan binary: " + e);
    }

  }

  private File downloadInlineScan(String latestVersion) throws IOException {
    File tmpBinary = File.createTempFile("inlinescan", "-" + latestVersion + ".bin");
    URL url = new URL("https://download.sysdig.com/scanning/inlinescan/inlinescan_" + latestVersion + "_linux_amd64");
    FileUtils.copyURLToFile(url, tmpBinary);
    return tmpBinary;
  }

  private String getInlineScanLatestVersion() throws IOException {
    URL url = new URL("https://download.sysdig.com/scanning/inlinescan/latest_version.txt");
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
      return reader.readLine();
    }
  }

}
