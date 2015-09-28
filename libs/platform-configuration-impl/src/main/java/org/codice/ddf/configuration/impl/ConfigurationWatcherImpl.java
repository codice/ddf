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
package org.codice.ddf.configuration.impl;

import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.configuration.SystemInfo;

/**
 * Implementation of {@link org.codice.ddf.configuration.ConfigurationWatcher} that allows bundles that need to use the
 * Configuration values and easy way to do so by injecting an instance of this class into the Object
 * via Blueprint (or similar).
 * <p>
 * This allows Objects to easily use a {@link org.codice.ddf.configuration.ConfigurationWatcher} instead of be a
 * {@link org.codice.ddf.configuration.ConfigurationWatcher}
 *
 * @deprecated As of 2.8.0, Use SystemBaseUrl and SystemInfo instead
 */
public class ConfigurationWatcherImpl {

    private SystemBaseUrl systemBaseUrl;

    private SystemInfo systemInfo;

    public ConfigurationWatcherImpl() {

    }

    /**
     * Helper method to get the hostname or IP from the configuration
     *
     * @return the value associated with {@link SystemBaseUrl#HOST} property name
     */
    public String getHostname() {
        return systemBaseUrl.getHost();
    }

    /**
     * Helper method to get the port from the configuration
     *
     * @return the Integer value associated with the {@link SystemBaseUrl#HTTP_PORT} or {@link SystemBaseUrl#HTTPS_PORT} property depending on the protocol
     */
    public Integer getPort() {
        return Integer.parseInt(systemBaseUrl.getPort());
    }

    /**
     * Helper method to get the Protocol which includes the slashes (e.g. http:// or https://)
     *
     * @return the value associated with the {@link SystemBaseUrl#PROTOCOL} property name
     */
    public String getProtocol() {
        return systemBaseUrl.getProtocol();
    }

    /**
     * Helper method to get the Scheme from the Protocol which omits everything after and including
     * the first ':' (e.g. http or https)
     *
     * @return the String value before the first ':' character associated with the
     * {@link SystemBaseUrl#PROTOCOL} property name
     */
    public String getSchemeFromProtocol() {
        return systemBaseUrl.getProtocol().split(":")[0];
    }

    /**
     * Helper method to get the site name from the configuration
     *
     * @return the value associated with {@link SystemInfo#SITE_NAME} property name
     */
    public String getSiteName() {
        return systemInfo.getSiteName();
    }

    /**
     * Helper method to get the version from the configuration
     *
     * @return the value associated with {@link SystemInfo#VERSION} property name
     */
    public String getVersion() {
        return systemInfo.getVersion();
    }

    /**
     * Helper method to get the version from the configuration
     *
     * @return the value associated with {@link SystemInfo#ORGANIZATION property name
     */
    public String getOrganization() {
        return systemInfo.getOrganization();
    }

    /**
     * Helper method to get the contact info from the configuration
     *
     * @return the value associated with {@link SystemInfo#SITE_CONTACT} property name
     */
    public String getContactEmailAddress() {
        return systemInfo.getSiteContatct();
    }

    /**
     * Method to get property values from the configuration.
     *
     * @return the value associated with property name passed in, null if the property name does not
     * exist in the configuration
     * @deprecated will always return null
     */
    public String getConfigurationValue(String name) {
        return null;
    }

    public SystemBaseUrl getSystemBaseUrl() {
        return systemBaseUrl;
    }

    public void setSystemBaseUrl(SystemBaseUrl systemBaseUrl) {
        this.systemBaseUrl = systemBaseUrl;
    }

    public SystemInfo getSystemInfo() {
        return systemInfo;
    }

    public void setSystemInfo(SystemInfo systemInfo) {
        this.systemInfo = systemInfo;
    }
}
