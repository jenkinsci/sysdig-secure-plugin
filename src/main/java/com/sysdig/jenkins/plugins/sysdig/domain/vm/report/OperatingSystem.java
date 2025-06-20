package com.sysdig.jenkins.plugins.sysdig.domain.vm.report;

import com.sysdig.jenkins.plugins.sysdig.domain.ValueObject;

public class OperatingSystem implements ValueObject<OperatingSystem> {
  public enum Family {
    Linux,
    Darwin,
    Windows
  }

  private final Family family;
  private final String name;

  public OperatingSystem(Family family, String name) {
    this.family = family;
    this.name = name;
  }

  public Family getFamily() {
    return family;
  }

  public String getName() {
    return name;
  }

  @Override
  public OperatingSystem clone() {
    return new OperatingSystem(this.family, this.name);
  }
}
