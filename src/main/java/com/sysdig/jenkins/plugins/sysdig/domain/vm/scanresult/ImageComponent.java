package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

/**
 * An image-wide component: the operating system of the image or the container image itself. Some
 * vulnerabilities are matched directly against them, but they are not found in any layer.
 */
public class ImageComponent extends AbstractComponent {
    private final ComponentKind kind;
    private final String type;

    ImageComponent(String id, ComponentKind kind, String type, String name, String version, ScanResult root) {
        super(id, name, version, "", null, root);
        if (kind != ComponentKind.OperatingSystem && kind != ComponentKind.ContainerImage) {
            throw new IllegalArgumentException("not an image component kind: " + kind);
        }
        this.kind = kind;
        this.type = type;
    }

    @Override
    public ComponentKind kind() {
        return kind;
    }

    /** Raw type reported by the scanner, e.g. {@code "os"} or {@code "oci"}. */
    public String type() {
        return type;
    }

    @Override
    public String typeName() {
        return kind == ComponentKind.OperatingSystem ? "OS" : "Container Image";
    }
}
