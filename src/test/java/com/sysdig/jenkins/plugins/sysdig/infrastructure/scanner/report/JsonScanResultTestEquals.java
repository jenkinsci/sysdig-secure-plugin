package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report;

import com.sysdig.jenkins.plugins.sysdig.infrastructure.json.GsonBuilder;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1.JsonScanResultV1;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner.report.v1beta3.JsonScanResultV1Beta3;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JsonScanResultTestEquals {
  @Test
  void theyHaveToBeEqual() {
    var v1 = jsonScanResultV1FromImage("ubuntu_22.04").toDomain().get();
    var v1beta3 = jsonScanResultV1Beta3FromImage("ubuntu_22.04").toDomain().get();

    assertEquals(v1, v1beta3);
  }

  private JsonScanResultV1 jsonScanResultV1FromImage(String image) {
    String resourcePath = "com/sysdig/jenkins/plugins/sysdig/infrastructure/scanner/report/v1/%s.json".formatted(image);
    InputStream imageStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
    assertNotNull(imageStream);

    return GsonBuilder.build().fromJson(new InputStreamReader(imageStream, StandardCharsets.UTF_8), JsonScanResultV1.class);
  }

  private JsonScanResultV1Beta3 jsonScanResultV1Beta3FromImage(String image) {
    String resourcePath = "com/sysdig/jenkins/plugins/sysdig/infrastructure/scanner/report/v1beta3/%s.json".formatted(image);
    InputStream imageStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
    assertNotNull(imageStream);

    return GsonBuilder.build().fromJson(new InputStreamReader(imageStream, StandardCharsets.UTF_8), JsonScanResultV1Beta3.class);
  }
}
