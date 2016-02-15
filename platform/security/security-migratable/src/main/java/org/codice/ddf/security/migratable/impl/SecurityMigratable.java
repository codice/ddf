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
import java.util.ArrayList;
import java.util.Collection;

import org.codice.ddf.migration.AbstractMigratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationMetadata;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.migration.util.MigratableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the export process for all Security system files
 */
public class SecurityMigratable extends AbstractMigratable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityMigratable.class);

    private static final Path PDP_POLICIES_DIR = Paths.get("etc", "pdp");

    private static final Path FILE_CONTAINING_CRL_LOCATION = Paths.get("etc",
            "ws-security",
            "server",
            "encryption.properties");

    private static final String CRL_PROP_KEY = "org.apache.ws.security.crypto.merlin.x509crl.file";

    private MigratableUtil migratableUtil;

    public SecurityMigratable(String description, boolean isOptional,
            MigratableUtil migratableUtil) {
        super(description, isOptional);
        this.migratableUtil = migratableUtil;
    }

    public MigrationMetadata export(Path exportPath) throws MigrationException {
        Collection<MigrationWarning> migrationWarnings = new ArrayList<>();
        exportCrlFile(exportPath, migrationWarnings);
        exportPdpDirectory(exportPath, migrationWarnings);
        return new MigrationMetadata(migrationWarnings);
    }

    private void exportCrlFile(Path exportDirectory, Collection<MigrationWarning> migrationWarnings)
            throws MigrationException {
        LOGGER.debug("Exporting CRL from property [{}] in file [{}]...",
                CRL_PROP_KEY,
                FILE_CONTAINING_CRL_LOCATION.toString());
        migratableUtil.copyFileFromJavaPropertyValue(FILE_CONTAINING_CRL_LOCATION,
                CRL_PROP_KEY,
                exportDirectory,
                migrationWarnings);
    }

    private void exportPdpDirectory(Path exportDirectory,
            Collection<MigrationWarning> migrationWarnings) throws MigrationException {
        LOGGER.debug("Exporting PDP Directory at [{}]...", PDP_POLICIES_DIR.toString());
        migratableUtil.copyDirectory(PDP_POLICIES_DIR, exportDirectory, migrationWarnings);
    }
}
