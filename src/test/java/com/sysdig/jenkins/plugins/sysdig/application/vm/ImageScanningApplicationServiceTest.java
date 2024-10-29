package com.sysdig.jenkins.plugins.sysdig.application.vm;

import com.sysdig.jenkins.plugins.sysdig.TestMother;
import com.sysdig.jenkins.plugins.sysdig.domain.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.ImageScanner;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.ImageScanningResult;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.ImageScanningResult.FinalAction;
import com.sysdig.jenkins.plugins.sysdig.application.vm.report.PolicyEvaluationReport;
import com.sysdig.jenkins.plugins.sysdig.application.vm.report.PolicyEvaluationSummary;
import org.junit.Before;
import org.junit.Test;

import hudson.AbortException;

import static org.mockito.Mockito.*;

public class ImageScanningApplicationServiceTest {

  private ReportStorage reportStorage;
  private ImageScanner scanner;
  private SysdigLogger logger;
  private ImageScanningApplicationService service;
  private ImageScanningConfig config;

  @Before
  public void setUp() {
    reportStorage = mock(ReportStorage.class);
    scanner = mock(ImageScanner.class);
    logger = mock(SysdigLogger.class);
    service = new ImageScanningApplicationService(reportStorage, scanner, logger);
    config = mock(ImageScanningConfig.class);
  }

  @Test
  public void whenRunScanIsExecutedItHandlesAllScenarios() throws Exception {
    // Given
    when(config.getImageName()).thenReturn("test-image");
    when(config.getBailOnPluginFail()).thenReturn(false);
    when(config.getBailOnFail()).thenReturn(false);
    ImageScanningResult result = TestMother.imageScanResult();
    PolicyEvaluationReport policyEvaluationReport = new PolicyEvaluationReport(false);
    PolicyEvaluationSummary policyEvaluationSummary = new PolicyEvaluationSummary();


    when(scanner.scanImage(anyString())).thenReturn(result);

    // When
    service.runScan(config);

    // Then
    verify(config).printWith(logger);
    verify(logger).logInfo(contains("Sysdig Secure Container Image Scanner Plugin step result - FAIL"));
    verify(reportStorage, times(1)).savePolicyReport(eq(result), any(PolicyEvaluationReport.class));
    verify(reportStorage, times(1)).saveVulnerabilityReport(eq(result));
    verify(reportStorage, times(1)).saveRawVulnerabilityReport(eq(result));
    verify(reportStorage, times(1)).archiveResults(eq(result), any(PolicyEvaluationSummary.class));
  }

  @Test(expected = AbortException.class)
  public void whenFinalActionIsFailAndBailOnFailIsTrueItThrowsAbortExceptionAndHandlesArchiving() throws Exception {
    // Given
    when(config.getImageName()).thenReturn("test-image");
    when(config.getBailOnPluginFail()).thenReturn(false);
    when(config.getBailOnFail()).thenReturn(true);
    ImageScanningResult result = mock(ImageScanningResult.class);
    when(result.getEvalStatus()).thenReturn("fail");
    PolicyEvaluationReport policyEvaluationReport = mock(PolicyEvaluationReport.class);
    PolicyEvaluationSummary policyEvaluationSummary = mock(PolicyEvaluationSummary.class);

    when(result.getFinalAction()).thenReturn(FinalAction.ActionFail);
    when(scanner.scanImage(anyString())).thenReturn(result);

    // When
    service.runScan(config);

    // Then
    verify(reportStorage, times(1)).savePolicyReport(eq(result), any(PolicyEvaluationReport.class));
    verify(reportStorage, times(1)).saveVulnerabilityReport(eq(result));
    verify(reportStorage, times(1)).saveRawVulnerabilityReport(eq(result));
    verify(reportStorage, times(1)).archiveResults(eq(result), any(PolicyEvaluationSummary.class));
  }

  @Test
  public void whenPluginFailsAndBailOnPluginFailIsFalseItMarksAsSuccessfulDespitePluginFailureAndLogs() throws Exception {
    // Given
    when(config.getImageName()).thenReturn("test-image");
    when(config.getBailOnPluginFail()).thenReturn(false);
    when(scanner.scanImage(anyString())).thenThrow(new RuntimeException("Scanning failed"));

    // When
    service.runScan(config);

    // Then
    verify(logger).logWarn(contains("Marking Sysdig Secure Container Image Scanner step as successful despite errors in plugin execution"));
  }

  @Test(expected = AbortException.class)
  public void whenPluginFailsAndBailOnPluginFailIsTrueItThrowsAbortException() throws Exception {
    // Given
    when(config.getImageName()).thenReturn("test-image");
    when(config.getBailOnPluginFail()).thenReturn(true);
    when(scanner.scanImage(anyString())).thenThrow(new RuntimeException("Scanning failed"));

    // When
    service.runScan(config);
  }
}
