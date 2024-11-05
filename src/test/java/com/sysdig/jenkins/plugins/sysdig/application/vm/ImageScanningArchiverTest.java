package com.sysdig.jenkins.plugins.sysdig.application.vm;

import com.sysdig.jenkins.plugins.sysdig.application.vm.report.PolicyEvaluationReport;
import com.sysdig.jenkins.plugins.sysdig.application.vm.report.PolicyEvaluationSummary;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.ImageScanningResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImageScanningArchiverTest {

  @Mock
  private ReportProcessor reportProcessor;

  @Mock
  private ReportStorage reportStorage;

  @InjectMocks
  private ImageScanningArchiver imageScanningArchiver;

  private ImageScanningResult mockScanResult;
  private PolicyEvaluationReport mockPolicyResult;
  private PolicyEvaluationSummary mockPolicySummary;
  private AutoCloseable mocks;

  @BeforeEach
  void initializeMocks() {
    mocks = MockitoAnnotations.openMocks(this);
    mockScanResult = mock(ImageScanningResult.class);
    mockPolicyResult = mock(PolicyEvaluationReport.class);
    mockPolicySummary = mock(PolicyEvaluationSummary.class);

    when(reportProcessor.processPolicyEvaluation(mockScanResult)).thenReturn(mockPolicyResult);
    when(reportProcessor.generateGatesSummary(mockPolicyResult, mockScanResult)).thenReturn(mockPolicySummary);
  }

  @AfterEach
  void closeMocks() throws Exception {
    mocks.close();
  }

  @Test
  void whenArchiveScanResultIsCalledShouldSaveAllReports() throws IOException, InterruptedException {
    imageScanningArchiver.archiveScanResult(mockScanResult);

    verify(reportStorage).savePolicyReport(mockScanResult, mockPolicyResult);
    verify(reportStorage).saveVulnerabilityReport(mockScanResult);
    verify(reportStorage).saveRawVulnerabilityReport(mockScanResult);
    verify(reportStorage).archiveResults(mockScanResult, mockPolicySummary);
  }

  @Test
  void whenSavePolicyReportFailsShouldThrowIOException() throws IOException, InterruptedException {
    doThrow(new IOException("IO Error")).when(reportStorage).savePolicyReport(mockScanResult, mockPolicyResult);

    IOException exception = assertThrows(IOException.class, () -> {
      imageScanningArchiver.archiveScanResult(mockScanResult);
    });

    assertEquals("IO Error", exception.getMessage());

    verify(reportStorage).savePolicyReport(mockScanResult, mockPolicyResult);
    verify(reportStorage, never()).saveVulnerabilityReport(any());
    verify(reportStorage, never()).saveRawVulnerabilityReport(any());
    verify(reportStorage, never()).archiveResults(any(), any());
  }

  @Test
  void whenSaveVulnerabilityReportIsInterruptedShouldThrowInterruptedException() throws IOException, InterruptedException {
    doThrow(new InterruptedException("Interrupted")).when(reportStorage).saveVulnerabilityReport(mockScanResult);

    InterruptedException exception = assertThrows(InterruptedException.class, () -> {
      imageScanningArchiver.archiveScanResult(mockScanResult);
    });

    assertEquals("Interrupted", exception.getMessage());

    verify(reportStorage).savePolicyReport(mockScanResult, mockPolicyResult);
    verify(reportStorage).saveVulnerabilityReport(mockScanResult);
    verify(reportStorage, never()).saveRawVulnerabilityReport(any());
    verify(reportStorage, never()).archiveResults(any(), any());
  }

  @Test
  void whenArchiveScanResultIsCalledShouldUseCorrectReportData() throws IOException, InterruptedException {
    imageScanningArchiver.archiveScanResult(mockScanResult);

    ArgumentCaptor<PolicyEvaluationReport> reportCaptor = ArgumentCaptor.forClass(PolicyEvaluationReport.class);
    ArgumentCaptor<PolicyEvaluationSummary> summaryCaptor = ArgumentCaptor.forClass(PolicyEvaluationSummary.class);

    verify(reportStorage).savePolicyReport(eq(mockScanResult), reportCaptor.capture());
    assertSame(mockPolicyResult, reportCaptor.getValue());

    verify(reportStorage).archiveResults(eq(mockScanResult), summaryCaptor.capture());
    assertSame(mockPolicySummary, summaryCaptor.getValue());
  }
}
