package com.sysdig.jenkins.plugins.sysdig;

import com.google.common.collect.ImmutableSet;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepMonitor;
import hudson.util.ListBoxModel;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

public class NewEngineStep extends Step implements BuildStep, NewEngineScanStep {

  final NewEngineBuilder builder;

  @Override
  public String getImageName() {
    return builder.getImageName();
  }

  @Override
  public boolean getBailOnFail() {
    return builder.getBailOnFail();
  }

  @Override
  public boolean getBailOnPluginFail() {
    return builder.getBailOnPluginFail();
  }

  @Override
  public String getPoliciesToApply() {
    return builder.getPoliciesToApply();
  }


  @Override
  public String getEngineURL() {
    return builder.getEngineURL();
  }

  @Override
  public String getEngineCredentialsId() {
    return builder.getEngineCredentialsId();
  }

  @Override
  public boolean getEngineVerify() {
    return builder.getEngineVerify();
  }

  @Override
  public String getInlineScanExtraParams() {
    return builder.getInlineScanExtraParams();
  }

  @Override
  public String getScannerBinaryPath() {
    return builder.getScannerBinaryPath();
  }

  @DataBoundSetter
  @Override
  public void setBailOnFail(boolean bailOnFail) {
    builder.setBailOnFail(bailOnFail);
  }

  @DataBoundSetter
  @Override
  public void setPoliciesToApply(String policiesToApply) {
    builder.setPoliciesToApply(policiesToApply);
  }


  @DataBoundSetter
  @Override
  public void setBailOnPluginFail(boolean bailOnPluginFail) {
    builder.setBailOnPluginFail(bailOnPluginFail);
  }

  @DataBoundSetter
  @Override
  public void setEngineURL(String engineurl) {
    builder.setEngineURL(engineurl);
  }

  @DataBoundSetter
  @Override
  public void setEngineCredentialsId(String engineCredentialsId) {
    builder.setEngineCredentialsId(engineCredentialsId);
  }

  @DataBoundSetter
  @Override
  public void setEngineVerify(boolean engineVerify) {
    builder.setEngineVerify(engineVerify);
  }

  @DataBoundSetter
  @Override
  public void setInlineScanExtraParams(String inlineScanExtraParams) {
    builder.setInlineScanExtraParams(inlineScanExtraParams);
  }

  @DataBoundSetter
  @Override
  public void setScannerBinaryPath(String scannerBinayPath) {
    builder.setScannerBinaryPath(scannerBinayPath);
  }

  // Fields in config.jelly must match the parameter names in the "DataBoundConstructor" or "DataBoundSetter"
  @DataBoundConstructor
  public NewEngineStep(String imageName) {
    this.builder = new NewEngineBuilder(imageName);
  }

  @Override
  public StepExecution start(StepContext stepContext) {
    return new Execution(stepContext, this.builder);
  }

  @Override
  public boolean prebuild(AbstractBuild<?, ?> abstractBuild, BuildListener buildListener) {
    return builder.prebuild(abstractBuild, buildListener);
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> abstractBuild, Launcher launcher, BuildListener buildListener) throws InterruptedException, IOException {
    return builder.perform(abstractBuild, launcher, buildListener);
  }

  @Override
  @Deprecated
  public Action getProjectAction(AbstractProject<?, ?> abstractProject) {
    return builder.getProjectAction(abstractProject);
  }

  @Nonnull
  @Override
  public Collection<? extends Action> getProjectActions(AbstractProject<?, ?> abstractProject) {
    return builder.getProjectActions(abstractProject);
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return builder.getRequiredMonitorService();
  }

  private final static class Execution extends SynchronousNonBlockingStepExecution {

    private static final long serialVersionUID = 1;
    private transient final NewEngineBuilder builder;

    private Execution(
      @Nonnull StepContext context,
      NewEngineBuilder builder) {
      super(context);
      this.builder = builder;
    }

    @Override
    protected Void run() throws Exception {

      FilePath workspace = getContext().get(FilePath.class);
      assert workspace != null;
      workspace.mkdirs();
      builder.perform(
        getContext().get(Run.class),
        workspace,
        getContext().get(Launcher.class),
        getContext().get(TaskListener.class),
        getContext().get(EnvVars.class));

      return null;
    }

  }

  @Extension // This indicates to Jenkins that this is an implementation of an extension point.
  public static final class DescriptorImpl extends StepDescriptor {

    public static final String DEFAULT_NAME = "sysdig_secure_images";
    public static final boolean DEFAULT_BAIL_ON_FAIL = true;
    public static final boolean DEFAULT_BAIL_ON_PLUGIN_FAIL = true;
    public static final String DEFAULT_ENGINE_URL = "https://secure.sysdig.com";
    public static final boolean DEFAULT_ENGINE_VERIFY = true;

    NewEngineBuilder.DescriptorImpl builderDescriptor;

    public DescriptorImpl() {
      builderDescriptor = new NewEngineBuilder.DescriptorImpl();
      builderDescriptor.load();
    }

    @Override
    @Nonnull
    public String getDisplayName() {
      return "Sysdig Image Scanning pipeline step";
    }

    @SuppressWarnings("unused")
    public ListBoxModel doFillEngineCredentialsIdItems(@QueryParameter String credentialsId) {
      return builderDescriptor.doFillEngineCredentialsIdItems(credentialsId);
    }

    @Override
    public Set<? extends Class<?>> getRequiredContext() {
      return ImmutableSet.of(FilePath.class, Run.class, Launcher.class, TaskListener.class, EnvVars.class);
    }

    @Override
    public String getFunctionName() {
      return "sysdigImageScan";
    }
  }
}
