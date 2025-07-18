package com.sysdig.jenkins.plugins.sysdig.e2e;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.iac.entrypoint.IaCScanningBuilder;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.vm.entrypoint.ImageScanningBuilder;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import java.util.function.Consumer;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jvnet.hudson.test.JenkinsRule;

public class JenkinsTestHelpers {
    private final JenkinsRule jenkins;

    public static class FreeStyleProjectBuilder {
        private final JenkinsRule jenkins;
        private final FreeStyleProject project;

        private FreeStyleProjectBuilder(JenkinsRule jenkins, FreeStyleProject project) {
            this.jenkins = jenkins;
            this.project = project;
        }

        public FreeStyleProject build() throws Exception {
            return this.simulateExecutionInRemoteNode().project;
        }

        public FreeStyleProjectBuilder simulateExecutionInRemoteNode() throws Exception {
            this.project.setAssignedNode(this.jenkins.createOnlineSlave());
            return this;
        }

        public FreeStyleProjectBuilder withConfig(Consumer<ImageScanningBuilder> func) {
            var builder = (ImageScanningBuilder) project.getBuildersList().get(0);
            func.accept(builder);
            return this;
        }

        public FreeStyleProjectBuilder withGlobalConfig(Consumer<ImageScanningBuilder.GlobalConfiguration> func) {
            var builder = (ImageScanningBuilder) project.getBuildersList().get(0);
            func.accept(builder.getDescriptor());
            return this;
        }
    }

    public static class PipelineJobBuilder {
        private final JenkinsRule jenkins;
        private final WorkflowJob job;
        private final String script;

        private PipelineJobBuilder(JenkinsRule jenkins, WorkflowJob job, String script) {
            this.jenkins = jenkins;
            this.job = job;
            this.script = script;
        }

        public PipelineJobBuilder withGlobalConfig(Consumer<ImageScanningBuilder.GlobalConfiguration> func) {
            var globalConf = jenkins.getInstance().getDescriptorByType(ImageScanningBuilder.GlobalConfiguration.class);
            func.accept(globalConf);
            return this;
        }

        public WorkflowJob buildWithRemoteExecution() throws Exception {
            var slave = jenkins.createOnlineSlave();
            String slaveLabel = slave.getSelfLabel().getName();
            String modifiedScript = "node('" + slaveLabel + "') {\n" + "    " + script + "\n" + "}";

            job.setDefinition(new CpsFlowDefinition(modifiedScript, true));
            return job;
        }

        public WorkflowJob buildWithLocalExecution() throws Descriptor.FormException {
            String modifiedScript = "node {\n" + "    " + script + "\n" + "}";
            job.setDefinition(new CpsFlowDefinition(modifiedScript, true));
            return job;
        }
    }

    public static class IaCScanProjectBuilder {
        private final JenkinsRule jenkins;
        private final FreeStyleProject project;

        private IaCScanProjectBuilder(JenkinsRule jenkins, FreeStyleProject project) {
            this.jenkins = jenkins;
            this.project = project;
        }

        public FreeStyleProject build() throws Exception {
            return this.simulateExecutionInRemoteNode().project;
        }

        public IaCScanProjectBuilder simulateExecutionInRemoteNode() throws Exception {
            this.project.setAssignedNode(this.jenkins.createOnlineSlave());
            return this;
        }

        public IaCScanProjectBuilder withConfig(Consumer<IaCScanningBuilder> func) {
            var builder = (IaCScanningBuilder) project.getBuildersList().get(0);
            func.accept(builder);
            return this;
        }
    }

    public JenkinsTestHelpers(JenkinsRule jenkins) {
        this.jenkins = jenkins;
    }

    public FreeStyleProjectBuilder createFreestyleProjectWithImageScanningBuilder(String imageName) throws Exception {
        var project = jenkins.createFreeStyleProject();
        var builder = new ImageScanningBuilder(imageName);
        project.getBuildersList().add(builder);
        return new FreeStyleProjectBuilder(jenkins, project);
    }

    public PipelineJobBuilder createPipelineJobWithScript(String script) throws Exception {
        var job = jenkins.createProject(WorkflowJob.class, "test-pipeline");
        return new PipelineJobBuilder(jenkins, job, script);
    }

    public IaCScanProjectBuilder createFreestyleProjectWithIaCScanBuilder() throws Exception {
        var project = jenkins.createFreeStyleProject();
        var builder = new IaCScanningBuilder("");
        project.getBuildersList().add(builder);
        return new IaCScanProjectBuilder(jenkins, project);
    }

    public void configureSysdigCredentials() throws Descriptor.FormException {
        var creds = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, "sysdig-secure", "sysdig-secure", "", "foo-token");
        SystemCredentialsProvider.getInstance().getCredentials().add(creds);
    }
}
