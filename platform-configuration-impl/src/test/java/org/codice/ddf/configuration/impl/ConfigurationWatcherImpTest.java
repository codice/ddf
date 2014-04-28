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
package org.codice.ddf.configuration.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.codice.ddf.configuration.ConfigurationManager;
import org.junit.Test;

public class ConfigurationWatcherImpTest {

    @Test
    public void testGetters() {
        Map<String, String> properties = getProperties();
        ConfigurationWatcherImpl configWatcher = new ConfigurationWatcherImpl();
        configWatcher.configurationUpdateCallback(properties);

        assertEquals(configWatcher.getContactEmailAddress(), "contactValue");
        assertEquals(configWatcher.getHostname(), "hostValue");
        assertEquals(configWatcher.getPort(), Integer.valueOf("8888"));
        assertEquals(configWatcher.getOrganization(), "orgValue");
        assertEquals(configWatcher.getProtocol(), "http://");
        assertEquals(configWatcher.getSchemeFromProtocol(), "http");
        assertEquals(configWatcher.getSiteName(), "siteNameValue");
        assertEquals(configWatcher.getVersion(), "versionValue");
        assertEquals(configWatcher.getConfigurationValue("BlahKey"), "BlahValue");

        Map<String, String> updatedProperties = new HashMap<String, String>();
        updatedProperties.put(ConfigurationManager.CONTACT, "updatedcontactValue");
        updatedProperties.put(ConfigurationManager.HOST, "updatedhostValue");
        updatedProperties.put(ConfigurationManager.PORT, "9999");
        updatedProperties.put(ConfigurationManager.ORGANIZATION, "updatedorgValue");
        updatedProperties.put(ConfigurationManager.PROTOCOL, "https://");
        updatedProperties.put(ConfigurationManager.SITE_NAME, "updatedsiteNameValue");
        updatedProperties.put(ConfigurationManager.VERSION, "updatedversionValue");
        updatedProperties.put("BlahKey", "UpdatedBlahValue");
        configWatcher.configurationUpdateCallback(updatedProperties);

        assertEquals(configWatcher.getContactEmailAddress(), "updatedcontactValue");
        assertEquals(configWatcher.getHostname(), "updatedhostValue");
        assertEquals(configWatcher.getPort(), Integer.valueOf("9999"));
        assertEquals(configWatcher.getOrganization(), "updatedorgValue");
        assertEquals(configWatcher.getProtocol(), "https://");
        assertEquals(configWatcher.getSchemeFromProtocol(), "https");
        assertEquals(configWatcher.getSiteName(), "updatedsiteNameValue");
        assertEquals(configWatcher.getVersion(), "updatedversionValue");
        assertEquals(configWatcher.getConfigurationValue("BlahKey"), "UpdatedBlahValue");
    }

    @Test
    public void testPortValues() {
        Map<String, String> invalidProps = new HashMap<String, String>();
        ConfigurationWatcherImpl configWatcher = new ConfigurationWatcherImpl();

        invalidProps.put(ConfigurationManager.PORT, "Blah");
        configWatcher.configurationUpdateCallback(invalidProps);
        assertNull(configWatcher.getPort());

        invalidProps.put(ConfigurationManager.PORT, null);
        configWatcher.configurationUpdateCallback(invalidProps);
        assertNull(configWatcher.getPort());

        invalidProps.put(ConfigurationManager.PORT, "5555");
        configWatcher.configurationUpdateCallback(invalidProps);
        assertEquals(configWatcher.getPort(), Integer.valueOf("5555"));

        // If set to an invalid value, it will keep the last known good port value
        invalidProps.put(ConfigurationManager.PORT, "Blah");
        configWatcher.configurationUpdateCallback(invalidProps);
        assertEquals(configWatcher.getPort(), Integer.valueOf("5555"));
    }

