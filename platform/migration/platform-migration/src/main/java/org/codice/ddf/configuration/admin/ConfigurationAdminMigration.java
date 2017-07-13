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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Collections;

import javax.validation.constraints.NotNull;

import org.apache.commons.io.FileUtils;
import org.codice.ddf.configuration.persistence.PersistenceStrategy;
import org.codice.ddf.migration.ConfigurationMigratable;
import org.codice.ddf.migration.DescribableBean;
import org.codice.ddf.migration.MigrationMetadata;
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
public class ConfigurationAdminMigration extends DescribableBean
        implements ConfigurationMigratable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationAdminMigration.class);

    private static final String FELIX_FILEINSTALL_FILENAME = "felix.fileinstall.filename";

    private final String configurationFileExtension;

    private final ConfigurationAdmin configurationAdmin;

    private final PersistenceStrategy persistenceStrategy;

    /**
     * Constructor.
     *
     * @param configDirectoryStream reference to a {@link DirectoryStream} that will return the
     *                              configuration files to read in upon startup
     * @throws IllegalArgumentException thrown if any of the arguments is invalid
     */
    public ConfigurationAdminMigration(@NotNull DirectoryStream configDirectoryStream,
            @NotNull ConfigurationAdmin configurationAdmin,
            @NotNull PersistenceStrategy persistenceStrategy, @NotNull DescribableBean info,
            @NotNull String configurationFileExtension) {

        super(info);

        notNull(configDirectoryStream, "Config directory stream cannot be null");
        notNull(configurationAdmin, "ConfigurationAdmin cannot be null");
        notNull(persistenceStrategy, "persistenceStrategy cannot be null");
        notNull(info, "info cannot be null");
        notNull(configurationFileExtension, "ConfigFileExtension cannot be null");

        this.configurationAdmin = configurationAdmin;
        this.persistenceStrategy = persistenceStrategy;
        this.configurationFileExtension = configurationFileExtension;
    }

    @Override
    public MigrationMetadata export(@NotNull Path exportDirectory) {
        notNull(exportDirectory, "exportDirectory cannot be null");

        try {
            Path etcDirectory = createEtcDirectory(exportDirectory);
            Configuration[] configurations = configurationAdmin.listConfigurations(null);
            if (configurations != null) {
                for (Configuration configuration : configurations) {
                    Path destination = etcDirectory.resolve(getBaseFileName(configuration));
                    try (FileOutputStream fileOutputStream = new FileOutputStream(destination.toFile())) {
                        persistenceStrategy.write(fileOutputStream, configuration.getProperties());
                    }
                }
            }
        } catch (InvalidSyntaxException e) {
            throw new UnexpectedMigrationException(
                    "Unable to get configurations from Configuration Admin.",
                    e);
        } catch (IOException e) {
            String message = String.format(
                    "There was an issue retrieving configurations from ConfigurationAdmin: %s",
                    e.getMessage());
            LOGGER.info(message);
            throw new UnexpectedMigrationException(message, e);
        }

        return new MigrationMetadata(Collections.emptyList());
    }

    private Path createEtcDirectory(Path exportDirectory) throws IOException {
        Path etcDirectory = exportDirectory.resolve("etc");
        File etcDirAsFile = etcDirectory.toFile();
        FileUtils.forceMkdir(etcDirAsFile);
        LOGGER.debug("Output directory {} created", etcDirectory.toString());
        return etcDirectory;
    }

    private String getBaseFileName(Configuration configuration) {
        try {
            String fileUrl = (String) configuration.getProperties()
                    .get(FELIX_FILEINSTALL_FILENAME);
            if (fileUrl != null) {
                File file = new File(new URL(fileUrl).toURI());
                return file.getName();
            } else {
                return new StringBuilder(configuration.getPid()).append(configurationFileExtension)
                        .toString();
            }
        } catch (MalformedURLException | URISyntaxException e) {
            String message = String.format(
                    "Unable to get base file name from %s configuration property Defaulting to service pid for file name. %s",
                    FELIX_FILEINSTALL_FILENAME,
                    e.getMessage());
            throw new UnexpectedMigrationException(message, e);
        }
    }
}
