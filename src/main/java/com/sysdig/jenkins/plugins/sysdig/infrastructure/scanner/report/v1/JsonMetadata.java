package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1;

import java.util.Map;

record JsonMetadata(
  String architecture,
  String author,
  String baseOs,
  String createdAt,
  String digest,
  String imageId,
  Map<String, String> labels,
  String os,
  String pullString,
  Long size
) {
}
