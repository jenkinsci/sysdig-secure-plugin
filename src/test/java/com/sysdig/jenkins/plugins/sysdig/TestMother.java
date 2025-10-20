package com.sysdig.jenkins.plugins.sysdig;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.sysdig.jenkins.plugins.sysdig.infrastructure.json.GsonBuilder;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1.JsonScanResultV1;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Provides pre-configured objects for testing, following the Object Mother pattern.
 */
public class TestMother {

    /**
     * Returns a sample Result object for testing.
     *
     * @return a test Result object.
     */
    public static JsonScanResultV1 scanResultForUbuntu2204() {
        String resourcePath = "com/sysdig/jenkins/plugins/sysdig/infrastructure/scanner/report/v1/ubuntu_22.04.json";
        InputStream imageStream = TestMother.class.getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull(imageStream);

        return GsonBuilder.build()
                .fromJson(new InputStreamReader(imageStream, StandardCharsets.UTF_8), JsonScanResultV1.class);
    }

    /**
     * Returns a sample Result object for testing.
     *
     * @return a test Result object.
     */
    public static JsonScanResultV1 scanResultForUbuntu2404() {
        String resourcePath = "com/sysdig/jenkins/plugins/sysdig/infrastructure/scanner/report/v1/ubuntu_24.04.json";
        InputStream imageStream = TestMother.class.getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull(imageStream);

        return GsonBuilder.build()
                .fromJson(new InputStreamReader(imageStream, StandardCharsets.UTF_8), JsonScanResultV1.class);
    }
}
