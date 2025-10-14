package com.sysdig.jenkins.plugins.sysdig.domain.vm;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.sysdig.jenkins.plugins.sysdig.domain.SysdigLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    void whenTheScannerFailsItThrowsInterruptedException() throws Exception {
        String imageName = "my-image";
        when(mockScanner.scanImage(imageName)).thenThrow(new InterruptedException());
        assertThrows(
                InterruptedException.class,
                () ->
                        // When
                        service.scan(imageName));

        // Then (the expected exception is declared in the annotation)
    }

    @Test
    void whenImageNameIsNullItThrowsNullPointerException() {
        assertThrows(
                NullPointerException.class,
                () ->
                        // When
                        service.scan(null)); // Passing null image name

        // Then (the expected exception is declared in the annotation)
    }

    @Test
    void whenImageNameIsEmptyItThrowsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        // When
                        service.scan("")); // Passing empty image name

        // Then (the expected exception is declared in the annotation)
    }
}
