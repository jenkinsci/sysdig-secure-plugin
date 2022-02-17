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

public class SysdigStep extends Step implements BuildStep, SysdigScanStep {

  final SysdigBuilder builder;

  @Override
  public String getName() {
    return builder.getName();
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
  public String getEngineurl() {
    return builder.getEngineurl();
  }

  @Override
  public String getEngineCredentialsId() {
    return builder.getEngineCredentialsId();
  }

  @Override
  public boolean getEngineverify() {
    return builder.getEngineverify();
  }

  @Override
  public String getRunAsUser() { return builder.getRunAsUser(); }

  @Override
  public String getInlineScanExtraParams() { return builder.getInlineScanExtraParams(); }

  @Override
  public boolean isInlineScanning() {
    return builder.isInlineScanning();
  }

  @Override
  public boolean getForceScan() {
    return builder.getForceScan();
  }

  @DataBoundSetter
  @Override
  public void setBailOnFail(boolean bailOnFail) {
    builder.setBailOnFail(bailOnFail);
  }

  @DataBoundSetter
  @Override
  public void setBailOnPluginFail(boolean bailOnPluginFail) {
    builder.setBailOnPluginFail(bailOnPluginFail);
  }

  @DataBoundSetter
  @Override
  public void setEngineurl(String engineurl) {
    builder.setEngineurl(engineurl);
  }

  @DataBoundSetter
  @Override
  public void setEngineCredentialsId(String engineCredentialsId) {
    builder.setEngineCredentialsId(engineCredentialsId);
  }

  @DataBoundSetter
  @Override
  public void setEngineverify(boolean engineverify) {
    builder.setEngineverify(engineverify);
  }

  @DataBoundSetter
  @Override
  public void setRunAsUser(String runAsUser) {
    builder.setRunAsUser(runAsUser);
  }

  @DataBoundSetter
  @Override
  public void setInlineScanExtraParams(String inlineScanExtraParams) {
    builder.setInlineScanExtraParams(inlineScanExtraParams);
  }

  @DataBoundSetter
  @Override
  public void setInlineScanning(boolean inlineScanning) {
    builder.setInlineScanning(inlineScanning);
  }

  @DataBoundSetter
  @Override
  public void setForceScan(boolean forceScan) {
    builder.setForceScan(forceScan);
  }

  // Fields in config.jelly must match the parameter names in the "DataBoundConstructor" or "DataBoundSetter"
  @DataBoundConstructor
  public SysdigStep(String name) {
    this.builder = new SysdigBuilder(name);
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

  private final static class Execution extends SynchronousNonBlockingStepExecution<Void> {

    private static final long serialVersionUID = 1;
    private transient final SysdigBuilder builder;

    private Execution(
      @Nonnull StepContext context,
      SysdigBuilder builder) {
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
    public static final boolean DEFAULT_INLINE_SCANNING = false;
    public static final boolean DEFAULT_FORCE_SCAN = false;
    public static final String DEFAULT_ENGINE_URL = "https://secure.sysdig.com";
    public static final boolean DEFAULT_ENGINE_VERIFY = true;
    SysdigBuilder.DescriptorImpl builderDescriptor;

    public DescriptorImpl() {
      builderDescriptor = new SysdigBuilder.DescriptorImpl();
      builderDescriptor.load();
    }

    @Override
    @Nonnull
    public String getDisplayName() {
      return "Sysdig Secure Container Image Scanner pipeline step";
    }

    //public FormValidation doCheckName(@QueryParameter String value) {
    //  return builderDescriptor.doCheckName(value);
    //}

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
      return "sysdig";
    }
  }
}