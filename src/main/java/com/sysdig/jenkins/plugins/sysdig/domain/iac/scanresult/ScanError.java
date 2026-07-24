package com.sysdig.jenkins.plugins.sysdig.domain.iac.scanresult;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * A source file the scanner could not parse, along with the details of why it failed.
 */
public record ScanError(String source, List<String> details) implements Serializable {

    public ScanError {
        details = details == null ? List.of() : List.copyOf(details);
    }

    @Override
    public List<String> details() {
        return Collections.unmodifiableList(details);
    }
}
