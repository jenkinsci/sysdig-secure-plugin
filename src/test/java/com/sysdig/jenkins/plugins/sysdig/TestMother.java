package com.sysdig.jenkins.plugins.sysdig;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.sysdig.jenkins.plugins.sysdig.infrastructure.json.GsonBuilder;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.iac.v1.JsonIaCScanResultV1;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1.JsonScanResultV1;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

/**
 * Provides pre-configured objects for testing, following the Object Mother pattern.
 */
public class TestMother {

    private static final String FIXTURE_DIR = "com/sysdig/jenkins/plugins/sysdig/infrastructure/scanner/report/v1/";

    /**
     * Versions of the sysdig-cli-scanner whose recorded output is checked into the repository as a
     * fixture. These track the "testing window" managed from the justfile: the newest published
     * version and the oldest version still within the support window. When they are bumped
     * (via {@code just update-cli-scanner} / {@code just update-oldest-cli-scanner}), regenerate the
     * matching fixtures with {@code just generate-scanner-fixtures}.
     */
    public static final String NEWEST_FIXTURE_VERSION = "1.29.0"; // newest-version-marker

    public static final String OLDEST_FIXTURE_VERSION = "1.23.0"; // oldest-version-marker

    private static final String NEWEST_FIXTURE = FIXTURE_DIR + "scanner_newest_scan_result.json.gz";
    private static final String OLDEST_FIXTURE = FIXTURE_DIR + "scanner_oldest_scan_result.json.gz";
    private static final String IAC_FIXTURE =
            "com/sysdig/jenkins/plugins/sysdig/infrastructure/scanner/report/iac/v1/iac_scan_result.json";

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

    /**
     * Returns a sample Result object for testing.
     *
     * @return a test Result object.
     */
    public static JsonScanResultV1 scanResultWithWholeImageAcceptedRisk() {
        String resourcePath =
                "com/sysdig/jenkins/plugins/sysdig/infrastructure/scanner/report/v1/dummy-vuln-app_latest_accepted_risk_in_image.json";
        InputStream imageStream = TestMother.class.getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull(imageStream);

        return GsonBuilder.build()
                .fromJson(new InputStreamReader(imageStream, StandardCharsets.UTF_8), JsonScanResultV1.class);
    }

    /**
     * Returns a scan result produced by sysdig-cli-scanner 1.27.2, the first version
     * emitting the fpkev and providersMetadata.vulndb.cvssScore.temporal_score fields.
     * The fixture is the raw scanner output, gzipped to keep the repository small.
     *
     * @return a test Result object.
     */
    public static JsonScanResultV1 scanResultFromScanner1_27_2() {
        String resourcePath =
                "com/sysdig/jenkins/plugins/sysdig/infrastructure/scanner/report/v1/scanner_1.27.2_debian_11.4-slim.json.gz";
        InputStream imageStream = TestMother.class.getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull(imageStream);

        try {
            return GsonBuilder.build()
                    .fromJson(
                            new InputStreamReader(new GZIPInputStream(imageStream), StandardCharsets.UTF_8),
                            JsonScanResultV1.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static JsonScanResultV1 scanResultWithPackageWithoutLayer() {
        String resourcePath =
                "com/sysdig/jenkins/plugins/sysdig/infrastructure/scanner/report/v1/scanner_1.26.0_package_with_null_layer.json";
        InputStream imageStream = TestMother.class.getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull(imageStream);

        return GsonBuilder.build()
                .fromJson(new InputStreamReader(imageStream, StandardCharsets.UTF_8), JsonScanResultV1.class);
    }

    /**
     * Returns the raw output recorded from the newest supported sysdig-cli-scanner
     * ({@link #NEWEST_FIXTURE_VERSION}). Regenerate with {@code just generate-scanner-fixtures}.
     *
     * @return a test Result object.
     */
    public static JsonScanResultV1 scanResultFromNewestScanner() {
        return loadGzippedScanResult(NEWEST_FIXTURE);
    }

    /**
     * Returns the raw output recorded from the oldest still-maintained sysdig-cli-scanner
     * ({@link #OLDEST_FIXTURE_VERSION}). Regenerate with {@code just generate-scanner-fixtures}.
     *
     * @return a test Result object.
     */
    public static JsonScanResultV1 scanResultFromOldestScanner() {
        return loadGzippedScanResult(OLDEST_FIXTURE);
    }

    private static JsonScanResultV1 loadGzippedScanResult(String resourcePath) {
        InputStream imageStream = TestMother.class.getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull(imageStream, "Missing fixture: " + resourcePath + " (run `just generate-scanner-fixtures`)");

        try {
            return GsonBuilder.build()
                    .fromJson(
                            new InputStreamReader(new GZIPInputStream(imageStream), StandardCharsets.UTF_8),
                            JsonScanResultV1.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns a sample IaC scan result, recorded from {@code sysdig-cli-scanner --iac --output-json}
     * against a Terraform project.
     *
     * @return a test IaC Result object.
     */
    public static JsonIaCScanResultV1 iacScanResult() {
        InputStream stream = TestMother.class.getClassLoader().getResourceAsStream(IAC_FIXTURE);
        assertNotNull(stream);

        return GsonBuilder.build()
                .fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonIaCScanResultV1.class);
    }

    /**
     * Returns the raw JSON of the sample IaC scan result, as persisted by the build action.
     */
    public static String iacScanResultJson() {
        try (InputStream stream = TestMother.class.getClassLoader().getResourceAsStream(IAC_FIXTURE)) {
            assertNotNull(stream);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
