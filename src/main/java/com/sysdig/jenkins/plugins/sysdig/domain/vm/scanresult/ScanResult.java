package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;

public class ScanResult  implements Serializable {
  private final ScanType type;
  private final Metadata metadata;
  private final HashMap<String, Layer> layers;
  private final Map<Package, Package> packages;
  private final Map<String, Vulnerability> vulnerabilities;
  private final Map<String, Policy> policies;
  private final Map<String, PolicyBundle> policyBundles;
  private final Map<String, AcceptedRisk> acceptedRisks;

  public ScanResult(ScanType type, String pullString, String imageID, String digest, OperatingSystem baseOS, BigInteger sizeInBytes, Architecture architecture, Map<String, String> labels, Date createdAt) {
    this.type = type;
    this.metadata = new Metadata(pullString, imageID, digest, baseOS, sizeInBytes, architecture, labels, createdAt, this);
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

  public Package addPackage(PackageType type, String name, String version, String path, Layer layer) {
    Package aPackage = new Package(type, name, version, path, layer, this);
    Package addedPackage = Optional.ofNullable(packages.putIfAbsent(aPackage, aPackage)).orElse(aPackage);

    layer.addPackage(addedPackage);
    return addedPackage;
  }

  public Collection<Package> packages() {
    return Collections.unmodifiableCollection(packages.values());
  }

  public Vulnerability addVulnerability(String cve, Severity severity, Date disclosureDate, @Nullable Date solutionDate, boolean exploitable,
                                        @Nullable String fixVersion) {
    return vulnerabilities.computeIfAbsent(cve, k -> new Vulnerability(cve, severity, disclosureDate, solutionDate, exploitable, fixVersion, this));
  }

  public Optional<Vulnerability> findVulnerabilityByCVE(String cve) {
    return Optional.ofNullable(vulnerabilities.get(cve));
  }

  public Collection<Vulnerability> vulnerabilities() {
    return Collections.unmodifiableCollection(vulnerabilities.values());
  }

  public Policy addPolicy(String id, String name, PolicyType type, Date createdAt, Date updatedAt) {
    return policies.computeIfAbsent(id, k -> new Policy(id, name, type, createdAt, updatedAt, this));
  }

  public Optional<Policy> findPolicyByID(String id) {
    return Optional.ofNullable(policies.get(id));
  }

  public Collection<Policy> policies() {
    return Collections.unmodifiableCollection(policies.values());
  }

  public PolicyBundle addPolicyBundle(String id, String name, Date createdAt, Date updatedAt, Policy policy) {
    PolicyBundle policyBundle = policyBundles.computeIfAbsent(id, k -> new PolicyBundle(id, name, createdAt, updatedAt, this));
    policyBundle.addPolicy(policy);
    return policyBundle;
  }

  public Optional<PolicyBundle> findPolicyBundleByID(String id) {
    return Optional.ofNullable(policyBundles.get(id));
  }

  public Collection<PolicyBundle> policyBundles() {
    return Collections.unmodifiableCollection(policyBundles.values());
  }

  public AcceptedRisk addAcceptedRisk(String id, AcceptedRiskReason reason, String description, Date expirationDate, boolean isActive, Date createdAt, Date updatedAt) {
    AcceptedRisk acceptedRisk = new AcceptedRisk(id, reason, description, expirationDate, isActive, createdAt, updatedAt, this);
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
    boolean allPoliciesPassed = policies().stream().allMatch(p -> p.evaluationResult().isPassed());

    return allPoliciesPassed ? EvaluationResult.Passed : EvaluationResult.Failed;
  }
}
