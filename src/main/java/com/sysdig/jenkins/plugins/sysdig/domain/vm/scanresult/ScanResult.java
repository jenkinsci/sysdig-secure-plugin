package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.diff.ScanResultDiff;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;
import javax.annotation.Nullable;

public class ScanResult implements Serializable {
    private final EvaluationResult globalEvaluationResult;
    private final ScanType type;
    private final Metadata metadata;
    private final Map<String, Layer> layers;
    private final Map<String, Package> packages;
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
        this.packages = new HashMap<>();
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

    public Package addPackage(String id, PackageType type, String name, String version, String path, Layer layer) {
        Package aPackage = new Package(id, type, name, version, path, layer, this);
        this.packages.put(id, aPackage);
        layer.addPackage(aPackage);
        return aPackage;
    }

    public Optional<Package> findPackageById(String id) {
        return Optional.ofNullable(this.packages.get(id));
    }

    public Collection<Package> packages() {
        return Collections.unmodifiableCollection(packages.values());
    }

    public Vulnerability addVulnerability(
            String cve,
            Severity severity,
            Date disclosureDate,
            @Nullable Date solutionDate,
            boolean exploitable,
            @Nullable String fixVersion) {
        return vulnerabilities.computeIfAbsent(
                cve,
                k -> new Vulnerability(cve, severity, disclosureDate, solutionDate, exploitable, fixVersion, this));
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
                && Objects.equals(packages, that.packages)
                && Objects.equals(vulnerabilities, that.vulnerabilities)
                && Objects.equals(policies, that.policies)
                && Objects.equals(policyBundles, that.policyBundles)
                && Objects.equals(acceptedRisks, that.acceptedRisks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, metadata, layers, packages, vulnerabilities, policies, policyBundles, acceptedRisks);
    }
}
