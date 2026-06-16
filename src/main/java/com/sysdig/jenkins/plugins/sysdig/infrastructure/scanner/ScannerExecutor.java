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

import com.sysdig.jenkins.plugins.sysdig.application.vm.ImageScanningConfig;
import com.sysdig.jenkins.plugins.sysdig.domain.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.RunContext;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.FilePath;

/** Runs the Sysdig CLI scanner binary and returns its raw JSON output. */
public class ScannerExecutor {
    private final RunContext runContext;
    private final ImageScanningConfig config;
    private final ScannerPaths scannerPaths;
    private final String imageName;
    private final SysdigLogger logger;

    public ScannerExecutor(
            @NonNull RunContext runContext, ImageScanningConfig config, ScannerPaths scannerPaths, String imageName) {
        this.runContext = runContext;
        this.config = config;
        this.scannerPaths = scannerPaths;
        this.imageName = imageName;
        this.logger = runContext.getLogger();
    }

    public String execute(final FilePath scannerBinFile) throws AbortException {
        try {
            final FilePath scannerJsonOutputFile =
                    this.scannerPaths.getBaseFolder().child("inlinescan.json");
            SysdigImageScanningProcessBuilder processBuilder =
                    createProcessBuilder(scannerBinFile, scannerJsonOutputFile);

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
                jsonOutput =
                        "{error:\"Unexpected error when executing scan. Check that the API token is provided and is valid for the specified URL.\"}";
            } else if (scannerExitCode != 0 && scannerExitCode != 1) {
                throw new Exception("Cannot manage return code");
            }

            return jsonOutput;
        } catch (Exception e) {
            throw new AbortException("Error executing inlinescan binary: " + e);
        }
    }

    private SysdigImageScanningProcessBuilder createProcessBuilder(
            FilePath scannerBinFile, FilePath scannerJsonOutputFile) {
        SysdigImageScanningProcessBuilder processBuilder = new SysdigImageScanningProcessBuilder(
                        scannerBinFile.getRemote(), this.config.getSysdigToken())
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
                .withLogLevel(
                        config.getDebug()
                                ? SysdigImageScanningProcessBuilder.LogLevel.DEBUG
                                : SysdigImageScanningProcessBuilder.LogLevel.INFO)
                .withTLSVerification(config.getEngineverify());

        return processBuilder.withImageToScan(imageName);
    }
}
