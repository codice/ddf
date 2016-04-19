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

import static org.apache.commons.lang.Validate.notNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.migration.ConfigurationMigratable;
import org.codice.ddf.migration.DescribableBean;
import org.codice.ddf.migration.ExportMigrationException;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationMetadata;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.migration.util.MigratableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

/**
 * This class handles the export process for all Security system files
 */
public class SecurityMigratable extends DescribableBean implements ConfigurationMigratable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityMigratable.class);

    private static final Path PDP_POLICIES_DIR = Paths.get("etc", "pdp");

    private static final List<Path> PROPERTIES_FILES = ImmutableList.of(Paths.get("etc",
            "ws-security",
            "server",
            "encryption.properties"),
            Paths.get("etc", "ws-security", "server", "signature.properties"),
            Paths.get("etc", "ws-security", "issuer", "encryption.properties"),
            Paths.get("etc", "ws-security", "issuer", "signature.properties"));

    private static final String CRL_PROP_KEY = "org.apache.ws.security.crypto.merlin.x509crl.file";

    private final MigratableUtil migratableUtil;

    public SecurityMigratable(@NotNull DescribableBean info,
            @NotNull MigratableUtil migratableUtil) {

        super(info);

        notNull(migratableUtil);
        this.migratableUtil = migratableUtil;
    }

    public MigrationMetadata export(Path exportPath) throws MigrationException {
        Collection<MigrationWarning> migrationWarnings = new ArrayList<>();
        exportCrlFiles(exportPath, migrationWarnings);
        exportPdpDirectory(exportPath, migrationWarnings);
        return new MigrationMetadata(migrationWarnings);
    }

    private void exportCrlFiles(Path exportDirectory,
            Collection<MigrationWarning> migrationWarnings) throws MigrationException {
        for (Path propertiesPath : PROPERTIES_FILES) {
            exportCrlFile(propertiesPath, exportDirectory, migrationWarnings);
        }
    }

    private void exportCrlFile(Path propertiesPath, Path exportDirectory,
            Collection<MigrationWarning> migrationWarnings) throws MigrationException {
        LOGGER.debug("Exporting CRL from property [{}] in file [{}]...",
                CRL_PROP_KEY,
                propertiesPath.toString());
        String crlPathStr = migratableUtil.getJavaPropertyValue(propertiesPath, CRL_PROP_KEY);

        if (crlPathStr == null) {
            return;
        }

        if (StringUtils.isWhitespace(crlPathStr)) {
            String error = String.format(
                    "Failed to export CRL. No CRL path found in file [%s]. Property [%s] from properties file [%s] has a blank value.",
                    propertiesPath,
                    CRL_PROP_KEY,
                    propertiesPath);
            throw new ExportMigrationException(error);
        }

        Path crlPath = Paths.get(crlPathStr);
        migratableUtil.copyFile(crlPath, exportDirectory, migrationWarnings);
    }

    private void exportPdpDirectory(Path exportDirectory,
            Collection<MigrationWarning> migrationWarnings) throws MigrationException {
        LOGGER.debug("Exporting PDP Directory at [{}]...", PDP_POLICIES_DIR.toString());
        migratableUtil.copyDirectory(PDP_POLICIES_DIR, exportDirectory, migrationWarnings);
    }
}
