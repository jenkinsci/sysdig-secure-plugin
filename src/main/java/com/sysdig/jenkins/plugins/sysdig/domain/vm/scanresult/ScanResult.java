package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.diff.ScanResultDiff;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;

public class ScanResult implements Serializable {
    private final EvaluationResult globalEvaluationResult;
    private final ScanType type;
    private final Metadata metadata;
    private final Map<String, Layer> layers;
    private final Map<String, Component> components;
    private final Map<String, Vulnerability> vulnerabilities;
    private final Map<String, Policy> policies;
    private final Map<String, PolicyBundle> policyBundles;
    private final Map<String, AcceptedRisk> acceptedRisks;

    public ScanResult(
            EvaluationResult globalEvaluationResult,
            ScanType type,
            String pullString,
            String imageID,
            String digest,
            OperatingSystem baseOS,
            BigInteger sizeInBytes,
            Architecture architecture,
            Map<String, String> labels,
            Date createdAt) {
        this.globalEvaluationResult = globalEvaluationResult;
        this.type = type;
        this.metadata =
                new Metadata(pullString, imageID, digest, baseOS, sizeInBytes, architecture, labels, createdAt, this);
        this.layers = new HashMap<>();
        this.components = new HashMap<>();
        this.vulnerabilities = new HashMap<>();
        this.policies = new HashMap<>();
        this.policyBundles = new HashMap<>();
        this.acceptedRisks = new HashMap<>();
    }

    public Layer addLayer(String digest, BigInteger size, String command) {
        Layer layer = new Layer(digest, size, command, this);
        this.layers.put(digest, layer);
        return layer;
    }

    public Optional<Layer> findLayerByDigest(String digest) {
        return Optional.ofNullable(this.layers.get(digest));
    }

    public Collection<Layer> layers() {
        return Collections.unmodifiableCollection(this.layers.values());
    }

    public Package addPackage(
            String id, PackageType type, String name, String version, String path, @Nullable Layer layer) {
        return register(new Package(id, type, name, version, path, layer, this));
    }

    public Application addApplication(
            String id, String type, String name, String version, String path, @Nullable Layer layer) {
        return register(new Application(id, type, name, version, path, layer, this));
    }

    public ImageComponent addImageComponent(
            String id, Component.ComponentKind kind, String type, String name, String version) {
        return register(new ImageComponent(id, kind, type, name, version, this));
    }

    private <T extends Component> T register(T component) {
        this.components.put(component.id(), component);
        component.foundInLayer().ifPresent(layer -> layer.addComponent(component));
        return component;
    }

    public Optional<Component> findComponentById(String id) {
        return Optional.ofNullable(this.components.get(id));
    }

    /** Every component of the scan result: packages, applications and image components. */
    public Collection<Component> components() {
        return Collections.unmodifiableCollection(components.values());
    }

    public Collection<Package> packages() {
        return componentsOf(Package.class);
    }

    public Collection<Application> applications() {
        return componentsOf(Application.class);
    }

    public Collection<ImageComponent> imageComponents() {
        return componentsOf(ImageComponent.class);
    }

    private <T extends Component> Collection<T> componentsOf(Class<T> kind) {
        return components.values().stream()
                .filter(kind::isInstance)
                .map(kind::cast)
                .toList();
    }

    public Vulnerability addVulnerability(
            String cve,
            Severity severity,
            Date disclosureDate,
            @Nullable Date solutionDate,
            boolean exploitable,
            @Nullable String fixVersion,
            boolean fpkev,
            @Nullable Float cvssTemporalScore) {
        return vulnerabilities.computeIfAbsent(
                cve,
                k -> new Vulnerability(
                        cve,
                        severity,
                        disclosureDate,
                        solutionDate,
                        exploitable,
                        fixVersion,
                        fpkev,
                        cvssTemporalScore,
                        this));
    }

    public Optional<Vulnerability> findVulnerabilityByCVE(String cve) {
        return Optional.ofNullable(vulnerabilities.get(cve));
    }

    public Collection<Vulnerability> vulnerabilities() {
        return Collections.unmodifiableCollection(vulnerabilities.values());
    }

    public Policy addPolicy(String id, String name, Date createdAt, Date updatedAt) {
        return policies.computeIfAbsent(id, k -> new Policy(id, name, createdAt, updatedAt, this));
    }

    public Optional<Policy> findPolicyByID(String id) {
        return Optional.ofNullable(policies.get(id));
    }

    public Collection<Policy> policies() {
        return Collections.unmodifiableCollection(policies.values());
    }

    public PolicyBundle addPolicyBundle(String id, String name, Policy policy) {
        PolicyBundle policyBundle = policyBundles.computeIfAbsent(id, k -> new PolicyBundle(id, name, this));
        policyBundle.addPolicy(policy);
        return policyBundle;
    }

    public Optional<PolicyBundle> findPolicyBundleByID(String id) {
        return Optional.ofNullable(policyBundles.get(id));
    }

    public Collection<PolicyBundle> policyBundles() {
        return Collections.unmodifiableCollection(policyBundles.values());
    }

    public AcceptedRisk addAcceptedRisk(
            String id,
            AcceptedRiskReason reason,
            String description,
            Date expirationDate,
            boolean isActive,
            Date createdAt,
            Date updatedAt) {
        AcceptedRisk acceptedRisk =
                new AcceptedRisk(id, reason, description, expirationDate, isActive, createdAt, updatedAt, this);
        this.acceptedRisks.put(id, acceptedRisk);
        return acceptedRisk;
    }

    public Optional<AcceptedRisk> findAcceptedRiskByID(String id) {
        return Optional.ofNullable(this.acceptedRisks.get(id));
    }

    public Collection<AcceptedRisk> acceptedRisks() {
        return Collections.unmodifiableCollection(acceptedRisks.values());
    }

    public ScanType type() {
        return type;
    }

    public Metadata metadata() {
        return metadata;
    }

    public EvaluationResult evaluationResult() {
        return globalEvaluationResult;
    }

    public ScanResultDiff diffWithPrevious(ScanResult previous) {
        return ScanResultDiff.betweenPreviousAndNew(previous, this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ScanResult that = (ScanResult) o;
        return type == that.type
                && Objects.equals(metadata, that.metadata)
                && Objects.equals(layers, that.layers)
                && Objects.equals(components, that.components)
                && Objects.equals(vulnerabilities, that.vulnerabilities)
                && Objects.equals(policies, that.policies)
                && Objects.equals(policyBundles, that.policyBundles)
                && Objects.equals(acceptedRisks, that.acceptedRisks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                type, metadata, layers, components, vulnerabilities, policies, policyBundles, acceptedRisks);
    }
}
