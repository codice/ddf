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
package org.codice.ddf.configuration.admin;

import static org.apache.commons.lang.Validate.notNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;

import javax.validation.constraints.NotNull;

import org.apache.commons.io.FileUtils;
import org.codice.ddf.configuration.status.ConfigurationFileException;
import org.codice.ddf.migration.ExportMigrationException;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.UnexpectedMigrationException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to migrate {@link ConfigurationAdmin} configurations. This includes importing
 * configuration files from a configuration directory and creating {@link Configuration} objects
 * for those and exporting {@link Configuration} objects to configuration files.
 */
public class ConfigurationAdminMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationAdminMigration.class);

    private static final String FILTER =
            "(&(!(service.pid=jmx*))(!(service.pid=org.apache*))(!(service.pid=org.ops4j*)))";

    private final String configurationFileExtension;

    private final ConfigurationFileFactory configurationFileFactory;

    private final ConfigurationAdmin configurationAdmin;

    /**
     * Constructor.
     *
     * @param configDirectoryStream    reference to a {@link DirectoryStream} that will return the
     *                                 configuration files to read in upon startup
     * @param configurationFileFactory factory object used to create {@link ConfigurationFile}
     *                                 instances based on their type
     * @throws IllegalArgumentException thrown if any of the arguments is invalid
     */
    public ConfigurationAdminMigration(@NotNull DirectoryStream configDirectoryStream, @NotNull ConfigurationFileFactory configurationFileFactory,
            @NotNull ConfigurationAdmin configurationAdmin,
            @NotNull String configurationFileExtension) {

        notNull(configDirectoryStream, "Config directory stream cannot be null");
        notNull(configurationFileFactory, "Configuration file factory cannot be null");
        notNull(configurationAdmin, "ConfigurationAdmin cannot be null");
        notNull(configurationFileExtension, "ConfigFileExtension cannot be null");

        this.configurationFileFactory = configurationFileFactory;
        this.configurationAdmin = configurationAdmin;
        this.configurationFileExtension = configurationFileExtension;
    }

    public void export(@NotNull Path exportDirectory) throws MigrationException, IOException {
        notNull(exportDirectory, "exportDirectory cannot be null");
        Path etcDirectory = createEtcDirectory(exportDirectory);

        try {
            Configuration[] configurations = configurationAdmin.listConfigurations(FILTER);
            if (configurations != null) {
                for (Configuration configuration : configurations) {
                    Path exportedFilePath = etcDirectory.resolve(
                            configuration.getPid() + configurationFileExtension);
                    try {
                        configurationFileFactory.createConfigurationFile(configuration.getProperties())
                                .exportConfig(exportedFilePath.toString());
                    } catch (ConfigurationFileException e) {
                        LOGGER.info("Could not create configuration file {} for configuration {}.",
                                exportedFilePath,
                                configuration.getPid());
                        throw new ExportMigrationException(e);
                    } catch (IOException e) {
                        LOGGER.info("Could not export configuration {} to {}.",
                                configuration.getPid(),
                                exportedFilePath);
                        throw new ExportMigrationException(e);
                    }
                }
            }
        } catch (InvalidSyntaxException e) {
            LOGGER.info("Invalid filter string {}", FILTER, e);
            throw new UnexpectedMigrationException("Export failed", e);
        } catch (IOException e) {
            LOGGER.info("There was an issue retrieving configurations from ConfigurationAdmin: {}",
                    e.getMessage());
            throw new UnexpectedMigrationException("Export failed", e);
        }
    }

    private Path createEtcDirectory(Path exportDirectory) throws IOException {
        Path etcDirectory = exportDirectory.resolve("etc");
        File etcDirAsFile = etcDirectory.toFile();
        FileUtils.forceMkdir(etcDirAsFile);
        LOGGER.debug("Output directory {} created", etcDirectory.toString());
        return etcDirectory;
    }

}
