package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

abstract class AbstractComponent implements Component {
    private final String id;
    private final String name;
    private final String version;
    private final String path;
    private final Layer foundInLayer;
    private final Set<Vulnerability> vulnerabilities;
    private final Set<AcceptedRisk> acceptedRisks;
    private final ScanResult root;

    AbstractComponent(
            String id, String name, String version, String path, @Nullable Layer foundInLayer, ScanResult root) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.path = path;
        this.foundInLayer = foundInLayer;
        this.root = root;
        this.vulnerabilities = new HashSet<>();
        this.acceptedRisks = new HashSet<>();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public Optional<Layer> foundInLayer() {
        return Optional.ofNullable(foundInLayer);
    }

    @Override
    public void addVulnerabilityFound(Vulnerability vulnerability) {
        if (this.vulnerabilities.add(vulnerability)) {
            vulnerability.addFoundIn(this);
        }
    }

    @Override
    public Set<Vulnerability> vulnerabilities() {
        return Collections.unmodifiableSet(vulnerabilities);
    }

    @Override
    public void addAcceptedRisk(AcceptedRisk acceptedRisk) {
        if (this.acceptedRisks.add(acceptedRisk)) {
            acceptedRisk.addForComponent(this);
        }
    }

    @Override
    public Set<AcceptedRisk> acceptedRisks() {
        return Collections.unmodifiableSet(acceptedRisks);
    }

    @Override
    public ScanResult root() {
        return root;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractComponent that = (AbstractComponent) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
