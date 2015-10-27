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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Dictionary;

import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

public abstract class ConfigurationFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationFile.class);

    protected Dictionary<String, Object> properties;

    protected ConfigurationAdmin configAdmin;

    protected Path configFile;

    protected Path processedDirectory;

    protected Path failedDirectory;

    protected void processed() throws IOException {
        Files.move(configFile.toFile().getCanonicalFile(), processedDirectory.toAbsolutePath()
                .resolve(configFile.getFileName()).toFile().getCanonicalFile());
    }

    protected void failed() {
        File source = null;
        File destination = null;
        try {
            source = configFile.toFile().getCanonicalFile();
            destination = failedDirectory.toAbsolutePath().resolve(configFile.getFileName())
                    .toFile().getCanonicalFile();
            Files.move(source, destination);
        } catch (IOException e) {
            LOGGER.error("Unable to move file [{}] to the failed directory [{}].", source,
                    destination);
        }
    }

    public abstract void createConfig();
}
