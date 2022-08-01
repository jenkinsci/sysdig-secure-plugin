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
import java.net.*;
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
  private final String[] noProxy;

  public NewEngineRemoteExecutor(String imageName, String dockerFile, NewEngineBuildConfig config, SysdigLogger logger, EnvVars envVars) {
    this.imageName = imageName;
    this.dockerFile = dockerFile;
    this.config = config;
    this.logger = logger;
    this.envVars = envVars;


    if (envVars.containsKey("no_proxy") || envVars.containsKey("NO_PROXY")) {
      String noProxy= envVars.getOrDefault("no_proxy",envVars.get("NO_PROXY"));
      this.noProxy = noProxy.split(",");
    } else {
      this.noProxy = new String[0];
    }

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

    if (!config.getScannerBinaryPath().isEmpty()) {
      tmpBinary = new File(config.getScannerBinaryPath());
      logger.logInfo("Inlinescan binary globally defined to* " + tmpBinary.getPath());
    }else {
      try {
        String latestVersion = getInlineScanLatestVersion();
        logger.logInfo("Downloading inlinescan v" + latestVersion);
        tmpBinary = downloadInlineScan(latestVersion);
        logger.logInfo("Inlinescan binary downloaded to " + tmpBinary.getPath());
        Files.setPosixFilePermissions(tmpBinary.toPath(), EnumSet.of(PosixFilePermission.OWNER_EXECUTE));
      } catch (IOException e) {
        throw new AbortException("Error downloading inlinescan binary: " + e);
      }
    }
    //Prepare args and execute
    try {
      File scanLog = File.createTempFile("inlinescan", ".log");
      File scanResult = File.createTempFile("inlinescan", ".json");
      List<String> command = new ArrayList<>();
      command.add(tmpBinary.getPath());
      command.add("--apiurl");
      command.add(config.getEngineurl());
      command.add("--logfile");
      command.add(scanLog.getAbsolutePath());
      command.add("--output-json");
      command.add(scanResult.getAbsolutePath());

      for (String extraParam : config.getInlineScanExtraParams().split(" ")) {
        if (!Strings.isNullOrEmpty(extraParam)) {
          command.add(extraParam);
        }
      }

      for (String policyId : config.getPoliciesToApply().split(" ")) {
        if (!Strings.isNullOrEmpty(policyId)) {
          command.add("--policy");
          command.add(policyId);
        }
      }

      if (!config.getEngineverify()) {
        command.add("--skiptlsverify");
      }

      if (config.getDebug()) {
        command.add("--loglevel=debug");
      }

      command.add(this.imageName);

      List<String> env = new ArrayList<>();
      env.add("SECURE_API_TOKEN=" + config.getSysdigToken());
      for (Map.Entry<String, String> entry : envVars.entrySet()) {
        env.add(entry.getKey() + "=" + entry.getValue());
      }

      logger.logInfo("Executing: " + String.join(" ", command));
      Process p = Runtime.getRuntime().exec(command.toArray(new String[0]), env.toArray(new String[0]));

      //     String stderr = IOUtils.toString(p.getErrorStream(), Charset.defaultCharset());
     // String stdout = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());


      class PrimeThread extends Thread {
        Process p;
        SysdigLogger logger;
        BufferedReader or=null;
        String output = "";
        PrimeThread(Process p, SysdigLogger logger) {
          this.p = p;
          this.logger=logger;
        }

        public void run() {
          try {
            SequenceInputStream message = new SequenceInputStream(p.getInputStream(),p.getErrorStream());

            or = new BufferedReader(new InputStreamReader(message,Charset.defaultCharset()));

            while ((output = or.readLine()) != null) {
             logger.logInfo(output);
            }
          }
          catch (IOException ioe) {
           logger.logError("Exception while reading input " + ioe);
          }
          finally {
            // close the streams using close method
            try {
              if (or != null) {
                or.close();
              }
            }
            catch (IOException ioe) {
              logger.logError("Error while closing stream: " + ioe);
            }
          }
        }
      }

      PrimeThread thread = new PrimeThread(p,logger);
      thread.start();


      int retCode = p.waitFor();
          thread.join();

      logger.logInfo("Inlinescan exit code: " + retCode);

     // logger.logInfo("Inline scan output:\n" + stdout);
     // String stderr = IOUtils.toString(p.getErrorStream(), Charset.defaultCharset());
     // logger.logInfo("Inline scan error:\n" + stderr);

      logger.logDebug("Inline scan logs:\n" + new String(Files.readAllBytes(Paths.get(scanLog.getAbsolutePath())), Charset.defaultCharset()));

      //TODO: For exit code 2 (wrong params), just show the output (should not happen, but just in case)
      String jsonOutput = new String(Files.readAllBytes(Paths.get(scanResult.getAbsolutePath())), Charset.defaultCharset());
      logger.logDebug("Inline scan JSON output:\n" + jsonOutput);

      if ( retCode == 2 ) {
        jsonOutput = "{error:\"Wrong parameters in call to inline scanner\"}";
      } else if ( retCode == 3 ) {
        jsonOutput = "{error:\"Unexpected error when executing scan\"}";
      }else if ( retCode != 0 && retCode != 1 ) {
        throw new Exception("Cannot manage return code");
      }

      return jsonOutput;

    } catch (Exception e) {
      throw new AbortException("Error executing inlinescan binary: " + e);
    }

  }

  private File downloadInlineScan(String latestVersion) throws IOException {
    File tmpBinary = File.createTempFile("inlinescan", "-" + latestVersion + ".bin");
    logger.logInfo(System.getProperty("os.name"));

    String os = System.getProperty("os.name").toLowerCase().startsWith("mac") ? "darwin":"linux";
    URL url = new URL("https://download.sysdig.com/scanning/bin/sysdig-cli-scanner/" + latestVersion + "/"+os+"/amd64/sysdig-cli-scanner");
    Proxy proxy = getHttpProxy();
    Boolean proxyException = Arrays.asList(noProxy).contains("sysdig.com") || Arrays.asList(noProxy).contains("download.sysdig.com");
    if (proxy != Proxy.NO_PROXY && proxy.type() != Proxy.Type.DIRECT && !proxyException) {
      FileUtils.copyInputStreamToFile(url.openConnection(proxy).getInputStream(),tmpBinary);
    } else {
      FileUtils.copyURLToFile(url, tmpBinary);
    }

    return tmpBinary;
  }

  private String getInlineScanLatestVersion() throws IOException {
    /*URL url = new URL("https://download.sysdig.com/scanning/sysdig-cli-scanner/latest_version.txt");
    Proxy proxy = getHttpProxy();
    Boolean proxyException = Arrays.asList(noProxy).contains("sysdig.com") || Arrays.asList(noProxy).contains("download.sysdig.com");
    if (proxy != Proxy.NO_PROXY && proxy.type() != Proxy.Type.DIRECT && !proxyException) {
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection(proxy).getInputStream(), StandardCharsets.UTF_8))) {
        return reader.readLine();
      }
    } else {
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
        return reader.readLine();
      }
    }*/
    //Fixed to avoid compatibility issues.
    return "1.2.4";
  }

  private Proxy getHttpProxy() throws IOException {
    Proxy proxy;
    String address="";
    Integer port;
    URL proxyURL;



    if (envVars.containsKey("https_proxy") || envVars.containsKey("HTTPS_PROXY")) {
      address = envVars.getOrDefault("https_proxy",envVars.get("HTTPS_PROXY"));
    } else if (envVars.containsKey("http_proxy") || envVars.containsKey("HTTP_PROXY")) {
      address = envVars.getOrDefault("https_proxy",envVars.get("HTTPS_PROXY"));
    }

    if (!address.isEmpty()) {
      if (!address.startsWith("http://") && !address.startsWith("https://")){
        address = "http://" + address;
      }
      proxyURL = new URL(address);
      port = proxyURL.getPort()!=-1 ? proxyURL.getPort() : 80;
      proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyURL.getHost(),port));
    } else {
      proxy = Proxy.NO_PROXY;
    }
    logger.logDebug("Inline scan proxy: " + proxy);
    return proxy;
  }


}
