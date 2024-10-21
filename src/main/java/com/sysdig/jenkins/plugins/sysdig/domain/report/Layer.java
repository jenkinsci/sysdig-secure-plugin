package com.sysdig.jenkins.plugins.sysdig.domain.report;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Layer implements Serializable {
  private  String digest;
  private  Long size;
  private String command;
  private  Vulns vulns;
  private Map<String, Object> runningVulns;
  private List<?> baseImages;

  public Optional<String> getDigest() {
    return Optional.ofNullable(digest);
  }

  public void setDigest(String digest) {
    this.digest = digest;
  }

  public Optional<Long> getSize() {
    return Optional.ofNullable(size);
  }

  public void setSize(Long size) {
    this.size = size;
  }

  public Optional<String> getCommand() {
    return Optional.ofNullable(command);
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public Optional<Vulns> getVulns() {
    return Optional.ofNullable(vulns);
  }

  public void setVulns(Vulns vulns) {
    this.vulns = vulns;
  }

  public Optional<Map<String, Object>> getRunningVulns() {
    return Optional.ofNullable(runningVulns);
  }

  public void setRunningVulns(Map<String, Object> runningVulns) {
    this.runningVulns = runningVulns;
  }

  public Optional<List<?>> getBaseImages() {
    return Optional.ofNullable(baseImages);
  }

  public void setBaseImages(List<?> baseImages) {
    this.baseImages = baseImages;
  }
}
