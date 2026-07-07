package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sysdig.jenkins.plugins.sysdig.application.vm.ImageScanningConfig;
import com.sysdig.jenkins.plugins.sysdig.domain.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.http.ExecutableDownloader;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.RunContext;
import hudson.FilePath;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.Test;

class ScannerBinaryProviderTest {
    private static final String BASE = "https://download.sysdig.com/scanning/bin/sysdig-cli-scanner/1.27.2/";

    private final ImageScanningConfig config = mock(ImageScanningConfig.class);
    private final ExecutableDownloader downloader = mock(ExecutableDownloader.class);
    private final RunContext runContext = mock(RunContext.class);

    private ScannerBinaryProvider provider() {
        when(runContext.getLogger()).thenReturn(mock(SysdigLogger.class));
        return new ScannerBinaryProvider(runContext, downloader, config, new ScannerVersionResolver(config));
    }

    @Test
    void whenALocalCliBinaryIsConfiguredItIsUsedWithoutDownloading() throws Exception {
        FilePath configured = mock(FilePath.class);
        when(configured.getRemote()).thenReturn("/ws/custom-scanner");
        when(config.getScannerBinaryPath()).thenReturn("custom-scanner");
        when(runContext.getPathFromWorkspace("custom-scanner")).thenReturn(configured);

        assertSame(configured, provider().retrieve());
        verify(downloader, never()).downloadExecutable(any(), any());
    }

    @Test
    void whenNoScannerBinaryPathIsConfiguredItDownloadsThePinnedVersion() throws Exception {
        FilePath downloaded = mock(FilePath.class);
        when(downloaded.getRemote()).thenReturn("/ws/bin/inlinescan-1.27.2.bin");
        when(config.getScannerBinaryPath()).thenReturn("");
        when(downloader.downloadExecutable(any(URL.class), eq("inlinescan-1.27.2.bin")))
                .thenReturn(downloaded);

        assertSame(downloaded, provider().retrieve());
        // The exact URL depends on the host OS/arch; assert the resolved version flows into it and the filename.
        verify(downloader)
                .downloadExecutable(ScannerBinaryProvider.downloadURLForVersion("1.27.2"), "inlinescan-1.27.2.bin");
    }

    @Test
    void whenACustomVersionIsConfiguredItDownloadsThatVersion() throws Exception {
        FilePath downloaded = mock(FilePath.class);
        when(downloaded.getRemote()).thenReturn("/ws/bin/inlinescan-1.30.0.bin");
        when(config.getScannerBinaryPath()).thenReturn("");
        when(config.getCliVersionToApply()).thenReturn("custom");
        when(config.getCustomCliVersion()).thenReturn("1.30.0");
        when(downloader.downloadExecutable(any(URL.class), eq("inlinescan-1.30.0.bin")))
                .thenReturn(downloaded);

        assertSame(downloaded, provider().retrieve());
        verify(downloader)
                .downloadExecutable(ScannerBinaryProvider.downloadURLForVersion("1.30.0"), "inlinescan-1.30.0.bin");
    }

    @Test
    void linuxAmd64() throws MalformedURLException {
        assertEquals(
                BASE + "linux/amd64/sysdig-cli-scanner",
                ScannerBinaryProvider.downloadURLForVersion("1.27.2", "Linux", "amd64")
                        .toString());
    }

    @Test
    void linuxArm64() throws MalformedURLException {
        assertEquals(
                BASE + "linux/arm64/sysdig-cli-scanner",
                ScannerBinaryProvider.downloadURLForVersion("1.27.2", "Linux", "aarch64")
                        .toString());
    }

    @Test
    void macAmd64() throws MalformedURLException {
        assertEquals(
                BASE + "darwin/amd64/sysdig-cli-scanner",
                ScannerBinaryProvider.downloadURLForVersion("1.27.2", "Mac OS X", "x86_64")
                        .toString());
    }

    @Test
    void macArm64() throws MalformedURLException {
        assertEquals(
                BASE + "darwin/arm64/sysdig-cli-scanner",
                ScannerBinaryProvider.downloadURLForVersion("1.27.2", "Mac OS X", "aarch64")
                        .toString());
    }

    @Test
    void unknownOsDefaultsToLinuxAndUnknownArchDefaultsToAmd64() throws MalformedURLException {
        assertEquals(
                BASE + "linux/amd64/sysdig-cli-scanner",
                ScannerBinaryProvider.downloadURLForVersion("1.27.2", "Windows 11", "ppc64")
                        .toString());
    }
}
