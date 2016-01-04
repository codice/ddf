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
package org.codice.ddf.configuration.migration;

import static org.apache.commons.lang.Validate.notNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import javax.validation.constraints.NotNull;

import org.codice.ddf.configuration.admin.ConfigurationAdminMigration;
import org.codice.ddf.configuration.status.MigrationException;
import org.codice.ddf.configuration.status.MigrationWarning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link ConfigurationMigrationService} that allows migration of
 * {@link org.osgi.service.cm.Configuration} objects as well as any other configuration files
 * needed.
 */
public class ConfigurationMigrationManager implements ConfigurationMigrationService {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ConfigurationMigrationManager.class);

    private ConfigurationAdminMigration configurationAdminMigration;

    private SystemConfigurationMigration systemConfigurationMigration;

    /**
     * Constructor
     *
     * @param configurationAdminMigration  object used to export {@link org.osgi.service.cm.Configuration}
     *                                     objects from {@link org.osgi.service.cm.ConfigurationAdmin}
     * @param systemConfigurationMigration object used to export other system configuration files
     */
    public ConfigurationMigrationManager(
            @NotNull ConfigurationAdminMigration configurationAdminMigration,
            @NotNull SystemConfigurationMigration systemConfigurationMigration) {
        notNull(configurationAdminMigration, "ConfigurationAdminMigration cannot be null");
        notNull(systemConfigurationMigration, "SystemConfigurationMigration cannot be null");

        this.configurationAdminMigration = configurationAdminMigration;
        this.systemConfigurationMigration = systemConfigurationMigration;
    }

    @Override
    public Collection<MigrationWarning> export(@NotNull Path exportDirectory) throws MigrationException {
        notNull(exportDirectory, "Export directory cannot be null");
        Collection<MigrationWarning> migrationWarnings = new ArrayList<>();

        try {
            Files.createDirectories(exportDirectory);
            configurationAdminMigration.export(exportDirectory);
            migrationWarnings.addAll(systemConfigurationMigration.export(exportDirectory));
        } catch (IOException e) {
            String message = "Unable to create export directories.";
            LOGGER.error(message, e);
            throw new MigrationException(message, e);
        } catch (RuntimeException e) {
            String message = "Failure to export, internal error occurred.";
            LOGGER.error(message, e);
            throw new MigrationException(message, e);
        }
        return migrationWarnings;
    }
}
