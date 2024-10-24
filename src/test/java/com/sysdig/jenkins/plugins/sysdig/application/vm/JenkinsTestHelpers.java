package com.sysdig.jenkins.plugins.sysdig.application.vm;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.vm.entrypoint.ImageScanningBuilder;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.function.Consumer;

public class JenkinsTestHelpers {
  private final JenkinsRule jenkins;

  public static class FreeStyleProjectBuilder {
    private final FreeStyleProject project;

    private FreeStyleProjectBuilder(FreeStyleProject project) {
      this.project = project;
    }

    public FreeStyleProject build() {
      return this.project;
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

    public WorkflowJob build() throws Descriptor.FormException {
      job.setDefinition(new CpsFlowDefinition(script, true));
      return job;
    }
  }


  public JenkinsTestHelpers(JenkinsRule jenkins) {
    this.jenkins = jenkins;
  }

  public FreeStyleProjectBuilder createFreestyleProjectWithImageScanningBuilder(String imageName) throws Exception {
    var project = jenkins.createFreeStyleProject();
    var builder = new ImageScanningBuilder(imageName);
    project.getBuildersList().add(builder);
    return new FreeStyleProjectBuilder(project);
  }

  public PipelineJobBuilder createPipelineJobWithScript(String script) throws Exception {
    var job = jenkins.createProject(WorkflowJob.class, "test-pipeline");
    return new PipelineJobBuilder(jenkins, job, script);
  }

  public void configureSysdigCredentials() throws Descriptor.FormException {
    var creds = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "sysdig-secure", "sysdig-secure", "", "foo-token");
    SystemCredentialsProvider.getInstance().getCredentials().add(creds);
  }
}
