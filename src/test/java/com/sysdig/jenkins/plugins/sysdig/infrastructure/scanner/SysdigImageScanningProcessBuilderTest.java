package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.SysdigProcessBuilderBase.LogLevel;
import java.util.List;
import org.junit.jupiter.api.Test;

class SysdigImageScanningProcessBuilderTest {

    private SysdigImageScanningProcessBuilder builder() {
        return new SysdigImageScanningProcessBuilder("/path/to/scanner", "my-token");
    }

    @Test
    void byDefaultItEmitsTheCliPathApiUrlLogLevelAndImage() {
        List<String> args = builder().withImageToScan("ubuntu:22.04").toCommandLineArguments();

        assertEquals("/path/to/scanner", args.get(0));
        assertTrue(args.contains("--apiurl=https://secure.sysdig.com"));
        assertTrue(args.contains("--loglevel=info"));
        // The image is always the last argument
        assertEquals("ubuntu:22.04", args.get(args.size() - 1));
    }

    @Test
    void theApiTokenIsNeverPassedAsACommandLineArgument() {
        List<String> args = builder().withImageToScan("ubuntu:22.04").toCommandLineArguments();
        assertFalse(args.stream().anyMatch(arg -> arg.contains("my-token")));
    }

    @Test
    void itEmitsTheEngineUrlOutputDbAndCachePaths() {
        List<String> args = builder()
                .withEngineURL("https://eu1.sysdig.com")
                .withScanResultOutputPath("/tmp/out.json")
                .withDBPath("/tmp/db")
                .withCachePath("/tmp/cache")
                .withImageToScan("alpine")
                .toCommandLineArguments();

        assertTrue(args.contains("--apiurl=https://eu1.sysdig.com"));
        assertTrue(args.contains("--output=json-file=/tmp/out.json"));
        assertTrue(args.contains("--dbpath=/tmp/db"));
        assertTrue(args.contains("--cachepath=/tmp/cache"));
    }

    @Test
    void debugLogLevelIsEmittedWhenConfigured() {
        List<String> args = builder().withLogLevel(LogLevel.DEBUG).toCommandLineArguments();
        assertTrue(args.contains("--loglevel=debug"));
        assertFalse(args.contains("--loglevel=info"));
    }

    @Test
    void skipTlsVerifyIsEmittedOnlyWhenVerificationIsDisabled() {
        assertFalse(builder().withTLSVerification(true).toCommandLineArguments().contains("--skiptlsverify"));
        assertTrue(builder().withTLSVerification(false).toCommandLineArguments().contains("--skiptlsverify"));
    }

    @Test
    void consoleLogAndSeparateByLayerAreEmittedOnlyWhenEnabled() {
        List<String> off = builder().toCommandLineArguments();
        assertFalse(off.contains("--console-log"));
        assertFalse(off.contains("--separate-by-layer"));

        List<String> on = builder().withConsoleLog().withSeparateByLayer(true).toCommandLineArguments();
        assertTrue(on.contains("--console-log"));
        assertTrue(on.contains("--separate-by-layer"));
    }

    @Test
    void eachConfiguredPolicyIsEmittedAsItsOwnFlag() {
        List<String> args = builder()
                .withPoliciesToApplySeparatedBySpace("policy-a policy-b")
                .toCommandLineArguments();
        assertTrue(args.contains("--policy=policy-a"));
        assertTrue(args.contains("--policy=policy-b"));
    }

    @Test
    void extraParametersArePlacedBeforeTheImage() {
        List<String> args = builder()
                .withExtraParametersSeparatedBySpace("--foo --bar")
                .withImageToScan("alpine")
                .toCommandLineArguments();

        assertTrue(args.contains("--foo"));
        assertTrue(args.contains("--bar"));
        assertEquals("alpine", args.get(args.size() - 1));
        assertTrue(args.indexOf("--foo") < args.indexOf("alpine"));
    }

    @Test
    void blankPoliciesAndExtraParametersAreIgnored() {
        List<String> args = builder()
                .withPoliciesToApplySeparatedBySpace("   ")
                .withExtraParametersSeparatedBySpace("   ")
                .toCommandLineArguments();

        assertFalse(args.stream().anyMatch(arg -> arg.startsWith("--policy=")));
    }

    @Test
    void withMethodsAreImmutableAndDoNotMutateTheOriginalBuilder() {
        SysdigImageScanningProcessBuilder original = builder();
        original.withImageToScan("alpine").withConsoleLog().withSeparateByLayer(true);

        List<String> args = original.toCommandLineArguments();
        assertFalse(args.contains("--console-log"));
        assertFalse(args.contains("--separate-by-layer"));
        // image untouched on the original (defaults to empty)
        assertEquals("", args.get(args.size() - 1));
    }
}
