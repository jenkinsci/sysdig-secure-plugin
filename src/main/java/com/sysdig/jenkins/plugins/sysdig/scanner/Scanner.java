package com.sysdig.jenkins.plugins.sysdig.scanner;

import com.sysdig.jenkins.plugins.sysdig.BuildConfig;
import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningResult;
import com.sysdig.jenkins.plugins.sysdig.log.ConsoleLog;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.AbortException;
import hudson.Launcher;
import hudson.model.TaskListener;

import java.util.ArrayList;
import java.util.Map;

public abstract class Scanner {

  protected final Launcher launcher;
  protected final BuildConfig config;
  protected final SysdigLogger logger;

  public Scanner(Launcher launcher, TaskListener listener, BuildConfig config) throws AbortException {
    this.launcher = launcher;
    this.logger = new ConsoleLog(this.getClass().getSimpleName(), listener.getLogger(), false);
    this.config = config;
  }

  public abstract ArrayList<ImageScanningResult> scanImages(Map<String, String> imagesAndDockerfiles) throws AbortException, InterruptedException;

}
