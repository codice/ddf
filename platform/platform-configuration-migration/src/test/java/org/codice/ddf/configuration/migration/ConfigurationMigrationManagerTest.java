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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.codice.ddf.configuration.admin.ConfigurationAdminMigration;
import org.codice.ddf.configuration.status.ConfigurationFileException;
import org.codice.ddf.configuration.status.MigrationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ConfigurationMigrationManager.class)
public class ConfigurationMigrationManagerTest {
    @Mock
    ConfigurationAdminMigration configurationAdminMigration;

    @Mock
    SystemConfigurationMigration systemConfigurationMigration;

    Path exportPath = Paths.get("/export/dir");

    @Before
    public void setup() {
        mockStatic(Files.class);
    }

    @Test
    public void export() throws Exception {
        when(Files.createDirectories(exportPath)).thenReturn(exportPath);

        ConfigurationMigrationManager configurationMigrationManager =
                new ConfigurationMigrationManager(configurationAdminMigration,
                        systemConfigurationMigration);

        configurationMigrationManager.export(exportPath);

        verifyStatic();
        Files.createDirectories(exportPath);

        verify(configurationAdminMigration, times(1)).export(exportPath);
        verify(systemConfigurationMigration, times(1)).export(exportPath);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullConfigurationAdminMigrator() {
        new ConfigurationMigrationManager(null, systemConfigurationMigration);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullSystemConfigurationMigrator() {
        new ConfigurationMigrationManager(configurationAdminMigration, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void exportWithNullPath() throws Exception {
        ConfigurationMigrationManager configurationMigrationManager =
                new ConfigurationMigrationManager(configurationAdminMigration,
                        systemConfigurationMigration);

        configurationMigrationManager.export(null);
    }

    @Test(expected = MigrationException.class)
    public void exportFailsToCreateDirectory() throws Exception {
        when(Files.createDirectories(exportPath)).thenThrow(new IOException());

        ConfigurationMigrationManager configurationMigrationManager =
                new ConfigurationMigrationManager(configurationAdminMigration,
                        systemConfigurationMigration);

        configurationMigrationManager.export(exportPath);
    }

    @Test(expected = MigrationException.class)
    public void exportWhenConfigurationAdminMigratorThrowsIOException() throws Exception {
        when(Files.createDirectories(exportPath)).thenReturn(exportPath);
        doThrow(new IOException()).when(configurationAdminMigration)
                .export(exportPath);

        ConfigurationMigrationManager configurationMigrationManager =
                new ConfigurationMigrationManager(configurationAdminMigration,
                        systemConfigurationMigration);

        configurationMigrationManager.export(exportPath);
    }

    @Test(expected = MigrationException.class)
    public void exportWhenConfigurationAdminMigratorThrowsConfigurationFileException()
            throws Exception {
        when(Files.createDirectories(exportPath)).thenReturn(exportPath);
        doThrow(new ConfigurationFileException("")).when(configurationAdminMigration)
                .export(exportPath);

        ConfigurationMigrationManager configurationMigrationManager =
                new ConfigurationMigrationManager(configurationAdminMigration,
                        systemConfigurationMigration);

        configurationMigrationManager.export(exportPath);
    }

    @Test(expected = MigrationException.class)
    public void exportWhenConfigurationAdminMigratorThrowsRuntimeException() throws Exception {
        when(Files.createDirectories(exportPath)).thenReturn(exportPath);
        doThrow(new RuntimeException("")).when(configurationAdminMigration)
                .export(exportPath);

        ConfigurationMigrationManager configurationMigrationManager =
                new ConfigurationMigrationManager(configurationAdminMigration,
                        systemConfigurationMigration);

        configurationMigrationManager.export(exportPath);
    }

    @Test(expected = MigrationException.class)
    public void exportWhenSystemConfigurationMigratorThrowsMigrationException() throws Exception {
        when(Files.createDirectories(exportPath)).thenReturn(exportPath);
        doThrow(new MigrationException("")).when(systemConfigurationMigration)
                .export(exportPath);

        ConfigurationMigrationManager configurationMigrationManager =
                new ConfigurationMigrationManager(configurationAdminMigration,
                        systemConfigurationMigration);

        configurationMigrationManager.export(exportPath);
    }

    @Test(expected = MigrationException.class)
    public void exportWhenSystemConfigurationMigratorThrowsRuntimeException() throws Exception {
        when(Files.createDirectories(exportPath)).thenReturn(exportPath);
        doThrow(new RuntimeException("")).when(systemConfigurationMigration)
                .export(exportPath);

        ConfigurationMigrationManager configurationMigrationManager =
                new ConfigurationMigrationManager(configurationAdminMigration,
                        systemConfigurationMigration);

        configurationMigrationManager.export(exportPath);
    }
}
