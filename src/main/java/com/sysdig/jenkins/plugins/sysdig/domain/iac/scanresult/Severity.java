package com.sysdig.jenkins.plugins.sysdig.domain.iac.scanresult;

import java.io.Serializable;

/**
 * Severities are declared from most to least severe; {@link #values()} therefore already yields a
 * ranked order. {@link #rank()} gives a numeric weight for sorting in the UI.
 */
public enum Severity implements Serializable {
    Critical,
    High,
    Medium,
    Low,
    Negligible,
    Unknown;

    public int rank() {
        return switch (this) {
            case Critical -> 5;
            case High -> 4;
            case Medium -> 3;
            case Low -> 2;
            case Negligible -> 1;
            case Unknown -> 0;
        };
    }
}
