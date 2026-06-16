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
import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.ScanResult;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.http.RetriableRemoteDownloader;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.RunContext;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.json.GsonBuilder;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1.JsonScanResultV1;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.FilePath;

public class RemoteSysdigImageScanner {
    private final ScannerPaths scannerPaths;
    private final ScannerBinaryProvider binaryProvider;
    private final ScannerExecutor executor;
    private final SysdigLogger logger;

    public RemoteSysdigImageScanner(
            @NonNull RunContext runContext,
            RetriableRemoteDownloader retriableRemoteDownloader,
            String imageName,
            ImageScanningConfig config) {
        this.scannerPaths = new ScannerPaths(runContext.getPathFromWorkspace());
        this.binaryProvider = new ScannerBinaryProvider(
                runContext, retriableRemoteDownloader, config, new ScannerVersionResolver(config));
        this.executor = new ScannerExecutor(runContext, config, scannerPaths, imageName);
        this.logger = runContext.getLogger();
    }

    public ScanResult performScan() throws AbortException {
        // Create all the necessary folders to store execution temp files and such
        createExecutionWorkspace();
        // Retrieve the scanner bin file
        final FilePath scannerBinaryFile = binaryProvider.retrieve();
        // Execute the scanner bin file and retrieves its json output
        String imageScanningResultJSON = executor.execute(scannerBinaryFile);
        logger.logDebug("Raw scan result as JSON: ");
        logger.logDebug(imageScanningResultJSON);
        JsonScanResultV1 jsonScanResult = GsonBuilder.build().fromJson(imageScanningResultJSON, JsonScanResultV1.class);

        return jsonScanResult
                .toDomain()
                .orElseThrow(() -> new AbortException(
                        String.format("unable to obtain result from scan: %s", imageScanningResultJSON)));
    }

    private void createExecutionWorkspace() throws AbortException {
        try {
            this.scannerPaths.create();
        } catch (Exception e) {
            logger.logError("Unable to create scanner execution workspace", e);
            throw new AbortException("Unable to create scanner execution workspace");
        }
    }
}
