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

package org.codice.ddf.configuration.store;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Dictionary;

import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagedServiceConfigurationFile extends ConfigurationFile {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ManagedServiceConfigurationFile.class);

    public ManagedServiceConfigurationFile(Path configFilePath, Path processedDirectory,
            Path failedDirectory, Dictionary<String, Object> properties, ConfigurationAdmin configAdmin) {
        this.properties = properties;
        this.configFile = configFilePath;
        this.processedDirectory = processedDirectory;
        this.failedDirectory = failedDirectory;
        this.configAdmin = configAdmin;
    }

    @Override
    public void createConfig() {
        String servicePid = getServicePid();
        try {
            Configuration configuration = configAdmin.getConfiguration(servicePid, null);
            configuration.update(properties);
            processed();
        } catch (IOException e) {
            failed();
        }
    }

    private String getServicePid() {
        String servicePid = (String) properties.get(Constants.SERVICE_PID);
        return servicePid;
    }
}
