package com.sysdig.jenkins.plugins.sysdig;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.InstallSourceProperty;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class NewEngineToolInstaller extends ToolInstaller {

  @CheckForNull
  private /* almost final */ InstallSourceProperty installers;

  protected NewEngineToolInstaller(String label) {
    super(label);
  }

  @Override
  public FilePath performInstallation(ToolInstallation toolInstallation, Node node, TaskListener taskListener) throws IOException, InterruptedException {

    return null;
  }

  @CheckForNull
  public InstallSourceProperty getInstallers() {
    return installers;
  }

  @DataBoundSetter
  public void setInstallers(@Nullable final InstallSourceProperty installers) {
    this.installers = installers;
    if (super.tool != null) {
      installers.setTool(super.tool);
    }
  }

  @Override
  protected void setTool(final ToolInstallation t) {
    super.setTool(t);
    if (installers != null) {
      installers.setTool(t);
    }
  }

  @Override
  public boolean appliesTo(final Node node) {
    // We "apply" if any of our installers apply.
    // We have no separate existence of our own.
    final List<? extends ToolInstaller> ourInstallers = getOurInstallers();
    for (final ToolInstaller installer : ourInstallers) {
      if (installer.appliesTo(node)) {
        return true;
      }
    }
    return false;
  }

  @Nonnull
  private List<? extends ToolInstaller> getOurInstallers() {
    if (installers == null || installers.installers == null) {
      return Collections.emptyList();
    }
    return installers.installers.getAll(ToolInstaller.class);
  }
}
