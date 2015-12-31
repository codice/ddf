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
package org.codice.ddf.ui.admin.api.impl;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.apache.felix.utils.properties.Properties;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.configuration.SystemInfo;
import org.codice.ddf.ui.admin.api.SystemPropertiesAdminMBean;
import org.codice.ddf.ui.admin.api.SystemPropertyDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemPropertiesAdmin implements SystemPropertiesAdminMBean {

    private MBeanServer mbeanServer;

    private ObjectName objectName;

    private SystemBaseUrl baseUrl = new SystemBaseUrl();

    private String oldHostName = baseUrl.getHost();

    private static final String KARAF_ETC = "karaf.etc";

    private static final String SYSTEM_PROPERTIES_FILE = "system.properties";

    private static final String USERS_PROPERTIES_FILE = "users.properties";

    private static final String HOST_TITLE = "Host";

    private static final String HOST_DESCRIPTION =
            "The host name or IP address used to advertise the system. Possibilities include the address of a single node of that of a load balancer in a multi-node deployment. NOTE: Does not change the address the system runs on.";

    private static final String PROTOCOL_TITLE = "Default Protocol";

    private static final String PROTOCOL_DESCRIPTION =
            "The protocol used to advertise the system. When selecting the protocol, be sure to enter the port number corresponding to that protocol.";

    private static final ArrayList<String> PROTOCOL_OPTIONS = new ArrayList<>();

    static {
        PROTOCOL_OPTIONS.add("https://");
        PROTOCOL_OPTIONS.add("http://");
    }

    private static final String DEFAULT_PORT_TITLE = "Default Port";

    private static final String DEFAULT_PORT_DESCRIPTION =
            "The default port used to advertise the system. Possibilities include the port of a single node of that of a load balancer in a multi-node deployment. NOTE: Does not change the port the system runs on.";

    private static final String HTTP_PORT_TITLE = "HTTP Port";

    private static final String HTTP_PORT_DESCRIPTION =
            "The port used to advertise the system. Possibilities include the port of a single node of that of a load balancer in a multi-node deployment. NOTE: Does not change the port the system runs on.";

    private static final String HTTPS_PORT_TITLE = "HTTPS Port";

    private static final String HTTPS_PORT_DESCRIPTION =
            "The secure port used to advertise the system. Possibilities include the port of a single node of that of a load balancer in a multi-node deployment. NOTE: Does not change the port the system runs on.";

    private static final String ORGANIZATION_TITLE = "Organization";

    private static final String ORGANIZATION_DESCRIPTION =
            "The name of the organization that runs this instance.";

    private static final String SITE_CONTACT_TITLE = "Site Contact";

    private static final String SITE_CONTACT_DESCRIPTION = "The email address of the site contact.";

    private static final String SITE_NAME_TITLE = "Site Name";

    private static final String SITE_NAME_DESCRIPTION =
            "The unique name of this instance. This name will be provided via web services that ask for the name.";

    private static final String VERSION_TITLE = "Version";

    private static final String VERSION_DESCRIPTION = "The version of this instance.";

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemPropertiesAdmin.class);

    public SystemPropertiesAdmin() {
        configureMBean();
    }

    @Override
    public List<SystemPropertyDetails> readSystemProperties() {
        LOGGER.info("get system properties");

        ArrayList<SystemPropertyDetails> properties = new ArrayList<>();
        properties.add(getSystemPropertyDetails(SystemBaseUrl.PROTOCOL,
                PROTOCOL_TITLE,
                PROTOCOL_DESCRIPTION,
                PROTOCOL_OPTIONS));
        properties.add(getSystemPropertyDetails(SystemBaseUrl.HOST,
                HOST_TITLE,
                HOST_DESCRIPTION,
                null));
        properties.add(getSystemPropertyDetails(SystemBaseUrl.PORT,
                DEFAULT_PORT_TITLE,
                DEFAULT_PORT_DESCRIPTION,
                null));
        properties.add(getSystemPropertyDetails(SystemBaseUrl.HTTP_PORT,
                HTTP_PORT_TITLE,
                HTTP_PORT_DESCRIPTION,
                null));
        properties.add(getSystemPropertyDetails(SystemBaseUrl.HTTPS_PORT,
                HTTPS_PORT_TITLE,
                HTTPS_PORT_DESCRIPTION,
                null));
        properties.add(getSystemPropertyDetails(SystemInfo.ORGANIZATION,
                ORGANIZATION_TITLE,
                ORGANIZATION_DESCRIPTION,
                null));
        properties.add(getSystemPropertyDetails(SystemInfo.SITE_CONTACT,
                SITE_CONTACT_TITLE,
                SITE_CONTACT_DESCRIPTION,
                null));
        properties.add(getSystemPropertyDetails(SystemInfo.SITE_NAME,
                SITE_NAME_TITLE,
                SITE_NAME_DESCRIPTION,
                null));
        properties.add(getSystemPropertyDetails(SystemInfo.VERSION,
                VERSION_TITLE,
                VERSION_DESCRIPTION,
                null));

        return properties;
    }

    @Override
    public void writeSystemProperties(Map<String, String> updatedSystemProperties) {
        if (updatedSystemProperties == null) {
            return;
        }
        // Get system.properties file
        //save off the current/old hostname before we make any changes
        oldHostName = baseUrl.getHost();

        String etcDir = System.getProperty(KARAF_ETC);
        String systemPropertyFilename = etcDir + File.separator + SYSTEM_PROPERTIES_FILE;
        String userPropertiesFilename = etcDir + File.separator + USERS_PROPERTIES_FILE;

        File systemPropertiesFile = new File(systemPropertyFilename);
        File userPropertiesFile = new File(userPropertiesFilename);

        try {
            Properties systemDotProperties = new Properties(systemPropertiesFile);

            updateProperty(SystemBaseUrl.PORT, updatedSystemProperties, systemDotProperties);
            updateProperty(SystemBaseUrl.HOST, updatedSystemProperties, systemDotProperties);
            updateProperty(SystemBaseUrl.PROTOCOL, updatedSystemProperties, systemDotProperties);
            updateProperty(SystemBaseUrl.HTTP_PORT, updatedSystemProperties, systemDotProperties);
            updateProperty(SystemBaseUrl.HTTPS_PORT, updatedSystemProperties, systemDotProperties);
            updateProperty(SystemInfo.ORGANIZATION, updatedSystemProperties, systemDotProperties);
            updateProperty(SystemInfo.SITE_CONTACT, updatedSystemProperties, systemDotProperties);
            updateProperty(SystemInfo.SITE_NAME, updatedSystemProperties, systemDotProperties);
            updateProperty(SystemInfo.VERSION, updatedSystemProperties, systemDotProperties);

            systemDotProperties.save();

        } catch (IOException e) {
            LOGGER.warn("Exception while writing to system.properties file.", e);
        }

        try {
            Properties userDotProperties = new Properties(userPropertiesFile);

            if (!userDotProperties.isEmpty()) {
                String oldHostValue = userDotProperties.getProperty(oldHostName);

                if (oldHostValue != null) {
                    userDotProperties.remove(oldHostName);
                    userDotProperties.setProperty(System.getProperty(SystemBaseUrl.HOST),
                            oldHostValue);

                    userDotProperties.save();
                }

            }

        } catch (IOException e) {
            LOGGER.warn("Exception while writing to users.properties file.", e);

        }

    }

    private SystemPropertyDetails getSystemPropertyDetails(String key, String title,
            String description, List<String> options) {
        String property = System.getProperty(key, "");
        return new SystemPropertyDetails(title, description, options, key, property);
    }

    private void updateProperty(String key, Map<String, String> updatedProperties,
            Properties systemDotProperties) {
        if (updatedProperties.containsKey(key)) {
            String value = updatedProperties.get(key);
            systemDotProperties.put(key, value);
            System.setProperty(key, value);
        }
    }

    private void configureMBean() {
        mbeanServer = ManagementFactory.getPlatformMBeanServer();

        try {
            objectName = new ObjectName(SystemPropertiesAdminMBean.OBJECT_NAME);
        } catch (MalformedObjectNameException e) {
            LOGGER.warn("Exception while creating object name: "
                    + SystemPropertiesAdminMBean.OBJECT_NAME, e);
        }

        try {
            try {
                mbeanServer.registerMBean(new StandardMBean(this, SystemPropertiesAdminMBean.class),
                        objectName);
            } catch (InstanceAlreadyExistsException e) {
                mbeanServer.unregisterMBean(objectName);
                mbeanServer.registerMBean(new StandardMBean(this, SystemPropertiesAdminMBean.class),
                        objectName);
            }
        } catch (Exception e) {
            LOGGER.error("Could not register mbean.", e);
        }
    }

    public void shutdown() {
        try {
            if (objectName != null && mbeanServer != null) {
                mbeanServer.unregisterMBean(objectName);
            }
        } catch (Exception e) {
            LOGGER.warn("Exception unregistering mbean: ", e);
            throw new RuntimeException(e);
        }
    }
}