    @Test
    public void testSchemeValues() {
        Map<String, String> invalidProps = new HashMap<String, String>();
        ConfigurationWatcherImpl configWatcher = new ConfigurationWatcherImpl();

        invalidProps.put(ConfigurationManager.PROTOCOL, "Blah");
        configWatcher.configurationUpdateCallback(invalidProps);
        assertEquals(configWatcher.getSchemeFromProtocol(), "Blah");
        assertEquals(configWatcher.getProtocol(), "Blah");

        invalidProps.put(ConfigurationManager.PROTOCOL, null);
        configWatcher.configurationUpdateCallback(invalidProps);
        assertNull(configWatcher.getSchemeFromProtocol());
        assertNull(configWatcher.getProtocol());

        invalidProps.put(ConfigurationManager.PROTOCOL, "http://");
        configWatcher.configurationUpdateCallback(invalidProps);
        assertEquals(configWatcher.getSchemeFromProtocol(), "http");
        assertEquals(configWatcher.getProtocol(), "http://");

        invalidProps.put(ConfigurationManager.PROTOCOL, "Blah");
        configWatcher.configurationUpdateCallback(invalidProps);
        assertEquals(configWatcher.getSchemeFromProtocol(), "Blah");
        assertEquals(configWatcher.getProtocol(), "Blah");

        invalidProps.put(ConfigurationManager.PROTOCOL, "https://");
        configWatcher.configurationUpdateCallback(invalidProps);
        assertEquals(configWatcher.getSchemeFromProtocol(), "https");
        assertEquals(configWatcher.getProtocol(), "https://");
    }

    @Test
    public void testNonGetterValues() {
        Map<String, String> properties = new HashMap<String, String>();
        ConfigurationWatcherImpl configWatcher = new ConfigurationWatcherImpl();

        properties.put("custom1", "custom1Value");
        properties.put("custom2", "custom2Value");
        configWatcher.configurationUpdateCallback(properties);

        assertEquals(configWatcher.getConfigurationValue("custom1"), "custom1Value");
        assertEquals(configWatcher.getConfigurationValue("custom2"), "custom2Value");

        properties.put("custom1", null);
        properties.put("custom2", "updatedcustom2Value");

        assertNull(configWatcher.getConfigurationValue("custom1"));
        assertEquals(configWatcher.getConfigurationValue("custom2"), "updatedcustom2Value");

    }

    @Test
    public void testUpdateWithEmptyMap() {
        setNullOrEmptyMapThenTest(new HashMap<String, String>());
    }

    @Test
    public void testUpdateWithNullMap() {
        setNullOrEmptyMapThenTest(null);
    }

    private void setNullOrEmptyMapThenTest(Map<String, String> emptyProps) {
        Map<String, String> properties = getProperties();
        ConfigurationWatcherImpl configWatcher = new ConfigurationWatcherImpl();
        configWatcher.configurationUpdateCallback(properties);

        configWatcher.configurationUpdateCallback(emptyProps);

        // This will fail if new getters are added and the values are not set to null
        Method[] methods = configWatcher.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getParameterTypes().length == 0 && method.getName().startsWith("get")) {
                try {
                    assertNull(method.invoke(configWatcher, null));
                } catch (Exception e) {
                    fail(e.getMessage());
                }
            }
        }
        assertNull(configWatcher.getConfigurationValue("BlahKey"));
    }

    private Map<String, String> getProperties() {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(ConfigurationManager.CONTACT, "contactValue");
        properties.put(ConfigurationManager.HOST, "hostValue");
        properties.put(ConfigurationManager.PORT, "8888");
        properties.put(ConfigurationManager.ORGANIZATION, "orgValue");
        properties.put(ConfigurationManager.PROTOCOL, "http://");
        properties.put(ConfigurationManager.SITE_NAME, "siteNameValue");
        properties.put(ConfigurationManager.VERSION, "versionValue");
        properties.put("BlahKey", "BlahValue");
        return properties;
    }

}
