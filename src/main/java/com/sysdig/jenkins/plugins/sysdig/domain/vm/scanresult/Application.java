package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * A standalone application bundled in the asset, e.g. an embedded web server or database. Unlike a
 * {@link Package} it has no ecosystem, only a free-text subtype reported by the scanner, such as
 * {@code "Web Server"}.
 */
public class Application extends AbstractComponent {
    private final String type;

    Application(
            String id,
            String type,
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
        return ComponentKind.Application;
    }

    public String type() {
        return type;
    }

    @Override
    public String typeName() {
        return type;
    }
}
