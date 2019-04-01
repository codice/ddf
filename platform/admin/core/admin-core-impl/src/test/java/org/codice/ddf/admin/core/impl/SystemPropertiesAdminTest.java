/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.admin.core.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.management.NotCompliantMBeanException;
import org.apache.commons.io.FileUtils;
import org.codice.ddf.admin.core.api.SystemPropertyDetails;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.configuration.SystemInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SystemPropertiesAdminTest {

  @Mock private GuestClaimsHandlerExt mockGuestClaimsHandlerExt;

  File etcFolder = null;

  File systemPropsFile = null;

  File userPropsFile = null;

  File userAttrsFile = null;

  @Before
  public void setUp() throws IOException {
    TemporaryFolder temporaryFolder = new TemporaryFolder();
    temporaryFolder.create();
    etcFolder = temporaryFolder.newFolder("etc");
    System.setProperty("karaf.etc", etcFolder.getAbsolutePath());

    systemPropsFile = new File(etcFolder, "custom.system.properties");
    userPropsFile = new File(etcFolder, "users.properties");
    userAttrsFile = new File(etcFolder, "users.attributes");

    try (InputStream is =
        SystemPropertiesAdminTest.class.getResourceAsStream("/custom.system.properties")) {
      FileUtils.copyToFile(is, systemPropsFile);
    }
  }

  @Test
  public void testReadSystemProperties() throws NotCompliantMBeanException {
    SystemPropertiesAdmin spa = new SystemPropertiesAdmin(mockGuestClaimsHandlerExt);
    List<SystemPropertyDetails> details = spa.readSystemProperties();
    assertThat(getDetailsValue(details, SystemBaseUrl.EXTERNAL_HOST), equalTo("localhost"));
    assertThat(getDetailsValue(details, SystemBaseUrl.EXTERNAL_HTTP_PORT), equalTo("5678"));
    assertThat(getDetailsValue(details, SystemBaseUrl.EXTERNAL_HTTPS_PORT), equalTo("1234"));
    assertThat(getDetailsValue(details, SystemBaseUrl.INTERNAL_HOST), equalTo("localhost"));
    assertThat(getDetailsValue(details, SystemBaseUrl.INTERNAL_HTTP_PORT), equalTo("5678"));
    assertThat(getDetailsValue(details, SystemBaseUrl.INTERNAL_HTTPS_PORT), equalTo("1234"));
    assertThat(getDetailsValue(details, SystemInfo.ORGANIZATION), equalTo("org"));
    assertThat(getDetailsValue(details, SystemInfo.SITE_CONTACT), equalTo("contact"));
    assertThat(getDetailsValue(details, SystemInfo.SITE_NAME), equalTo("site"));
    assertThat(getDetailsValue(details, SystemInfo.VERSION), equalTo("version"));
  }

  @Test
  public void testWriteSystemPropertiesNullProps() throws NotCompliantMBeanException, IOException {
    SystemPropertiesAdmin spa = new SystemPropertiesAdmin(mockGuestClaimsHandlerExt);
    spa.writeSystemProperties(null);

    // Read the system dot properties file and make sure that the new values evaluate to what we
    // expect
    org.apache.felix.utils.properties.Properties systemProps =
        new org.apache.felix.utils.properties.Properties(systemPropsFile);
    assertThat(systemProps.getProperty(SystemBaseUrl.EXTERNAL_HOST), equalTo("localhost"));
    assertThat(systemProps.getProperty(SystemBaseUrl.EXTERNAL_HTTP_PORT), equalTo("5678"));
    assertThat(systemProps.getProperty(SystemBaseUrl.EXTERNAL_HTTPS_PORT), equalTo("1234"));
    assertThat(systemProps.getProperty(SystemBaseUrl.INTERNAL_HOST), equalTo("localhost"));
    assertThat(systemProps.getProperty(SystemBaseUrl.INTERNAL_HTTP_PORT), equalTo("5678"));
    assertThat(systemProps.getProperty(SystemBaseUrl.INTERNAL_HTTPS_PORT), equalTo("1234"));
    assertThat(systemProps.getProperty(SystemInfo.ORGANIZATION), equalTo("org"));
    assertThat(systemProps.getProperty(SystemInfo.SITE_CONTACT), equalTo("contact"));
    assertThat(systemProps.getProperty(SystemInfo.SITE_NAME), equalTo("site"));
    assertThat(systemProps.getProperty(SystemInfo.VERSION), equalTo("version"));
  }

  @Test
  public void testWriteSystemProperties() throws Exception {
    Properties userProps = new Properties();
    userProps.put("admin", "admin,group,somethingelse");
    userProps.put("localhost", "host,group,somethingelse");
    try (FileOutputStream outProps = new FileOutputStream(userPropsFile)) {
      userProps.store(outProps, null);
    }

    try (FileOutputStream outAttrs = new FileOutputStream(userAttrsFile)) {
      String json =
          "{\n"
              + "    \"admin\" : {\n"
              + "\n"
              + "    },\n"
              + "    \"localhost\" : {\n"
              + "\n"
              + "    }\n"
              + "}";
      outAttrs.write(json.getBytes());
    }

    SystemPropertiesAdmin spa = new SystemPropertiesAdmin(mockGuestClaimsHandlerExt);
    Map<String, String> map = new HashMap<>();
    map.put(SystemBaseUrl.INTERNAL_HOST, "newhost");
    map.put(SystemBaseUrl.EXTERNAL_HOST, "newhost");
    spa.writeSystemProperties(map);

    // Read the system dot properties file and make sure that the new values evaluate to what we
    // expect
    org.apache.felix.utils.properties.Properties systemProps =
        new org.apache.felix.utils.properties.Properties(systemPropsFile);
    assertThat(systemProps.getProperty(SystemBaseUrl.EXTERNAL_HOST), equalTo("newhost"));
    assertThat(systemProps.getProperty(SystemBaseUrl.EXTERNAL_HTTP_PORT), equalTo("5678"));
    assertThat(systemProps.getProperty(SystemBaseUrl.EXTERNAL_HTTPS_PORT), equalTo("1234"));
    assertThat(systemProps.getProperty(SystemBaseUrl.INTERNAL_HOST), equalTo("newhost"));
    assertThat(systemProps.getProperty(SystemBaseUrl.INTERNAL_HTTP_PORT), equalTo("5678"));
    assertThat(systemProps.getProperty(SystemBaseUrl.INTERNAL_HTTPS_PORT), equalTo("1234"));
    assertThat(systemProps.getProperty(SystemInfo.ORGANIZATION), equalTo("org"));
    assertThat(systemProps.getProperty(SystemInfo.SITE_CONTACT), equalTo("contact"));
    assertThat(systemProps.getProperty(SystemInfo.SITE_NAME), equalTo("site"));
    assertThat(systemProps.getProperty(SystemInfo.VERSION), equalTo("version"));

    // only writes out the changed props
    assertTrue(systemPropsFile.exists());
    Properties sysProps = new Properties();
    try (FileReader sysPropsReader = new FileReader(systemPropsFile)) {
      sysProps.load(sysPropsReader);
      assertThat(sysProps.getProperty(SystemBaseUrl.INTERNAL_HOST), equalTo("newhost"));
    }

    userProps = new Properties();
    try (FileReader userPropsReader = new FileReader(userPropsFile)) {
      userProps.load(userPropsReader);
      assertThat(userProps.size(), is(2));
      assertThat(userProps.getProperty("newhost"), equalTo("host,group,somethingelse"));
    }

    map.put(SystemBaseUrl.INTERNAL_HOST, "anotherhost");
    spa.writeSystemProperties(map);
    userProps = new Properties();
    try (FileReader userPropsReader = new FileReader(userPropsFile)) {
      userProps.load(userPropsReader);
      assertThat(userProps.size(), is(2));
      assertThat(userProps.getProperty("anotherhost"), equalTo("host,group,somethingelse"));
      assertNull(userProps.getProperty("newhost"));
      assertNull(userProps.getProperty("host"));
    }
    try (BufferedReader userAttrsReader = new BufferedReader(new FileReader(userAttrsFile))) {
      String line = null;
      boolean hasHost = false;
      while ((line = userAttrsReader.readLine()) != null) {
        if (line.contains("anotherhost")) {
          hasHost = true;
        }
      }
      if (!hasHost) {
        fail("User attribute file did not get updated.");
      }
    }
  }

  @Test
  public void testWritePlaceholderProperties() throws Exception {
    SystemPropertiesAdmin spa = new SystemPropertiesAdmin(mockGuestClaimsHandlerExt);
    Map<String, String> map = new HashMap<>();
    map.put(SystemBaseUrl.INTERNAL_HOST, "localhost");
    map.put(SystemBaseUrl.INTERNAL_HTTP_PORT, "9999");
    map.put(SystemBaseUrl.INTERNAL_HTTPS_PORT, "1111");
    map.put(SystemBaseUrl.EXTERNAL_HOST, "localhost");
    map.put(SystemBaseUrl.EXTERNAL_HTTP_PORT, "5678");
    map.put(SystemBaseUrl.EXTERNAL_HTTPS_PORT, "1234");

    spa.writeSystemProperties(map);

    // Read the system dot properties file and make sure that the new values evaluate to what we
    // expect
    org.apache.felix.utils.properties.Properties systemProps =
        new org.apache.felix.utils.properties.Properties(systemPropsFile);

    assertThat(systemProps.getProperty(SystemBaseUrl.EXTERNAL_HOST), equalTo("localhost"));
    assertThat(systemProps.getProperty(SystemBaseUrl.EXTERNAL_HOST), equalTo("localhost"));
    assertThat(systemProps.getProperty(SystemBaseUrl.EXTERNAL_HTTP_PORT), equalTo("5678"));
    assertThat(systemProps.getProperty(SystemBaseUrl.EXTERNAL_HTTPS_PORT), equalTo("1234"));
    assertThat(systemProps.getProperty(SystemBaseUrl.INTERNAL_HOST), equalTo("localhost"));
    assertThat(systemProps.getProperty(SystemBaseUrl.INTERNAL_HTTP_PORT), equalTo("9999"));
    assertThat(systemProps.getProperty(SystemBaseUrl.INTERNAL_HTTPS_PORT), equalTo("1111"));
    assertThat(systemProps.getProperty(SystemInfo.ORGANIZATION), equalTo("org"));
    assertThat(systemProps.getProperty(SystemInfo.SITE_CONTACT), equalTo("contact"));
    assertThat(systemProps.getProperty(SystemInfo.SITE_NAME), equalTo("site"));
    assertThat(systemProps.getProperty(SystemInfo.VERSION), equalTo("version"));
  }

  @Test
  public void testCorrectValuesAreWrittenAfterAnotherWrite() throws Exception {
    String testPropertyKey = "test.password";
    String testPropertyOldValue = "password";
    String testPropertyNewValue = "aNewValue";

    // Init a system properties admin
    SystemPropertiesAdmin spa = new SystemPropertiesAdmin(mockGuestClaimsHandlerExt);

    // Check the value of test property
    org.apache.felix.utils.properties.Properties systemDotProperties =
        new org.apache.felix.utils.properties.Properties(systemPropsFile);
    assertThat(systemDotProperties.getProperty(testPropertyKey), equalTo(testPropertyOldValue));

    // Write the value of test property manually
    systemDotProperties.setProperty(testPropertyKey, testPropertyNewValue);
    systemDotProperties.save();

    // Write out the installer properties
    Map<String, String> map = new HashMap<>();
    map.put(SystemBaseUrl.INTERNAL_HOST, "localhost");
    map.put(SystemBaseUrl.INTERNAL_HTTP_PORT, "9999");
    map.put(SystemBaseUrl.INTERNAL_HTTPS_PORT, "1111");
    map.put(SystemBaseUrl.EXTERNAL_HOST, "localhost");
    map.put(SystemBaseUrl.EXTERNAL_HTTP_PORT, "5678");
    map.put(SystemBaseUrl.EXTERNAL_HTTPS_PORT, "1234");

    spa.writeSystemProperties(map);
    // Read the system dot properties file and make sure that the new values evaluate to what we
    // expect
    org.apache.felix.utils.properties.Properties systemProps =
        new org.apache.felix.utils.properties.Properties(systemPropsFile);

    assertThat(systemProps.getProperty(SystemBaseUrl.EXTERNAL_HOST), equalTo("localhost"));
    assertThat(systemProps.getProperty(SystemBaseUrl.EXTERNAL_HOST), equalTo("localhost"));
    assertThat(systemProps.getProperty(SystemBaseUrl.EXTERNAL_HTTP_PORT), equalTo("5678"));
    assertThat(systemProps.getProperty(SystemBaseUrl.EXTERNAL_HTTPS_PORT), equalTo("1234"));
    assertThat(systemProps.getProperty(SystemBaseUrl.INTERNAL_HOST), equalTo("localhost"));
    assertThat(systemProps.getProperty(SystemBaseUrl.INTERNAL_HTTP_PORT), equalTo("9999"));
    assertThat(systemProps.getProperty(SystemBaseUrl.INTERNAL_HTTPS_PORT), equalTo("1111"));
    assertThat(systemProps.getProperty(SystemInfo.ORGANIZATION), equalTo("org"));
    assertThat(systemProps.getProperty(SystemInfo.SITE_CONTACT), equalTo("contact"));
    assertThat(systemProps.getProperty(SystemInfo.SITE_NAME), equalTo("site"));
    assertThat(systemProps.getProperty(SystemInfo.VERSION), equalTo("version"));

    // Test that the test property wasn't overridden
    assertThat(systemProps.getProperty(testPropertyKey), equalTo(testPropertyNewValue));
  }

  private String getDetailsValue(List<SystemPropertyDetails> props, String key) {
    for (SystemPropertyDetails spd : props) {
      if (spd.getKey().equals(key)) {
        return spd.getValue();
      }
    }
    return "KeyNotFound";
  }
}
