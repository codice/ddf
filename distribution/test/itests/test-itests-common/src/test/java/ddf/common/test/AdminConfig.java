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

import java.io.IOException;
import java.util.Dictionary;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.common.test.config.ConfigurationPredicate;

public class AdminConfig {
    public static final String LOG_CONFIG_PID = "org.ops4j.pax.logging";

    public static final String LOGGER_PREFIX = "log4j.logger.";

    public static final String DEFAULT_LOG_LEVEL = "TRACE";

    public static final String TEST_LOGLEVEL_PROPERTY = "org.codice.test.defaultLoglevel";

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminConfig.class);

    public static final int CONFIG_WAIT_POLLING_INTERVAL = 50;

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

    /**
     * Waits until a {@link ConfigurationPredicate} returns {@code true}.
     *
     * @param pid       ID of the {@link Configuration} object that the {@link ConfigurationPredicate}
     *                  will use to validate the condition
     * @param predicate predicate to wait on
     * @param timeoutMs wait timeout
     * @return {@link Configuration} object
     * @throws IOException          thrown if the {@link Configuration} object cannot be accessed
     * @throws InterruptedException thrown if the wait times out
     * @deprecated Use {@link WaitCondition} instead.
     */
    public Configuration waitForConfiguration(String pid, ConfigurationPredicate predicate,
            long timeoutMs) throws IOException, InterruptedException {
        LOGGER.debug("Waiting for condition {} in Configuration object [{}] to be true",
                predicate.toString(),
                pid);

        Configuration configuration = configAdmin.getConfiguration(pid, null);

        int waitPeriod = 0;

        while ((waitPeriod < timeoutMs) && !predicate.test(configuration)) {
            TimeUnit.MILLISECONDS.sleep(CONFIG_WAIT_POLLING_INTERVAL);
            waitPeriod += CONFIG_WAIT_POLLING_INTERVAL;
            configuration = configAdmin.getConfiguration(pid, null);
        }

        LOGGER.debug("Waited for {}ms", waitPeriod);

        if (waitPeriod >= timeoutMs) {
            LOGGER.error(
                    "Timed out after waiting {}ms for condition {} to be true on configuration object {}",
                    waitPeriod,
                    predicate.toString(),
                    pid);
            throw new InterruptedException(String.format(
                    "Timed out waiting for condition [%s] to be true in configuration object [%s]",
                    predicate.toString(),
                    pid));
        }

        return configuration;
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
}
