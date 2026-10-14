package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

import edu.umd.cs.findbugs.annotations.Nullable;

/** A software package identified by its ecosystem ({@link PackageType}). */
public class Package extends AbstractComponent {
    private final PackageType type;

    Package(
            String id,
            PackageType type,
            String name,
            String version,
            String path,
            @Nullable Layer foundInLayer,
            ScanResult root) {
        super(id, name, version, path, foundInLayer, root);
        this.type = type;
    }

    @Override
    public ComponentKind kind() {
        return ComponentKind.Package;
    }

    public PackageType type() {
        return type;
    }

    @Override
    public String typeName() {
        return type.toString();
    }
}
