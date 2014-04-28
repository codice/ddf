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

import java.util.Collections;
import java.util.Map;

import org.codice.ddf.configuration.ConfigurationManager;
import org.codice.ddf.configuration.ConfigurationWatcher;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

/**
 * Implementation of {@link ConfigurationWatcher} that allows bundles that need to use the
 * Configuration values and easy way to do so by injecting an instance of this class into the Object
 * via Blueprint (or similar).
 * 
 * This allows Objects to easily use a {@link ConfigurationWatcher} instead of be a
 * {@link ConfigurationWatcher}
 * 
 */
public class ConfigurationWatcherImpl implements ConfigurationWatcher {

    private static final XLogger logger = new XLogger(
            LoggerFactory.getLogger(ConfigurationWatcherImpl.class));

    private Map<String, String> propertyMap = null;

    // Pull out variables for the more common properties and properties that have to be massaged
    // before they are returned
    private String hostname = null;

    private Integer port = null;

    private String protocol = null;

    private String schemeFromProtocol = null;

    private String siteName = null;

    private String version = null;

    private String organization = null;

    private String contactInfo = null;

    @SuppressWarnings("unchecked")
    public ConfigurationWatcherImpl() {
        propertyMap = Collections.EMPTY_MAP;
    }

    /**
     * Helper method to get the hostname or IP from the configuration
     * 
     * @return the value associated with {@link ConfigurationManager#HOST} property name
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Helper method to get the port from the configuration
     * 
     * @return the Integer value associated with the {@link ConfigurationManager#PORT} property name
     *         if it is an Integer, otherwise null
     */
    public Integer getPort() {
        return port;
    }

    /**
     * Helper method to get the Protocol which includes the slashes (e.g. http:// or https://)
     * 
     * @return the value associated with the {@link ConfigurationManager#PROTOCOL} property name
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Helper method to get the Scheme from the Protocol which omits everything after and including
     * the first ':' (e.g. http or https)
     * 
     * @return the String value before the first ':' character associated with the
     *         {@link ConfigurationManager#PROTOCOL} property name
     */
    public String getSchemeFromProtocol() {
        return schemeFromProtocol;
    }

    /**
     * Helper method to get the site name from the configuration
     * 
     * @return the value associated with {@link ConfigurationManager#SITE_NAME} property name
     */
    public String getSiteName() {
        return siteName;
    }

    /**
     * Helper method to get the version from the configuration
     * 
     * @return the value associated with {@link ConfigurationManager#VERSION} property name
     */
    public String getVersion() {
        return version;
    }

    /**
     * Helper method to get the version from the configuration
     * 
     * @return the value associated with {@link ConfigurationManager#ORGANIZATION property name
     */
    public String getOrganization() {
        return organization;
    }

    /**
     * Helper method to get the contact info from the configuration
     * 
     * @return the value associated with {@link ConfigurationManager#CONTACT} property name
     */
    public String getContactEmailAddress() {
        return contactInfo;
    }

    /**
     * Method to get property values from the configuration. Refer to the Constants in the
     * {@link ConfigurationManager} class for a list of property names
     * 
     * @return the value associated with property name passed in, null if the property name does not
     *         exist in the configuration
     */
    public String getConfigurationValue(String name) {
        return propertyMap.get(name);
    }

    @Override
    public void configurationUpdateCallback(Map<String, String> properties) {
        logger.entry("configurationUpdateCallback");

        if (properties != null && !properties.isEmpty()) {
            propertyMap = properties;

            logger.debug("Configuration values: {}", properties);

            String oldValue = hostname;
            hostname = properties.get(ConfigurationManager.HOST);
            logConfigurationValue(ConfigurationManager.HOST, oldValue, hostname);

            String portString = properties.get(ConfigurationManager.PORT);
            try {
                if (portString != null && !portString.isEmpty()) {
                    port = Integer.parseInt(portString);
                } else {
                    port = null;
                }
                logConfigurationValue(ConfigurationManager.PORT, oldValue, port);
            } catch (NumberFormatException e) {
                logger.warn(
                        "Error Updating Configuration value for '{}', not a valid Integer [{}] reverting back to old value [{}]",
                        new Object[] {ConfigurationManager.PORT, portString, port});
            }

            oldValue = protocol;
            protocol = properties.get(ConfigurationManager.PROTOCOL);
            logConfigurationValue(ConfigurationManager.PROTOCOL, oldValue, protocol);
            if (protocol != null) {
                int index = protocol.indexOf(':');
                schemeFromProtocol = index > -1 ? protocol.substring(0, index) : protocol;
            } else {
                schemeFromProtocol = null;
            }

            oldValue = siteName;
            siteName = properties.get(ConfigurationManager.SITE_NAME);
            logConfigurationValue(ConfigurationManager.SITE_NAME, oldValue, siteName);

            oldValue = version;
            version = properties.get(ConfigurationManager.VERSION);
            logConfigurationValue(ConfigurationManager.VERSION, oldValue, version);

            oldValue = organization;
            organization = properties.get(ConfigurationManager.ORGANIZATION);
            logConfigurationValue(ConfigurationManager.ORGANIZATION, oldValue, organization);

            oldValue = contactInfo;
            contactInfo = properties.get(ConfigurationManager.CONTACT);
            logConfigurationValue(ConfigurationManager.CONTACT, oldValue, contactInfo);

        } else {
            propertyMap.clear();
            hostname = null;
            port = null;
            protocol = null;
            schemeFromProtocol = null;
            siteName = null;
            version = null;
            organization = null;
            contactInfo = null;
            logger.debug("Platform Configuration Properties are NULL or empty, setting all values to null");
        }
        logger.exit();
    }

    protected void logConfigurationValue(String propertyName, Object oldValue, Object newValue) {
        logger.debug("Updating Configuration value '{}' oldValue = [{}], newValue = [{}]",
                new Object[] {propertyName, oldValue, newValue});
    }

}
