package com.sysdig.jenkins.plugins.sysdig.domain.report;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public class Bundle implements Serializable {
  private String name;
  private String identifier;
  private String type;
  private List<Rule> rules;

  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  public void setName(String name) {
    this.name = name;
  }

  public Optional<String> getIdentifier() {
    return Optional.ofNullable(identifier);
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public Optional<String> getType() {
    return Optional.ofNullable(type);
  }

  public void setType(String type) {
    this.type = type;
  }

  public Optional<List<Rule>> getRules() {
    return Optional.ofNullable(rules);
  }

  public void setRules(List<Rule> rules) {
    this.rules = rules;
  }
}
