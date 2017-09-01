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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.codice.ddf.configuration.persistence.PersistenceStrategy;
import org.codice.ddf.migration.ImportMigrationContext;
import org.codice.ddf.migration.ImportMigrationContextProxy;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationReport;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class extends on the {@link ImportMigrationContext} interface to pre-process exported entries
 * for configuration objects and compare them with the configuration objects currently in memory.
 */
public class ImportMigrationConfigurationAdminContext extends ImportMigrationContextProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            ImportMigrationConfigurationAdminContext.class);

    private final ConfigurationAdminMigratable admin;

    private final ConfigurationAdmin configurationAdmin;

    /**
     * Keeps track of all managed services in memory that were not found in the export file so
     * we know what to delete at the end.
     */
    private final Map<String, Configuration> memoryServices;

    /**
     * Keeps track of all managed service factories in memory that were not found in the export file
     * so we know what to delete at the end.
     */
    private final Map<String, List<Configuration>> memoryFactoryServices;

    private final Map<String, ImportMigrationConfigurationAdminEntry> exportedServices;

    private final Map<String, List<ImportMigrationConfigurationAdminEntry>> exportedFactoryServices;

    private boolean isValid = true; // until proven otherwise

    public ImportMigrationConfigurationAdminContext(ImportMigrationContext context,
            ConfigurationAdminMigratable admin, ConfigurationAdmin configurationAdmin,
            Configuration[] memoryConfigs) {
        super(context);
        Validate.notNull(admin, "invalid null configuration admin migratable");
        Validate.notNull(configurationAdmin, "invalid null configuration admin");
        Validate.notNull(memoryConfigs, "invalid null configurations");
        this.admin = admin;
        this.configurationAdmin = configurationAdmin;
        // categorize memory configurations
        this.memoryServices = Stream.of(memoryConfigs)
                .filter(ConfigurationAdminMigratable::isManagedService)
                .collect(Collectors.toMap(Configuration::getPid, Function.identity()));
        this.memoryFactoryServices = Stream.of(memoryConfigs)
                .filter(ConfigurationAdminMigratable::isManagedServiceFactory)
                .collect(Collectors.groupingBy(Configuration::getFactoryPid));
        // categorize exported configurations
        final ImportMigrationConfigurationAdminEntry[] entries = context.entries()
                .map(this::proxy)
                .filter(Objects::nonNull)
                .toArray(ImportMigrationConfigurationAdminEntry[]::new);

        this.exportedServices = Stream.of(entries)
                .filter(ImportMigrationConfigurationAdminEntry::isManagedService)
                .collect(Collectors.toMap(ImportMigrationConfigurationAdminEntry::getPid,
                        Function.identity()));
        this.exportedFactoryServices = Stream.of(entries)
                .filter(ImportMigrationConfigurationAdminEntry::isManagedServiceFactory)
                .collect(Collectors.groupingBy(ImportMigrationConfigurationAdminEntry::getFactoryPid));
        context.getReport()
                .doAfterCompletion(this::deleteUnexportedConfigurationsAfterCompletion);
    }

    @Override
    public Stream<ImportMigrationEntry> entries() {
        if (!isValid) {
            return Stream.empty();
        }
        return Stream.concat(exportedServices.values()
                        .stream(),
                exportedFactoryServices.values()
                        .stream()
                        .flatMap(List::stream));
    }

    @Override
    public ImportMigrationEntry getEntry(Path path) {
        throw new UnsupportedOperationException("should not be called");
    }

    @Override
    public Stream<ImportMigrationEntry> entries(Path path) {
        throw new UnsupportedOperationException("should not be called");
    }

    @Override
    public Stream<ImportMigrationEntry> entries(Path path, PathMatcher filter) {
        throw new UnsupportedOperationException("should not be called");
    }

    // PMD.DefaultPackage - designed to be called only from ImportMigrationConfigurationAdminEntry within this package
    @SuppressWarnings("PMD.DefaultPackage")
    List<Configuration> getMemoryFactoryService(String factoryPid) {
        return memoryFactoryServices.get(factoryPid);
    }

    // PMD.DefaultPackage - designed to be called only from ImportMigrationConfigurationAdminEntry within this package
    @SuppressWarnings("PMD.DefaultPackage")
    void removeMemoryFactoryService(String factoryPid) {
        memoryFactoryServices.remove(factoryPid);
    }

    // PMD.DefaultPackage - designed to be called only from ImportMigrationConfigurationAdminEntry within this package
    @SuppressWarnings("PMD.DefaultPackage")
    Configuration removeMemoryService(String pid) {
        return memoryServices.remove(pid);
    }

    // PMD.DefaultPackage - designed to be called only from ImportMigrationConfigurationAdminEntry within this package
    @SuppressWarnings("PMD.DefaultPackage")
    Configuration createConfiguration(ImportMigrationConfigurationAdminEntry entry)
            throws IOException {
        // Question: should we use the bundle location that was exported???
        // If we do, should we perform additional checks to make sure we're not loading a malicious bundle?
        // This might be unnecessary if we are comfortable with the encryption of the zip file as our only countermeasure.
        if (entry.isManagedServiceFactory()) {
            return configurationAdmin.createFactoryConfiguration(entry.getFactoryPid(), null);
        }
        return configurationAdmin.getConfiguration(entry.getPid());
    }

    private ImportMigrationConfigurationAdminEntry proxy(ImportMigrationEntry entry) {
        final Path path = entry.getPath();
        final String extn = FilenameUtils.getExtension(path.toString());
        final PersistenceStrategy ps = admin.getPersister(extn);

        if (ps == null) {
            getReport().record(new MigrationException(
                    "Import error: persistence strategy [%s] for configuration [%s] is not defined.",
                    extn,
                    path));
        } else {
            final Dictionary<String, Object> properties;
            InputStream is = null;

            try {
                is = entry.getInputStream()
                        .orElseThrow(() -> new MigrationException(
                                "Import error: failed to read configuration [%s]; not exported.",
                                path));
                try {
                    properties = ps.read(is);
                } catch (IOException e) {
                    throw new MigrationException(
                            "Import error: failed to read configuration [%s] using persistent strategy [%s]; %s.",
                            path,
                            ps.getExtension(),
                            e);
                }
            } catch (IOException e) {
                throw new MigrationException("Import error: failed to read configuration [%s]; %s.",
                        path,
                        e);
            } finally {
                IOUtils.closeQuietly(is);
            }
            try {
                return new ImportMigrationConfigurationAdminEntry(this, entry, properties);
            } catch (MigrationException e) {
                // don't throw it back yet as we want to detect as many as possible so just record it
                getReport().record(e);
            }
        }
        this.isValid = false;
        return null;
    }

    // PMD.UnusedFormalParameter - report parameter is require as this method is used as a functional interface
    @SuppressWarnings("PMD.UnusedFormalParameter")
    private void deleteUnexportedConfigurationsAfterCompletion(MigrationReport report) {
        if (isValid) {
            Stream.concat(memoryServices.values()
                            .stream(),
                    memoryFactoryServices.values()
                            .stream()
                            .flatMap(List::stream))
                    .forEach(this::delete);
        }
    }

    private void delete(Configuration configuration) {
        try {
            LOGGER.debug("Importing configuration for [{}]; deleting existing configuration...",
                    configuration.getPid());
            configuration.delete();
        } catch (IOException e) {
            getReport().record(new MigrationException(
                    "Import error: failed to delete configuration [%s]; %s.",
                    configuration.getPid(),
                    e));
        }
    }
}
