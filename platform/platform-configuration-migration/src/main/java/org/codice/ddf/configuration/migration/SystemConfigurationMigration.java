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
package org.codice.ddf.configuration.migration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.configuration.status.MigrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to migrate system configuration files.
 */
public class SystemConfigurationMigration {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SystemConfigurationMigration.class);

    private static final String KEYSTORE_SYSTEM_PROP = "javax.net.ssl.keyStore";

    private static final String TRUSTSTORE_SYSTEM_PROP = "javax.net.ssl.trustStore";

    private static final String FILE_CONTAINING_CRL_LOCATION =
            "etc/ws-security/server/encryption.properties";

    private static final String WS_SECURITY_DIR = "etc/ws-security";

    private static final String SECURITY_DIRECTORY = "security";

    private static final String PDP_POLICIES_DIR = "etc/pdp";

    private static final String CRL_PROP_KEY = "org.apache.ws.security.crypto.merlin.x509crl.file";

    private static final String SYSTEM_PROPERTIES = "etc/system.properties";

    private static final String USERS_PROPERTIES = "etc/users.properties";

    private Path ddfHome;

    public SystemConfigurationMigration(Path ddfHome) throws MigrationException {
        this.ddfHome = getRealPath(ddfHome);
    }

    public void export(Path exportDirectory) throws MigrationException {
        exportSecurity(exportDirectory);
        exportSystemFiles(exportDirectory);
    }

    private void exportSecurity(Path exportDirectory) throws MigrationException {
        exportSecurityDirectory(exportDirectory);
        exportKeystores(exportDirectory);
        exportWsSecurity(exportDirectory);
        exportCrl(exportDirectory);
        exportPdpPolicies(exportDirectory);
    }

    private void exportSystemFiles(Path exportDirectory) throws MigrationException {
        exportUsersProperties(exportDirectory);
        exportSystemProperties(exportDirectory);
    }

    private void exportKeystores(Path exportDirectory) throws MigrationException {
        exportKeystore(exportDirectory);
        exportTruststore(exportDirectory);
    }

    private void exportKeystore(Path exportDirectory) throws MigrationException {
        String keystore = getProperty(KEYSTORE_SYSTEM_PROP);
        verifyPathIsNotAbsolute(Paths.get(keystore));
        Path source = getRealPath(ddfHome.resolve(keystore));
        verifyWithinDdfHome(source);
        Path destination = constructDestination(source, exportDirectory);
        copyFile(source, destination);
    }

    private void exportTruststore(Path exportDirectory) throws MigrationException {
        String truststore = getProperty(TRUSTSTORE_SYSTEM_PROP);
        verifyPathIsNotAbsolute(Paths.get(truststore));
        Path source = getRealPath(ddfHome.resolve(truststore));
        verifyWithinDdfHome(source);
        Path destination = constructDestination(source, exportDirectory);
        copyFile(source, destination);
    }

    private void exportCrl(Path exportDirectory) throws MigrationException {
        Path encryptionPropertiesFile = ddfHome.resolve(FILE_CONTAINING_CRL_LOCATION);
        Properties properties = readPropertiesFile(encryptionPropertiesFile);
        String crlLocation = properties.getProperty(CRL_PROP_KEY);
        if (StringUtils.isNotBlank(crlLocation)) {
            verifyPathIsNotAbsolute(Paths.get(crlLocation));
            Path source = getRealPath(ddfHome.resolve(crlLocation));
            verifyWithinDdfHome(source);
            Path destination = constructDestination(source, exportDirectory);
            copyFile(source, destination);
        } else {
            LOGGER.warn("Unable to find CRL location in [%s] using property [%s].",
                    encryptionPropertiesFile.toString(),
                    CRL_PROP_KEY);
        }
    }

    private void exportSecurityDirectory(Path exportDirectory) {
        try {
            Path source = ddfHome.resolve(Paths.get(SECURITY_DIRECTORY));
            Path destination = constructDestination(source, exportDirectory);
            copyDirectory(source, destination);
        } catch (MigrationException e) {
            LOGGER.info(String.format("Unable to copy %s. It doesn't exist.", SECURITY_DIRECTORY),
                    e);
        }
    }

    private Path constructDestination(Path pathToExport, Path exportDirectory)
            throws MigrationException {
        Path destination = exportDirectory.resolve(ddfHome.relativize(pathToExport));
        return destination;
    }

    private void exportWsSecurity(Path exportDirectory) throws MigrationException {
        Path source = ddfHome.resolve(Paths.get(WS_SECURITY_DIR));
        Path destination = constructDestination(source, exportDirectory);
        copyDirectory(source, destination);
    }

    private void exportSystemProperties(Path exportDirectory) throws MigrationException {
        Path source = ddfHome.resolve(SYSTEM_PROPERTIES);
        Path destination = exportDirectory.resolve(SYSTEM_PROPERTIES);
        copyFile(source, destination);
    }

    private void exportUsersProperties(Path exportDirectory) throws MigrationException {
        Path source = ddfHome.resolve(USERS_PROPERTIES);
        Path destination = exportDirectory.resolve(USERS_PROPERTIES);
        copyFile(source, destination);
    }

    private void exportPdpPolicies(Path exportDirectory) throws MigrationException {
        Path source = ddfHome.resolve(Paths.get(PDP_POLICIES_DIR));
        Path destination = constructDestination(source, exportDirectory);
        copyDirectory(source, destination);
    }

    private String getProperty(String property) throws MigrationException {
        String prop = System.getProperty(property);
        if (StringUtils.isBlank(prop)) {
            String message = String.format("System property %s is not set.", property);
            LOGGER.error(message);
            throw new MigrationException(message);
        }

        return prop;
    }

    private void copyFile(Path source, Path destination) throws MigrationException {
        try {
            FileUtils.copyFile(source.toFile(), destination.toFile());
        } catch (IOException e) {
            String message = String.format("Unable to copy [%s] to [%s].",
                    source.toString(),
                    destination.toString());
            LOGGER.error(message, e);
            throw new MigrationException(message, e);
        }
    }

    private void copyDirectory(Path source, Path destination) throws MigrationException {
        try {
            FileUtils.copyDirectory(source.toFile(), destination.toFile());
        } catch (IOException e) {
            String message = String.format("Unable to copy [%s] to [%s].",
                    source.toAbsolutePath()
                            .toString(),
                    source.toAbsolutePath()
                            .toString());
            LOGGER.error(message, e);
            throw new MigrationException(message, e);
        }
    }

    private void verifyWithinDdfHome(Path path) throws MigrationException {
        if (!getRealPath(path).startsWith(ddfHome)) {
            String message = String.format("The path [%s] is outside of [%s].",
                    path.toString(),
                    ddfHome.toString());
            LOGGER.error(message);
            throw new MigrationException(message);
        }
    }

    private void verifyPathIsNotAbsolute(Path path) throws MigrationException {
        if (path.isAbsolute()) {
            String message = String.format(
                    "The path [%s] is absolute. This path must be relative to [%s].",
                    path.toString(),
                    ddfHome);
            LOGGER.error(message);
            throw new MigrationException(message);
        }
    }

    Properties readPropertiesFile(Path propertiesFile) throws MigrationException {
        Properties properties = new Properties();
        try (InputStream inputStream = new FileInputStream(propertiesFile.toString())) {
            properties.load(inputStream);
            return properties;
        } catch (IOException e) {
            String message = String.format("Unable to read properties file [%s].",
                    propertiesFile.toString());
            LOGGER.error(message, e);
            throw new MigrationException(message, e);
        }
    }

    Path getRealPath(Path path) throws MigrationException {
        try {
            Path realPath = path.toRealPath();
            return realPath;
        } catch (IOException e) {
            String message = String.format("Unable to construct real path from [%s].",
                    path.toString());
            LOGGER.error(message, e);
            throw new MigrationException(message, e);
        }
    }
}
