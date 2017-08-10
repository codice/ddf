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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.Validate;
import org.codice.ddf.configuration.persistence.PersistenceStrategy;
import org.codice.ddf.migration.ImportMigrationContext;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.ImportMigrationException;
import org.codice.ddf.migration.ImportPathMigrationException;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.ProxyImportMigrationContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportMigrationConfigurationAdminContext extends ProxyImportMigrationContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            ImportMigrationConfigurationAdminContext.class);

    private final ConfigurationAdminMigratable admin;

    private final ConfigurationAdmin configurationAdmin;

    /**
     * Keeps tracked of all managed services in memory that were not found in the export file so
     * we know what to delete at the end.
     */
    private final Map<String, Configuration> memoryServices;

    /**
     * Keeps tracked of all managed service factories in memory that were not found in the export file
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
                .filter(ImportMigrationConfigurationAdminContext::isManagedService)
                .collect(Collectors.toMap(Configuration::getPid, Function.identity()));
        this.memoryFactoryServices = Stream.of(memoryConfigs)
                .filter(ImportMigrationConfigurationAdminContext::isManagedServiceFactory)
                .collect(Collectors.groupingBy(Configuration::getFactoryPid));
        // categorize exported configurations
        final ImportMigrationConfigurationAdminEntry[] entries = context.entries()
                .map(this::proxy)
                .filter(e -> e != null)
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

    private static boolean isManagedServiceFactory(Configuration cfg) {
        return cfg.getFactoryPid() != null;
    }

    private static boolean isManagedService(Configuration cfg) {
        return cfg.getFactoryPid() == null;
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
        throw new IllegalStateException("should not be called");
    }

    @Override
    public Stream<ImportMigrationEntry> entries(Path path) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public Stream<ImportMigrationEntry> entries(Path path, PathMatcher filter) {
        throw new IllegalStateException("should not be called");
    }

    Configuration createConfiguration(ImportMigrationConfigurationAdminEntry entry)
            throws IOException {
        // Question: should we use the bundle location that was exported???
        if (entry.isManagedServiceFactory()) {
            return configurationAdmin.createFactoryConfiguration(entry.getFactoryPid(), null);
        }
        return configurationAdmin.getConfiguration(entry.getPid());
    }

    Configuration getMemoryConfiguration(ImportMigrationConfigurationAdminEntry entry) {
        final String fpid = entry.getFactoryPid();

        if (fpid != null) {
            final List<Configuration> mcfgs = memoryFactoryServices.get(fpid);

            if (mcfgs == null) {
                return null;
            }
            // search for it based on the felix file install property
            final Path epath = getPathFromConfiguration(entry.getProperties(),
                    () -> String.format("path '%s'", entry.getPath()));

            if (epath != null) {
                for (final Iterator<Configuration> i = mcfgs.iterator(); i.hasNext(); ) {
                    final Configuration mcfg = i.next();
                    final Path mpath = getPathFromConfiguration(mcfg.getProperties(),
                            () -> String.format("configuration '%s'", mcfg.getPid()));

                    if (epath.equals(mpath)) {
                        // remove it from memory list and clean the map if it was the last one
                        i.remove();
                        if (mcfgs.isEmpty()) {
                            memoryFactoryServices.remove(fpid);
                        }
                        return mcfg;
                    }
                }
            }  // else - this means we will not be able to correlate an exported managed service factory
            //           with its counterpart here, as such we will be forced to treat it as a new one
            return null;
        }
        return memoryServices.remove(entry.getPid());
    }

    private ImportMigrationConfigurationAdminEntry proxy(ImportMigrationEntry entry) {
        final Path path = entry.getPath();
        final String extn = FilenameUtils.getExtension(path.toString());
        final PersistenceStrategy ps = admin.getPersister(extn);

        if (ps == null) {
            getReport().record(new ImportPathMigrationException(path,
                    String.format("persistence strategy [%s] is not defined", extn)));
        } else {
            final Dictionary<String, Object> properties;

            try {
                final InputStream is = entry.getInputStream()
                        .orElse(null);

                if (is == null) {
                    throw new ImportPathMigrationException(path,
                            String.format("unable to read %s configuration; not exported",
                                    ps.getExtension()));
                }
                try {
                    properties = ps.read(is);
                } catch (IOException e) {
                    throw new ImportPathMigrationException(path,
                            String.format("unable to read %s configuration", ps.getExtension()),
                            e);
                }
            } catch (IOException e) {
                throw new ImportPathMigrationException(path,
                        String.format("unable to read %s configuration", ps.getExtension()),
                        e);
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

    @Nullable
    private Path getPathFromConfiguration(@Nullable Dictionary<String, Object> properties,
            Supplier<String> from) {
        if (properties == null) {
            return null;
        }
        final Object o = properties.get(ConfigurationAdminMigratable.FELIX_FILEINSTALL_FILENAME);
        final Path path;

        if (o != null) {
            try {
                if (o instanceof URL) {
                    path = new File(((URL) o).toURI()).toPath();
                } else if (o instanceof URI) {
                    path = new File((URI) o).toPath();
                } else if (o instanceof String) {
                    path = new File(new URL((String) o).toURI()).toPath();
                } else if (o instanceof File) {
                    path = ((File) o).toPath();
                } else if (o instanceof Path) {
                    path = (Path) o;
                } else {
                    LOGGER.debug("unsupported {} property '{}' from {}",
                            ConfigurationAdminMigratable.FELIX_FILEINSTALL_FILENAME,
                            o,
                            from.get());
                    return null;
                }
            } catch (MalformedURLException | URISyntaxException e) {
                LOGGER.debug(String.format("failed to parse %s property '%s' from %s; ",
                        ConfigurationAdminMigratable.FELIX_FILEINSTALL_FILENAME,
                        o,
                        from.get()), e);
                return null;
            }
        } else {
            return null;
        }
        // ignore the whole path if any (there shouldn't be any other than etc) and force it to be under etc
        return Paths.get("etc")
                .resolve(path.getFileName());
    }

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
            getReport().record(new ImportMigrationException(String.format(
                    "Configuration [%] cannot be deleted",
                    configuration.getPid()), e));
        }
    }
}
