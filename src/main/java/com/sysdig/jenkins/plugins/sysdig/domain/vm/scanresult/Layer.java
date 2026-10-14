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
    private final Set<Component> components;
    private final ScanResult root;

    Layer(String digest, BigInteger size, String command, ScanResult root) {
        this.digest = digest;
        this.size = size;
        this.command = command;
        this.components = new HashSet<>();
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

    void addComponent(Component component) {
        this.components.add(component);
    }

    public Collection<Component> components() {
        return Collections.unmodifiableCollection(this.components);
    }

    public Collection<Package> packages() {
        return componentsOf(Package.class);
    }

    public Collection<Application> applications() {
        return componentsOf(Application.class);
    }

    private <T extends Component> Collection<T> componentsOf(Class<T> kind) {
        return components.stream().filter(kind::isInstance).map(kind::cast).toList();
    }

    public Collection<Vulnerability> vulnerabilities() {
        return this.components().stream()
                .flatMap(c -> c.vulnerabilities().stream())
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
