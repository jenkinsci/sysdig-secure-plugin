/*
Copyright (C) 2016-2020 Sysdig

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.sysdig.jenkins.plugins.sysdig;

import com.sysdig.jenkins.plugins.sysdig.json.GsonBuilder;
import com.sysdig.jenkins.plugins.sysdig.uireport.PolicyEvaluationSummary;
import hudson.model.Action;
import hudson.model.Run;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import java.util.Map;

/**
 * Sysdig Secure plugin results for a given build are stored and subsequently retrieved from an instance of this class. Rendering/display of
 * the results is defined in the appropriate index and summary jelly files. This Jenkins Action is associated with a build (and not the
 * project which is one level up)
 */
public class SysdigAction implements Action {

  private final Run<?, ?> build;
  private final String gateStatus;
  private final String gateOutputUrl;
  private final PolicyEvaluationSummary gateSummary;
  private final String cveListingUrl;

  // For backwards compatibility
  @Deprecated
  private String gateReportUrl;
  @Deprecated
  private Map<String, String> queries;


  public SysdigAction(Run<?, ?> build, String gateStatus, final String jenkinsOutputDirName, String gateReport, PolicyEvaluationSummary gateSummary, String cveListingFileName) {
    this.build = build;
    this.gateStatus = gateStatus;
    this.gateOutputUrl = "../artifact/" + jenkinsOutputDirName + "/" + gateReport;
    this.gateSummary = gateSummary;
    this.cveListingUrl = String.format("../artifact/%s/%s", jenkinsOutputDirName, cveListingFileName);
  }

  @Override
  public String getIconFileName() {
    return Jenkins.RESOURCE_PATH + "/plugin/sysdig-secure/images/sysdig-shovel.png";
  }

  @Override
  public String getDisplayName() {
    return "Sysdig Secure Report (" + gateStatus + ")";
  }

  @Override
  public String getUrlName() {
    return "sysdig-secure-results";
  }

  public Run<?, ?> getBuild() {
    return this.build;
  }

  public String getGateStatus() {
    return gateStatus;
  }

  public String getGateOutputUrl() {
    return this.gateOutputUrl;
  }

  public String getGateSummary() {
    return GsonBuilder.build().toJson(this.gateSummary);
  }

  public String getCveListingUrl() {
    return cveListingUrl;
  }

  public String getGateReportUrl() {
    return this.gateReportUrl;
  }

  public Map<String, String> getQueries() {
    return this.queries;
  }

  //FIXME(fede): remove this lol
  public Boolean getLegacyEngine() {
    return false;
  }

}
