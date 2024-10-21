package com.sysdig.jenkins.plugins.sysdig.application.vm;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jvnet.hudson.test.JenkinsRule;

public class JenkinsTestHelpers {
  private JenkinsRule jenkins;

  public JenkinsTestHelpers(JenkinsRule jenkins) {
    this.jenkins = jenkins;
  }

  public FreeStyleProject createFreestyleProjectWithNewEngineBuilder(String imageName) throws Exception {
    var project = jenkins.createFreeStyleProject();
    var builder = new ImageScanningBuilder(imageName);
    project.getBuildersList().add(builder);
    return project;
  }

  FreeStyleProject createFreestyleProjectWithNewEngineBuilder(String imageName, String credentialsId) throws Exception {
    var project = createFreestyleProjectWithNewEngineBuilder(imageName);
    var builder = (ImageScanningBuilder) project.getBuildersList().get(0);
    builder.setEngineCredentialsId(credentialsId);
    return project;
  }

  WorkflowJob createPipelineJobWithScript(String script) throws Exception {
    var job = jenkins.createProject(WorkflowJob.class, "test-pipeline");
    job.setDefinition(new CpsFlowDefinition(script, true));
    return job;
  }

  void configureSysdigCredentials() throws Descriptor.FormException {
    var creds = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "sysdig-secure", "sysdig-secure", "", "foo-token");
    SystemCredentialsProvider.getInstance().getCredentials().add(creds);
  }
}
