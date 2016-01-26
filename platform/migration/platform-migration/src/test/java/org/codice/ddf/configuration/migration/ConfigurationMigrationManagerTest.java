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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.codice.ddf.configuration.admin.ConfigurationAdminMigration;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationMetadata;
import org.codice.ddf.migration.MigrationWarning;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableList;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ConfigurationMigrationManager.class)
public class ConfigurationMigrationManagerTest {

    private static final String SERVICE_PID_1 = "migratable1";

    private static final String SERVICE_PID_2 = "migratable2";

    private static ObjectName configMigrationServiceObjectName;

    @Mock
    private ConfigurationAdminMigration configurationAdminMigration;

    @Mock
    private SystemConfigurationMigration systemConfigurationMigration;

    @Mock
    private MBeanServer mBeanServer;

    @Mock
    BundleContext bundleContext;

    private Collection<ServiceReference<Migratable>> serviceReferences;

    @Mock
    ServiceReference<Migratable> serviceReference1;

    @Mock
    ServiceReference<Migratable> serviceReference2;

    @Mock
    Migratable migratable1;

    @Mock
    Migratable migratable2;

    MigrationMetadata noWarnings = new MigrationMetadata(ImmutableList.of());

    Path exportPath = Paths.get("/export/dir");

    @BeforeClass
    public static void setupClass() throws MalformedObjectNameException {
        configMigrationServiceObjectName = new ObjectName(
                ConfigurationMigrationManager.class.getName() + ":service=configuration-migration");
    }

    @Before
    public void setup() throws InvalidSyntaxException {
        serviceReferences = new ArrayList<>(2);
        Collections.addAll(serviceReferences, serviceReference1, serviceReference2);

        when(bundleContext.getServiceReferences(Migratable.class, null)).thenReturn(
                serviceReferences);
        when(serviceReference1.getProperty("service.pid")).thenReturn(SERVICE_PID_1);
        when(bundleContext.getService(serviceReference1)).thenReturn(migratable1);
        when(migratable1.export(any(Path.class))).thenReturn(noWarnings);
        when(serviceReference2.getProperty("service.pid")).thenReturn(SERVICE_PID_2);
        when(bundleContext.getService(serviceReference2)).thenReturn(migratable2);
        when(migratable2.export(any(Path.class))).thenReturn(noWarnings);

        mockStatic(Files.class);
        mockStatic(FrameworkUtil.class);
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
                createConfigurationMigrationManager();
        configurationMigrationManager.init();

        verify(mBeanServer).registerMBean(configurationMigrationManager,
                configMigrationServiceObjectName);
    }

    @Test
    public void initWhenServiceAlreadyRegisteredAsMBean() throws Exception {
        ConfigurationMigrationManager configurationMigrationManager =
                createConfigurationMigrationManager();

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
                createConfigurationMigrationManager();

        when(mBeanServer.registerMBean(configurationMigrationManager,
                configMigrationServiceObjectName)).thenThrow(new InstanceAlreadyExistsException());
        doThrow(new MBeanRegistrationException(new Exception())).when(mBeanServer)
                .unregisterMBean(configMigrationServiceObjectName);

        configurationMigrationManager.init();
    }

    @Test(expected = MBeanRegistrationException.class)
    public void initWhenMBeanReRegistrationFails() throws Exception {
        ConfigurationMigrationManager configurationMigrationManager =
                createConfigurationMigrationManager();

        when(mBeanServer.registerMBean(configurationMigrationManager,
                configMigrationServiceObjectName)).thenThrow(new InstanceAlreadyExistsException(),
                new MBeanRegistrationException(new Exception()));

        configurationMigrationManager.init();
    }

    @Test(expected = MigrationException.class)
    public void exportWhenServiceReferencesCannotBeRetrieved() throws Exception {
        when(bundleContext.getServiceReferences(any(Class.class),
                any(String.class))).thenThrow(new InvalidSyntaxException("", ""));
        ConfigurationMigrationManager configurationMigrationManager =
                createConfigurationMigrationManager();

        export(() -> configurationMigrationManager.export(exportPath));
    }

    @Test
    public void exportWithPath() throws Exception {
        ConfigurationMigrationManager configurationMigrationManager =
                createConfigurationMigrationManager();

        Collection<MigrationWarning> migrationWarnings =
                export(() -> configurationMigrationManager.export(exportPath));
        assertThat(migrationWarnings, is(empty()));
    }

