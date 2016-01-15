/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.configuration.status.MigrationException;
import org.codice.ddf.configuration.status.MigrationWarning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to migrate system configuration files.
 */
public class SystemConfigurationMigration {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(SystemConfigurationMigration.class);

    private static final String KEYSTORE_SYSTEM_PROP = "javax.net.ssl.keyStore";

    private static final String TRUSTSTORE_SYSTEM_PROP = "javax.net.ssl.trustStore";

    private static final String FILE_CONTAINING_CRL_LOCATION = "etc/ws-security/server/encryption.properties";

    private static final String WS_SECURITY_DIR = "etc/ws-security";

    private static final String SECURITY_DIRECTORY = "security";

    private static final String PDP_POLICIES_DIR = "etc/pdp";

    private static final String CRL_PROP_KEY = "org.apache.ws.security.crypto.merlin.x509crl.file";

    private static final String SYSTEM_PROPERTIES = "etc/system.properties";

    private static final String USERS_PROPERTIES = "etc/users.properties";

    private static final String ABSOLUTE_PATH_WARNING =
            "The value for property [%s] is set to a path [%s] that is absolute; "
                    + "therefore, the file will not be included in the export.  "
                    + "Check that the file exists on the system you're migrating to "
                    + "or update the property value and export again.";

    private static final String OUTSIDE_PATH_WARNING =
            "The value for property [%s] is set to a path [%s] that is outside [%s]; "
                    + "therefore, the file will not be included in the export.  "
                    + "Check that the file exists on the system you're migrating to "
                    + "or update the property value and export again.";

    private static final String UNREAL_PATH_WARNING =
            "The value for property [%s] is set to a path [%s] that could not coerced into a real path; "
                    + "therefore, the file will not be included in the export.  "
                    + "Check that the file exists on the system you're migrating to "
                    + "or update the property value and export again.";

    private static final String SYMBOLIC_LINK_PATH_WARNING =
            "The value for property [%s] is set to a symbolic path [%s] that could not coerced into a real path; "
                    + "therefore, the file will not be included in the export.  "
                    + "Check that the file exists on the system you're migrating to "
                    + "or update the property value and export again.";

    private Path ddfHome;

    public SystemConfigurationMigration(Path ddfHome) throws MigrationException {
        this.ddfHome = getRealPath(ddfHome);
    }

    public Collection<MigrationWarning> export(Path exportDirectory) throws MigrationException {
        exportSystemFiles(exportDirectory);

        Collection<MigrationWarning> migrationWarnings = new ArrayList<>();
        migrationWarnings.addAll(exportSecurity(exportDirectory));

        return migrationWarnings;
    }

    private Collection<MigrationWarning> exportSecurity(Path exportDirectory) {
        exportSecurityDirectory(exportDirectory);
        exportDirectory(exportDirectory, WS_SECURITY_DIR);
        exportDirectory(exportDirectory, PDP_POLICIES_DIR);

        Collection<MigrationWarning> migrationWarnings = new ArrayList<>();
        migrationWarnings.addAll(exportCrl(exportDirectory));
        migrationWarnings.addAll(exportKeystores(exportDirectory));
        return migrationWarnings;
    }

    private void exportSystemFiles(Path exportDirectory) {
        copyFile(ddfHome.resolve(SYSTEM_PROPERTIES), exportDirectory.resolve(SYSTEM_PROPERTIES));
        copyFile(ddfHome.resolve(USERS_PROPERTIES), exportDirectory.resolve(USERS_PROPERTIES));
    }

    private Collection<MigrationWarning> exportKeystores(Path exportDirectory) {
        Collection<MigrationWarning> migrationWarnings = new ArrayList<>();
        migrationWarnings.addAll(exportExternalFile(exportDirectory, KEYSTORE_SYSTEM_PROP));
        migrationWarnings.addAll(exportExternalFile(exportDirectory, TRUSTSTORE_SYSTEM_PROP));
        return migrationWarnings;
    }

    private Collection<MigrationWarning> exportExternalFile(Path exportDirectory,
            String propertyWithPath) {
        Collection<MigrationWarning> migrationWarnings = new ArrayList<>();
        String keystore = getProperty(propertyWithPath);
        migrationWarnings.addAll(checkIfPathIsMigratable(propertyWithPath, Paths.get(keystore)));
        if (migrationWarnings.isEmpty()) {
            Path source = ddfHome.resolve(keystore);
            copyFile(source, constructDestination(source, exportDirectory));
        }
        return migrationWarnings;
    }

