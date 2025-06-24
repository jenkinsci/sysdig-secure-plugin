package com.sysdig.jenkins.plugins.sysdig.domain.vm.report;

import java.util.Objects;

public record PolicyBundleRule(
  String id,
  String description,
  EvaluationResult evaluationResult
) {
  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    PolicyBundleRule that = (PolicyBundleRule) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
