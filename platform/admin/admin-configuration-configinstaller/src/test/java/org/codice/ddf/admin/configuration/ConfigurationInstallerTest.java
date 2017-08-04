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
package org.codice.ddf.admin.configuration;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.osgi.service.cm.ConfigurationEvent.CM_DELETED;
import static org.osgi.service.cm.ConfigurationEvent.CM_UPDATED;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.codice.ddf.admin.configurator.Configurator;
import org.codice.ddf.admin.configurator.ConfiguratorFactory;
import org.codice.ddf.admin.configurator.OperationReport;
import org.codice.ddf.internal.admin.configurator.actions.ConfigActions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationInstallerTest {
    private static final String FELIX_FILENAME_PROP = "felix.fileinstall.filename";

    // Will be tracked in the installer's map after init()
    private static final String PID_001 = "001";

    // Will NOT be tracked in the installer's map after init()
    private static final String PID_002 = "002";

    @Mock(answer = Answers.RETURNS_MOCKS)
    private ConfiguratorFactory configuratorFactory;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private Configurator configurator;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private ConfigActions configActions;

    @Mock
    private Configuration configuration;

    @Mock
    private ConfigurationAdmin configurationAdmin;

    @Mock
    private ConfigurationEvent configurationEvent;

    private ConfigurationInstaller installer;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File fileA;

    private File fileB;

    @Before
    public void before() throws Exception {
        when(configuratorFactory.getConfigurator()).thenReturn(configurator);
        when(configurator.commit(anyString())).thenAnswer(i -> {
            fileB.delete();
            return mock(OperationReport.class);
        });

        when(configurationEvent.getType()).thenReturn(CM_UPDATED);
        fileA = temporaryFolder.newFile();
        fileB = temporaryFolder.newFile();

        Configuration mockConfig1b = createMockConfig(PID_001, fileB.toURI());
        Configuration mockConfig2 = createMockConfig(PID_002, null);

        Configuration[] configs = new Configuration[] {mockConfig1b, mockConfig2};
        when(configurationAdmin.listConfigurations(null)).thenReturn(configs);

        installer = new ConfigurationInstaller(configurationAdmin,
                configuratorFactory,
                configActions);
    }

    @Test
    public void testInit() throws Exception {
        installer.init();
        Map<String, File> pidFileMap = installer.getPidFileMap();
        assertThat(pidFileMap.entrySet(), hasSize(1));
        assertThat(pidFileMap.get(PID_001), is(fileB));
    }

    @Test
    public void testInitWhenListConfigsReturnsNull() throws Exception {
        when(configurationAdmin.listConfigurations(anyString())).thenReturn(null);
        installer.init();
        Map<String, File> pidFileMap = installer.getPidFileMap();
        assertThat(pidFileMap.entrySet(), is(empty()));
    }

    @Test
    public void testUpdatedEventNotTrackingWontTrack() throws Exception {
        configuration = createMockConfig(PID_002, null);

        when(configurationEvent.getPid()).thenReturn(PID_002);
        when(configurationAdmin.getConfiguration(PID_002, null)).thenReturn(configuration);

        installer.init();
        installer.configurationEvent(configurationEvent);

        Map<String, File> pidFileMap = installer.getPidFileMap();
        assertThat(pidFileMap.entrySet(), hasSize(1));
        assertThat(pidFileMap.get(PID_001), is(fileB));
        // Not tracking and won't track, so filesystem should not have changed
        assertThat(fileB.exists(), is(true));
    }

    @Test
    public void testUpdatedEventPropChanged() throws Exception {
        configuration = createMockConfig(PID_001, fileA.toURI());

        when(configurationEvent.getPid()).thenReturn(PID_001);
        when(configurationAdmin.getConfiguration(PID_001, null)).thenReturn(configuration);

        installer.init();
        installer.configurationEvent(configurationEvent);

        Map<String, File> pidFileMap = installer.getPidFileMap();
        assertThat(pidFileMap.entrySet(), hasSize(1));
        assertThat(pidFileMap.get(PID_001), is(fileB));
        assertThat(fileB.exists(), is(true));
    }

    @Test
    public void testUpdatedEventPropAdded() throws Exception {
        configuration = createMockConfig(PID_002, fileA.toURI());

        when(configurationEvent.getPid()).thenReturn(PID_002);
        when(configurationAdmin.getConfiguration(PID_002, null)).thenReturn(configuration);

        installer.init();
        installer.configurationEvent(configurationEvent);

        Map<String, File> pidFileMap = installer.getPidFileMap();
        assertThat(pidFileMap.entrySet(), hasSize(2));
        assertThat(pidFileMap.get(PID_001), is(fileB));
        assertThat(pidFileMap.get(PID_002), is(fileA));
    }

    @Test
    public void testUpdatedEventConfigNull() throws Exception {
        when(configurationAdmin.getConfiguration(anyString(), anyString())).thenReturn(null);
        installer.init();
        installer.configurationEvent(configurationEvent);
        verifyZeroInteractions(configuratorFactory, configurator, configActions);
    }

    @Test
    public void testUpdatedEventConfigThrowsIOException() throws Exception {
        when(configurationAdmin.getConfiguration(anyString(),
                anyString())).thenThrow(IOException.class);
        installer.init();
        installer.configurationEvent(configurationEvent);
        verifyZeroInteractions(configuratorFactory, configurator, configActions);
    }

    @Test
    public void testDeletedEvent() throws Exception {
        when(configurationEvent.getType()).thenReturn(CM_DELETED);
        when(configurationEvent.getPid()).thenReturn(PID_001);
        installer.init();
        installer.configurationEvent(configurationEvent);
        assertThat(installer.getPidFileMap()
                .entrySet(), is(empty()));
        assertThat(fileB.exists(), is(false));
    }

    private Configuration createMockConfig(String pid, Object path) {
        Dictionary<String, Object> props = new Hashtable<>();
        if (path != null) {
            props.put(FELIX_FILENAME_PROP, path);
        }
        Configuration mockConfig = mock(Configuration.class);
        when(mockConfig.getPid()).thenReturn(pid);
        when(mockConfig.getProperties()).thenReturn(props);
        return mockConfig;
    }
}
