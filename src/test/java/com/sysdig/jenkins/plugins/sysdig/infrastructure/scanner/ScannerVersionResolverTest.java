package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.sysdig.jenkins.plugins.sysdig.application.vm.ImageScanningConfig;
import org.junit.jupiter.api.Test;

class ScannerVersionResolverTest {
    private final String pinnedVersion = new ScannerVersionResolver(mock(ImageScanningConfig.class)).pinnedVersion();

    private ImageScanningConfig configWith(String cliVersionToApply, String customCliVersion) {
        ImageScanningConfig config = mock(ImageScanningConfig.class);
        lenient().when(config.getCliVersionToApply()).thenReturn(cliVersionToApply);
        lenient().when(config.getCustomCliVersion()).thenReturn(customCliVersion);
        return config;
    }

    @Test
    void whenCliVersionToApplyIsNullItReturnsThePinnedVersion() {
        ScannerVersionResolver resolver = new ScannerVersionResolver(configWith(null, ""));
        assertEquals(pinnedVersion, resolver.resolve());
    }

    @Test
    void whenCliVersionToApplyIsEmptyItReturnsThePinnedVersion() {
        ScannerVersionResolver resolver = new ScannerVersionResolver(configWith("", ""));
        assertEquals(pinnedVersion, resolver.resolve());
    }

    @Test
    void whenCliVersionToApplyIsNotCustomItReturnsThePinnedVersion() {
        ScannerVersionResolver resolver = new ScannerVersionResolver(configWith("global", "9.9.9"));
        assertEquals(pinnedVersion, resolver.resolve());
    }

    @Test
    void whenCliVersionToApplyIsCustomButCustomVersionIsEmptyItReturnsThePinnedVersion() {
        ScannerVersionResolver resolver = new ScannerVersionResolver(configWith("custom", ""));
        assertEquals(pinnedVersion, resolver.resolve());
    }

    @Test
    void whenCliVersionToApplyIsCustomWithACustomVersionItReturnsThatVersion() {
        ScannerVersionResolver resolver = new ScannerVersionResolver(configWith("custom", "1.30.0"));
        assertEquals("1.30.0", resolver.resolve());
    }
}
