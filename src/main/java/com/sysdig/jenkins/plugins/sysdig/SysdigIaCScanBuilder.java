
package com.sysdig.jenkins.plugins.sysdig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletException;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;

public class SysdigIaCScanBuilder extends Builder implements SimpleBuildStep {

    public class FailedCLIScan extends Exception {
        public FailedCLIScan(String errorMessage) {
            super(errorMessage);
        }
    }

    public class BadParamCLIScan extends Exception {
        public BadParamCLIScan(String errorMessage) {
            super(errorMessage);
        }
    }

    private boolean listUnsupported; // --list-unsupported-resources
    private boolean isRecursive = true;
    private final String secureAPIToken;
    private String path;
    private String severityThreshold = "h";
    private String sysdigEnv = "https://secure-staging.sysdig.com";
    private String version = "latest";

    @DataBoundConstructor
    public SysdigIaCScanBuilder(String secureAPIToken) {
        this.secureAPIToken = secureAPIToken;
    }

    public boolean isListUnsupported() {
        return listUnsupported;
    }

    @DataBoundSetter
    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public boolean getIsRecursive() {
        return isRecursive;
    }

    public String getVersion() {
        return this.version;
    }

    public String getSysdigEnv() {
        return sysdigEnv;
    }

    public String getSecureAPIToken() {
        return secureAPIToken;
    }

    @DataBoundSetter
    public void setSysdigEnv(String env) {
        this.sysdigEnv = env;
    }

    @DataBoundSetter
    public void setSeverityThreshold(String severityThreshold) {
        this.severityThreshold = severityThreshold;
    }

    @DataBoundSetter
    public void setVersion(String ver) {
        this.version = ver;
    }

    @DataBoundSetter
    public void setListUnsupported(boolean listUnsupported) {
        this.listUnsupported = listUnsupported;
    }

    @DataBoundSetter
    public void setIsRecursive(boolean isRecursive) {
        this.isRecursive = isRecursive;
    }

    private String getProcessOutput(Process p) throws IOException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
            builder.append(System.getProperty("line.separator"));
        }
        String output = builder.toString();
        reader.close();
        return output;
    }

    @Symbol("greet")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheckSysdigEnv(@QueryParameter String value, @QueryParameter boolean useFrench)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("missing field");
            if (value.length() < 4)
                return FormValidation.warning("too");

            return FormValidation.ok();
        }

        public FormValidation doCheckSecureAPIToken(@QueryParameter String value, @QueryParameter boolean useFrench)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("missing field");
            if (value.length() < 4)
                return FormValidation.warning("too");

            return FormValidation.ok();
        }

        public FormValidation doCheckPath(@QueryParameter String value, @QueryParameter boolean useFrench)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("missing field");

            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Sysdig Secure Code Scan";
        }
    }

    private Vector<String> buildCommand(String exec) {
        Vector<String> cmd = new Vector<String>();
        cmd.add(exec);
        cmd.add("--iac");
        cmd.add("-a");
        if (sysdigEnv.isEmpty()) {
            sysdigEnv = "https://secure-staging.sysdig.com";
        }
        cmd.add(sysdigEnv);

        if (isRecursive) {
            cmd.add("-r");
        }

        if (listUnsupported) {
            cmd.add("--list-unsupported-resources");
        }

        severity(cmd);

        cmd.add(path);

        return cmd;
    }

    private void severity(Vector<String> cmd) {
        cmd.add("-f");
        cmd.add(severityThreshold);
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {
        
    }

    @Override
    public boolean perform(Build<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        // TODO Auto-generated method stub
        super.perform(build, launcher, listener);

        CLIDownloadAction act = null;
        try {
            listener.getLogger().println("trying to download cli");
            String cwd = System.getProperty("user.dir");
            act = new CLIDownloadAction("IaC scanner", cwd,version);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            listener.getLogger().println("failed to download cli");

            e.printStackTrace();

            listener.error("failed:%s", e.getMessage());
            return false;
        }
        build.addAction(act);
        listener.getLogger().println(act.cliExecPath());

        listener.getLogger().println("starting to scan");
        try {
            if (act.cliExecPath().isEmpty()) {
                listener.error("failed empty path");

                throw new Exception("empty path");
            }
            String exec = act.cliExecPath();
            ProcessBuilder pb = new ProcessBuilder(buildCommand(exec));
            Map<String, String> envv = pb.environment();
            envv.put("SECURE_API_TOKEN", secureAPIToken);

            listener.getLogger().println(pb.command());

            Process p = pb.start();
            listener.getLogger().println("started...");

            String output = getProcessOutput(p);
            int exitCode = p.exitValue();
            listener.getLogger().printf("finished status %d", exitCode);

            listener.getLogger().printf("%s", output);

            switch (exitCode) {
                case 1:
                    throw new FailedCLIScan(String.format("scan failed \n\n%s", output));

                case 2:
                    throw new FailedCLIScan(String.format("scan failed \n\n%s", output));
                default:

                    break;
            }

        } catch (FailedCLIScan e) {
            listener.error("iac scan %s", e.getMessage());
            return false;
        } catch (BadParamCLIScan e) {
            listener.error("iac scan %s", e.getMessage());
            return false;
        } catch (Exception e) {
            listener.error("failed processing output:%s", e.getMessage());
            e.printStackTrace();
            return false;
        }
        listener.getLogger().println("done");
        return true;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        // TODO Auto-generated method stub
        super.perform(build, launcher, listener);

        CLIDownloadAction act = null;
        try {
            listener.getLogger().println("trying to download cli");
            String cwd = System.getProperty("user.dir");
            act = new CLIDownloadAction("IaC scanner", cwd,version);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            listener.getLogger().println("failed to download cli");

            e.printStackTrace();

            listener.error("failed:%s", e.getMessage());
            return false;
        }
        build.addAction(act);
        listener.getLogger().println(act.cliExecPath());

        listener.getLogger().println("starting to scan");
        try {
            if (act.cliExecPath().isEmpty()) {
                listener.error("failed empty path");

                throw new Exception("empty path");
            }
            String exec = act.cliExecPath();
            ProcessBuilder pb = new ProcessBuilder(buildCommand(exec));
            Map<String, String> envv = pb.environment();
            envv.put("SECURE_API_TOKEN", secureAPIToken);

            listener.getLogger().println(pb.command());

            Process p = pb.start();
            listener.getLogger().println("started...");

            String output = getProcessOutput(p);
            int exitCode = p.exitValue();
            listener.getLogger().printf("finished status %d\n", exitCode);

            listener.getLogger().printf("%s", output);

            switch (exitCode) {
                case 1:
                    throw new FailedCLIScan(String.format("scan failed \n\n%s", output));

                case 2:
                    throw new FailedCLIScan(String.format("scan failed \n\n%s", output));
                default:

                    break;
            }

        } catch (FailedCLIScan | BadParamCLIScan e) {
            listener.error("iac scan %s", e.getMessage());
            return false;
        } catch (Exception e) {
            listener.error("failed processing output:%s", e.getMessage());
            e.printStackTrace();
            return false;
        }
        listener.getLogger().println("done");
        return true;
    }

}
