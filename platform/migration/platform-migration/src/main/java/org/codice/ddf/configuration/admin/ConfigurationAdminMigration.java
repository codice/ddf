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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.codice.ddf.configuration.persistence.PersistenceStrategy;
import org.codice.ddf.configuration.persistence.felix.FelixCfgPersistenceStrategy;
import org.codice.ddf.configuration.persistence.felix.FelixConfigPersistenceStrategy;
import org.codice.ddf.migration.ConfigurationMigratable;
import org.codice.ddf.migration.DescribableBean;
import org.codice.ddf.migration.ExportMigrationContext;
import org.codice.ddf.migration.ImportMigrationContext;
import org.codice.ddf.migration.MigrationContext;
import org.codice.ddf.migration.MigrationEntry;
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

    static final String FELIX_FILEINSTALL_FILENAME = "felix.fileinstall.filename";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationAdminMigration.class);

    private static final Path DDF_HOME = Paths.get(System.getProperty("ddf.home"));

    private final ConfigurationAdmin configurationAdmin;

    private final PersistenceStrategy defaultStrategy;

    public ConfigurationAdminMigration(DescribableBean info, ConfigurationAdmin configurationAdmin,
            String defaultFileExtension) {
        super(info);
        notNull(info, "info cannot be null");
        notNull(configurationAdmin, "ConfigurationAdmin cannot be null");
        notNull(defaultFileExtension, "default file extension cannot be null");
        this.configurationAdmin = configurationAdmin;
        this.defaultStrategy = getPersister(defaultFileExtension);
        notNull(defaultStrategy, "unknown persistence strategy extension: " + defaultFileExtension);
    }

    @Override
    public void doExport(ExportMigrationContext context) {
        final ExportMigrationConfigurationAdminContext adminContext =
                new ExportMigrationConfigurationAdminContext(context,
                        this,
                        getConfigurations(context));

        adminContext.entries()
                .forEach(MigrationEntry::store);
    }

    @Override
    public void doImport(ImportMigrationContext context) {
        final ImportMigrationConfigurationAdminContext adminContext =
                new ImportMigrationConfigurationAdminContext(context,
                        this,
                        configurationAdmin,
                        getConfigurations(context));

        adminContext.entries()
                .forEach(MigrationEntry::store);
    }

    public PersistenceStrategy getDefaultPersister() {
        return defaultStrategy;
    }

    public PersistenceStrategy getPersister(String extension) {
        // TODO: change to dynamically lookup strategies
        if ("cfg".equals(extension)) {
            return new FelixCfgPersistenceStrategy();
        } else if ("config".equals(extension)) {
            return new FelixConfigPersistenceStrategy();
        }
        return null;
    }

    private Stream<Configuration> getConfigurations(MigrationContext context) {
        try {
            final Configuration[] configurations = configurationAdmin.listConfigurations(null);

            if (configurations != null) {
                return Stream.of(configurations);
            }
        } catch (IOException | InvalidSyntaxException e) { // InvalidSyntaxException should never happen since the filter is null
            String message = String.format(
                    "There was an issue retrieving configurations from ConfigurationAdmin: %s",
                    e.getMessage());

            LOGGER.info(message);
            context.getReport()
                    .record(new UnexpectedMigrationException(message, e));
        }
        return Stream.empty();
    }
}
