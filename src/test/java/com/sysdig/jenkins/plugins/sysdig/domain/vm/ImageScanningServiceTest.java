package com.sysdig.jenkins.plugins.sysdig.domain.vm;

import com.sysdig.jenkins.plugins.sysdig.domain.SysdigLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ImageScanningServiceTest {
  private ImageScanner mockScanner;
  private ScanResultArchiver mockArchiver;
  private SysdigLogger mockLogger;
  private ImageScanningService service;

  @BeforeEach
  void setUp() {
    // Inicializa los mocks
    mockScanner = mock(ImageScanner.class);
    mockArchiver = mock(ScanResultArchiver.class);
    mockLogger = mock(SysdigLogger.class);
    // Crea la instancia del servicio con los mocks
    service = new ImageScanningService(mockScanner, mockArchiver, mockLogger);
  }

  @Test
  void whenTheScannerReturnsSuccessItReturnsTheSuccessBack() throws InterruptedException, IOException {
    // Given
    String imageName = "my-image";
    ImageScanningResult mockResult = mock(ImageScanningResult.class);
    when(mockScanner.scanImage(imageName)).thenReturn(mockResult);
    when(mockResult.getFinalAction()).thenReturn(ImageScanningResult.FinalAction.ActionPass);

    // When
    ImageScanningResult.FinalAction finalAction = service.scanAndArchiveResult(imageName);

    // Then
    verify(mockScanner).scanImage(imageName);
    verify(mockArchiver).archiveScanResult(mockResult);
    assertEquals(ImageScanningResult.FinalAction.ActionPass, finalAction);
  }

  @Test
  void whenTheScannerFailsItThrowsInterruptedException() throws Exception {
    String imageName = "my-image";
    when(mockScanner.scanImage(imageName)).thenThrow(new InterruptedException());
    assertThrows(InterruptedException.class, () ->
      // When
      service.scanAndArchiveResult(imageName));

    // Then (the expected exception is declared in the annotation)
  }

  @Test
  void whenArchivingFailsItLogsAnError() throws InterruptedException, IOException {
    // Given
    String imageName = "my-image";
    ImageScanningResult mockResult = mock(ImageScanningResult.class);
    when(mockScanner.scanImage(imageName)).thenReturn(mockResult);
    when(mockResult.getFinalAction()).thenReturn(ImageScanningResult.FinalAction.ActionPass);
    doThrow(new IOException()).when(mockArchiver).archiveScanResult(mockResult);

    // When
    service.scanAndArchiveResult(imageName);

    // Then
    verify(mockLogger).logError(eq("Recording failure to build reports and moving on with plugin operation"), any()); // Verify that an error was logged
  }

  @Test
  void whenImageNameIsNullItThrowsNullPointerException() {
    assertThrows(NullPointerException.class, () ->
      // When
      service.scanAndArchiveResult(null)); // Passing null image name

    // Then (the expected exception is declared in the annotation)
  }

  @Test
  void whenImageNameIsEmptyItThrowsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () ->
      // When
      service.scanAndArchiveResult("")); // Passing empty image name

    // Then (the expected exception is declared in the annotation)
  }


}
