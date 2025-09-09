package com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.vm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sysdig.jenkins.plugins.sysdig.TestMother;
import com.sysdig.jenkins.plugins.sysdig.e2e.JenkinsTestHelpers;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class SysdigActionPersistenceTest {
    private JenkinsRule rule;
    private JenkinsTestHelpers helpers;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        this.rule = rule;
        helpers = new JenkinsTestHelpers(rule);
        helpers.configureSysdigCredentials();
    }

    @Test
    public void itPersistsCorrectlyAndReloadsFromDisk() throws Exception {
        var project = helpers.createFreestyleProjectWithImageScanningBuilder("freestyle")
                .withConfig(b -> b.setEngineCredentialsId("sysdig-secure"))
                .build();

        var build = rule.buildAndAssertStatus(Result.FAILURE, project);
        rule.waitUntilNoActivity();

        build.addAction(new SysdigAction(
                build,
                TestMother.scanResultForUbuntu2204().toDomain().get(),
                "SysdigSecureReport_11",
                "policy.json",
                "cve.json"));
        build.save();

        rule.jenkins.reload();

        FreeStyleProject projectFromDisk =
                rule.jenkins.getItemByFullName(project.getFullName(), FreeStyleProject.class);
        var buildLoadedFromDisk = projectFromDisk.getBuildByNumber(build.getNumber());
        var actionLoadedFromDisk = buildLoadedFromDisk.getAction(
                com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.vm.ui.SysdigAction.class);

        assertEquals("Sysdig Secure Report (ubuntu:22.04) (Failed)", actionLoadedFromDisk.getDisplayName());
    }
}
