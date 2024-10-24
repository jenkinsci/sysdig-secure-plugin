package com.sysdig.jenkins.plugins.sysdig.application.vm;

import com.sysdig.jenkins.plugins.sysdig.application.vm.report.PolicyEvaluationReport;
import com.sysdig.jenkins.plugins.sysdig.application.vm.report.PolicyEvaluationSummary;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.ImageScanningResult;

import javax.annotation.Nonnull;

public interface ReportProcessor {
  PolicyEvaluationReport processPolicyEvaluation(ImageScanningResult result);

  PolicyEvaluationSummary generateGatesSummary(@Nonnull PolicyEvaluationReport gatesJson, @Nonnull ImageScanningResult imageScanningResult);
}
