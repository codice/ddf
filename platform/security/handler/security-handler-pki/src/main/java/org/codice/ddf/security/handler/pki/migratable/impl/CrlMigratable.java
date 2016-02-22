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
package org.codice.ddf.security.handler.pki.migratable.impl;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import org.codice.ddf.migration.AbstractMigratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationMetadata;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.migration.util.MigratableUtil;
import org.codice.ddf.security.handler.pki.CrlChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class exports the CRL if CRL checking is enabled (CRL property in etc/ws-security/server/encryption.properties
 * is not commented out).
 */
public class CrlMigratable extends AbstractMigratable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrlMigratable.class);

    private final CrlChecker crlChecker;

    private final MigratableUtil migratableUtil;

    public CrlMigratable(String description, boolean isOptional, CrlChecker crlChecker,
            MigratableUtil migratableUtil) {
        super(description, isOptional);
        this.crlChecker = crlChecker;
        this.migratableUtil = migratableUtil;
    }

    @Override
    public MigrationMetadata export(Path exportPath) throws MigrationException {
        return exportCrlFile(exportPath);
    }

    private MigrationMetadata exportCrlFile(Path exportDirectory) throws MigrationException {
        Collection<MigrationWarning> migrationWarnings = new ArrayList<>();
        LOGGER.debug("Attempting to export CRL...");
        if (crlChecker.isCrlEnabled()) {
            migratableUtil.copyFile(crlChecker.getCrlPath(), exportDirectory, migrationWarnings);
        } else {
            migrationWarnings
                    .add(new MigrationWarning(String.format(
                            "Unable to export CRL. CRL property [%s] not found in file [%s].",
                            CrlChecker.CRL_PROPERTY_KEY,
                            CrlChecker.ENCRYPTION_PROPERTIES_PATH.toString())));
        }

        return new MigrationMetadata(migrationWarnings);
    }
}
