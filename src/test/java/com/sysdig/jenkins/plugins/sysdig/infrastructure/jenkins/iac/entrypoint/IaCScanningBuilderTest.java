package com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.iac.entrypoint;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.sysdig.jenkins.plugins.sysdig.TestMother;
import com.sysdig.jenkins.plugins.sysdig.domain.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.iac.ui.IaCAction;
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.slaves.WorkspaceList;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.ArgumentCaptor;

@WithJenkins
class IaCScanningBuilderTest {
    private JenkinsRule jenkins;

    @TempDir
    private Path workspaceDir;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    /**
     * A subdirectory of the temporary directory, not the temporary directory itself: the report goes to
     * the workspace's {@code @tmp} <em>sibling</em>, which has to land inside the tree JUnit cleans up.
     */
    private FilePath workspace() throws Exception {
        FilePath workspace = new FilePath(workspaceDir.toFile()).child("workspace");
        workspace.mkdirs();
        return workspace;
    }

    /** Two failed controls over three resources, one of them at a severity outside high/medium/low. */
    private static final String CRITICAL_FINDING_REPORT = """
            {
              "result": {
                "type": "IaCGitScan",
                "metadata": {"sources": ["/"], "totalModules": 1, "totalResource": 3, "totalFolders": 1},
                "findingsSummaryBySeverity": {"critical": 2, "high": 1},
                "findings": [
                  {
                    "controlId": 1,
                    "name": "S3 - Block Public Access",
                    "severity": "Critical",
                    "resources": [
                      {"name": "aws_s3_bucket.one", "type": "AWS_S3_BUCKET",
                       "location": "source file: /", "source": "/"},
                      {"name": "aws_s3_bucket.two", "type": "AWS_S3_BUCKET",
                       "location": "source file: /", "source": "/"}
                    ],
                    "policies": ["All Posture Findings"],
                    "requirements": []
                  },
                  {
                    "controlId": 2,
                    "name": "ECR - Enabled Vulnerability Scanning",
                    "severity": "High",
                    "resources": [
                      {"name": "aws_ecr_repository.three", "type": "AWS_ECR_REPOSITORY",
                       "location": "source file: /", "source": "/"}
                    ],
                    "policies": ["All Posture Findings"],
                    "requirements": []
                  }
                ]
              }
            }
            """;

    private IaCScanningBuilder builderScanning(String path) {
        IaCScanningBuilder builder = new IaCScanningBuilder("sysdig-secure");
        builder.setPath(path);
        return builder;
    }

    /**
     * Two IaC steps share the workspace, so a fixed report name lets one scan read the previous
     * scan's report when its own run produced none (bad params, killed scanner, parallel stages).
     * The report also stays out of the workspace, to keep it out of any tree that gets scanned and out
     * of the checkout.
     */
    @Test
    void eachScanGetsItsOwnReportFileOutsideTheWorkspace() throws Exception {
        FilePath first = IaCScanningBuilder.createScanResultOutputFile(workspace());
        FilePath second = IaCScanningBuilder.createScanResultOutputFile(workspace());

        assertNotEquals(first.getRemote(), second.getRemote());
        assertTrue(first.getName().startsWith("sysdig-iac-scan-result"), first.getName());
        assertTrue(first.getName().endsWith(".json"), first.getName());

        assertEquals(
                WorkspaceList.tempDir(workspace()).getRemote(),
                first.getParent().getRemote(),
                "the report belongs in the workspace's @tmp sibling");
        assertEquals(0, workspace().list().size(), "nothing written inside the scanned workspace");
    }

    @Test
    void aMissingOrEmptyReportAttachesNoAction() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        IaCScanningBuilder builder = builderScanning("infra");

        builder.reportAndAttachScanResult(
                build, mock(SysdigLogger.class), workspace().child("does-not-exist.json"), workspace());
        FilePath empty = IaCScanningBuilder.createScanResultOutputFile(workspace());
        builder.reportAndAttachScanResult(build, mock(SysdigLogger.class), empty, workspace());

