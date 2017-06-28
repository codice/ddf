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
package org.codice.ddf.configuration.migration;

import static org.apache.commons.lang.Validate.notNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.validation.constraints.NotNull;

import org.codice.ddf.configuration.admin.ConfigurationAdminMigration;
import org.codice.ddf.migration.ConfigurationMigratable;
import org.codice.ddf.migration.DataMigratable;
import org.codice.ddf.migration.ExportMigrationException;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationMetadata;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.migration.UnexpectedMigrationException;
import org.codice.ddf.platform.services.common.Describable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

/**
 * Implementation of the {@link ConfigurationMigrationService} that allows migration of
 * {@link org.osgi.service.cm.Configuration} objects as well as any other configuration files
 * needed.
 */
public class ConfigurationMigrationManager
        implements ConfigurationMigrationService, ConfigurationMigrationManagerMBean {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ConfigurationMigrationManager.class);

    private static final String CLASS_NAME = ConfigurationMigrationManager.class.getName();

    private static final String OBJECT_NAME = CLASS_NAME + ":service=configuration-migration";

    private final ConfigurationAdminMigration configurationAdminMigration;

    private final MBeanServer mBeanServer;

    private final List<ConfigurationMigratable> configurationMigratables;

    private final List<DataMigratable> dataMigratables;

    /**
     * Constructor.
     *
     * @param configurationAdminMigration object used to export {@link org.osgi.service.cm.Configuration}
     *                                    objects from {@link org.osgi.service.cm.ConfigurationAdmin}
     * @param mBeanServer                 object used to register this object as an MBean
     * @param configurationMigratables    list of {@link ConfigurationMigratable} services. Needs
     *                                    to be kept up-to-date by the client of this class.
     * @param dataMigratables             list of {@link DataMigratable} services. Needs
     *                                    to be kept up-to-date by the client of this class.
     */
    public ConfigurationMigrationManager(
            @NotNull ConfigurationAdminMigration configurationAdminMigration,
            @NotNull MBeanServer mBeanServer,
            @NotNull List<ConfigurationMigratable> configurationMigratables,
            @NotNull List<DataMigratable> dataMigratables) {
        notNull(configurationAdminMigration, "ConfigurationAdminMigration cannot be null");
        notNull(mBeanServer, "MBeanServer cannot be null");
        notNull(configurationMigratables,
                "List of ConfigurationMigratable services cannot be null");
        notNull(dataMigratables, "List of DataMigratable services cannot be null");

        this.configurationAdminMigration = configurationAdminMigration;
        this.mBeanServer = mBeanServer;
        this.configurationMigratables = configurationMigratables;
        this.dataMigratables = dataMigratables;
    }

    public void init() throws Exception {
        ObjectName objectName = new ObjectName(OBJECT_NAME);

        try {
            mBeanServer.registerMBean(this, objectName);
        } catch (InstanceAlreadyExistsException e) {
            LOGGER.debug("{} already registered as an MBean. Re-registering.", CLASS_NAME);

            mBeanServer.unregisterMBean(objectName);
            mBeanServer.registerMBean(this, objectName);

            LOGGER.debug("Successfully re-registered {} as an MBean.", CLASS_NAME);
        }
    }

    @Override
    public Collection<MigrationWarning> export(@NotNull Path exportDirectory)
            throws MigrationException {
        notNull(exportDirectory, "Export directory cannot be null");
        Collection<MigrationWarning> migrationWarnings = new ArrayList<>();

        try {
            Files.createDirectories(exportDirectory);
            configurationAdminMigration.export(exportDirectory);
            migrationWarnings.addAll(exportMigratables(exportDirectory));
        } catch (IOException e) {
            LOGGER.info("Unable to create export directories", e);
            throw new ExportMigrationException("Unable to create export directories", e);
        } catch (MigrationException e) {
            LOGGER.info("Export operation failed", e);
            throw e;
        } catch (RuntimeException e) {
            LOGGER.info("Failure to export, internal error occurred", e);
            throw new UnexpectedMigrationException("Export failed", e);
        }

        return migrationWarnings;
    }

    public Collection<MigrationWarning> export(@NotNull String exportDirectory)
            throws MigrationException {
        notNull(exportDirectory, "Export directory cannot be null");

        return export(Paths.get(exportDirectory));
    }

    @Override
    public Collection<Describable> getOptionalMigratableInfo() {
        return ImmutableList.copyOf(dataMigratables);
    }

    private Collection<MigrationWarning> exportMigratable(Migratable migratable,
            Path exportDirectory) {
        Stopwatch stopwatch = null;

        if (LOGGER.isDebugEnabled()) {
            stopwatch = Stopwatch.createStarted();
        }

        MigrationMetadata migrationMetadata = migratable.export(exportDirectory);

        if (LOGGER.isDebugEnabled() && stopwatch != null) {
            LOGGER.debug("Export time: {}",
                    stopwatch.stop()
                            .toString());
        }

        return migrationMetadata.getMigrationWarnings();
    }

    private Collection<MigrationWarning> exportMigratables(Path exportDirectory)
            throws IOException {
        List<MigrationWarning> warnings = new LinkedList<>();

        for (ConfigurationMigratable configMigratable : configurationMigratables) {
            warnings.addAll(exportMigratable(configMigratable, exportDirectory));

        }

        for (DataMigratable dataMigratable : dataMigratables) {

            Path dataMigratableDirectory = exportDirectory.resolve(dataMigratable.getId());
            Files.createDirectories(dataMigratableDirectory);

            warnings.addAll(exportMigratable(dataMigratable, exportDirectory));

        }

        return warnings;
    }
}