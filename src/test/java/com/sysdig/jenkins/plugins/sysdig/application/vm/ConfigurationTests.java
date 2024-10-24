package com.sysdig.jenkins.plugins.sysdig.application.vm;

import com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.vm.entrypoint.ImageScanningBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;

public class ConfigurationTests {
  @Rule
  public JenkinsRule jenkins = new JenkinsRule();
  private final JenkinsTestHelpers helpers = new JenkinsTestHelpers(jenkins);

  @Before
  public void setUp() throws Exception {
    helpers.configureSysdigCredentials();
  }

  @Test
  public void testConfigurationPersistenceAfterRoundTrip() throws Exception {
    var project = helpers.createFreestyleProjectWithImageScanningBuilder("test").build();

    project = jenkins.configRoundtrip(project);

    jenkins.assertEqualDataBoundBeans(new ImageScanningBuilder("test"), project.getBuildersList().get(0));
  }

  @Test
  public void testDefaultGlobalConfigurationValues() {
    var globalConfig = jenkins.getInstance().getDescriptorByType(ImageScanningBuilder.GlobalConfiguration.class);

    assertEquals("https://secure.sysdig.com", globalConfig.getEngineURL());
    assertEquals("", globalConfig.getEngineCredentialsId());
    assertTrue(globalConfig.getEngineVerify());
    assertEquals("", globalConfig.getInlineScanExtraParams());
    assertEquals("", globalConfig.getScannerBinaryPath());
    assertEquals("", globalConfig.getPoliciesToApply());
    assertEquals("", globalConfig.getCliVersionToApply());
    assertEquals("", globalConfig.getCustomCliVersion());
  }

  @Test
  public void testGlobalConfigurationRoundTrip() throws Exception {
    // Access global configuration using Jenkins.getDescriptorByType()
    var globalConfig = jenkins.getInstance().getDescriptorByType(ImageScanningBuilder.GlobalConfiguration.class);

    // Change configuration values
    globalConfig.setEngineURL("https://custom.sysdig.com");
    globalConfig.setEngineCredentialsId("sysdig-secure");
    globalConfig.setEngineVerify(false);
    globalConfig.setInlineScanExtraParams("--some-extra-param");
    globalConfig.setScannerBinaryPath("/path/to/scanner");
    globalConfig.setPoliciesToApply("policy1,policy2");
    globalConfig.setCliVersionToApply("custom");
    globalConfig.setCustomCliVersion("1.5.0");

    // Save the new configuration
    globalConfig.save();

    // Perform a configuration round-trip to simulate saving through the UI
    jenkins.configRoundtrip();

    // Reload the configuration to ensure the changes persist after the round-trip
    var reloadedConfig = jenkins.getInstance().getDescriptorByType(ImageScanningBuilder.GlobalConfiguration.class);

    // Assert that the values were saved and reloaded correctly after the round-trip
    assertEquals("https://custom.sysdig.com", reloadedConfig.getEngineURL());
    assertEquals("sysdig-secure", reloadedConfig.getEngineCredentialsId());
    assertFalse(reloadedConfig.getEngineVerify());
    assertEquals("--some-extra-param", reloadedConfig.getInlineScanExtraParams());
    assertEquals("/path/to/scanner", reloadedConfig.getScannerBinaryPath());
    assertEquals("policy1,policy2", reloadedConfig.getPoliciesToApply());
    assertEquals("custom", reloadedConfig.getCliVersionToApply());
    assertEquals("1.5.0", reloadedConfig.getCustomCliVersion());
  }
}
