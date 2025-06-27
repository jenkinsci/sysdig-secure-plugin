package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

import com.sysdig.jenkins.plugins.sysdig.domain.AggregateChild;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

public class Metadata implements AggregateChild<ScanResult>, Serializable {
  private final String pullString;
  private final String imageID;
  private final String digest;
  private final OperatingSystem baseOS;
  private final BigInteger sizeInBytes;
  private final Architecture architecture;
  private final Map<String, String> labels;
  private final Date createdAt;
  private final ScanResult root;

  Metadata(
    String pullString,
    String imageID,
    String digest,
    OperatingSystem baseOS,
    BigInteger sizeInBytes,
    Architecture architecture,
    Map<String, String> labels,
    Date createdAt,
    ScanResult root
  ) {
    this.pullString = pullString;
    this.imageID = imageID;
    this.digest = digest;
    this.baseOS = baseOS;
    this.sizeInBytes = sizeInBytes;
    this.architecture = architecture;
    this.labels = labels;
    this.createdAt = createdAt;
    this.root = root;
  }

  public String pullString() {
    return pullString;
  }

  public String imageID() {
    return imageID;
  }

  public Optional<String> digest() {
    if (digest == null || digest.isBlank()) {
      return Optional.empty();
    }

    return Optional.of(digest);
  }

  public OperatingSystem baseOS() {
    return baseOS;
  }

  public BigInteger sizeInBytes() {
    return sizeInBytes;
  }

  public Architecture architecture() {
    return architecture;
  }

  public Map<String, String> labels() {
    return labels;
  }

  public Date createdAt() {
    return createdAt;
  }

  @Override
  public ScanResult root() {
    return root;
  }

  long layersCount() {
    return root().layers().stream().filter(l -> l.size().isPresent()).count();
  }
}
