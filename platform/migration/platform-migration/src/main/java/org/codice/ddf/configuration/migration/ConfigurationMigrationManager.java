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
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationMetadata;
import org.codice.ddf.migration.MigrationWarning;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final SystemConfigurationMigration systemConfigurationMigration;

    private final MBeanServer mBeanServer;

    /**
     * Constructor.
     *
     * @param configurationAdminMigration  object used to export {@link org.osgi.service.cm.Configuration}
     *                                     objects from {@link org.osgi.service.cm.ConfigurationAdmin}
     * @param systemConfigurationMigration object used to export other system configuration files
     * @param mBeanServer                  object used to register this object as an MBean
     */
    public ConfigurationMigrationManager(
            @NotNull ConfigurationAdminMigration configurationAdminMigration,
            @NotNull SystemConfigurationMigration systemConfigurationMigration,
            @NotNull MBeanServer mBeanServer) {
        notNull(configurationAdminMigration, "ConfigurationAdminMigration cannot be null");
        notNull(systemConfigurationMigration, "SystemConfigurationMigration cannot be null");
        notNull(mBeanServer, "MBeanServer cannot be null");

        this.configurationAdminMigration = configurationAdminMigration;
        this.systemConfigurationMigration = systemConfigurationMigration;
        this.mBeanServer = mBeanServer;
    }

    public void init() throws Exception {
        ObjectName objectName = new ObjectName(OBJECT_NAME);

        try {
            mBeanServer.registerMBean(this, objectName);
        } catch (InstanceAlreadyExistsException e) {
            LOGGER.info("{} already registered as an MBean. Re-registering.", CLASS_NAME);

            mBeanServer.unregisterMBean(objectName);
            mBeanServer.registerMBean(this, objectName);

            LOGGER.info("Successfully re-registered {} as an MBean.", CLASS_NAME);
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
            migrationWarnings.addAll(systemConfigurationMigration.export(exportDirectory));
            migrationWarnings.addAll(exportMigratables(exportDirectory));
        } catch (IOException e) {
            LOGGER.error("Unable to create export directories", e);
            throw new MigrationException(String.format(
                    "Failed to export configurations: Unable to create export directories. %s",
                    e.getMessage()), e);
        } catch (MigrationException e) {
            LOGGER.error("Export operation failed", e);
            throw e;
        } catch (RuntimeException e) {
            LOGGER.error("Failure to export, internal error occurred", e);
            throw new MigrationException("Failed to export configurations: Internal error", e);
        }

        return migrationWarnings;
    }

    @Override
    public Collection<MigrationWarning> export(@NotNull String exportDirectory)
            throws MigrationException {
        notNull(exportDirectory, "Export directory cannot be null");

        return export(Paths.get(exportDirectory));
    }

    BundleContext getBundleContext() {
        return FrameworkUtil.getBundle(ConfigurationMigrationManager.class)
                .getBundleContext();
    }

    private Collection<MigrationWarning> exportMigratables(Path exportDirectory) {
        List<MigrationWarning> warnings = new LinkedList<>();

        for (ServiceReference<Migratable> serviceReference : getMigratableServiceReferences()) {

            Migratable migratable = getBundleContext().getService(serviceReference);
            MigrationMetadata migrationMetadata = migratable.export(exportDirectory);
            warnings.addAll(migrationMetadata.getMigrationWarnings());
        }

        return warnings;
    }

    private Collection<ServiceReference<Migratable>> getMigratableServiceReferences() {
        try {
            return getBundleContext().getServiceReferences(Migratable.class, null);
        } catch (InvalidSyntaxException e) {
            LOGGER.error(
                    "Export failed: could not get list of Migratable service references from bundle context",
                    e);
            throw new MigrationException("Failed to export configurations: Internal error", e);
        }
    }
}
