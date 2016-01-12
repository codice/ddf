/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.ui.admin.api;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.configuration.SystemInfo;
import org.codice.ddf.ui.admin.api.impl.SystemPropertiesAdmin;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SystemPropertiesAdminTest {
    File etcFolder = null;

    File systemPropsFile = null;

    File userPropsFile = null;

    SystemInfo info = null;

    int expectedSystemPropertiesCount = 0;

    @Before
    public void setUp() throws IOException {
        System.setProperty(SystemBaseUrl.HOST, "host");
        expectedSystemPropertiesCount++;
        System.setProperty(SystemBaseUrl.PORT, "1234");
        expectedSystemPropertiesCount++;
        System.setProperty(SystemBaseUrl.HTTP_PORT, "4567");
        expectedSystemPropertiesCount++;
        System.setProperty(SystemBaseUrl.HTTPS_PORT, "8901");
        expectedSystemPropertiesCount++;
        System.setProperty(SystemBaseUrl.PROTOCOL, "https://");
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

        systemPropsFile = new File(etcFolder, "system.properties");
        userPropsFile = new File(etcFolder, "users.properties");

        info = new SystemInfo();
    }

    @Test
    public void testReadSystemProperties() {
        SystemPropertiesAdmin spa = new SystemPropertiesAdmin();
        List<SystemPropertyDetails> details = spa.readSystemProperties();
        assertThat(getDetailsValue(details, SystemBaseUrl.HOST), equalTo("host"));
        assertThat(getDetailsValue(details, SystemBaseUrl.PORT), equalTo("1234"));
        assertThat(getDetailsValue(details, SystemBaseUrl.HTTP_PORT), equalTo("4567"));
        assertThat(getDetailsValue(details, SystemBaseUrl.HTTPS_PORT), equalTo("8901"));
        assertThat(getDetailsValue(details, SystemBaseUrl.PROTOCOL), equalTo("https://"));
        assertThat(getDetailsValue(details, SystemInfo.ORGANIZATION), equalTo("org"));
        assertThat(getDetailsValue(details, SystemInfo.SITE_CONTACT), equalTo("contact"));
        assertThat(getDetailsValue(details, SystemInfo.SITE_NAME), equalTo("site"));
        assertThat(getDetailsValue(details, SystemInfo.VERSION), equalTo("version"));
        assertThat(details.size(), is(expectedSystemPropertiesCount));
    }

    @Test
    public void testWriteSystemPropertiesNullProps() {
        SystemPropertiesAdmin spa = new SystemPropertiesAdmin();
        spa.writeSystemProperties(null);
        List<SystemPropertyDetails> details = spa.readSystemProperties();
        assertThat(SystemBaseUrl.getHost(), equalTo("host"));
        assertThat(SystemBaseUrl.getPort(), equalTo("1234"));
        assertThat(SystemBaseUrl.getHttpPort(), equalTo("4567"));
        assertThat(SystemBaseUrl.getHttpsPort(), equalTo("8901"));
        assertThat(SystemBaseUrl.getProtocol(), equalTo("https://"));
        assertThat(info.getOrganization(), equalTo("org"));
        assertThat(info.getSiteContatct(), equalTo("contact"));
        assertThat(info.getSiteName(), equalTo("site"));
        assertThat(info.getVersion(), equalTo("version"));
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

        SystemPropertiesAdmin spa = new SystemPropertiesAdmin();
        Map<String, String> map = new HashMap<>();
        map.put(SystemBaseUrl.HOST, "newhost");
        spa.writeSystemProperties(map);
        List<SystemPropertyDetails> details = spa.readSystemProperties();
        assertThat(SystemBaseUrl.getHost(), equalTo("newhost"));
        assertThat(SystemBaseUrl.getPort(), equalTo("1234"));
        assertThat(SystemBaseUrl.getHttpPort(), equalTo("4567"));
        assertThat(SystemBaseUrl.getHttpsPort(), equalTo("8901"));
        assertThat(SystemBaseUrl.getProtocol(), equalTo("https://"));
        assertThat(info.getOrganization(), equalTo("org"));
        assertThat(info.getSiteContatct(), equalTo("contact"));
        assertThat(info.getSiteName(), equalTo("site"));
        assertThat(info.getVersion(), equalTo("version"));
        assertThat(details.size(), is(expectedSystemPropertiesCount));

        //only writes out the changed props
        assertTrue(systemPropsFile.exists());
        Properties sysProps = new Properties();
        try (FileReader sysPropsReader = new FileReader(systemPropsFile)) {
            sysProps.load(sysPropsReader);
            assertThat(sysProps.size(), is(1));
            assertThat(sysProps.getProperty(SystemBaseUrl.HOST), equalTo("newhost"));
        }

        userProps = new Properties();
        try (FileReader userPropsReader = new FileReader(userPropsFile)) {
            userProps.load(userPropsReader);
            assertThat(userProps.size(), is(2));
            assertThat(userProps.getProperty("newhost"), equalTo("host,group,somethingelse"));
        }

        map.put(SystemBaseUrl.HOST, "anotherhost");
        spa.writeSystemProperties(map);
        userProps = new Properties();
        try (FileReader userPropsReader = new FileReader(userPropsFile)) {
            userProps.load(userPropsReader);
            assertThat(userProps.size(), is(2));
            assertThat(userProps.getProperty("anotherhost"), equalTo("host,group,somethingelse"));
            assertNull(userProps.getProperty("newhost"));
            assertNull(userProps.getProperty("host"));
        }
    }

    private String getDetailsValue(List<SystemPropertyDetails> props, String key) {
        for (SystemPropertyDetails spd : props) {
            if (spd.getKey()
                    .equals(key)) {
                return spd.getValue();
            }
        }
        return "KeyNotFound";
    }
}
