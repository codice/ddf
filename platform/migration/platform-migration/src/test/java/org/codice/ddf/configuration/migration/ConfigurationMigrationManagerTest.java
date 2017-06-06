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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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
import java.util.List;
import java.util.function.Supplier;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.validation.constraints.NotNull;

import org.codice.ddf.configuration.admin.ConfigurationAdminMigration;
import org.codice.ddf.migration.ConfigurationMigratable;
import org.codice.ddf.migration.DataMigratable;
import org.codice.ddf.migration.DescribableBean;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationMetadata;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.platform.services.common.Describable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.InvalidSyntaxException;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import com.google.common.collect.ImmutableList;

@PrepareForTest(ConfigurationMigrationManager.class)
public class ConfigurationMigrationManagerTest {

    @Rule
    public PowerMockRule powerMockRule = new PowerMockRule();

    private static ObjectName configMigrationServiceObjectName;

    @Mock
    private ConfigurationAdminMigration configurationAdminMigration;

    @Mock
    private MBeanServer mBeanServer;

    private List<ConfigurationMigratable> configurationMigratables;

    private List<DataMigratable> dataMigratables;

    @Mock
    private ConfigurationMigratable configurationMigratable;

    @Mock
    private DataMigratable dataMigratable;

    @Mock
    private Path exportDirectory;

    private final MigrationMetadata noWarnings = new MigrationMetadata(ImmutableList.of());

    private final Path exportPath = Paths.get("export", "dir");

    @BeforeClass
    public static void setupClass() throws MalformedObjectNameException {
        configMigrationServiceObjectName = new ObjectName(
                ConfigurationMigrationManager.class.getName() + ":service=configuration-migration");
    }

    @Before
    public void setup() throws InvalidSyntaxException {
        MockitoAnnotations.initMocks(this);

        mockStatic(Files.class);
        mockStatic(Paths.class);

        configurationMigratables = Collections.singletonList(configurationMigratable);
        dataMigratables = Collections.singletonList(dataMigratable);

        when(configurationMigratable.export(any(Path.class))).thenReturn(noWarnings);
        when(dataMigratable.export(any(Path.class))).thenReturn(noWarnings);
    }

