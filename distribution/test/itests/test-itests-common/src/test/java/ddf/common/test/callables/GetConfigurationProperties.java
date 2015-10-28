/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.common.test.callables;

import java.util.Dictionary;
import java.util.concurrent.Callable;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * {@link Callable} that retrieves the properties of a {@link Configuration} object.
 */
public class GetConfigurationProperties implements Callable<Dictionary<String, Object>> {
    private ConfigurationAdmin configAdmin;

    private String configurationPid;

    /**
     * Constructor.
     *
     * @param configAdmin      reference to the container's {@link ConfigurationAdmin}
     * @param configurationPid persistence ID of the {@link Configuration} object to retrieve the
     *                         properties for
     */
    public GetConfigurationProperties(ConfigurationAdmin configAdmin, String configurationPid) {
        this.configAdmin = configAdmin;
        this.configurationPid = configurationPid;
    }

    /**
     * Retrieves the {@link Configuration} object's properties.
     *
     * @return {@link Configuration} object's properties. Null if the {@link Configuration} object
     * does not exist or has no properties.
     * @throws Exception thrown if the call fails
     */
    @Override
    public Dictionary<String, Object> call() throws Exception {
        Configuration configuration = configAdmin.getConfiguration(configurationPid);

        if (configuration == null) {
            return null;
        }

        return configuration.getProperties();
    }
}
