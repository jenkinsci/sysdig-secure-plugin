package com.sysdig.jenkins.plugins.sysdig.domain.iac.scanresult;

import java.io.Serializable;

/**
 * A single IaC resource (e.g. a Terraform resource) a policy control was evaluated against.
 */
public record Resource(String name, String type, String location, String source) implements Serializable {}
