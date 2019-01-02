package com.sysdig.jenkins.plugins.sysdig;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import java.io.Serializable;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Wrapper class for Sysdig Secure query
 */
public class Annotation extends AbstractDescribableImpl<Annotation> implements Serializable {

  private static final long serialVersionUID = 1L;

  private String key;
  private String value;

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }

  @DataBoundConstructor
  public Annotation(String key, String value) {
    this.key = key;
    this.value = value;
  }

  @Extension
  public static class DescriptorImpl extends Descriptor<Annotation> {

    @Override
    public String getDisplayName() {
      return "Sysdig Secure Engine Image Annotation";
    }
  }
}
