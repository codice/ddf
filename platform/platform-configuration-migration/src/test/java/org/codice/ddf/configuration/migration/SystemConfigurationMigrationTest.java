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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.codice.ddf.configuration.status.MigrationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

//import org.junit.runner.RunWith;
//import org.powermock.modules.junit4.PowerMockRunner;

/**
 * If you want to run any of these tests in the debugger, you will need to do the following:
 * 1) uncomment @RunWith(PowerMockRunner.class)
 * 2) uncomment import org.powermock.modules.junit4.PowerMockRunner;
 * 3) uncomment import org.junit.runner.RunWith;
 * 4) comment out:
 *        @Rule
 *        public PowerMockRule rule = new PowerMockRule();
 * 4) comment out import org.powermock.modules.junit4.rule.PowerMockRule;
 *
 * If you want to see Jacoco code coverage, the following must be commented out:
 * 1) import org.junit.runner.RunWith;
 * 2) import org.powermock.modules.junit4.PowerMockRunner;
 * 3) @RunWith(PowerMockRunner.class)
 *
 */
//@RunWith(PowerMockRunner.class)
@PrepareForTest({SystemConfigurationMigration.class, FileUtils.class})
public class SystemConfigurationMigrationTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private static final Path DDF_BASE_DIR = Paths.get("ddf");

    private static final String KEYSTORE_REL_PATH = "etc/keystores/keystore.jks";

    private static final String INVALID_KEYSTORE_REL_PATH = "invalidKeystore.jks";

    private static final String KEYSTORES_DIR = "etc/keystores";

    private static final String KEYSTORE_SYSTEM_PROP = "javax.net.ssl.keyStore";

    private static final String TRUSTSTORE_REL_PATH = "etc/keystores/truststore.jks";

    private static final String INVALID_TRUSTSTORE_REL_PATH = "invalidTruststore.jks";

    private static final String TRUSTSTORE_SYSTEM_PROP = "javax.net.ssl.trustStore";

    private static final String WS_SECURITY_DIR_REL_PATH = "etc/ws-security";

    private static final String WS_SECURITY_SERVER_DIR = "etc/ws-security/server";

    private static final String WS_SECURITY_SERVER_ENC_PROP_FILE_REL_PATH =
            WS_SECURITY_SERVER_DIR + "/encryption.properties";

    private static final String WS_SECURITY_SERVER_SIG_PROP_FILE_REL_PATH =
            WS_SECURITY_SERVER_DIR + "/signature.properties";

    private static final String WS_SECURITY_ISSUER_DIR = "etc/ws-security/issuer";

    private static final String WS_SECURITY_ISSUER_ENC_PROP_FILE_REL_PATH =
            WS_SECURITY_ISSUER_DIR + "/encryption.properties";

    private static final String WS_SECURITY_ISSUER_SIG_PROP_FILE_REL_PATH =
            WS_SECURITY_ISSUER_DIR + "/signature.properties";

    private static final String SECURITY_DIRECTORY_REL_PATH = "security";

    private static final String PDP_POLICIES_DIR_REL_PATH = "etc/pdp";

    private static final String SYSTEM_PROPERTIES_REL_PATH = "etc/system.properties";

    private static final String USERS_PROPERTIES_REL_PATH = "etc/users.properties";

    private static final String CRL_DIR = "etc/certs/demoCA/crl";

    private static final String CRL_REL_PATH = "etc/certs/demoCA/crl/crl.pem";

    private static final String INVALID_CRL_REL_PATH = "invalidCrl.pem";

    private static final String CRL_PROP_KEY = "org.apache.ws.security.crypto.merlin.x509crl.file";

    private static final String EXPORTED_REL_PATH = "etc/exported";

    private Path ddfHome;

    private Path exportDirectory;

    private Path keystoreAbsolutePath;

    private Path truststoreAbsolutePath;

    private Path crlAbsolutePath;

    private Path existingFilePath;

    private Path symLinkPath;

    @Before
    public void setup() throws Exception {
        mockStatic(FileUtils.class);
        ddfHome = Paths.get(tempDir.getRoot()
                .getAbsolutePath() + File.separator + DDF_BASE_DIR);
        exportDirectory = ddfHome.resolve(EXPORTED_REL_PATH);
        keystoreAbsolutePath = ddfHome.resolve(KEYSTORE_REL_PATH);
        truststoreAbsolutePath = ddfHome.resolve(TRUSTSTORE_REL_PATH);
        crlAbsolutePath = ddfHome.resolve(CRL_REL_PATH);
    }

    @Test
    public void testSymbolicLinkPath() throws Exception {

        System.setProperty(KEYSTORE_SYSTEM_PROP, KEYSTORE_REL_PATH);
        System.setProperty(TRUSTSTORE_SYSTEM_PROP, TRUSTSTORE_REL_PATH);
        SystemConfigurationMigration securityConfigurationMigrator =
                new SystemConfigurationMigration(createTempDdf()) {

                    @Override
                    Properties readPropertiesFile(Path propertiesFile) throws MigrationException {
                        Properties properties = new Properties();
                        properties.setProperty(CRL_PROP_KEY, symLinkPath.toString());
                        return properties;
                    }
                };

        // Perform Test
        assertFalse("A migration warning wasn't returned.",
                securityConfigurationMigrator.export(symLinkPath)
                        .isEmpty());

    }

    @Test
    public void testExportValidRelativePaths() throws Exception {
        // Setup
        System.setProperty(KEYSTORE_SYSTEM_PROP, KEYSTORE_REL_PATH);
        System.setProperty(TRUSTSTORE_SYSTEM_PROP, TRUSTSTORE_REL_PATH);
        SystemConfigurationMigration securityConfigurationMigrator =
                new SystemConfigurationMigration(createTempDdf()) {
                    @Override
                    Properties readPropertiesFile(Path propertiesFile) throws MigrationException {
                        Properties properties = new Properties();
                        properties.setProperty(CRL_PROP_KEY, CRL_REL_PATH);
                        return properties;
                    }
                };

        // Perform Test
        securityConfigurationMigrator.export(exportDirectory);

        // Verify
        ArgumentCaptor<File> sourceDirCaptor = ArgumentCaptor.forClass(File.class);
        ArgumentCaptor<File> destinationDirCaptor = ArgumentCaptor.forClass(File.class);
        verifyStatic(times(3));
        FileUtils.copyDirectory(sourceDirCaptor.capture(), destinationDirCaptor.capture());
        List<File> destinationDirs = destinationDirCaptor.getAllValues();
        assertDestinationDirectories(destinationDirs);

        ArgumentCaptor<File> sourceFileCaptor = ArgumentCaptor.forClass(File.class);
        ArgumentCaptor<File> destinationFileCaptor = ArgumentCaptor.forClass(File.class);
        verifyStatic(times(5));
        FileUtils.copyFile(sourceFileCaptor.capture(), destinationFileCaptor.capture());
        List<File> destinationFiles = destinationFileCaptor.getAllValues();
        assertDestinationFiles(destinationFiles);
    }

    /**
     * Verify that if an absolute path is encountered during the export, a warning is returned.
     */
    @Test
    public void testExportAbsolutePaths() throws Exception {
        // Setup
        System.setProperty(KEYSTORE_SYSTEM_PROP, keystoreAbsolutePath.toString());
        System.setProperty(TRUSTSTORE_SYSTEM_PROP, truststoreAbsolutePath.toString());
        SystemConfigurationMigration securityConfigurationMigrator =
                new SystemConfigurationMigration(createTempDdf()) {

                    @Override
                    Properties readPropertiesFile(Path propertiesFile) throws MigrationException {
                        Properties properties = new Properties();
                        properties.setProperty(CRL_PROP_KEY, crlAbsolutePath.toString());
                        return properties;
                    }
                };

        // Perform Test
        assertFalse("A migration warning wasn't returned.",
                securityConfigurationMigrator.export(exportDirectory)
                        .isEmpty());
    }

    @Test
    public void testExportTrustoreAbsolutePath() throws Exception {
        // Setup
        Path truststoreAbsolutePath = ddfHome.resolve(Paths.get(TRUSTSTORE_REL_PATH));
        System.setProperty(KEYSTORE_SYSTEM_PROP, KEYSTORE_REL_PATH);
        System.setProperty(TRUSTSTORE_SYSTEM_PROP, truststoreAbsolutePath.toString());
        SystemConfigurationMigration securityConfigurationMigrator =
                new SystemConfigurationMigration(createTempDdf()) {

                    @Override
                    Properties readPropertiesFile(Path propertiesFile) throws MigrationException {
                        Properties properties = new Properties();
                        properties.setProperty(CRL_PROP_KEY, CRL_REL_PATH);
                        return properties;
                    }
                };

        // Perform Test
        assertFalse("A migration warning wasn't returned.",
                securityConfigurationMigrator.export(exportDirectory)
                        .isEmpty());
    }

    @Test
    public void testExportInvalidTrustoreRelativePath() throws Exception {
        // Setup
        Path invalidTruststoreRelativepath = tempDir.getRoot()
                .toPath()
                .resolve(INVALID_TRUSTSTORE_REL_PATH);
        System.setProperty(KEYSTORE_SYSTEM_PROP, KEYSTORE_REL_PATH);
        System.setProperty(TRUSTSTORE_SYSTEM_PROP,
                ddfHome.relativize(invalidTruststoreRelativepath)
                        .toString());
        SystemConfigurationMigration securityConfigurationMigrator =
                new SystemConfigurationMigration(createTempDdf()) {

                    @Override
                    Properties readPropertiesFile(Path propertiesFile) throws MigrationException {
                        Properties properties = new Properties();
                        properties.setProperty(CRL_PROP_KEY, CRL_REL_PATH);
                        return properties;
                    }
                };

        // Perform Test
        assertFalse("A migration warning wasn't returned.",
                securityConfigurationMigrator.export(exportDirectory)
                        .isEmpty());
    }

    @Test
    public void testExportKeystoreAbsolutePath() throws Exception {
        // Setup
        Path keystoreAbsolutePath = ddfHome.resolve(Paths.get(KEYSTORE_REL_PATH));
        System.setProperty(KEYSTORE_SYSTEM_PROP, keystoreAbsolutePath.toString());
        System.setProperty(TRUSTSTORE_SYSTEM_PROP, TRUSTSTORE_REL_PATH);
        SystemConfigurationMigration securityConfigurationMigrator =
                new SystemConfigurationMigration(createTempDdf()) {

                    @Override
                    Properties readPropertiesFile(Path propertiesFile) throws MigrationException {
                        Properties properties = new Properties();
                        properties.setProperty(CRL_PROP_KEY, CRL_REL_PATH);
                        return properties;
                    }
                };

        // Perform Test
        assertFalse("A migration warning wasn't returned.",
                securityConfigurationMigrator.export(exportDirectory)
                        .isEmpty());
    }

    @Test
    public void testExportInvalidKeystoreRelativePath() throws Exception {
        // Setup
        Path invalidKeystoreRelativepath = tempDir.getRoot()
                .toPath()
                .resolve(INVALID_KEYSTORE_REL_PATH);
        System.setProperty(KEYSTORE_SYSTEM_PROP,
                ddfHome.relativize(invalidKeystoreRelativepath)
                        .toString());
        System.setProperty(TRUSTSTORE_SYSTEM_PROP, TRUSTSTORE_REL_PATH);
        SystemConfigurationMigration securityConfigurationMigrator =
                new SystemConfigurationMigration(createTempDdf()) {

                    @Override
                    Properties readPropertiesFile(Path propertiesFile) throws MigrationException {
                        Properties properties = new Properties();
                        properties.setProperty(CRL_PROP_KEY, CRL_REL_PATH);
                        return properties;
                    }
                };

        // Perform Test
        assertFalse("A migration warning wasn't returned.",
                securityConfigurationMigrator.export(exportDirectory)
                        .isEmpty());
    }

    @Test
    public void testExportCrlAbsolutePath() throws Exception {
        // Setup
        Path crlAbsolutePath = ddfHome.resolve(Paths.get(KEYSTORE_REL_PATH));
        System.setProperty(KEYSTORE_SYSTEM_PROP, KEYSTORE_REL_PATH);
        System.setProperty(TRUSTSTORE_SYSTEM_PROP, TRUSTSTORE_REL_PATH);
        SystemConfigurationMigration securityConfigurationMigrator =
                new SystemConfigurationMigration(createTempDdf()) {

                    @Override
                    Properties readPropertiesFile(Path propertiesFile) throws MigrationException {
                        Properties properties = new Properties();
                        properties.setProperty(CRL_PROP_KEY, crlAbsolutePath.toString());
                        return properties;
                    }
                };

        // Perform Test
        assertFalse("A migration warning wasn't returned.",
                securityConfigurationMigrator.export(exportDirectory)
                        .isEmpty());
    }

    @Test
    public void testExportInvalidCrlRelativePath() throws Exception {
        // Setup
        Path invalidCrlRelativePath = tempDir.getRoot()
                .toPath()
                .resolve(INVALID_CRL_REL_PATH);
        System.setProperty(KEYSTORE_SYSTEM_PROP, KEYSTORE_REL_PATH);
        System.setProperty(TRUSTSTORE_SYSTEM_PROP, TRUSTSTORE_REL_PATH);
        SystemConfigurationMigration securityConfigurationMigrator =
                new SystemConfigurationMigration(createTempDdf()) {

                    @Override
                    Properties readPropertiesFile(Path propertiesFile) throws MigrationException {
                        Properties properties = new Properties();
                        properties.setProperty(CRL_PROP_KEY,
                                ddfHome.relativize(invalidCrlRelativePath)
                                        .toString());
                        return properties;
                    }
                };

        // Perform Test
        assertFalse("A migration warning wasn't returned.",
                securityConfigurationMigrator.export(exportDirectory)
                        .isEmpty());
    }

    @Test(expected = MigrationException.class)
    public void testExportExceptionReadingCrl() throws Exception {
        // Setup
        System.setProperty(KEYSTORE_SYSTEM_PROP, KEYSTORE_REL_PATH);
        System.setProperty(TRUSTSTORE_SYSTEM_PROP, TRUSTSTORE_REL_PATH);
        SystemConfigurationMigration securityConfigurationMigrator =
                new SystemConfigurationMigration(createTempDdf()) {

                    @Override
                    Properties readPropertiesFile(Path propertiesFile) throws MigrationException {
                        throw new MigrationException("Error reading CRL");
                    }
                };

        // Perform Test
        securityConfigurationMigrator.export(exportDirectory);
    }

    /**
     * The crl path can be commented out in <ddf home>/etc/ws-security/server/encryption.properties
     * if not in use. This test verifies that no exception is thrown if the crl path is not found.
     */
    @Test
    public void testExportCrlPathNotSet() throws Exception {
        // Setup
        System.setProperty(KEYSTORE_SYSTEM_PROP, KEYSTORE_REL_PATH);
        System.setProperty(TRUSTSTORE_SYSTEM_PROP, TRUSTSTORE_REL_PATH);
        SystemConfigurationMigration securityConfigurationMigrator =
                new SystemConfigurationMigration(createTempDdf()) {

                    @Override
                    Properties readPropertiesFile(Path propertiesFile) throws MigrationException {
                        Properties properties = new Properties();
                        return properties;
                    }
                };

        // Perform Test
        securityConfigurationMigrator.export(exportDirectory);
    }

    @Test(expected = MigrationException.class)
    public void testExportExceptionCopyingFile() throws Exception {
        // Setup
        System.setProperty(KEYSTORE_SYSTEM_PROP, KEYSTORE_REL_PATH);
        System.setProperty(TRUSTSTORE_SYSTEM_PROP, TRUSTSTORE_REL_PATH);
        doThrow(new IOException("Error copying file")).when(FileUtils.class);
        FileUtils.copyFile(any(File.class), any(File.class));
        SystemConfigurationMigration securityConfigurationMigrator =
                new SystemConfigurationMigration(createTempDdf()) {

                    @Override
                    Properties readPropertiesFile(Path propertiesFile) throws MigrationException {
                        Properties properties = new Properties();
                        properties.setProperty(CRL_PROP_KEY, CRL_REL_PATH);
                        return properties;
                    }
                };

        // Perform Test
        securityConfigurationMigrator.export(exportDirectory);
    }

    @Test(expected = MigrationException.class)
    public void testExportExceptionCopyingDirectory() throws Exception {
        // Setup
        System.setProperty(KEYSTORE_SYSTEM_PROP, KEYSTORE_REL_PATH);
        System.setProperty(TRUSTSTORE_SYSTEM_PROP, TRUSTSTORE_REL_PATH);
        doThrow(new IOException("Error copying directory")).when(FileUtils.class);
        FileUtils.copyDirectory(any(File.class), any(File.class));
        SystemConfigurationMigration securityConfigurationMigrator =
                new SystemConfigurationMigration(createTempDdf()) {

                    @Override
                    Properties readPropertiesFile(Path propertiesFile) throws MigrationException {
                        Properties properties = new Properties();
                        properties.setProperty(CRL_PROP_KEY, CRL_REL_PATH);
                        return properties;
                    }
                };

        // Perform Test
        securityConfigurationMigrator.export(exportDirectory);
    }

    @Test(expected = MigrationException.class)
    public void testExportKeystorePropertyNotSet() throws Exception {
        // Setup
        System.clearProperty(KEYSTORE_SYSTEM_PROP);
        System.setProperty(TRUSTSTORE_SYSTEM_PROP, TRUSTSTORE_REL_PATH);
        SystemConfigurationMigration securityConfigurationMigrator =
                new SystemConfigurationMigration(createTempDdf()) {
                    @Override
                    Properties readPropertiesFile(Path propertiesFile) throws MigrationException {
                        Properties properties = new Properties();
                        properties.setProperty(CRL_PROP_KEY, CRL_REL_PATH);
                        return properties;
                    }
                };

        // Perform Test
        securityConfigurationMigrator.export(exportDirectory);
    }

    @Test(expected = MigrationException.class)
    public void testExportTruststorePropertyNotSet() throws Exception {
        // Setup
        System.setProperty(KEYSTORE_SYSTEM_PROP, KEYSTORE_REL_PATH);
        System.clearProperty(TRUSTSTORE_SYSTEM_PROP);

        SystemConfigurationMigration securityConfigurationMigrator =
                new SystemConfigurationMigration(createTempDdf()) {
                    @Override
                    Properties readPropertiesFile(Path propertiesFile) throws MigrationException {
                        Properties properties = new Properties();
                        properties.setProperty(CRL_PROP_KEY, CRL_REL_PATH);
                        return properties;
                    }
                };

        // Perform Test
        securityConfigurationMigrator.export(exportDirectory);
    }

    @Test(expected = MigrationException.class)
    public void testGetRealPathThrowsException() throws Exception {
        SystemConfigurationMigration securityConfigurationMigrator =
                new SystemConfigurationMigration(createTempDdf());
        securityConfigurationMigrator.getRealPath(ddfHome.resolve("fakeFile.txt"));
    }

    private void assertDestinationFiles(List<File> destinationFiles) {
        List<File> expectedDestinationFiles = new ArrayList<>(5);
        expectedDestinationFiles.add(new File(
                exportDirectory + File.separator + KEYSTORE_REL_PATH));
        expectedDestinationFiles.add(new File(
                exportDirectory + File.separator + TRUSTSTORE_REL_PATH));
        expectedDestinationFiles.add(new File(exportDirectory + File.separator + CRL_REL_PATH));
        expectedDestinationFiles.add(new File(
                exportDirectory + File.separator + SYSTEM_PROPERTIES_REL_PATH));
        expectedDestinationFiles.add(new File(
                exportDirectory + File.separator + USERS_PROPERTIES_REL_PATH));

        assertThat(destinationFiles, containsInAnyOrder(expectedDestinationFiles.toArray()));
    }

    private void assertDestinationDirectories(List<File> destinationDirs) {
        List<File> expectedDestinationDirs = new ArrayList<>(3);
        expectedDestinationDirs.add(new File(
                exportDirectory + File.separator + SECURITY_DIRECTORY_REL_PATH));
        expectedDestinationDirs.add(new File(
                exportDirectory + File.separator + WS_SECURITY_DIR_REL_PATH));
        expectedDestinationDirs.add(new File(
                exportDirectory + File.separator + PDP_POLICIES_DIR_REL_PATH));

        assertThat(destinationDirs, containsInAnyOrder(expectedDestinationDirs.toArray()));
    }

    private Path createTempDdf() throws IOException {

        Path rootTempDir = tempDir.getRoot()
                .toPath();
        Path ddfHome = rootTempDir.resolve(DDF_BASE_DIR);
        Files.createDirectories(ddfHome);
        Files.createDirectories(ddfHome.resolve(SECURITY_DIRECTORY_REL_PATH));
        Files.createDirectories(ddfHome.resolve(KEYSTORES_DIR));
        Files.createFile(ddfHome.resolve(Paths.get(KEYSTORE_REL_PATH)));
        Files.createFile(ddfHome.resolve(Paths.get(TRUSTSTORE_REL_PATH)));
        Files.createDirectories(ddfHome.resolve(WS_SECURITY_SERVER_DIR));
        Files.createFile(ddfHome.resolve(Paths.get(WS_SECURITY_SERVER_ENC_PROP_FILE_REL_PATH)));
        Files.createFile(ddfHome.resolve(Paths.get(WS_SECURITY_SERVER_SIG_PROP_FILE_REL_PATH)));
        Files.createDirectories(ddfHome.resolve(WS_SECURITY_ISSUER_DIR));
        Files.createFile(ddfHome.resolve(Paths.get(WS_SECURITY_ISSUER_ENC_PROP_FILE_REL_PATH)));
        Files.createFile(ddfHome.resolve(Paths.get(WS_SECURITY_ISSUER_SIG_PROP_FILE_REL_PATH)));
        Files.createDirectories(ddfHome.resolve(PDP_POLICIES_DIR_REL_PATH));
        Files.createDirectories(ddfHome.resolve(CRL_DIR));
        Files.createFile(ddfHome.resolve(Paths.get(CRL_REL_PATH)));

        Files.createFile(rootTempDir.resolve(INVALID_KEYSTORE_REL_PATH));
        Files.createFile(rootTempDir.resolve(INVALID_TRUSTSTORE_REL_PATH));
        Files.createFile(rootTempDir.resolve(INVALID_CRL_REL_PATH));

        existingFilePath = Paths.get(ddfHome.resolve("orig.config")
                .toString());
        symLinkPath = Paths.get(ddfHome.resolve("symlink.config")
                .toString());
        Files.createFile(existingFilePath);
        Files.createSymbolicLink(symLinkPath, existingFilePath);

        return ddfHome;
    }
}
