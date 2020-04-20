package com.sysdig.jenkins.plugins.sysdig;

import hudson.AbortException;

public interface BuildWorker {
    void runAnalyzer() throws AbortException;
    Util.GATE_ACTION runGates() throws AbortException;
    void runQueries() throws AbortException;
    void setupBuildReports() throws AbortException;
    void cleanup();
}
