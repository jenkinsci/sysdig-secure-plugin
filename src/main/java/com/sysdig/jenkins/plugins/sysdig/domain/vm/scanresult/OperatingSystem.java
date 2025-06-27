package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

import java.io.Serializable;
import java.util.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    OperatingSystem that = (OperatingSystem) o;
    return family == that.family && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(family, name);
  }
}
