package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

import com.sysdig.jenkins.plugins.sysdig.domain.AggregateChild;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class Layer implements AggregateChild<ScanResult>, Serializable {
    private final String digest;
    private final BigInteger size;
    private final String command;
    private final Set<Package> packages;
    private final ScanResult root;

    Layer(String digest, BigInteger size, String command, ScanResult root) {
        this.digest = digest;
        this.size = size;
        this.command = command;
        this.packages = new HashSet<>();
        this.root = root;
    }

    public Optional<String> digest() {
        if (digest == null || digest.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(digest);
    }

    public Optional<BigInteger> size() {
        return Optional.ofNullable(size);
    }

    public String command() {
        return command;
    }

    @Override
    public ScanResult root() {
        return root;
    }

    void addPackage(Package aPackage) {
        this.packages.add(aPackage);
    }

    public Collection<Package> packages() {
        return Collections.unmodifiableCollection(this.packages);
    }

    public Collection<Vulnerability> vulnerabilities() {
        return this.packages().stream()
                .flatMap(p -> p.vulnerabilities().stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Layer layer = (Layer) o;
        return Objects.equals(digest, layer.digest);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(digest);
    }
}
