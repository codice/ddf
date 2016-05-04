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

import static org.apache.commons.lang.Validate.notNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

import javax.validation.constraints.NotNull;

import org.codice.ddf.migration.ConfigurationMigratable;
import org.codice.ddf.migration.DescribableBean;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationMetadata;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.migration.util.MigratableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the export process for all Platform system files.
 */
public class PlatformMigratable extends DescribableBean implements ConfigurationMigratable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformMigratable.class);

    private static final String KEYSTORE_SYSTEM_PROP = "javax.net.ssl.keyStore";

    private static final String TRUSTSTORE_SYSTEM_PROP = "javax.net.ssl.trustStore";

    private static final Path WS_SECURITY_DIR = Paths.get("etc", "ws-security");

    private static final Path SYSTEM_PROPERTIES = Paths.get("etc", "system.properties");

    private static final Path USERS_PROPERTIES = Paths.get("etc", "users.properties");

    private static final Path DDF_METACARD_ATTRIBUTE_RULESET = Paths.get("etc",
            "pdp",
            "ddf-metacard-attribute-ruleset.cfg");

    private static final Path DDF_USER_ATTRIBUTE_RULESET = Paths.get("etc",
            "pdp",
            "ddf-user-attribute-ruleset.cfg");

    private static final Path APPLICATION_LIST = Paths.get("etc",
            "org.codice.ddf.admin.applicationlist.properties");

    private final MigratableUtil migratableUtil;

    public PlatformMigratable(@NotNull DescribableBean info,
            @NotNull MigratableUtil migratableUtil) {

        super(info);

        notNull(migratableUtil, "migratable utility should not be null");
        this.migratableUtil = migratableUtil;
    }

    public MigrationMetadata export(Path exportPath) throws MigrationException {
        LOGGER.debug("Exporting system files...");
        Collection<MigrationWarning> migrationWarnings = new ArrayList<>();
        exportSystemFiles(exportPath, migrationWarnings);
        exportWsSecurity(exportPath, migrationWarnings);
        return new MigrationMetadata(migrationWarnings);
    }

    private void exportSystemFiles(Path exportDirectory,
            Collection<MigrationWarning> migrationWarnings) {
        LOGGER.debug("Exporting system files: [{}], [{}], and [{}]",
                SYSTEM_PROPERTIES.toString(),
                USERS_PROPERTIES.toString(),
                APPLICATION_LIST.toString());
        migratableUtil.copyFile(SYSTEM_PROPERTIES, exportDirectory, migrationWarnings);
        migratableUtil.copyFile(USERS_PROPERTIES, exportDirectory, migrationWarnings);
        migratableUtil.copyFile(APPLICATION_LIST, exportDirectory, migrationWarnings);
        migratableUtil.copyFile(DDF_METACARD_ATTRIBUTE_RULESET, exportDirectory, migrationWarnings);
        migratableUtil.copyFile(DDF_USER_ATTRIBUTE_RULESET, exportDirectory, migrationWarnings);
    }

    private void exportWsSecurity(Path exportDirectory,
            Collection<MigrationWarning> migrationWarnings) {
        LOGGER.debug("Exporting [{}]...", WS_SECURITY_DIR.toString());
        migratableUtil.copyDirectory(WS_SECURITY_DIR, exportDirectory, migrationWarnings);
        exportKeystores(exportDirectory, migrationWarnings);
    }

    private void exportKeystores(Path exportDirectory,
            Collection<MigrationWarning> migrationWarnings) {
        LOGGER.debug("Exporting keystore and truststore...");
        migratableUtil.copyFileFromSystemPropertyValue(KEYSTORE_SYSTEM_PROP,
                exportDirectory,
                migrationWarnings);
        migratableUtil.copyFileFromSystemPropertyValue(TRUSTSTORE_SYSTEM_PROP,
                exportDirectory,
                migrationWarnings);
    }
}