    @Test
    public void testGetOptionalMigratableInfo() {
        DescribableBean bean1 = new DescribableBean("1.0",
                "ddf.platform",
                "Platform Migratable",
                "Exports platform config",
                "Codice");
        DescribableBean bean2 = new DescribableBean("2.0",
                "ddf.catalog",
                "Catalog Migratable",
                "Exports catalog metacards",
                "Codice");

        List<ConfigurationMigratable> mockConfigs = mock(List.class);
        List<DataMigratable> migratables = new ArrayList<>();
        migratables.add(new TestMigratable(bean1, 3));
        migratables.add(new TestMigratable(bean2, 4));

        ConfigurationMigrationManager manager = new ConfigurationMigrationManager(
                configurationAdminMigration,
                mBeanServer,
                mockConfigs,
                migratables);

        Collection<Describable> describables = manager.getOptionalMigratableInfo();

        verifyDescriptionEqual((Describable) describables.toArray()[0], bean1);
        verifyDescriptionEqual((Describable) describables.toArray()[1], bean2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullConfigurationAdminMigrator() {
        new ConfigurationMigrationManager(null,
                mBeanServer,
                new ArrayList<>(),
                new ArrayList<>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullMBeanServer() {
        new ConfigurationMigrationManager(configurationAdminMigration,
                null,
                new ArrayList<>(),
                new ArrayList<>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullConfigurationMigratablesList() {
        new ConfigurationMigrationManager(configurationAdminMigration,
                mBeanServer,
                null,
                new ArrayList<>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullDataMigratablesList() {
        new ConfigurationMigrationManager(configurationAdminMigration,
                mBeanServer,
                new ArrayList<>(),
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

    @Test
    public void exportWithPath() throws Exception {
        ConfigurationMigrationManager configurationMigrationManager =
                createConfigurationMigrationManager();

        when(exportDirectory.resolve(any(Path.class))).thenReturn(any(Path.class));

        Collection<MigrationWarning> migrationWarnings =
                export(() -> configurationMigrationManager.export(exportDirectory));
        assertThat(migrationWarnings, is(empty()));

    }

    @Test
    public void exportWithString() throws Exception {
        ConfigurationMigrationManager configurationMigrationManager =
                createConfigurationMigrationManager();

        when(Paths.get("/export/dir")).thenReturn(exportDirectory);

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

        Collection<MigrationWarning> warningList = new ArrayList<>();
        warningList.add(migrationWarning);
        MigrationMetadata warning = new MigrationMetadata(warningList);

        when(configurationMigratable.export(any(Path.class))).thenReturn(warning);

        Collection<MigrationWarning> migrationWarnings = configurationMigrationManager.export(
                exportDirectory);
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

    @Test
    public void exportCallsMigratables() throws Exception {
        ConfigurationMigrationManager configurationMigrationManager =
                createConfigurationMigrationManager();

        Collection<MigrationWarning> migrationWarnings =
                export(() -> configurationMigrationManager.export(exportDirectory));

        assertThat(migrationWarnings, is(empty()));

        verify(configurationMigratable).export(exportDirectory);
        verify(dataMigratable).export(exportDirectory);
    }

    @Test
    public void exportWhenMigratablesReturnWarnings() throws Exception {
        ConfigurationMigrationManager configurationMigrationManager =
                createConfigurationMigrationManager();

        MigrationWarning[] expectedWarnings = new MigrationWarning[] {new MigrationWarning(
                "Warning1"), new MigrationWarning("Warning2")};

        when(configurationMigratable.export(any(Path.class))).thenReturn(new MigrationMetadata(
                ImmutableList.of(expectedWarnings[0])));

        when(dataMigratable.export(any(Path.class))).thenReturn(new MigrationMetadata(ImmutableList.of(
                expectedWarnings[1])));

        Collection<MigrationWarning> migrationWarnings =
                export(() -> configurationMigrationManager.export(exportDirectory));

        assertThat(migrationWarnings, containsInAnyOrder(expectedWarnings));

        verify(configurationMigratable).export(exportDirectory);
        verify(dataMigratable).export(exportDirectory);
    }

    @Test(expected = MigrationException.class)
    public void exportFailsWhenMigratableThrowsMigrationException() throws Exception {
        ConfigurationMigrationManager configurationMigrationManager =
                createConfigurationMigrationManager();

        when(configurationMigratable.export(any(Path.class))).thenThrow(new MigrationException(""));
        when(dataMigratable.export(any(Path.class))).thenReturn(new MigrationMetadata(ImmutableList.of()));

        try {
            export(() -> configurationMigrationManager.export(exportPath));
        } finally {
            verify(configurationMigratable).export(exportPath);
            verify(dataMigratable, never()).export(any(Path.class));
        }
    }

    @Test(expected = MigrationException.class)
    public void exportFailsWhenMigratableThrowsRuntimeException() throws Exception {
        ConfigurationMigrationManager configurationMigrationManager =
                createConfigurationMigrationManager();

        when(configurationMigratable.export(any(Path.class))).thenThrow(new RuntimeException());

        try {
            export(() -> configurationMigrationManager.export(exportPath));
        } finally {
            verify(configurationMigratable).export(exportPath);
            verify(dataMigratable, never()).export(any(Path.class));
        }
    }

    private ConfigurationMigrationManager createConfigurationMigrationManager() {
        return new ConfigurationMigrationManager(configurationAdminMigration,
                mBeanServer,
                configurationMigratables,
                dataMigratables);
    }

    private Collection<MigrationWarning> export(Supplier<Collection<MigrationWarning>> exportCall)
            throws Exception {
        when(Files.createDirectories(exportDirectory)).thenReturn(exportDirectory);

        Collection<MigrationWarning> migrationWarnings = exportCall.get();

        verifyStatic();

        Files.createDirectories(exportDirectory);

        verify(configurationAdminMigration, times(1)).export(exportDirectory);

        return migrationWarnings;
    }

    private void verifyDescriptionEqual(Describable describable, DescribableBean bean) {
        assert (describable.getId()
                .equals(bean.getId()));
        assert (describable.getTitle()
                .equals(bean.getTitle()));
        assert (describable.getDescription()
                .equals(bean.getDescription()));
        assert (describable.getOrganization()
                .equals(bean.getOrganization()));
        assert (describable.getVersion()
                .equals(bean.getVersion()));
    }
}

class TestMigratable extends DescribableBean implements DataMigratable {

    public TestMigratable(DescribableBean info, int wrappedNumber) {
        super(info);
    }

    @Override
    public MigrationMetadata export(@NotNull Path exportPath) throws MigrationException {
        return null;
    }

}