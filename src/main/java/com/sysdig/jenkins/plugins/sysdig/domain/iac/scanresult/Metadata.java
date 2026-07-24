package com.sysdig.jenkins.plugins.sysdig.domain.iac.scanresult;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * High-level information about what was scanned: the source paths and the number of modules,
 * resources and folders the scanner walked through.
 */
public record Metadata(List<String> sources, int totalModules, int totalResources, int totalFolders)
        implements Serializable {

    public Metadata {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }

    @Override
    public List<String> sources() {
        return Collections.unmodifiableList(sources);
    }
}
