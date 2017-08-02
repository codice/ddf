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
package org.codice.ddf.platform.migratable.impl;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.codice.ddf.migration.ConfigurationMigratable;
import org.codice.ddf.migration.ExportMigrationContext;
import org.codice.ddf.migration.ImportMigrationContext;
import org.codice.ddf.migration.ImportMigrationException;
import org.codice.ddf.migration.MigrationEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

/**
 * This class handles the export process for all Platform system files.
 */
public class PlatformMigratable implements ConfigurationMigratable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformMigratable.class);

    /**
     * Holds the current export version.
     * <p>
     * 1.0 - initial version
     */
    private static final String VERSION = "1.0";

    private static final String KEYSTORE_SYSTEM_PROP = "javax.net.ssl.keyStore";

    private static final String TRUSTSTORE_SYSTEM_PROP = "javax.net.ssl.trustStore";

    private static final Path ETC_DIR = Paths.get("etc");

    private static final Path WS_SECURITY_DIR = Paths.get("etc", "ws-security");

    private static final List<Path> SYSTEM_FILES = ImmutableList.of( //
            Paths.get("etc", "system.properties"),
            Paths.get("etc", "users.properties"),
            Paths.get("etc", "users.attributes"),
            Paths.get("etc", "pdp", "ddf-metacard-attribute-ruleset.cfg"),
            Paths.get("etc", "pdp", "ddf-user-attribute-ruleset.cfg"),
            Paths.get("etc", "org.codice.ddf.admin.applicationlist.properties"),
            Paths.get("etc", "fipsToIso.properties"),
            Paths.get("etc", "startup.properties"),
            Paths.get("etc", "log4j2.config.xml"),
            Paths.get("etc", "custom.properties"),
            Paths.get("etc", "config.properties"),
            Paths.get("etc", "certs", "meta"),
            Paths.get("etc", "certs", "1"),
            Paths.get("bin", "karaf"),
            Paths.get("bin", "karaf.bat"));

    private static final PathMatcher SERVICE_WRAPPER_CONF_FILTER = FileSystems.getDefault()
            .getPathMatcher("glob:**/*-wrapper.conf");

    public PlatformMigratable() {
    }

    @Override
    public String getVersion() {
        return PlatformMigratable.VERSION;
    }

    @Override
    public String getId() {
        return "ddf.platform";
    }

    @Override
    public String getTitle() {
        return "Platform Migration";
    }

    @Override
    public String getDescription() {
        return "Exports Platform system files";
    }

    @Override
    public String getOrganization() {
        return "Codice";
    }

    @Override
    public void doExport(ExportMigrationContext context) {
        LOGGER.debug("Exporting system files...");
        PlatformMigratable.SYSTEM_FILES.stream()
                .map(context::getEntry)
                .forEach(MigrationEntry::store);
        LOGGER.debug("Exporting security files from [{}]...", PlatformMigratable.WS_SECURITY_DIR);
        context.entries(PlatformMigratable.WS_SECURITY_DIR)
                .forEach(MigrationEntry::store);
        LOGGER.debug("Exporting keystore and truststore...");
        context.getSystemPropertyReferencedEntry(PlatformMigratable.KEYSTORE_SYSTEM_PROP)
                .ifPresent(MigrationEntry::store);
        context.getSystemPropertyReferencedEntry(PlatformMigratable.TRUSTSTORE_SYSTEM_PROP)
                .ifPresent(MigrationEntry::store);
        LOGGER.debug("Exporting service wrapper config file");
        context.entries(PlatformMigratable.ETC_DIR, PlatformMigratable.SERVICE_WRAPPER_CONF_FILTER)
                .forEach(MigrationEntry::store);
    }

    @Override
    public void doImport(ImportMigrationContext context) {
        if (!PlatformMigratable.VERSION.equals(context.getVersion())) {
            context.getReport()
                    .record(new ImportMigrationException(String.format(
                            "unsupported exported migrated version [%s] for migratable [%s]; currently supporting [%s]",
                            context.getVersion(),
                            getId(),
                            PlatformMigratable.VERSION)));
            return;
        }
        LOGGER.debug("Importing system files...");
        PlatformMigratable.SYSTEM_FILES.stream()
                .map(context::getEntry)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(MigrationEntry::store);
        LOGGER.debug("Importing [{}]...", PlatformMigratable.WS_SECURITY_DIR);
        context.cleanDirectory(PlatformMigratable.WS_SECURITY_DIR);
        context.entries(PlatformMigratable.WS_SECURITY_DIR)
                .forEach(MigrationEntry::store);
        LOGGER.debug("Importing keystore and truststore...");
        context.getSystemPropertyReferencedEntry(PlatformMigratable.KEYSTORE_SYSTEM_PROP)
                .ifPresent(MigrationEntry::store);
        context.getSystemPropertyReferencedEntry(PlatformMigratable.TRUSTSTORE_SYSTEM_PROP)
                .ifPresent(MigrationEntry::store);
        LOGGER.debug("Importing service wrapper config file");
        context.entries(PlatformMigratable.ETC_DIR, PlatformMigratable.SERVICE_WRAPPER_CONF_FILTER)
                .forEach(MigrationEntry::store);
    }
}