    @Test
    public void exportWithString() throws Exception {
        ConfigurationMigrationManager configurationMigrationManager =
                createConfigurationMigrationManager();

        Collection<MigrationWarning> migrationWarnings =
                export(() -> configurationMigrationManager.export("/export/dir"));
        assertThat(migrationWarnings, is(empty()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void exportWithNullPath() throws Exception {
        ConfigurationMigrationManager configurationMigrationManager =
                createConfigurationMigrationManager();

        configurationMigrationManager.export((Path) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void exportWithNullPathString() throws Exception {
        ConfigurationMigrationManager configurationMigrationManager =
                createConfigurationMigrationManager();

        configurationMigrationManager.export((String) null);
    }

    @Test
    public void exportWithWarnings() {
        ConfigurationMigrationManager configurationMigrationManager =
                createConfigurationMigrationManager();

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
                createConfigurationMigrationManager();

        configurationMigrationManager.export(exportPath);
    }

    @Test(expected = MigrationException.class)
    public void exportWhenConfigurationAdminMigratorThrowsIOException() throws Exception {
        when(Files.createDirectories(exportPath)).thenReturn(exportPath);
        doThrow(new IOException()).when(configurationAdminMigration)
                .export(exportPath);

        ConfigurationMigrationManager configurationMigrationManager =
                createConfigurationMigrationManager();

        configurationMigrationManager.export(exportPath);
    }

    @Test(expected = MigrationException.class)
    public void exportWhenConfigurationAdminMigratorThrowsConfigurationFileException()
            throws Exception {
        when(Files.createDirectories(exportPath)).thenReturn(exportPath);
        doThrow(new MigrationException("")).when(configurationAdminMigration)
                .export(exportPath);

        ConfigurationMigrationManager configurationMigrationManager =
                createConfigurationMigrationManager();

        configurationMigrationManager.export(exportPath);
    }

    @Test(expected = MigrationException.class)
    public void exportWhenConfigurationAdminMigratorThrowsRuntimeException() throws Exception {
        when(Files.createDirectories(exportPath)).thenReturn(exportPath);
        doThrow(new RuntimeException("")).when(configurationAdminMigration)
                .export(exportPath);

        ConfigurationMigrationManager configurationMigrationManager =
                createConfigurationMigrationManager();

        configurationMigrationManager.export(exportPath);
    }

    @Test(expected = MigrationException.class)
    public void exportWhenSystemConfigurationMigratorThrowsMigrationException() throws Exception {
        when(Files.createDirectories(exportPath)).thenReturn(exportPath);
        doThrow(new MigrationException("")).when(systemConfigurationMigration)
                .export(exportPath);

        ConfigurationMigrationManager configurationMigrationManager =
                createConfigurationMigrationManager();

        configurationMigrationManager.export(exportPath);
    }

    @Test(expected = MigrationException.class)
    public void exportWhenSystemConfigurationMigratorThrowsRuntimeException() throws Exception {
        when(Files.createDirectories(exportPath)).thenReturn(exportPath);
        doThrow(new RuntimeException("")).when(systemConfigurationMigration)
                .export(exportPath);

        ConfigurationMigrationManager configurationMigrationManager =
                createConfigurationMigrationManager();

        configurationMigrationManager.export(exportPath);
    }

    @Test
    public void exportCallsMigratables() throws Exception {
        ConfigurationMigrationManager configurationMigrationManager =
                createConfigurationMigrationManager();

        Collection<MigrationWarning> migrationWarnings =
                export(() -> configurationMigrationManager.export(exportPath));

        assertThat(migrationWarnings, is(empty()));
        verify(migratable1).export(exportPath.resolve(SERVICE_PID_1));
        verify(migratable2).export(exportPath.resolve(SERVICE_PID_2));
    }

    @Test
    public void exportWhenMigratablesReturnWarnings() throws Exception {
        ConfigurationMigrationManager configurationMigrationManager =
                createConfigurationMigrationManager();

        MigrationWarning[] expectedWarnings = new MigrationWarning[] {new MigrationWarning(
                "Warning1"), new MigrationWarning("Warning2")};

        when(migratable1.export(any(Path.class))).thenReturn(new MigrationMetadata(ImmutableList.of(
                expectedWarnings[0])));

        when(migratable2.export(any(Path.class))).thenReturn(new MigrationMetadata(ImmutableList.of(
                expectedWarnings[1])));

        Collection<MigrationWarning> migrationWarnings =
                export(() -> configurationMigrationManager.export(exportPath));

        assertThat(migrationWarnings, containsInAnyOrder(expectedWarnings));
        verify(migratable1).export(exportPath.resolve(SERVICE_PID_1));
        verify(migratable2).export(exportPath.resolve(SERVICE_PID_2));
    }

    @Test(expected = MigrationException.class)
    public void exportFailsWhenMigratableThrowsMigrationException() throws Exception {
        ConfigurationMigrationManager configurationMigrationManager =
                createConfigurationMigrationManager();

        when(migratable1.export(any(Path.class))).thenThrow(new MigrationException(""));
        when(migratable2.export(any(Path.class))).thenReturn(new MigrationMetadata(ImmutableList.of()));

        try {
            export(() -> configurationMigrationManager.export(exportPath));
        } finally {
            verify(migratable1).export(exportPath.resolve(SERVICE_PID_1));
            verify(migratable2, never()).export(any(Path.class));
        }
    }

    @Test(expected = MigrationException.class)
    public void exportFailsWhenMigratableThrowsRuntimeException() throws Exception {
        ConfigurationMigrationManager configurationMigrationManager =
                createConfigurationMigrationManager();

        when(migratable1.export(any(Path.class))).thenThrow(new RuntimeException());

        try {
            export(() -> configurationMigrationManager.export(exportPath));
        } finally {
            verify(migratable1).export(exportPath.resolve(SERVICE_PID_1));
            verify(migratable2, never()).export(any(Path.class));
        }
    }

    private ConfigurationMigrationManager createConfigurationMigrationManager() {
        return new ConfigurationMigrationManager(configurationAdminMigration,
                systemConfigurationMigration,
                mBeanServer) {
            @Override
            BundleContext getBundleContext() {
                return bundleContext;
            }
        };
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
