package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;

public class SysdigImageScanningProcessBuilder extends SysdigProcessBuilderBase<SysdigImageScanningProcessBuilder> {
    private String dbPath;
    private String cachePath;
    private String imageToScan;
    private boolean separateByLayer;

    public SysdigImageScanningProcessBuilder(String sysdigCLIPath, String sysdigAPIToken) {
        super(sysdigCLIPath, sysdigAPIToken);
        this.dbPath = "";
        this.cachePath = "";
        this.imageToScan = "";
        this.separateByLayer = false;
    }

    public SysdigImageScanningProcessBuilder withDBPath(String dbPath) {
        SysdigImageScanningProcessBuilder clone = this.clone();
        clone.dbPath = dbPath;
        return clone;
    }

    public SysdigImageScanningProcessBuilder withCachePath(String cachePath) {
        SysdigImageScanningProcessBuilder clone = this.clone();
        clone.cachePath = cachePath;
        return clone;
    }

    public SysdigImageScanningProcessBuilder withImageToScan(String imageToScan) {
        SysdigImageScanningProcessBuilder clone = this.clone();
        clone.imageToScan = imageToScan;
        return clone;
    }

    public SysdigImageScanningProcessBuilder withSeparateByLayer(boolean separateByLayer) {
        SysdigImageScanningProcessBuilder clone = this.clone();
        clone.separateByLayer = separateByLayer;
        return clone;
    }

    @Override
    public List<String> toCommandLineArguments() {
        var arguments = new ArrayList<String>();
        arguments.add(this.sysdigCLIPath);
        if (!Strings.isNullOrEmpty(engineURL)) arguments.add("--apiurl=" + engineURL);
        if (!Strings.isNullOrEmpty(scanResultOutputPath)) arguments.add("--output=json-file=" + scanResultOutputPath);
        policiesToApply.stream().map(policy -> "--policy=" + policy).forEach(arguments::add);
        if (!Strings.isNullOrEmpty(dbPath)) arguments.add("--dbpath=" + dbPath);
        if (!Strings.isNullOrEmpty(cachePath)) arguments.add("--cachepath=" + cachePath);
        arguments.add("--loglevel=" + logLevel.toString());
        if (consoleLogEnabled) arguments.add("--console-log");
        if (!verifyTLS) arguments.add("--skiptlsverify");
        if (separateByLayer) arguments.add("--separate-by-layer");
        arguments.addAll(extraParams);
        arguments.add(imageToScan);
        return arguments;
    }

    @Override
    public SysdigImageScanningProcessBuilder clone() {
        // Deep copy subclass-specific mutable fields if necessary
        return super.clone();
    }
}
