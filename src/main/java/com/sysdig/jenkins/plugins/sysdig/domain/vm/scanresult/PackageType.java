package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

import java.io.Serializable;

public enum PackageType implements Serializable {
    Unknown,
    OS,
    Python,
    Java,
    Javascript,
    Golang,
    Rust,
    Ruby,
    PHP,
    CSharp,
}
