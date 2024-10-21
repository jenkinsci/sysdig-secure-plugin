package com.sysdig.jenkins.plugins.sysdig.domain.report;

import java.io.Serializable;
import java.util.Optional;

public class PublishDateByVendor implements Serializable {
  private String nvd;
  private String vulndb;
  private String cisakev;

  public Optional<String> getNvd() {
    return Optional.ofNullable(nvd);
  }

  public void setNvd(String nvd) {
    this.nvd = nvd;
  }

  public Optional<String> getVulndb() {
    return Optional.ofNullable(vulndb);
  }

  public void setVulndb(String vulndb) {
    this.vulndb = vulndb;
  }

  public Optional<String> getCisakev() {
    return Optional.ofNullable(cisakev);
  }

  public void setCisakev(String cisakev) {
    this.cisakev = cisakev;
  }
}
