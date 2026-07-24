/*
Copyright (C) 2016-2024 Sysdig

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.iac.entrypoint;

import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.Serial;
import java.util.Set;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 * First-class Pipeline step for IaC scanning. Thin, delegating wrapper around {@link IaCScanningBuilder}
 * (the Freestyle build step) so declarative and scripted pipelines share the exact same, backwards-compatible
 * parameter set. Mirrors how VM scanning exposes {@code sysdigImageScan}.
 */
public class IaCScanningStep extends Step {

    final IaCScanningBuilder builder;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor" or "DataBoundSetter"
    @DataBoundConstructor
    public IaCScanningStep(String engineCredentialsId) {
        this.builder = new IaCScanningBuilder(engineCredentialsId);
    }

    public String getEngineCredentialsId() {
        return builder.getEngineCredentialsId();
    }

    @DataBoundSetter
    public void setEngineCredentialsId(String engineCredentialsId) {
        builder.setEngineCredentialsId(engineCredentialsId);
    }

    public String getPath() {
        return builder.getPath();
    }

    @DataBoundSetter
    public void setPath(String path) {
        builder.setPath(path);
    }

    public boolean getIsRecursive() {
        return builder.getIsRecursive();
    }

    @DataBoundSetter
    public void setIsRecursive(boolean isRecursive) {
        builder.setIsRecursive(isRecursive);
    }

    public boolean isListUnsupported() {
        return builder.isListUnsupported();
    }

    @DataBoundSetter
    public void setListUnsupported(boolean listUnsupported) {
        builder.setListUnsupported(listUnsupported);
    }

    public String getSeverityThreshold() {
        return builder.getSeverityThreshold();
    }

    @DataBoundSetter
    public void setSeverityThreshold(String severityThreshold) {
        builder.setSeverityThreshold(severityThreshold);
    }

    public String getSysdigEnv() {
        return builder.getSysdigEnv();
    }

    @DataBoundSetter
    public void setSysdigEnv(String sysdigEnv) {
        builder.setSysdigEnv(sysdigEnv);
    }

    public String getVersion() {
        return builder.getVersion();
    }

    @DataBoundSetter
    public void setVersion(String version) {
        builder.setVersion(version);
    }

    @Override
    public StepExecution start(StepContext stepContext) {
        return new Execution(stepContext, this.builder);
    }

    private static final class Execution extends SynchronousNonBlockingStepExecution<Void> {

        @Serial
        private static final long serialVersionUID = 1;

        private final transient IaCScanningBuilder builder;

        private Execution(@NonNull StepContext context, IaCScanningBuilder builder) {
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
                    getContext().get(EnvVars.class),
                    getContext().get(Launcher.class),
                    getContext().get(TaskListener.class));

            return null;
        }
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends StepDescriptor {

        public static final boolean DEFAULT_IS_RECURSIVE = IaCScanningBuilder.DescriptorImpl.DEFAULT_IS_RECURSIVE;
        public static final String DEFAULT_CLI_VERSION = IaCScanningBuilder.DescriptorImpl.DEFAULT_CLI_VERSION;

        private final IaCScanningBuilder.DescriptorImpl builderDescriptor = new IaCScanningBuilder.DescriptorImpl();

        @Override
        @NonNull
        public String getDisplayName() {
            return "Sysdig Secure Code Scan pipeline step";
        }

        @Override
        public String getFunctionName() {
            return "sysdigIaCScan";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(FilePath.class, Run.class, Launcher.class, TaskListener.class, EnvVars.class);
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillEngineCredentialsIdItems(@QueryParameter String credentialsId) {
            return builderDescriptor.doFillEngineCredentialsIdItems(credentialsId);
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckPath(@QueryParameter String value, @QueryParameter boolean useFrench) {
            return builderDescriptor.doCheckPath(value, useFrench);
        }
    }
}
