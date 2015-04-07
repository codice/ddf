/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.configuration;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationAdmin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class ConfigurationManagerTest {

    public static final String PROTOCOL = "foo";

    public static final String HOST = "bar";

    public static final String PORT = "baz";

    public static final String ID = "spam";

    public static final String VERSION = "eggs";

    public static final String ORGANIZATION = "marmalade";

    public static final String CONTACT = "donut";

    MockConfigurationWatcher mockWatcher;

    ConfigurationManager ddfConfigMgr;

    String key;

    Map<String, String> config1;

    Map<String, String> config2;

    @Before
    public void setUp() throws Exception {
        key = ConfigurationManagerTest.class.getSimpleName() + "Key";
        mockWatcher = new MockConfigurationWatcher();
        List<ConfigurationWatcher> watchers = new ArrayList<ConfigurationWatcher>();
        watchers.add(mockWatcher);
        ddfConfigMgr = new ConfigurationManager(watchers, null);
        config1 = new HashMap<String, String>();
        config1.put(key, "config1");
        config2 = new HashMap<String, String>();
        config2.put(key, "config2");
        ddfConfigMgr.updated(config1);
    }

    @Test
    public void testDdfConfigurationManager() {
        assertNotNull(ddfConfigMgr);
        assertTrue(null == ddfConfigMgr.getConfigurationAdmin());
    }

    @Test
    public void testUpdated() {
        assertEquals(mockWatcher.getConfigValue(key), config1.get(key));

        ddfConfigMgr.updated(null);
        assertEquals(mockWatcher.getConfigValue(key), config1.get(key));

        ddfConfigMgr.updated(new HashMap<String, String>());
        assertEquals(mockWatcher.getConfigValue(key), config1.get(key));

        ddfConfigMgr.updated(config2);
        assertEquals(mockWatcher.getConfigValue(key), config2.get(key));
    }

    @Test
    public void testBind() {
        assertEquals(mockWatcher.getConfigValue(key), config1.get(key));

        ddfConfigMgr.bind(mockWatcher, null);

        assertEquals(mockWatcher.getConfigValue(key), config1.get(key));
        ddfConfigMgr.bind(null, null);
        assertEquals(mockWatcher.getConfigValue(key), config1.get(key));
        ddfConfigMgr.bind(null, null);
        assertEquals(mockWatcher.getConfigValue(key), config1.get(key));
        ddfConfigMgr.updated(config2);
        assertEquals(mockWatcher.getConfigValue(key), config2.get(key));
        assertTrue(ddfConfigMgr.configuration.size() > 1); // should have read-only props too.
    }

    @Test
    public void testGetConfigurationAdmin() {
        assertEquals(null, ddfConfigMgr.getConfigurationAdmin());
        ConfigurationAdmin mock = mock(ConfigurationAdmin.class);
        ddfConfigMgr.setConfigurationAdmin(mock);
        assertEquals(mock, ddfConfigMgr.getConfigurationAdmin());
    }

    @Test
    public void testGetConfigurationValue() {
        ddfConfigMgr.getConfigurationValue("1234", key);
    }

    @Test
    public void testInit() {
        ddfConfigMgr.setProtocol(PROTOCOL);
        ddfConfigMgr.setHost(HOST);
        ddfConfigMgr.setPort(PORT);
        ddfConfigMgr.setId(ID);
        ddfConfigMgr.setVersion(VERSION);
        ddfConfigMgr.setOrganization(ORGANIZATION);
        ddfConfigMgr.setContact(CONTACT);
        ddfConfigMgr.init();
        assertEquals(PROTOCOL, mockWatcher.getConfigValue(ConfigurationManager.PROTOCOL));
        assertEquals(HOST, mockWatcher.getConfigValue(ConfigurationManager.HOST));
        assertEquals(PORT, mockWatcher.getConfigValue(ConfigurationManager.PORT));
        assertEquals(ID, mockWatcher.getConfigValue(ConfigurationManager.SITE_NAME));
        assertEquals(VERSION, mockWatcher.getConfigValue(ConfigurationManager.VERSION));
        assertEquals(ORGANIZATION, mockWatcher.getConfigValue(ConfigurationManager.ORGANIZATION));
        assertEquals(CONTACT, mockWatcher.getConfigValue(ConfigurationManager.CONTACT));
    }
}