        assertTrue(build.getActions(IaCAction.class).isEmpty());
    }

    @Test
    void everyScanInABuildAttachesItsOwnReachableAction() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        IaCScanningBuilder builder = builderScanning("infra");

        FilePath report = IaCScanningBuilder.createScanResultOutputFile(workspace());
        report.write(TestMother.iacScanResultJson(), "UTF-8");
        builder.reportAndAttachScanResult(build, mock(SysdigLogger.class), report, workspace());
        builder.reportAndAttachScanResult(build, mock(SysdigLogger.class), report, workspace());

        var actions = build.getActions(IaCAction.class);
        assertEquals(2, actions.size());
        assertEquals("sysdig-secure-iac-results", actions.get(0).getUrlName());
        assertEquals("sysdig-secure-iac-results-2", actions.get(1).getUrlName());
        assertEquals("infra", actions.get(0).getScannedPath());
        assertTrue(
                actions.get(0).getDisplayName().contains("infra"),
                actions.get(0).getDisplayName());
    }

    /**
     * The console summary and the report page have to say the same thing about the same scan, so the
     * breakdown comes from the findings and covers every severity they carry: a fixed high/medium/low
     * triple, read off the scanner's own summary map, left a Critical out of the line that claims to
     * break the findings down, and counted something different from the page's violations tile.
     */
    @Test
    void theConsoleSummaryBreaksTheFindingsDownLikeTheReportPage() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        SysdigLogger logger = mock(SysdigLogger.class);

        FilePath report = IaCScanningBuilder.createScanResultOutputFile(workspace());
        report.write(CRITICAL_FINDING_REPORT, "UTF-8");
        builderScanning("infra").reportAndAttachScanResult(build, logger, report, workspace());

        var lines = ArgumentCaptor.forClass(String.class);
        verify(logger, atLeastOnce()).logInfo(lines.capture());
        String summary = String.join("\n", lines.getAllValues());

        assertTrue(summary.contains("Failed controls by severity: Critical=1, High=1"), summary);
        assertTrue(summary.contains("2 failed control(s) across 3 resource violation(s)"), summary);
    }

    /**
     * With no path configured the scan walks the workspace, so that is what the report has to name: the
     * step's own (empty) setting would leave the page unable to say what its module paths are relative to.
     */
    @Test
    void aScanWithoutAConfiguredPathReportsTheWorkspace() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        IaCScanningBuilder builder = builderScanning("");

        FilePath report = IaCScanningBuilder.createScanResultOutputFile(workspace());
        report.write(TestMother.iacScanResultJson(), "UTF-8");
        builder.reportAndAttachScanResult(build, mock(SysdigLogger.class), report, workspace());

        IaCAction attached = build.getActions(IaCAction.class).get(0);
        assertEquals(workspace().getRemote(), attached.getScannedPath());
        assertTrue(attached.getDisplayName().contains("workspace"), attached.getDisplayName());
    }

    /**
     * Deleting the report is cleanup: a failure to delete it must never reach the caller, where it would
     * skip the exit-code handling and turn a clean scan into a failed build, or replace the exception the
     * scan itself raised.
     */
    @Test
    void aReportThatCannotBeDeletedIsOnlyWarnedAbout() throws Exception {
        FilePath undeletable = mock(FilePath.class);
        when(undeletable.getRemote()).thenReturn("/ws@tmp/sysdig-iac-scan-result1.json");
        doThrow(new IOException("channel is closed")).when(undeletable).delete();
        SysdigLogger logger = mock(SysdigLogger.class);

        assertDoesNotThrow(() -> IaCScanningBuilder.deleteQuietly(undeletable, logger));

        var warning = ArgumentCaptor.forClass(String.class);
        verify(logger).logWarn(warning.capture());
        assertTrue(warning.getValue().contains("sysdig-iac-scan-result1.json"), warning.getValue());
        verify(logger, never()).logError(anyString());
        verify(logger, never()).logError(anyString(), any());
    }

    @Test
    void nothingToDeleteIsNotAWarning() {
        SysdigLogger logger = mock(SysdigLogger.class);

        assertDoesNotThrow(() -> IaCScanningBuilder.deleteQuietly(null, logger));

        verifyNoInteractions(logger);
    }
}
