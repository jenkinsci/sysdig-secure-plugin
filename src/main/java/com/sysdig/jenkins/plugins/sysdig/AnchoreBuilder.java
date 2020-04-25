package com.sysdig.jenkins.plugins.sysdig;


import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.google.common.base.Strings;
import com.sysdig.jenkins.plugins.sysdig.Util.GATE_ACTION;
import com.sysdig.jenkins.plugins.sysdig.client.ImageScanningSubmission;
import com.sysdig.jenkins.plugins.sysdig.log.ConsoleLog;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * <p>Sysdig Secure Plugin  enables Jenkins users to scan container images, generate analysis, evaluate gate policy, and execute customizable
 * queries. The plugin can be used in a freestyle project as a step or invoked from a pipeline script</p>
 *
 * <p>Requirements:</p>
 *
 * <ol> <li>Jenkins installed and configured either as a single system, or with multiple configured jenkins worker nodes</li>
 *
 * <li>Each host on which jenkins jobs will run must have docker installed and the jenkins user (or whichever user you have configured
 * jenkins to run jobs as) must be allowed to interact with docker (either directly or via sudo)</li>
 *
 * <li>Each host on which jenkins jobs will run must have the latest sysdig secure container image installed in the local docker host. To
 * install, run 'docker pull anchore/jenkins:latest' on each jenkins host to make the image available to the plugin. The plugin will
 * start an instance of the anchore/jenkins:latest docker container named 'jenkins_anchore' by default, on each host that runs a
 * jenkins job that includes an Sysdig Secure Container Image Scanner step.</li> </ol>
 */
public class AnchoreBuilder extends Builder implements SimpleBuildStep {

  //  Log handler for logging above INFO level events to jenkins log
  private static final Logger LOG = Logger.getLogger(AnchoreBuilder.class.getName());

  // Assigning the defaults here for pipeline builds
  private final String name;
  private String engineRetries = DescriptorImpl.DEFAULT_ENGINE_RETRIES;
  private boolean bailOnFail = DescriptorImpl.DEFAULT_BAIL_ON_FAIL;
  private boolean bailOnPluginFail = DescriptorImpl.DEFAULT_BAIL_ON_PLUGIN_FAIL;
  private boolean inlineScanning = DescriptorImpl.DEFAULT_INLINE_SCANNING;

  // Override global config. Supported for sysdig-secure-engine mode config only
  private String engineurl = DescriptorImpl.EMPTY_STRING;
  private String engineCredentialsId = DescriptorImpl.EMPTY_STRING;
  private boolean engineverify = false;
  // More flags to indicate boolean override, ugh!

  // Getters are used by config.jelly
  public String getName() {
    return name;
  }

  public String getEngineRetries() {
    return engineRetries;
  }

  public boolean getBailOnFail() {
    return bailOnFail;
  }

  public boolean getBailOnPluginFail() {
    return bailOnPluginFail;
  }

  public String getEngineurl() {
    return engineurl;
  }

  public String getEngineCredentialsId() {
    return engineCredentialsId;
  }

  public boolean getEngineverify() {
    return engineverify;
  }

  public boolean isInlineScanning() {
    return inlineScanning;
  }

  @DataBoundSetter
  public void setEngineRetries(String engineRetries) {
    this.engineRetries = engineRetries;
  }

  @DataBoundSetter
  public void setBailOnFail(boolean bailOnFail) {
    this.bailOnFail = bailOnFail;
  }

  @DataBoundSetter
  public void setBailOnPluginFail(boolean bailOnPluginFail) {
    this.bailOnPluginFail = bailOnPluginFail;
  }

  @DataBoundSetter
  public void setEngineurl(String engineurl) {
    this.engineurl = engineurl;
  }

  @DataBoundSetter
  public void setEngineCredentialsId(String engineCredentialsId) {
    this.engineCredentialsId = engineCredentialsId;
  }

  @DataBoundSetter
  public void setInlineScanning(boolean inlineScanning) {
    this.inlineScanning = inlineScanning;
  }

  // Fields in config.jelly must match the parameter names in the "DataBoundConstructor" or "DataBoundSetter"
  @DataBoundConstructor
  public AnchoreBuilder(String name) {
    this.name = name;
  }

