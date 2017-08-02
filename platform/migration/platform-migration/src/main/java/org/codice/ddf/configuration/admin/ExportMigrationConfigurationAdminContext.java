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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.Validate;
import org.codice.ddf.configuration.persistence.PersistenceStrategy;
import org.codice.ddf.migration.ExportMigrationContext;
import org.codice.ddf.migration.ExportMigrationEntry;
import org.codice.ddf.migration.ExportPathMigrationWarning;
import org.codice.ddf.migration.MigrationEntry;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.migration.ProxyExportMigrationContext;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportMigrationConfigurationAdminContext extends ProxyExportMigrationContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            ExportMigrationConfigurationAdminContext.class);

    private final ConfigurationAdminMigratable admin;

    private final Set<String> warnedExtensions = new HashSet<>(12);

    private final Map<Path, ExportMigrationEntry> entries;

    public ExportMigrationConfigurationAdminContext(ExportMigrationContext context,
            ConfigurationAdminMigratable admin, Stream<Configuration> configs) {
        super(context);
        Validate.notNull(admin, "invalid null configuration admin migratable");
        Validate.notNull(configs, "invalid null configurations");
        this.admin = admin;
        this.entries = configs.map(this::getEntry)
                .collect(Collectors.toMap(MigrationEntry::getPath, Function.identity()));
    }

    public Stream<ExportMigrationEntry> entries() {
        return entries.values().stream();
    }

    @Override
    public ExportMigrationEntry getEntry(Path path) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public Stream<ExportMigrationEntry> entries(Path path) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public Stream<ExportMigrationEntry> entries(Path path, PathMatcher filter) {
        throw new IllegalStateException("should not be called");
    }

    private ExportMigrationEntry getEntry(Configuration configuration) {
        Validate.notNull(configuration, "invalid null configuration");
        Path path = getPathFromConfiguration(configuration);
        final String extn = FilenameUtils.getExtension(path.toString());
        PersistenceStrategy ps = admin.getPersister(extn);

        if (ps == null) {
            ps = admin.getDefaultPersister();
            if (warnedExtensions.add(extn)) {
                getReport().record(new MigrationWarning(String.format(
                        "Persistence strategy [%s] is not defined; defaulting to %s",
                        extn,
                        ps.getExtension())));
            }
            path = Paths.get(
                    path.toString() + FilenameUtils.EXTENSION_SEPARATOR + ps.getExtension());
        }
        return new ExportMigrationConfigurationAdminEntry(super.getEntry(path), configuration, ps);
    }

    private Path getPathFromConfiguration(Configuration configuration) {
        final Object o = configuration.getProperties()
                .get(ConfigurationAdminMigratable.FELIX_FILEINSTALL_FILENAME);
        Path path = null;

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
                    path = constructPath(configuration);
                    LOGGER.debug("unsupported {} property from '{}'",
                            ConfigurationAdminMigratable.FELIX_FILEINSTALL_FILENAME,
                            o);
                    getReport().record(new ExportPathMigrationWarning(Objects.toString(o, null),
                            String.format(
                                    "from %s property [%s] for configuration [%s] is of an unsupported format; exporting as [%s]",
                                    ConfigurationAdminMigratable.FELIX_FILEINSTALL_FILENAME,
                                    o,
                                    configuration.getPid(),
                                    path)));
                }
            } catch (MalformedURLException | URISyntaxException e) {
                path = constructPath(configuration);
                LOGGER.debug(String.format("failed to parse %s property from '%s'; ",
                        ConfigurationAdminMigratable.FELIX_FILEINSTALL_FILENAME,
                        o), e);
                getReport().record(new ExportPathMigrationWarning(Objects.toString(o, null),
                        String.format(
                                "from %s property [%s] for configuration [%s] cannot be parsed; exporting as [%s]",
                                ConfigurationAdminMigratable.FELIX_FILEINSTALL_FILENAME,
                                o,
                                configuration.getPid(),
                                path)));
            }
        } else {
            path = constructPath(configuration);
        }
        // ignore the whole path if any (there shouldn't be any other than etc) and force it to be under etc
        return Paths.get("etc")
                .resolve(path.getFileName());
    }

    private Path constructPath(Configuration configuration) {
        final String fpid = configuration.getFactoryPid();
        final String bname;

        if (fpid != null) { // it is a managed service factory!!!
            // Felix Fileinstall uses the hyphen as separator between factoryPid and alias. For
            // safety reasons, all hyphens are removed from the generated UUID.
            final String alias = UUID.randomUUID()
                    .toString()
                    .replaceAll("-", "");

            bname = fpid + '-' + alias;
        } else {
            bname = configuration.getPid();
        }
        return Paths.get(bname + FilenameUtils.EXTENSION_SEPARATOR + admin.getDefaultPersister()
                .getExtension());
    }
}
