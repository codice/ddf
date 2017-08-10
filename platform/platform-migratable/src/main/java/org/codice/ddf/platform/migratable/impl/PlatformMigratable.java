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

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

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

    private static final Path USERS_ATTRIBUTES = Paths.get("etc", "users.attributes");

    private static final Path DDF_METACARD_ATTRIBUTE_RULESET = Paths.get("etc",
            "pdp",
            "ddf-metacard-attribute-ruleset.cfg");

    private static final Path DDF_USER_ATTRIBUTE_RULESET = Paths.get("etc",
            "pdp",
            "ddf-user-attribute-ruleset.cfg");

    private static final Path APPLICATION_LIST = Paths.get("etc",
            "org.codice.ddf.admin.applicationlist.properties");

    private static final Path FIPS_TO_ISO = Paths.get("etc", "fipsToIso.properties");

    private static final Path STARTUP_PROPERTIES = Paths.get("etc", "startup.properties");

    private static final Path LOG4J_CONFIG = Paths.get("etc", "log4j2.config.xml");

    private static final Path CUSTOM_PROPERTIES = Paths.get("etc", "custom.properties");

    private static final Path CONFIG_PROPERTIES = Paths.get("etc", "config.properties");

    private static final Path ENCRYPTION_KEYSET_METADATA = Paths.get("etc", "certs", "meta");

    private static final Path ENCRYPTION_KEY = Paths.get("etc", "certs", "1");

    private static final Path KARAF_SCRIPT_SH = Paths.get("bin", "karaf");

    private static final Path KARAF_SCRIPT_BAT = Paths.get("bin", "karaf.bat");

    private static final Path VERSION_FILE = Paths.get("Version.txt");

    private static final String SERVICE_WRAPPER_CONF_FILTER = "glob:**/*-wrapper.conf";

    private final MigratableUtil migratableUtil;

    public PlatformMigratable(DescribableBean info, MigratableUtil migratableUtil) {

        super(info);

        notNull(migratableUtil, "migratable utility should not be null");
        this.migratableUtil = migratableUtil;
    }

    public MigrationMetadata export(Path exportPath) throws MigrationException {
        LOGGER.debug("Exporting system files...");
        Collection<MigrationWarning> migrationWarnings = new ArrayList<>();
        exportSystemFiles(exportPath, migrationWarnings);
        exportWsSecurity(exportPath, migrationWarnings);
        exportLoggingConfiguration(exportPath, migrationWarnings);
        exportEncryptionServiceKeyData(exportPath, migrationWarnings);
        exportKarafStartScripts(exportPath, migrationWarnings);
        exportServiceWrapperConf(exportPath, migrationWarnings);
        exportVersionFile(exportPath, migrationWarnings);
        return new MigrationMetadata(migrationWarnings);
    }

    private void exportSystemFiles(Path exportDirectory,
            Collection<MigrationWarning> migrationWarnings) {
        LOGGER.debug("Exporting system files: [{}], [{}], [{}], [{}], [{}], [{}], and [{}]",
                SYSTEM_PROPERTIES.toString(),
                USERS_PROPERTIES.toString(),
                USERS_ATTRIBUTES.toString(),
                APPLICATION_LIST.toString(),
                CUSTOM_PROPERTIES.toString(),
                CONFIG_PROPERTIES.toString(),
                STARTUP_PROPERTIES.toString());

        migratableUtil.copyFile(SYSTEM_PROPERTIES, exportDirectory, migrationWarnings);
        migratableUtil.copyFile(USERS_PROPERTIES, exportDirectory, migrationWarnings);
        migratableUtil.copyFile(USERS_ATTRIBUTES, exportDirectory, migrationWarnings);
        migratableUtil.copyFile(APPLICATION_LIST, exportDirectory, migrationWarnings);
        migratableUtil.copyFile(CUSTOM_PROPERTIES, exportDirectory, migrationWarnings);
        migratableUtil.copyFile(CONFIG_PROPERTIES, exportDirectory, migrationWarnings);
        migratableUtil.copyFile(STARTUP_PROPERTIES, exportDirectory, migrationWarnings);

        migratableUtil.copyFile(DDF_METACARD_ATTRIBUTE_RULESET, exportDirectory, migrationWarnings);
        migratableUtil.copyFile(DDF_USER_ATTRIBUTE_RULESET, exportDirectory, migrationWarnings);
        migratableUtil.copyFile(FIPS_TO_ISO, exportDirectory, migrationWarnings);
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

    private void exportLoggingConfiguration(Path exportDirectory, Collection<MigrationWarning> migrationWarnings) {
        LOGGER.debug("Exporting logging configuration file: [{}]", LOG4J_CONFIG.toString());
        migratableUtil.copyFile(LOG4J_CONFIG, exportDirectory, migrationWarnings);
    }

    private void exportEncryptionServiceKeyData(Path exportDirectory, Collection<MigrationWarning> migrationWarnings) {
        LOGGER.debug("Exporting Encryption Service key data files: [{}] and [{}].", ENCRYPTION_KEYSET_METADATA.toString(), ENCRYPTION_KEY.toString());
        migratableUtil.copyFile(ENCRYPTION_KEYSET_METADATA, exportDirectory, migrationWarnings);
        migratableUtil.copyFile(ENCRYPTION_KEY, exportDirectory, migrationWarnings);
    }

    private void exportKarafStartScripts(Path exportDirectory, Collection<MigrationWarning> migrationWarnings) {
        LOGGER.debug("Exporting Karaf start scripts: [{}] and [{}].", KARAF_SCRIPT_SH.toString(), KARAF_SCRIPT_BAT.toString());
        migratableUtil.copyFile(KARAF_SCRIPT_SH, exportDirectory, migrationWarnings);
        migratableUtil.copyFile(KARAF_SCRIPT_BAT, exportDirectory, migrationWarnings);
    }

    private void exportServiceWrapperConf(Path exportDirectory, Collection<MigrationWarning> migrationWarnings) {
        LOGGER.debug("Exporting service wrapper config file");
        PathMatcher filter = FileSystems.getDefault().getPathMatcher(SERVICE_WRAPPER_CONF_FILTER);
        migratableUtil.copyFiles(Paths.get("etc"), filter, exportDirectory, migrationWarnings);
    }

    private void exportVersionFile(Path exportDirectory, Collection<MigrationWarning> migrationWarnings) {
        LOGGER.debug("Exporting version file: [{}]", VERSION_FILE.toString());
        migratableUtil.copyFile(VERSION_FILE, exportDirectory, migrationWarnings);
    }

}
