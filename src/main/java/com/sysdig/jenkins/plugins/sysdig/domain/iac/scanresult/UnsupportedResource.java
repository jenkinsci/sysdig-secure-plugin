package com.sysdig.jenkins.plugins.sysdig.domain.iac.scanresult;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * A source file containing resource kinds the scanner does not (yet) support, along with the list
 * of those unsupported resource kinds.
 */
public record UnsupportedResource(String source, List<String> details) implements Serializable {

    public UnsupportedResource {
        details = details == null ? List.of() : List.copyOf(details);
    }

    @Override
    public List<String> details() {
        return Collections.unmodifiableList(details);
    }
}
