package com.sysdig.jenkins.plugins.sysdig.application.vm;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.sysdig.jenkins.plugins.sysdig.TestMother;
import com.sysdig.jenkins.plugins.sysdig.application.vm.report.PolicyEvaluationReport;
import com.sysdig.jenkins.plugins.sysdig.domain.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.ImageScanner;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.EvaluationResult;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.Metadata;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.ScanResult;
import hudson.AbortException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImageScanningApplicationServiceTest {
    private ReportStorage reportStorage;
    private ImageScanner scanner;
    private SysdigLogger logger;
    private ImageScanningApplicationService service;
    private ImageScanningConfig config;

    @BeforeEach
    void setUp() {
        reportStorage = mock(ReportStorage.class);
        scanner = mock(ImageScanner.class);
        logger = mock(SysdigLogger.class);
        service = new ImageScanningApplicationService(reportStorage, scanner, logger);
        config = mock(ImageScanningConfig.class);
    }

    @Test
    void whenRunScanIsExecutedItHandlesAllScenarios() throws Exception {
        // Given
        when(config.getImageName()).thenReturn("test-image");
        when(config.getBailOnPluginFail()).thenReturn(false);
        when(config.getBailOnFail()).thenReturn(false);
        ScanResult result = TestMother.scanResultForUbuntu2204().toDomain().get();

        when(scanner.scanImage(anyString())).thenReturn(result);

        // When
        service.runScan(config);

        // Then
        verify(config).printWith(logger);
        verify(logger).logInfo(contains("Sysdig Secure Container Image Scanner Plugin step result - Failed"));
        verify(reportStorage, times(1)).savePolicyReport(eq(result), any(PolicyEvaluationReport.class));
        verify(reportStorage, times(1)).saveVulnerabilityReport(eq(result));
        verify(reportStorage, times(1)).saveRawVulnerabilityReport(eq(result));
        verify(reportStorage, times(1)).archiveResults(eq(result));
    }

    @Test
    void whenFinalActionIsFailAndBailOnFailIsTrueItThrowsAbortExceptionAndHandlesArchiving() throws Exception {
        when(config.getImageName()).thenReturn("test-image");
        when(config.getBailOnPluginFail()).thenReturn(false);
        when(config.getBailOnFail()).thenReturn(true);
        ScanResult result = mock(ScanResult.class);
        when(result.metadata()).thenReturn(mock(Metadata.class));
        when(result.evaluationResult()).thenReturn(EvaluationResult.Failed);
        when(scanner.scanImage(anyString())).thenReturn(result);

        assertThrows(AbortException.class, () -> service.runScan(config));

        verify(reportStorage, times(1)).savePolicyReport(eq(result), any(PolicyEvaluationReport.class));
        verify(reportStorage, times(1)).saveVulnerabilityReport(eq(result));
        verify(reportStorage, times(1)).saveRawVulnerabilityReport(eq(result));
        verify(reportStorage, times(1)).archiveResults(eq(result));
    }

    @Test
    void whenPluginFailsAndBailOnPluginFailIsFalseItMarksAsSuccessfulDespitePluginFailureAndLogs() throws Exception {
        // Given
        when(config.getImageName()).thenReturn("test-image");
        when(config.getBailOnPluginFail()).thenReturn(false);
        when(scanner.scanImage(anyString())).thenThrow(new RuntimeException("Scanning failed"));

        // When
        service.runScan(config);

        // Then
        verify(logger)
                .logWarn(
                        contains(
                                "Marking Sysdig Secure Container Image Scanner step as successful despite errors in plugin execution"));
    }

    @Test
    void whenPluginFailsAndBailOnPluginFailIsTrueItThrowsAbortException() throws Exception {
        when(config.getImageName()).thenReturn("test-image");
        when(config.getBailOnPluginFail()).thenReturn(true);
        when(scanner.scanImage(anyString())).thenThrow(new RuntimeException("Scanning failed"));
        assertThrows(AbortException.class, () -> service.runScan(config));
    }
}
