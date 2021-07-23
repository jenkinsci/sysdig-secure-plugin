package com.sysdig.jenkins.plugins.sysdig;

import com.google.common.collect.ImmutableSet;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepMonitor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

public class SysdigStep extends Step implements BuildStep, SysdigScanStep {

  final SysdigBuilder builder;

  // Getters are used by config.jelly
  public String getName() {
    return builder.getName();
  }

  public boolean getBailOnFail() {
    return builder.getBailOnFail();
  }

  public boolean getBailOnPluginFail() {
    return builder.getBailOnPluginFail();
  }

  public String getEngineurl() {
    return builder.getEngineurl();
  }

  public String getEngineCredentialsId() {
    return builder.getEngineCredentialsId();
  }

  public boolean getEngineverify() {
    return builder.getEngineverify();
  }

  public boolean isInlineScanning() {
    return builder.isInlineScanning();
  }

  public boolean getForceScan() {
    return builder.getForceScan();
  }

  @DataBoundSetter
  public void setBailOnFail(boolean bailOnFail) {
    builder.setBailOnFail(bailOnFail);
  }

  @DataBoundSetter
  public void setBailOnPluginFail(boolean bailOnPluginFail) {
    builder.setBailOnPluginFail(bailOnPluginFail);
  }

  @DataBoundSetter
  public void setEngineurl(String engineurl) {
    builder.setEngineurl(engineurl);
  }

  @DataBoundSetter
  public void setEngineCredentialsId(String engineCredentialsId) {
    builder.setEngineCredentialsId(engineCredentialsId);
  }

  @DataBoundSetter
  public void setEngineverify(boolean engineverify) {
    builder.setEngineverify(engineverify);
  }

  @DataBoundSetter
  public void setInlineScanning(boolean inlineScanning) {
    builder.setInlineScanning(inlineScanning);
  }

  @DataBoundSetter
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

    private transient final SysdigBuilder builder;

    protected Execution(
      @Nonnull StepContext context,
      SysdigBuilder builder) {
      super(context);
      this.builder = builder;
    }

    @Override
    protected Void run() throws Exception {

      FilePath workspace = getContext().get(FilePath.class);
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

    // Used in the jelly template
    public static final String DEFAULT_NAME = "sysdig_secure_images";
    public static final boolean DEFAULT_BAIL_ON_FAIL = true;
    public static final boolean DEFAULT_BAIL_ON_PLUGIN_FAIL = true;
    public static final boolean DEFAULT_INLINE_SCANNING = false;
    public static final boolean DEFAULT_FORCE_SCAN = false;
    public static final String DEFAULT_ENGINE_URL = "https://secure.sysdig.com";
    public static final boolean DEFAULT_ENGINE_VERIFY = true;

    SysdigBuilder.DescriptorImpl builderDescriptor;

    @Deprecated
    public void setEnabled(boolean enabled) {
      builderDescriptor.setEnabled(enabled);
    }

    public void setDebug(boolean debug) {
      builderDescriptor.setDebug(debug);
    }

    public void setEngineurl(String engineurl) {
      builderDescriptor.setEngineurl(engineurl);
    }

    public void setEngineCredentialsId(String engineCredentialsId) {
      builderDescriptor.setEngineCredentialsId(engineCredentialsId);
    }

    public void setEngineverify(boolean engineverify) {
      builderDescriptor.setEngineverify(engineverify);
    }

    public void setInlinescanimage(String inlinescanimage) {
      builderDescriptor.setInlinescanimage(inlinescanimage);
    }

    public void setForceinlinescan(boolean forceinlinescan) {
      builderDescriptor.setForceinlinescan(forceinlinescan);
    }

    @Deprecated
    public boolean getEnabled() {
      return builderDescriptor.getEnabled();
    }

    public boolean getDebug() {
      return builderDescriptor.getDebug();
    }

    public String getEngineurl() {
      return builderDescriptor.getEngineurl();
    }

    public String getEngineCredentialsId() {
      return builderDescriptor.getEngineCredentialsId();
    }

    public boolean getEngineverify() {
      return builderDescriptor.getEngineverify();
    }

    public String getInlinescanimage() {
      return builderDescriptor.getInlinescanimage();
    }

    public boolean getForceinlinescan() {
      return builderDescriptor.getForceinlinescan();
    }

    public DescriptorImpl() {
      builderDescriptor = new SysdigBuilder.DescriptorImpl();
      builderDescriptor.load();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) {
      return builderDescriptor.configure(req, formData);
    }

     @Override
    public String getDisplayName() {
      return "Sysdig Secure Container Image Scanner pipeline step";
    }

    public FormValidation doCheckName(@QueryParameter String value) {
      return builderDescriptor.doCheckName(value);
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
      return "sysdig";
    }

  }
}