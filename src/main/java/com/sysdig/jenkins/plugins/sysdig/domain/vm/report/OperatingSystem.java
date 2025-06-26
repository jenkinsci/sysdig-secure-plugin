package com.sysdig.jenkins.plugins.sysdig.domain.vm.report;

import java.io.Serializable;

public class OperatingSystem  implements Serializable {
  private final Family family;
  private final String name;
  public OperatingSystem(Family family, String name) {
    this.family = family;
    this.name = name;
  }

  public Family family() {
    return family;
  }

  public String name() {
    return name;
  }

  public enum Family {
    Linux,
    Darwin,
    Windows,
    Unknown
  }
}
