package com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.iac.entrypoint;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.sysdig.jenkins.plugins.sysdig.TestMother;
import com.sysdig.jenkins.plugins.sysdig.domain.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.iac.ui.IaCAction;
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.slaves.WorkspaceList;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class IaCScanningBuilderTest {
    private JenkinsRule jenkins;

    @TempDir
    private Path workspaceDir;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    private FilePath workspace() {
        return new FilePath(workspaceDir.toFile());
    }

    private IaCScanningBuilder builderScanning(String path) {
        IaCScanningBuilder builder = new IaCScanningBuilder("sysdig-secure");
        builder.setPath(path);
        return builder;
    }

    /**
     * Two IaC steps share the workspace, so a fixed report name lets one scan read the previous
     * scan's report when its own run produced none (bad params, killed scanner, parallel stages).
     * The report also has to stay out of the workspace: with the default path the scanner walks the
     * workspace itself and would run into the report of its own (or of a parallel) run.
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
                build, mock(SysdigLogger.class), workspace().child("does-not-exist.json"));
        FilePath empty = IaCScanningBuilder.createScanResultOutputFile(workspace());
        builder.reportAndAttachScanResult(build, mock(SysdigLogger.class), empty);

        assertTrue(build.getActions(IaCAction.class).isEmpty());
    }

    @Test
    void everyScanInABuildAttachesItsOwnReachableAction() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        IaCScanningBuilder builder = builderScanning("infra");

        FilePath report = IaCScanningBuilder.createScanResultOutputFile(workspace());
        report.write(TestMother.iacScanResultJson(), "UTF-8");
        builder.reportAndAttachScanResult(build, mock(SysdigLogger.class), report);
        builder.reportAndAttachScanResult(build, mock(SysdigLogger.class), report);

        var actions = build.getActions(IaCAction.class);
        assertEquals(2, actions.size());
        assertEquals("sysdig-secure-iac-results", actions.get(0).getUrlName());
        assertEquals("sysdig-secure-iac-results-2", actions.get(1).getUrlName());
        assertEquals("infra", actions.get(0).getScannedPath());
        assertTrue(
                actions.get(0).getDisplayName().contains("infra"),
                actions.get(0).getDisplayName());
    }
}
