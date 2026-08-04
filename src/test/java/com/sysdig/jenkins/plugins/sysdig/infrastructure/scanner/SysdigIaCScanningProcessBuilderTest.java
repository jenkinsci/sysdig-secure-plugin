package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SysdigIaCScanningProcessBuilderTest {

    private SysdigIaCScanningProcessBuilder builder() {
        return new SysdigIaCScanningProcessBuilder("/path/to/scanner", "my-token");
    }

    @Test
    void byDefaultItScansTheCurrentDirectory() {
        List<String> args = builder().toCommandLineArguments();

        assertEquals("/path/to/scanner", args.get(0));
        assertTrue(args.contains("--iac"));
        assertEquals(".", args.get(args.size() - 1), "the path to scan is the last argument");
    }

    /**
     * The step passes its configured path straight through, and that path is empty until someone fills
     * it in. An empty argument is not "no argument": it left the scanner with a path of "" instead of
     * the current directory.
     */
    @Test
    void anEmptyConfiguredPathFallsBackToTheCurrentDirectory() {
        assertEquals(".", lastPathOf(builder().withPathsToScan("")));
        assertEquals(".", lastPathOf(builder().withPathsToScan("   ")));
        assertEquals(".", lastPathOf(builder().withPathsToScan((String) null)));
    }

    @Test
    void itKeepsEveryConfiguredPathAndTrimsThem() {
        List<String> args =
                builder().withPathsToScan("infra/", "  charts/  ", "").toCommandLineArguments();

        assertEquals(List.of("infra/", "charts/"), args.subList(args.size() - 2, args.size()));
        assertFalse(args.contains(""), "no empty path argument");
    }

    private static String lastPathOf(SysdigIaCScanningProcessBuilder builder) {
        List<String> args = builder.toCommandLineArguments();
        return args.get(args.size() - 1);
    }
}
