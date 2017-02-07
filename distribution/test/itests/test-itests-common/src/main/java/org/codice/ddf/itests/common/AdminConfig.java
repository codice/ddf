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
 **/
package org.codice.ddf.itests.common;

import java.io.IOException;
import java.util.Dictionary;

import org.apache.commons.lang.StringUtils;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminConfig {
    public static final String LOG_CONFIG_PID = "org.ops4j.pax.logging";

    public static final String LOGGER_PREFIX = "log4j.logger.";

    public static final String DEFAULT_LOG_LEVEL = "WARN";

    public static final String TEST_LOGLEVEL_PROPERTY = "itestLogLevel";

    public static final String TEST_SECURITYLOGLEVEL_PROPERTY = "securityLogLevel";

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminConfig.class);

    public static final int CONFIG_WAIT_POLLING_INTERVAL = 50;

    private final ConfigurationAdmin configAdmin;

    public AdminConfig(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    public org.codice.ddf.ui.admin.api.ConfigurationAdmin getDdfConfigAdmin() {
        return new org.codice.ddf.ui.admin.api.ConfigurationAdmin(configAdmin) {
            @Override
            public boolean isPermittedToViewService(String servicePid) {
                return true;
            }
        };
    }

    public Configuration createFactoryConfiguration(String s) throws IOException {
        return configAdmin.createFactoryConfiguration(s);
    }

    public Configuration createFactoryConfiguration(String s, String s1) throws IOException {
        return configAdmin.createFactoryConfiguration(s, s1);
    }

    public Configuration getConfiguration(String s, String s1) throws IOException {
        return configAdmin.getConfiguration(s, s1);
    }

    public Configuration getConfiguration(String s) throws IOException {
        return configAdmin.getConfiguration(s);
    }

    public Configuration[] listConfigurations(String s) throws IOException, InvalidSyntaxException {
        return configAdmin.listConfigurations(s);
    }

    public void setLogLevels() throws IOException {
        String logLevel = System.getProperty(TEST_LOGLEVEL_PROPERTY);
        String securityLogLevel = System.getProperty(TEST_SECURITYLOGLEVEL_PROPERTY);

        Configuration logConfig = configAdmin.getConfiguration(LOG_CONFIG_PID, null);
        Dictionary<String, Object> properties = logConfig.getProperties();

        properties.put(LOGGER_PREFIX + "*", DEFAULT_LOG_LEVEL);

        if (!StringUtils.isEmpty(logLevel)) {
            properties.put(LOGGER_PREFIX + "ddf", logLevel);
            properties.put(LOGGER_PREFIX + "org.codice", logLevel);
            if (StringUtils.isEmpty(securityLogLevel)) {
                properties.put(LOGGER_PREFIX + "ddf.security.expansion.impl.RegexExpansion", logLevel);
                properties.put(LOGGER_PREFIX + "ddf.security.service.impl.AbstractAuthorizingRealm", logLevel);
            }
        }

        logConfig.update(properties);
    }
}
