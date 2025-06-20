package com.sysdig.jenkins.plugins.sysdig.domain;

public interface ValueObject<T> {
  T clone();
}