    private Collection<MigrationWarning> exportCrl(Path exportDirectory) {
        Collection<MigrationWarning> migrationWarnings = new ArrayList<>();
        Path encryptionPropertiesFile = ddfHome.resolve(FILE_CONTAINING_CRL_LOCATION);
        Properties properties = readPropertiesFile(encryptionPropertiesFile);
        String crlLocation = properties.getProperty(CRL_PROP_KEY);
        if (StringUtils.isNotBlank(crlLocation)) {
            migrationWarnings.addAll(checkIfPathIsMigratable(CRL_PROP_KEY, Paths.get(crlLocation)));
            if (migrationWarnings.isEmpty()) {
                Path source = ddfHome.resolve(crlLocation);
                copyFile(source, constructDestination(source, exportDirectory));
            }
        } else {
            LOGGER.debug("Unable to find CRL location in [%s] using property [%s].",
                    encryptionPropertiesFile.toString(), CRL_PROP_KEY);
        }
        return migrationWarnings;
    }

    private void exportSecurityDirectory(Path exportDirectory) {
        try {
            Path source = ddfHome.resolve(Paths.get(SECURITY_DIRECTORY));
            Path destination = constructDestination(source, exportDirectory);
            copyDirectory(source, destination);
        } catch (MigrationException e) {
            LOGGER.info(String.format("Unable to copy %s. It doesn't exist.", SECURITY_DIRECTORY), e);
        }
    }

    private Path constructDestination(Path pathToExport, Path exportDirectory) {
        Path destination = exportDirectory.resolve(ddfHome.relativize(pathToExport));
        return destination;
    }

    private void exportDirectory(Path destinationRoot, String directoryToCopy) {
        Path source = ddfHome.resolve(Paths.get(directoryToCopy));
        copyDirectory(source, constructDestination(source, destinationRoot));
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
            String message = String.format("Unable to copy [%s] to [%s].", source.toString(),
                    destination.toString());
            LOGGER.error(message, e);
            throw new MigrationException(message, e);
        }
    }

    private void copyDirectory(Path source, Path destination) throws MigrationException {
        try {
            FileUtils.copyDirectory(source.toFile(), destination.toFile());
        } catch (IOException e) {
            String message = String
                    .format("Unable to copy [%s] to [%s].", source.toAbsolutePath().toString(),
                            source.toAbsolutePath().toString());
            LOGGER.error(message, e);
            throw new MigrationException(message, e);
        }
    }

    /*
        Checks is a file is able to be migrated.  Returns warnings if the path is absolute,
        if the path leads somewhere outside DDF Home, or if there is an issue turning it into
        a real path.
     */
    private Collection<MigrationWarning> checkIfPathIsMigratable(String propertyName, Path path) {
        Collection<MigrationWarning> migrationWarnings = new ArrayList<>();

        try {
            if (path.isAbsolute()) {
                String message = String
                        .format(ABSOLUTE_PATH_WARNING, propertyName, path.toString());
                LOGGER.debug(message);
                migrationWarnings.add(new MigrationWarning(message));
            }  else if (Files.isSymbolicLink(path)) {
                String message = String.format(SYMBOLIC_LINK_PATH_WARNING, propertyName, path.toString());
                LOGGER.debug(message);
                migrationWarnings.add(new MigrationWarning(message));

            } else if (!getRealPath(ddfHome.resolve(path)).startsWith(ddfHome)) {
                String message = String.format(OUTSIDE_PATH_WARNING, propertyName, path.toString(),
                        ddfHome.toString());
                LOGGER.debug(message);
                migrationWarnings.add(new MigrationWarning(message));
            }
        } catch (MigrationException e) {
            String message = String.format(UNREAL_PATH_WARNING, propertyName, path.toString());
            LOGGER.debug(message);
            migrationWarnings.add(new MigrationWarning(message));
        } catch (UnsupportedOperationException e) {
            String message = String.format(SYMBOLIC_LINK_PATH_WARNING, propertyName, path.toString());
            LOGGER.debug(message);
            migrationWarnings.add(new MigrationWarning(message));
        }

        return migrationWarnings;
    }

    Properties readPropertiesFile(Path propertiesFile) throws MigrationException {
        Properties properties = new Properties();
        try (InputStream inputStream = new FileInputStream(propertiesFile.toString())) {
            properties.load(inputStream);
            return properties;
        } catch (IOException e) {
            String message = String
                    .format("Unable to read properties file [%s].", propertiesFile.toString());
            LOGGER.error(message, e);
            throw new MigrationException(message, e);
        }
    }

    Path getRealPath(Path path) throws MigrationException {
        try {
            Path realPath = path.toRealPath();
            return realPath;
        } catch (IOException e) {
            String message = String
                    .format("Unable to construct real path from [%s].", path.toString());
            LOGGER.error(message, e);
            throw new MigrationException(message, e);
        }
    }
}
