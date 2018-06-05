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
package org.codice.ddf.configuration.impl;

import static org.junit.Assert.assertEquals;

import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.configuration.SystemInfo;
import org.junit.Test;

public class ConfigurationWatcherImpTest {

  @Test
  public void testGetters() {
    ConfigurationWatcherImpl configWatcher = new ConfigurationWatcherImpl();
    configureProperties(
        "http://",
        "hostValue",
        "8888",
        "/services",
        "siteNameValue",
        "orgValue",
        "contactValue",
        "versionValue");
    assertEquals(configWatcher.getContactEmailAddress(), "contactValue");
    assertEquals(configWatcher.getHostname(), "hostValue");
    assertEquals(configWatcher.getPort(), Integer.valueOf("8888"));
    assertEquals(configWatcher.getOrganization(), "orgValue");
    assertEquals(configWatcher.getProtocol(), "http://");
    assertEquals(configWatcher.getSchemeFromProtocol(), "http");
    assertEquals(configWatcher.getSiteName(), "siteNameValue");
    assertEquals(configWatcher.getVersion(), "versionValue");
    assertEquals(configWatcher.getConfigurationValue("BlahKey"), null);

    configureProperties(
        "https://",
        "updatedhostValue",
        "9999",
        "/services",
        "updatedsiteNameValue",
        "updatedorgValue",
        "updatedcontactValue",
        "updatedversionValue");
    assertEquals(configWatcher.getContactEmailAddress(), "updatedcontactValue");
    assertEquals(configWatcher.getHostname(), "updatedhostValue");
    assertEquals(configWatcher.getPort(), Integer.valueOf("9999"));
    assertEquals(configWatcher.getOrganization(), "updatedorgValue");
    assertEquals(configWatcher.getProtocol(), "https://");
    assertEquals(configWatcher.getSchemeFromProtocol(), "https");
    assertEquals(configWatcher.getSiteName(), "updatedsiteNameValue");
    assertEquals(configWatcher.getVersion(), "updatedversionValue");
    assertEquals(configWatcher.getConfigurationValue("BlahKey"), null);
  }

  protected void configureProperties(
      String protocol,
      String host,
      String port,
      String contextRoot,
      String siteName,
      String org,
      String contact,
      String version) {

    System.setProperty(SystemBaseUrl.EXTERNAL_HTTP_PORT, port);
    System.setProperty(SystemBaseUrl.EXTERNAL_HTTPS_PORT, port);
    System.setProperty(SystemBaseUrl.EXTERNAL_HOST, host);
    System.setProperty(SystemBaseUrl.EXTERNAL_PROTOCOL, protocol);
    System.setProperty(SystemBaseUrl.ROOT_CONTEXT, contextRoot);
    System.setProperty(SystemInfo.SITE_NAME, siteName);
    System.setProperty(SystemInfo.SITE_CONTACT, contact);
    System.setProperty(SystemInfo.VERSION, version);
    System.setProperty(SystemInfo.ORGANIZATION, org);
  }
}
