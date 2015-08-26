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
package ddf.common.test;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class AdminConfig {
    public static final String LOG_CONFIG_PID = "org.ops4j.pax.logging";

    public static final String LOGGER_PREFIX = "log4j.logger.";

    public static final String DEFAULT_LOG_LEVEL = "TRACE";

    public static final String TEST_LOGLEVEL_PROPERTY = "org.codice.test.defaultLoglevel";

    private static final long CONFIG_UPDATE_MAX_WAIT_MILLIS = TimeUnit.MINUTES.toMillis(1);

    private static final int LOOP_SLEEP_MILLIS = 5;

    private final ConfigurationAdmin configAdmin;

    public AdminConfig(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    public org.codice.ddf.ui.admin.api.ConfigurationAdmin getDdfConfigAdmin() {
        return new org.codice.ddf.ui.admin.api.ConfigurationAdmin(configAdmin);
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

        Configuration logConfig = configAdmin.getConfiguration(LOG_CONFIG_PID, null);
        Dictionary<String, Object> properties = logConfig.getProperties();
        if (StringUtils.isEmpty(logLevel)) {
            properties.put(LOGGER_PREFIX + "ddf", DEFAULT_LOG_LEVEL);
            properties.put(LOGGER_PREFIX + "org.codice", DEFAULT_LOG_LEVEL);
        } else {
            properties.put(LOGGER_PREFIX + "*", logLevel);
        }

        logConfig.update(properties);
    }

    public void updateConfig(ConfigWaitable configWaitable)
            throws IOException, InterruptedException {
        getDdfConfigAdmin().update(configWaitable.getPid(), configWaitable.getConfigProps());

        long timeoutLimit = System.currentTimeMillis() + CONFIG_UPDATE_MAX_WAIT_MILLIS;
        while (true) {
            if (configWaitable.isConfigured()) {
                break;
            } else {
                if (System.currentTimeMillis() > timeoutLimit) {
                    fail(String.format("Timed out waiting for configuration change for %s",
                            configWaitable.getPid()));
                } else {
                    Thread.sleep(LOOP_SLEEP_MILLIS);
                }
            }
        }
    }

    public interface ConfigWaitable {
        String getPid();

        String getLocation();

        Map<String, Object> getConfigProps();

        boolean isConfigured();
    }
}
