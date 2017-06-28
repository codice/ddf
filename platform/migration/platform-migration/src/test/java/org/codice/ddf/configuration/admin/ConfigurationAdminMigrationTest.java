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
package org.codice.ddf.configuration.admin;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.codice.ddf.configuration.status.ConfigurationFileException;
import org.codice.ddf.migration.MigrationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

@RunWith(MockitoJUnitRunner.class)
@PrepareForTest({ConfigurationAdminMigration.class, FileUtils.class, Files.class})
public class ConfigurationAdminMigrationTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private static final Path CONFIG_FILES_EXPORT_PATH = Paths.get("/root/etc/exported/etc");

    private static final Path EXPORTED_DIRECTORY_PATH = Paths.get("/root/etc/exported");

    private static final String CONFIGURATION_FILE_EXTENSION = ".config";

    private static final String CONFIG_PID = "pid";

    private static final Path CONFIG_FILE_PATH = Paths.get(
            "/root/etc/exported/etc/" + CONFIG_PID + CONFIGURATION_FILE_EXTENSION);

    private static final String CONFIG_FILE_PATH1 = CONFIG_PID + "1" + CONFIGURATION_FILE_EXTENSION;

    private static final String CONFIG_FILE_PATH2 = CONFIG_PID + "2" + CONFIGURATION_FILE_EXTENSION;

    private static final Path CONFIG_PATH1 = (Paths.get("/root/etc")).resolve(CONFIG_FILE_PATH1);

    private static final Path CONFIG_PATH2 = (Paths.get("/root/etc")).resolve(CONFIG_FILE_PATH2);

    @Mock
    private DirectoryStream<Path> configurationDirectoryStream;

    @Mock
    private ConfigurationFileFactory configurationFileFactory;

    @Mock
    private ConfigurationAdmin configurationAdmin;

    @Mock
    private Iterator<Path> configFilesIterator;

    @Mock
    private ConfigurationFile configFile1;

    @Mock
    private ConfigurationFile configFile2;

    @Mock
    private Configuration configuration;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Files.class);
        PowerMockito.mockStatic(FileUtils.class);

        when(configuration.getPid()).thenReturn(CONFIG_PID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDirectoryStream() {
        new ConfigurationAdminMigration(null, configurationFileFactory,
                configurationAdmin,
                CONFIGURATION_FILE_EXTENSION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullConfigurationFileFactory() {
        new ConfigurationAdminMigration(configurationDirectoryStream, null,
                configurationAdmin,
                CONFIGURATION_FILE_EXTENSION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullConfigurationAdmin() {
        new ConfigurationAdminMigration(configurationDirectoryStream, configurationFileFactory,
                null,
                CONFIGURATION_FILE_EXTENSION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullConfigurationFileExtension() {
        new ConfigurationAdminMigration(configurationDirectoryStream, configurationFileFactory,
                configurationAdmin,
                null);
    }

    @Test
    public void testExport()
            throws IOException, InvalidSyntaxException, ConfigurationFileException {

        when(configurationAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {
                configuration});
        when(configurationFileFactory.createConfigurationFile(anyObject())).thenReturn(
                configFile1);

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();

        configurationAdminMigration.export(EXPORTED_DIRECTORY_PATH);

        verifyStatic();
        FileUtils.forceMkdir(CONFIG_FILES_EXPORT_PATH.toFile());
        verify(configFile1, atLeastOnce()).exportConfig(CONFIG_FILE_PATH.toString());
    }

    @Test
    public void testExportWhenEtcDirectoryCreationFails() throws Exception {

        try {
            PowerMockito.doThrow(new IOException())
                    .when(FileUtils.class);
            FileUtils.forceMkdir(CONFIG_FILES_EXPORT_PATH.toFile());

            ConfigurationAdminMigration configurationAdminMigration =
                    createConfigurationAdminMigratorWithNoFiles();

            configurationAdminMigration.export(EXPORTED_DIRECTORY_PATH);

        } catch (IOException e) {
            verifyStatic();
            FileUtils.forceMkdir(CONFIG_FILES_EXPORT_PATH.toFile());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExportWithNullExportDirectory() throws IOException, ConfigurationFileException {

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        configurationAdminMigration.export(null);
    }

    @Test(expected = MigrationException.class)
    public void testExportListConfigurationsIOException()
            throws IOException, InvalidSyntaxException, ConfigurationFileException {

        doThrow(new IOException()).when(configurationAdmin)
                .listConfigurations(anyString());
        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        configurationAdminMigration.export(EXPORTED_DIRECTORY_PATH);
    }

    @Test(expected = MigrationException.class)
    public void testExportListConfigurationsInvalidSyntaxException()
            throws IOException, InvalidSyntaxException, ConfigurationFileException {

        doThrow(new InvalidSyntaxException("", "")).when(configurationAdmin)
                .listConfigurations(anyString());
        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        configurationAdminMigration.export(EXPORTED_DIRECTORY_PATH);
    }

    @Test(expected = MigrationException.class)
    public void testExportConfigurationFileException()
            throws IOException, InvalidSyntaxException, ConfigurationFileException {

        when(configurationAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {
                configuration});
        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        when(configurationFileFactory.createConfigurationFile(anyObject())).thenThrow(
                new ConfigurationFileException(""));
        configurationAdminMigration.export(EXPORTED_DIRECTORY_PATH);
    }

    @Test(expected = MigrationException.class)
    public void testConfigFileExportConfigIOException()
            throws IOException, InvalidSyntaxException, ConfigurationFileException {

        when(configurationAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {
                configuration});
        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        when(configurationFileFactory.createConfigurationFile(anyObject())).thenReturn(
                configFile1);
        doThrow(new IOException()).when(configFile1)
                .exportConfig(anyString());
        configurationAdminMigration.export(EXPORTED_DIRECTORY_PATH);
    }

    @Test
    public void testExportWhenNoConfigurations()
            throws IOException, InvalidSyntaxException, ConfigurationFileException {

        when(configurationAdmin.listConfigurations(anyString())).thenReturn(null);
        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        configurationAdminMigration.export(EXPORTED_DIRECTORY_PATH);
        verify(configurationFileFactory,
                never()).createConfigurationFile(anyObject());
    }

    private ConfigurationAdminMigration createConfigurationAdminMigratorWithNoFiles() {
        when(configurationDirectoryStream.iterator()).thenReturn(configFilesIterator);
        when(configFilesIterator.hasNext()).thenReturn(false);

        return new ConfigurationAdminMigration(configurationDirectoryStream,
                configurationFileFactory,
                configurationAdmin,
                CONFIGURATION_FILE_EXTENSION);
    }

}
