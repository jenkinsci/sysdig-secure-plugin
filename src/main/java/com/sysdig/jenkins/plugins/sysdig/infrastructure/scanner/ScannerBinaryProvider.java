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
import com.sysdig.jenkins.plugins.sysdig.infrastructure.http.ExecutableDownloader;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.RunContext;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.FilePath;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Provides the Sysdig CLI scanner binary, either by resolving a user-configured
 * path or by downloading the appropriate release for the current OS/arch.
 */
public class ScannerBinaryProvider {
    private final RunContext runContext;
    private final ExecutableDownloader downloader;
    private final ImageScanningConfig config;
    private final ScannerVersionResolver versionResolver;
    private final SysdigLogger logger;

    public ScannerBinaryProvider(
            @NonNull RunContext runContext,
            ExecutableDownloader downloader,
            ImageScanningConfig config,
            ScannerVersionResolver versionResolver) {
        this.runContext = runContext;
        this.downloader = downloader;
        this.config = config;
        this.versionResolver = versionResolver;
        this.logger = runContext.getLogger();
    }

    public FilePath retrieve() throws AbortException {
        if (!Strings.isNullOrEmpty(config.getScannerBinaryPath())) {
            FilePath scannerBinaryPath = runContext.getPathFromWorkspace(config.getScannerBinaryPath());
            logger.logInfo("Inlinescan binary globally defined to* " + scannerBinaryPath.getRemote());
            return scannerBinaryPath;
        }

        try {
            String latestVersion = versionResolver.resolve();
            logger.logInfo("Downloading inlinescan v" + latestVersion);
            FilePath scannerBinaryPath = download(latestVersion);
            logger.logInfo("Inlinescan binary downloaded to " + scannerBinaryPath.getRemote());
            return scannerBinaryPath;
        } catch (IOException | InterruptedException e) {
            throw new AbortException("Error downloading inlinescan binary: " + e);
        }
    }

    private FilePath download(String version) throws IOException, UnsupportedOperationException, InterruptedException {
        URL url = downloadURLForVersion(version);
        return downloader.downloadExecutable(url, String.format("inlinescan-%s.bin", version));
    }

    static URL downloadURLForVersion(String version) throws MalformedURLException {
        return downloadURLForVersion(version, System.getProperty("os.name"), System.getProperty("os.arch"));
    }

    static URL downloadURLForVersion(String version, String osName, String osArch) throws MalformedURLException {
        String os = osName.toLowerCase().startsWith("mac") ? "darwin" : "linux";
        String arch = osArch.toLowerCase().startsWith("aarch64") ? "arm64" : "amd64";
        return new URL("https://download.sysdig.com/scanning/bin/sysdig-cli-scanner/" + version + "/" + os + "/" + arch
                + "/sysdig-cli-scanner");
    }
}
