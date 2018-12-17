package com.sysdig.jenkins.plugins.sysdig;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import java.io.Serializable;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Wrapper class for Sysdig Secure query
 */
public class AnchoreQuery extends AbstractDescribableImpl<AnchoreQuery> implements Serializable {

  private static final long serialVersionUID = 1L;

  private String query;

  public String getQuery() {
    return query;
  }

  @DataBoundConstructor
  public AnchoreQuery(String query) {
    this.query = query;
  }

  @Extension
  public static class DescriptorImpl extends Descriptor<AnchoreQuery> {

    @Override
    public String getDisplayName() {
      return "Sysdig Secure Query";
    }
  }
}
