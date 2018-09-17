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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.management.NotCompliantMBeanException;
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

  int expectedSystemPropertiesCount = 0;

  @Before
  public void setUp() throws IOException {
    System.setProperty(SystemBaseUrl.EXTERNAL_PORT, "1234");
    System.setProperty(SystemBaseUrl.EXTERNAL_HOST, "host");
    expectedSystemPropertiesCount++;
    System.setProperty(SystemBaseUrl.EXTERNAL_HTTP_PORT, "4567");
    expectedSystemPropertiesCount++;
    System.setProperty(SystemBaseUrl.EXTERNAL_HTTPS_PORT, "8901");
    expectedSystemPropertiesCount++;
    System.setProperty(SystemInfo.ORGANIZATION, "org");
    expectedSystemPropertiesCount++;
    System.setProperty(SystemInfo.SITE_CONTACT, "contact");
    expectedSystemPropertiesCount++;
    System.setProperty(SystemInfo.SITE_NAME, "site");
    expectedSystemPropertiesCount++;
    System.setProperty(SystemInfo.VERSION, "version");
    expectedSystemPropertiesCount++;

    TemporaryFolder temporaryFolder = new TemporaryFolder();
    temporaryFolder.create();
    etcFolder = temporaryFolder.newFolder("etc");
    System.setProperty("karaf.etc", etcFolder.getAbsolutePath());

    systemPropsFile = new File(etcFolder, "custom.system.properties");
    userPropsFile = new File(etcFolder, "users.properties");
    userAttrsFile = new File(etcFolder, "users.attributes");
  }

  @Test
  public void testReadSystemProperties() throws NotCompliantMBeanException {
    SystemPropertiesAdmin spa = new SystemPropertiesAdmin(mockGuestClaimsHandlerExt);
    List<SystemPropertyDetails> details = spa.readSystemProperties();
    assertThat(getDetailsValue(details, SystemBaseUrl.EXTERNAL_HOST), equalTo("host"));
    assertThat(getDetailsValue(details, SystemBaseUrl.EXTERNAL_HTTP_PORT), equalTo("4567"));
    assertThat(getDetailsValue(details, SystemBaseUrl.EXTERNAL_HTTPS_PORT), equalTo("8901"));
    assertThat(getDetailsValue(details, SystemInfo.ORGANIZATION), equalTo("org"));
    assertThat(getDetailsValue(details, SystemInfo.SITE_CONTACT), equalTo("contact"));
    assertThat(getDetailsValue(details, SystemInfo.SITE_NAME), equalTo("site"));
    assertThat(getDetailsValue(details, SystemInfo.VERSION), equalTo("version"));
    assertThat(details.size(), is(expectedSystemPropertiesCount));
  }

  @Test
  public void testWriteSystemPropertiesNullProps() throws NotCompliantMBeanException {
    SystemPropertiesAdmin spa = new SystemPropertiesAdmin(mockGuestClaimsHandlerExt);
    spa.writeSystemProperties(null);
    List<SystemPropertyDetails> details = spa.readSystemProperties();
    assertThat(SystemBaseUrl.EXTERNAL.getHost(), equalTo("host"));
    assertThat(SystemBaseUrl.EXTERNAL.getPort(), equalTo("1234"));
    assertThat(SystemBaseUrl.EXTERNAL.getHttpPort(), equalTo("4567"));
    assertThat(SystemBaseUrl.EXTERNAL.getHttpsPort(), equalTo("8901"));
    assertThat(SystemBaseUrl.EXTERNAL.getProtocol(), equalTo("https://"));
    assertThat(SystemInfo.getOrganization(), equalTo("org"));
    assertThat(SystemInfo.getSiteContatct(), equalTo("contact"));
    assertThat(SystemInfo.getSiteName(), equalTo("site"));
    assertThat(SystemInfo.getVersion(), equalTo("version"));
    assertThat(details.size(), is(expectedSystemPropertiesCount));
  }

  @Test
  public void testWriteSystemProperties() throws Exception {

    Properties userProps = new Properties();
    userProps.put("admin", "admin,group,somethingelse");
    userProps.put("host", "host,group,somethingelse");
    try (FileOutputStream outProps = new FileOutputStream(userPropsFile)) {
      userProps.store(outProps, null);
    }

    try (FileOutputStream outAttrs = new FileOutputStream(userAttrsFile)) {
      String json =
          "{\n"
              + "    \"admin\" : {\n"
              + "\n"
              + "    },\n"
              + "    \"host\" : {\n"
              + "\n"
              + "    }\n"
              + "}";
      outAttrs.write(json.getBytes());
    }

    SystemPropertiesAdmin spa = new SystemPropertiesAdmin(mockGuestClaimsHandlerExt);
    Map<String, String> map = new HashMap<>();
    map.put(SystemBaseUrl.EXTERNAL_HOST, "newhost");
    spa.writeSystemProperties(map);
    List<SystemPropertyDetails> details = spa.readSystemProperties();
    assertThat(SystemBaseUrl.EXTERNAL.getHost(), equalTo("newhost"));
    assertThat(SystemBaseUrl.EXTERNAL.getPort(), equalTo("8901"));
    assertThat(SystemBaseUrl.EXTERNAL.getHttpPort(), equalTo("4567"));
    assertThat(SystemBaseUrl.EXTERNAL.getHttpsPort(), equalTo("8901"));
    assertThat(SystemBaseUrl.EXTERNAL.getProtocol(), equalTo("https://"));
    assertThat(SystemInfo.getOrganization(), equalTo("org"));
    assertThat(SystemInfo.getSiteContatct(), equalTo("contact"));
    assertThat(SystemInfo.getSiteName(), equalTo("site"));
    assertThat(SystemInfo.getVersion(), equalTo("version"));
    assertThat(details.size(), is(expectedSystemPropertiesCount));

    // only writes out the changed props
    assertTrue(systemPropsFile.exists());
    Properties sysProps = new Properties();
    try (FileReader sysPropsReader = new FileReader(systemPropsFile)) {
      sysProps.load(sysPropsReader);
      assertThat(sysProps.size(), is(3));
      assertThat(sysProps.getProperty(SystemBaseUrl.EXTERNAL_HOST), equalTo("newhost"));
    }

    userProps = new Properties();
    try (FileReader userPropsReader = new FileReader(userPropsFile)) {
      userProps.load(userPropsReader);
      assertThat(userProps.size(), is(2));
      assertThat(userProps.getProperty("newhost"), equalTo("host,group,somethingelse"));
    }

    map.put(SystemBaseUrl.EXTERNAL_HOST, "anotherhost");
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

  private String getDetailsValue(List<SystemPropertyDetails> props, String key) {
    for (SystemPropertyDetails spd : props) {
      if (spd.getKey().equals(key)) {
        return spd.getValue();
      }
    }
    return "KeyNotFound";
  }
}
