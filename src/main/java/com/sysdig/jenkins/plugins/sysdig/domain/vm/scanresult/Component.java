package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

import com.sysdig.jenkins.plugins.sysdig.domain.AggregateChild;
import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

/**
 * An SBOM component found in the scanned asset: a software {@link Package}, an {@link Application}
 * (e.g. an embedded web server or database bundled inside the image) or an image-wide
 * {@link ImageComponent} (the operating system or the container image itself).
 *
 * <p>Vulnerabilities, accepted risks and policy failures are matched against components, not only
 * packages, so every kind takes part in those relationships.
 */
public interface Component extends AggregateChild<ScanResult>, Serializable {
    enum ComponentKind {
        Package,
        Application,
        OperatingSystem,
        ContainerImage,
    }

    ComponentKind kind();

    String id();

    String name();

    String version();

    String path();

    /** Human readable type of the component, e.g. {@code "OS"}, {@code "Java"} or {@code "Web Server"}. */
    String typeName();

    Optional<Layer> foundInLayer();

    void addVulnerabilityFound(Vulnerability vulnerability);

    Set<Vulnerability> vulnerabilities();

    void addAcceptedRisk(AcceptedRisk acceptedRisk);

    Set<AcceptedRisk> acceptedRisks();
}
