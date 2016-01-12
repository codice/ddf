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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
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
import java.util.Collection;
import java.util.function.Supplier;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.codice.ddf.configuration.admin.ConfigurationAdminMigration;
import org.codice.ddf.configuration.status.MigrationException;
import org.codice.ddf.configuration.status.MigrationWarning;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableList;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ConfigurationMigrationManager.class)
public class ConfigurationMigrationManagerTest {

    private static ObjectName configMigrationServiceObjectName;

    @Mock
    ConfigurationAdminMigration configurationAdminMigration;

    @Mock
    SystemConfigurationMigration systemConfigurationMigration;

    @Mock
    MBeanServer mBeanServer;

    Path exportPath = Paths.get("/export/dir");

    @BeforeClass
    public static void setupClass() throws MalformedObjectNameException {
        configMigrationServiceObjectName = new ObjectName(
                ConfigurationMigrationManager.class.getName() + ":service=configuration-migration");
    }

    @Before
    public void setup() {
        mockStatic(Files.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullConfigurationAdminMigrator() {
        new ConfigurationMigrationManager(null, systemConfigurationMigration, mBeanServer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullSystemConfigurationMigrator() {
        new ConfigurationMigrationManager(configurationAdminMigration, null, mBeanServer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullMBeanServer() {
        new ConfigurationMigrationManager(configurationAdminMigration,
                systemConfigurationMigration,
                null);
    }

    @Test
    public void init() throws Exception {
        ConfigurationMigrationManager configurationMigrationManager =
                new ConfigurationMigrationManager(configurationAdminMigration,
                        systemConfigurationMigration,
                        mBeanServer);
        configurationMigrationManager.init();

        verify(mBeanServer).registerMBean(configurationMigrationManager,
                configMigrationServiceObjectName);
    }

    @Test
    public void initWhenServiceAlreadyRegisteredAsMBean() throws Exception {
        ConfigurationMigrationManager configurationMigrationManager =
                new ConfigurationMigrationManager(configurationAdminMigration,
                        systemConfigurationMigration,
                        mBeanServer);

        when(mBeanServer.registerMBean(configurationMigrationManager,
                configMigrationServiceObjectName)).thenThrow(new InstanceAlreadyExistsException())
                .thenReturn(null);

        configurationMigrationManager.init();

        verify(mBeanServer, times(2)).registerMBean(configurationMigrationManager,
                configMigrationServiceObjectName);
        verify(mBeanServer).unregisterMBean(configMigrationServiceObjectName);
    }

    @Test(expected = MBeanRegistrationException.class)
    public void initWhenMBeanUnregistrationFails() throws Exception {
        ConfigurationMigrationManager configurationMigrationManager =
                new ConfigurationMigrationManager(configurationAdminMigration,
                        systemConfigurationMigration,
                        mBeanServer);

        when(mBeanServer.registerMBean(configurationMigrationManager,
                configMigrationServiceObjectName)).thenThrow(new InstanceAlreadyExistsException());
        doThrow(new MBeanRegistrationException(new Exception())).when(mBeanServer)
                .unregisterMBean(configMigrationServiceObjectName);

        configurationMigrationManager.init();
    }

    @Test(expected = MBeanRegistrationException.class)
    public void initWhenMBeanReRegistrationFails() throws Exception {
        ConfigurationMigrationManager configurationMigrationManager =
                new ConfigurationMigrationManager(configurationAdminMigration,
                        systemConfigurationMigration,
                        mBeanServer);

        when(mBeanServer.registerMBean(configurationMigrationManager,
                configMigrationServiceObjectName)).thenThrow(new InstanceAlreadyExistsException(),
                new MBeanRegistrationException(new Exception()));

        configurationMigrationManager.init();
    }

    @Test
    public void exportWithPath() throws Exception {
        ConfigurationMigrationManager configurationMigrationManager =
                new ConfigurationMigrationManager(configurationAdminMigration,
                        systemConfigurationMigration,
                        mBeanServer);

        Collection<MigrationWarning> migrationWarnings =
                export(() -> configurationMigrationManager.export(exportPath));
        assertThat(migrationWarnings, is(empty()));

    }

    @Test
    public void exportWithString() throws Exception {
        ConfigurationMigrationManager configurationMigrationManager =
                new ConfigurationMigrationManager(configurationAdminMigration,
                        systemConfigurationMigration,
                        mBeanServer);

        Collection<MigrationWarning> migrationWarnings =
                export(() -> configurationMigrationManager.export("/export/dir"));
        assertThat(migrationWarnings, is(empty()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void exportWithNullPath() throws Exception {
        ConfigurationMigrationManager configurationMigrationManager =
                new ConfigurationMigrationManager(configurationAdminMigration,
                        systemConfigurationMigration,
                        mBeanServer);

        configurationMigrationManager.export((Path) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void exportWithNullPathString() throws Exception {
        ConfigurationMigrationManager configurationMigrationManager =
                new ConfigurationMigrationManager(configurationAdminMigration,
                        systemConfigurationMigration,
                        mBeanServer);

        configurationMigrationManager.export((String) null);
    }

    @Test
    public void exportWithWarnings() {
        ConfigurationMigrationManager configurationMigrationManager =
                new ConfigurationMigrationManager(configurationAdminMigration,
                        systemConfigurationMigration,
                        mBeanServer);

        MigrationWarning migrationWarning = new MigrationWarning("");
        when(systemConfigurationMigration.export(exportPath)).thenReturn(ImmutableList.of(
                migrationWarning));

        Collection<MigrationWarning> migrationWarnings = configurationMigrationManager.export(
                exportPath);
        assertThat(migrationWarnings, contains(migrationWarning));
    }

    @Test(expected = MigrationException.class)
    public void exportFailsToCreateDirectory() throws Exception {
        when(Files.createDirectories(exportPath)).thenThrow(new IOException());

        ConfigurationMigrationManager configurationMigrationManager =
                new ConfigurationMigrationManager(configurationAdminMigration,
                        systemConfigurationMigration,
                        mBeanServer);

        configurationMigrationManager.export(exportPath);
    }

    @Test(expected = MigrationException.class)
    public void exportWhenConfigurationAdminMigratorThrowsIOException() throws Exception {
        when(Files.createDirectories(exportPath)).thenReturn(exportPath);
        doThrow(new IOException()).when(configurationAdminMigration)
                .export(exportPath);

        ConfigurationMigrationManager configurationMigrationManager =
                new ConfigurationMigrationManager(configurationAdminMigration,
                        systemConfigurationMigration,
                        mBeanServer);

        configurationMigrationManager.export(exportPath);
    }

    @Test(expected = MigrationException.class)
    public void exportWhenConfigurationAdminMigratorThrowsConfigurationFileException()
            throws Exception {
        when(Files.createDirectories(exportPath)).thenReturn(exportPath);
        doThrow(new MigrationException("")).when(configurationAdminMigration)
                .export(exportPath);

        ConfigurationMigrationManager configurationMigrationManager =
                new ConfigurationMigrationManager(configurationAdminMigration,
                        systemConfigurationMigration,
                        mBeanServer);

        configurationMigrationManager.export(exportPath);
    }

    @Test(expected = MigrationException.class)
    public void exportWhenConfigurationAdminMigratorThrowsRuntimeException() throws Exception {
        when(Files.createDirectories(exportPath)).thenReturn(exportPath);
        doThrow(new RuntimeException("")).when(configurationAdminMigration)
                .export(exportPath);

        ConfigurationMigrationManager configurationMigrationManager =
                new ConfigurationMigrationManager(configurationAdminMigration,
                        systemConfigurationMigration,
                        mBeanServer);

        configurationMigrationManager.export(exportPath);
    }

    @Test(expected = MigrationException.class)
    public void exportWhenSystemConfigurationMigratorThrowsMigrationException() throws Exception {
        when(Files.createDirectories(exportPath)).thenReturn(exportPath);
        doThrow(new MigrationException("")).when(systemConfigurationMigration)
                .export(exportPath);

        ConfigurationMigrationManager configurationMigrationManager =
                new ConfigurationMigrationManager(configurationAdminMigration,
                        systemConfigurationMigration,
                        mBeanServer);

        configurationMigrationManager.export(exportPath);
    }

    @Test(expected = MigrationException.class)
    public void exportWhenSystemConfigurationMigratorThrowsRuntimeException() throws Exception {
        when(Files.createDirectories(exportPath)).thenReturn(exportPath);
        doThrow(new RuntimeException("")).when(systemConfigurationMigration)
                .export(exportPath);

        ConfigurationMigrationManager configurationMigrationManager =
                new ConfigurationMigrationManager(configurationAdminMigration,
                        systemConfigurationMigration,
                        mBeanServer);

        configurationMigrationManager.export(exportPath);
    }

    private Collection<MigrationWarning> export(Supplier<Collection<MigrationWarning>> exportCall)
            throws Exception {
        when(Files.createDirectories(exportPath)).thenReturn(exportPath);

        Collection<MigrationWarning> migrationWarnings = exportCall.get();

        verifyStatic();
        Files.createDirectories(exportPath);

        verify(configurationAdminMigration, times(1)).export(exportPath);
        verify(systemConfigurationMigration, times(1)).export(exportPath);

        return migrationWarnings;
    }
}
