package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

import java.io.Serializable;

public enum EvaluationResult implements Serializable {
  Passed,
  Failed;

  public boolean isFailed() {
    return this == Failed;
  }

  public boolean isPassed() {
    return this == Passed;
  }
}
