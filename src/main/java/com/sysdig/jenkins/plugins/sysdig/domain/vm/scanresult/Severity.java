package com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult;

import java.io.Serializable;

public enum Severity  implements Serializable {
  Critical,
  High,
  Medium,
  Low,
  Negligible,
  Unknown,
}
