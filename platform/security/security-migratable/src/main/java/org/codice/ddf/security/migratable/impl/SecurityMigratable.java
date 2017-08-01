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
package org.codice.ddf.security.migratable.impl;

import java.nio.file.Path;
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
 * This class handles the export process for all Security system files
 */
public class SecurityMigratable implements ConfigurationMigratable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityMigratable.class);

    /**
     * Holds the current export version.
     * <p>
     * 1.0 - initial version
     */
    private static final String VERSION = "1.0";

    private static final Path PDP_POLICIES_DIR = Paths.get("etc", "pdp");

    private static final List<Path> PROPERTIES_FILES = ImmutableList.of( //
            Paths.get("etc", "ws-security", "server", "encryption.properties"),
            Paths.get("etc", "ws-security", "server", "signature.properties"),
            Paths.get("etc", "ws-security", "issuer", "encryption.properties"),
            Paths.get("etc", "ws-security", "issuer", "signature.properties"));

    private static final String CRL_PROP_KEY = "org.apache.ws.security.crypto.merlin.x509crl.file";

    public SecurityMigratable() {}

    @Override
    public String getVersion() {
        return SecurityMigratable.VERSION;
    }

    @Override
    public String getId() {
        return "ddf.security";
    }

    @Override
    public String getTitle() {
        return "Security Migration";
    }

    @Override
    public String getDescription() {
        return "Exports Security system files";
    }

    @Override
    public String getOrganization() {
        return "Codice";
    }

    @Override
    public void doExport(ExportMigrationContext context) {
        SecurityMigratable.PROPERTIES_FILES.stream()
                .map(context::getEntry)
                .peek(me -> LOGGER.debug("Exporting CRL from property [{}] in file [{}]...",
                        SecurityMigratable.CRL_PROP_KEY,
                        me.getPath()))
                // do not automatically record an error if property is not defined (just skip that file)
                .map(me -> me.getPropertyReferencedEntry(SecurityMigratable.CRL_PROP_KEY,
                        (r, v) -> (v != null)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(MigrationEntry::store);
        LOGGER.debug("Exporting PDP files from [{}]...", SecurityMigratable.PDP_POLICIES_DIR);
        context.entries(SecurityMigratable.PDP_POLICIES_DIR)
                .forEach(MigrationEntry::store);
    }

    @Override
    public void doImport(ImportMigrationContext context) {
        if (!SecurityMigratable.VERSION.equals(context.getVersion())) {
            context.getReport()
                    .record(new ImportMigrationException(String.format(
                            "unsupported exported migrated version [%s] for migratable [%s]; currently supporting [%s]",
                            context.getVersion(),
                            getId(),
                            SecurityMigratable.VERSION)));
            return;
        }
        SecurityMigratable.PROPERTIES_FILES.stream()
                .map(context::getEntry)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .peek(me -> LOGGER.debug("Importing CRL from property [{}] in file [{}]...",
                        SecurityMigratable.CRL_PROP_KEY,
                        me.getPath()))
                .map(me -> me.getPropertyReferencedEntry(SecurityMigratable.CRL_PROP_KEY))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(MigrationEntry::store);
        LOGGER.debug("Importing PDP Directory at [{}]...", SecurityMigratable.PDP_POLICIES_DIR);
        context.cleanDirectory(SecurityMigratable.PDP_POLICIES_DIR);
        context.entries(SecurityMigratable.PDP_POLICIES_DIR)
                .forEach(MigrationEntry::store);
    }
}