  @Override
  public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws IOException, InterruptedException {

    LOG.warning(String.format("Starting Sysdig Secure Container Image Scanner step, project: %s, job: %d", run.getParent().getDisplayName(), run.getNumber()));

    boolean failedByGate = false;
    BuildConfig config = null;
    BuildWorker worker = null;
    DescriptorImpl globalConfig = getDescriptor();
    ConsoleLog console = new ConsoleLog("SysdigSecurePlugin", listener.getLogger(), globalConfig.getDebug());

    GATE_ACTION finalAction;

    try {
      // We are expecting that either the job credentials or global credentials will be set, otherwise, fail the build
      if (Strings.isNullOrEmpty(engineCredentialsId) && Strings.isNullOrEmpty(globalConfig.getEngineCredentialsId())) {
        throw new AbortException(String.format("Cannot find Jenkins credentials by ID: '%s'. Ensure credentials are defined in Jenkins before using them", engineCredentialsId));
      }
      //Prefer the job credentials set by the user and fallback to the global ones
      String credID = !Strings.isNullOrEmpty(engineCredentialsId) ? engineCredentialsId : globalConfig.getEngineCredentialsId();
      console.logDebug("Processing Jenkins credential ID " + credID);

      /* Fetch Jenkins creds first, can't push this lower down the chain since it requires Jenkins instance object */
      String enginepass;
      String engineuser;
      try {
        StandardUsernamePasswordCredentials creds = CredentialsProvider.findCredentialById(credID, StandardUsernamePasswordCredentials.class, run, Collections.emptyList());
        if (null != creds) {
          //This is to maintain backward compatibility with how the API layer is fetching the information. This will be changed in the next version to use
          //the Authorization header instead.
          engineuser = creds.getPassword().getPlainText();
          enginepass = "";
        } else {
          throw new AbortException(String.format("Cannot find Jenkins credentials by ID: '%s'. Ensure credentials are defined in Jenkins before using them", credID));
        }
      } catch (AbortException e) {
        throw e;
      } catch (Exception e) {
        console.logError(String.format("Error looking up Jenkins credentials by ID: '%s'", credID), e);
        throw new AbortException(String.format("Error looking up Jenkins credentials by ID: '%s", credID));
      }

      /* Instantiate config and a new build worker */
      config = new BuildConfig(name, engineRetries, bailOnFail, bailOnPluginFail, globalConfig.getDebug(),
        globalConfig.getInlineScanning(),
        // messy build time overrides, ugh!
        !Strings.isNullOrEmpty(engineurl) ? engineurl : globalConfig.getEngineurl(),
        engineuser,
        enginepass,
        engineverify, globalConfig.getContainerImageId(),
        globalConfig.getContainerId(), globalConfig.getLocalVol(), globalConfig.getModulesVol());

      if (config.isInlineScanning()) {
        worker = new BuildWorkerInline(run, workspace, launcher, listener, config);
      } else {
        worker = new BuildWorkerBackend(run, workspace, launcher, listener, config);
      }

      /* Log any build time overrides are at play */
      if (!Strings.isNullOrEmpty(engineurl)) {
        console.logInfo("Build override set for Sysdig Secure Engine URL");
      }

      Map<String, String> imagesAndDockerfiles = worker.readImagesAndDockerfilesFromPath(new FilePath(workspace, config.getName()));
      /* Run analysis */
      ArrayList<ImageScanningSubmission> submissionList = worker.scanImages(imagesAndDockerfiles);

      /* Run gates */
      finalAction = worker.runGates(submissionList);

      /* Run queries and continue even if it fails */
      try {
        worker.runQueries();
      } catch (Exception e) {
        console.logWarn("Recording failure to execute Sysdig Secure queries and moving on with plugin operation", e);
      }

      /* Setup reports */
      worker.setupBuildReports();

      /* Evaluate result of step based on gate action */
      if (null != finalAction) {
        if ((config.getBailOnFail() && (GATE_ACTION.STOP.equals(finalAction) || GATE_ACTION.FAIL.equals(finalAction)))) {
          console.logWarn("Failing Sysdig Secure Container Image Scanner Plugin step due to final result " + finalAction);
          failedByGate = true;
          throw new AbortException("Failing Sysdig Secure Container Image Scanner Plugin step due to final result " + finalAction);
        } else {
          console.logInfo("Marking Sysdig Secure Container Image Scanner step as successful, final result " + finalAction);
        }
      } else {
        console.logInfo("Marking Sysdig Secure Container Image Scanner step as successful, no final result");
      }

    } catch (Exception e) {
      if (failedByGate) {
        throw e;
      } else if ((null != config && config.getBailOnPluginFail()) || bailOnPluginFail) {
        console.logError("Failing Sysdig Secure Container Image Scanner Plugin step due to errors in plugin execution", e);
        if (e instanceof AbortException) {
          throw e;
        } else {
          throw new AbortException("Failing Sysdig Secure Container Image Scanner Plugin step due to errors in plugin execution");
        }
      } else {
        console.logWarn("Marking Sysdig Secure Container Image Scanner step as successful despite errors in plugin execution");
      }
    } finally {
      // Wrap cleanup in try catch block to ensure this finally block does not throw an exception
      if (null != worker) {
        try {
          worker.cleanup();
        } catch (Exception e) {
          console.logDebug("Failed to cleanup after the plugin, ignoring the errors", e);
        }
      }
      console.logInfo("Completed Sysdig Secure Container Image Scanner step");
      LOG.warning("Completed Sysdig Secure Container Image Scanner step, project: " + run.getParent().getDisplayName() + ", job: " + run
        .getNumber());
    }
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  @Symbol("sysdig") // For Jenkins pipeline workflow. This lets pipeline refer to step using the defined identifier
  @Extension // This indicates to Jenkins that this is an implementation of an extension point.
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

    // Default job level config that may be used both by config.jelly and an instance of AnchoreBuilder
    public static final String DEFAULT_NAME = "sysdig_secure_images";
    public static final String DEFAULT_ENGINE_RETRIES = "300";
    public static final boolean DEFAULT_BAIL_ON_FAIL = true;
    public static final boolean DEFAULT_BAIL_ON_PLUGIN_FAIL = true;
    public static final boolean DEFAULT_INLINE_SCANNING = false;
    public static final String EMPTY_STRING = "";
    public static final String DEFAULT_ENGINE_URL = "https://secure.sysdig.com/api/scanning/v1/anchore";

    // Global configuration
    private boolean debug;
    private String engineurl = DEFAULT_ENGINE_URL;
    private String engineuser = EMPTY_STRING;
    private Secret enginepass = Secret.fromString(EMPTY_STRING);
    private boolean engineverify;
    private String containerImageId;
    private String containerId;
    private String localVol;
    private String modulesVol;
    private String engineCredentialsId;
    private boolean inlineScanning = DEFAULT_INLINE_SCANNING;

    // Upgrade case, you can never really remove these variables once they are introduced
    @Deprecated
    private boolean enabled;

    public void setDebug(boolean debug) {
      this.debug = debug;
    }

    @Deprecated
    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public void setEngineurl(String engineurl) {
      this.engineurl = engineurl;
    }

    public void setEngineCredentialsId(String engineCredentialsId) {
      this.engineCredentialsId = engineCredentialsId;
    }

    public void setEngineuser(String engineuser) {
      this.engineuser = engineuser;
    }

    public void setEnginepass(Secret enginepass) {
      this.enginepass = enginepass;
    }

    public void setEngineverify(boolean engineverify) {
      this.engineverify = engineverify;
    }

    public void setContainerImageId(String containerImageId) {
      this.containerImageId = containerImageId;
    }

    public void setContainerId(String containerId) {
      this.containerId = containerId;
    }

    public void setLocalVol(String localVol) {
      this.localVol = localVol;
    }

    public void setModulesVol(String modulesVol) {
      this.modulesVol = modulesVol;
    }

    public void setInlineScanning(boolean inlineScanning) {
      this.inlineScanning = inlineScanning;
    }

    public boolean getDebug() {
      return debug;
    }

    @Deprecated
    public boolean getEnabled() {
      return enabled;
    }

    public String getEngineurl() {
      return engineurl;
    }

    public String getEngineuser() {
      return engineuser;
    }

    public String getEngineCredentialsId() {
      return engineCredentialsId;
    }

    public Secret getEnginepass() {
      return enginepass;
    }

    public boolean getEngineverify() {
      return engineverify;
    }

    public String getContainerImageId() {
      return containerImageId;
    }

    public String getContainerId() {
      return containerId;
    }

    public String getLocalVol() {
      return localVol;
    }

    public String getModulesVol() {
      return modulesVol;
    }

    public boolean getInlineScanning() {
      return inlineScanning;
    }

    public DescriptorImpl() {
      load();
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return "Sysdig Secure Container Image Scanner";
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) {
      req.bindJSON(this, formData); // Use stapler request to bind
      save();
      return true;
    }

    /**
     * Performs on-the-fly validation of the form field 'name' (Image list file)
     *
     * @param value This parameter receives the value that the user has typed in the 'Image list file' box
     * @return Indicates the outcome of the validation. This is sent to the browser. <p> Note that returning {@link
     * FormValidation#error(String)} does not prevent the form from being saved. It just means that a message will be displayed to the
     * user
     */
    @SuppressWarnings("unused")
    public FormValidation doCheckName(@QueryParameter String value) {
      if (!Strings.isNullOrEmpty(value)) {
        return FormValidation.ok();
      } else {
        return FormValidation.error("Please enter a valid file name");
      }
    }

    @SuppressWarnings("unused")
    public FormValidation doCheckContainerImageId(@QueryParameter String value) {
      if (!Strings.isNullOrEmpty(value)) {
        return FormValidation.ok();
      } else {
        return FormValidation.error("Please provide a valid Sysdig Secure Container Image ID");
      }
    }

    @SuppressWarnings("unused")
    public FormValidation doCheckContainerId(@QueryParameter String value) {
      if (!Strings.isNullOrEmpty(value)) {
        return FormValidation.ok();
      } else {
        return FormValidation.error("Please provide a valid Sysdig Secure Container ID");
      }
    }

    @SuppressWarnings("unused")
    public ListBoxModel doFillEngineCredentialsIdItems(@QueryParameter String credentialsId) {
      StandardListBoxModel result = new StandardListBoxModel();

      if (!Jenkins.getActiveInstance().hasPermission(Jenkins.ADMINISTER)) {
        return result.includeCurrentValue(credentialsId);
      }

      return result.includeEmptyValue().includeMatchingAs(ACL.SYSTEM,
        Jenkins.getActiveInstance(),
        StandardUsernamePasswordCredentials.class,
        Collections.emptyList(),
        CredentialsMatchers.always());
    }
  }
}

