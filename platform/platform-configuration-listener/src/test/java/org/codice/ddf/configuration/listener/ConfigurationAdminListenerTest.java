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

package org.codice.ddf.configuration.listener;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.codice.ddf.configuration.store.FileHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationAdminListenerTest {

    private static final String PID = "my.pid";

    private static final int INVALID_CM_EVENT = 999;

    private static final Dictionary<String, Object> PROPERTIES = new Hashtable<String, Object>(1);

    @Mock
    private FileHandler mockFileHandler;

    static {
        PROPERTIES.put("myKey", "myValue");
    }

    @Test
    public void testCmUpdateEvent() throws Exception {
        // Setup
        Configuration mockConfiguration = getMockConfiguration(PROPERTIES);
        ConfigurationAdmin mockConfigurationAdmin = getMockConfigurationAdmin(mockConfiguration);
        ServiceReference<ConfigurationAdmin> mockConfigurationAdminServiceReference = getMockConfigurationAdminServiceReference();
        BundleContext mockBundleContext = getMockBundleContext(mockConfigurationAdmin,
                mockConfigurationAdminServiceReference);
        ConfigurationEvent mockConfigurationEvent = getMockConfigurationEvent(
                mockConfigurationAdminServiceReference, ConfigurationEvent.CM_UPDATED);
        ConfigurationAdminListener configurationAdminListener = new ConfigurationAdminListener(
                mockBundleContext, mockFileHandler);

        // Perform Test
        configurationAdminListener.configurationEvent(mockConfigurationEvent);

        // Verify
        verify(mockFileHandler, times(1)).write(PID, PROPERTIES);
    }

    @Test
    public void testCmDeletedEvent() throws Exception {
        // Setup
        Configuration mockConfiguration = getMockConfiguration(PROPERTIES);
        ConfigurationAdmin mockConfigurationAdmin = getMockConfigurationAdmin(mockConfiguration);
        ServiceReference<ConfigurationAdmin> mockConfigurationAdminServiceReference = getMockConfigurationAdminServiceReference();
        BundleContext mockBundleContext = getMockBundleContext(mockConfigurationAdmin,
                mockConfigurationAdminServiceReference);
        ConfigurationEvent mockConfigurationEvent = getMockConfigurationEvent(
                mockConfigurationAdminServiceReference, ConfigurationEvent.CM_DELETED);
        ConfigurationAdminListener configurationAdminListener = new ConfigurationAdminListener(
                mockBundleContext, mockFileHandler);

        // Perform Test
        configurationAdminListener.configurationEvent(mockConfigurationEvent);

        // Verify
        verify(mockFileHandler, times(1)).delete(PID);
    }

    @Test
    public void testCmLocationChangedEvent() throws Exception {
        // Setup
        Configuration mockConfiguration = getMockConfiguration(PROPERTIES);
        ConfigurationAdmin mockConfigurationAdmin = getMockConfigurationAdmin(mockConfiguration);
        ServiceReference<ConfigurationAdmin> mockConfigurationAdminServiceReference = getMockConfigurationAdminServiceReference();
        BundleContext mockBundleContext = getMockBundleContext(mockConfigurationAdmin,
                mockConfigurationAdminServiceReference);
        ConfigurationEvent mockConfigurationEvent = getMockConfigurationEvent(
                mockConfigurationAdminServiceReference, ConfigurationEvent.CM_LOCATION_CHANGED);
        ConfigurationAdminListener configurationAdminListener = new ConfigurationAdminListener(
                mockBundleContext, mockFileHandler);

        // Perform Test
        configurationAdminListener.configurationEvent(mockConfigurationEvent);

        // Verify
        verify(mockFileHandler, times(0)).write(PID, PROPERTIES);
        verify(mockFileHandler, times(0)).delete(PID);

    }

    @Test
    public void testInvalidCmEvent() throws Exception {
        // Setup
        Configuration mockConfiguration = getMockConfiguration(PROPERTIES);
        ConfigurationAdmin mockConfigurationAdmin = getMockConfigurationAdmin(mockConfiguration);
        ServiceReference<ConfigurationAdmin> mockConfigurationAdminServiceReference = getMockConfigurationAdminServiceReference();
        BundleContext mockBundleContext = getMockBundleContext(mockConfigurationAdmin,
                mockConfigurationAdminServiceReference);
        ConfigurationEvent mockConfigurationEvent = getMockConfigurationEvent(
                mockConfigurationAdminServiceReference, INVALID_CM_EVENT);
        ConfigurationAdminListener configurationAdminListener = new ConfigurationAdminListener(
                mockBundleContext, mockFileHandler);

        // Perform Test
        configurationAdminListener.configurationEvent(mockConfigurationEvent);

        // Verify
        verify(mockFileHandler, times(0)).write(PID, PROPERTIES);
        verify(mockFileHandler, times(0)).delete(PID);
    }

    @Test
    public void testIOExceptionWhenRetrievingConfiguration() throws Exception {
        // Setup
        Configuration mockConfiguration = getMockConfiguration(PROPERTIES);
        ConfigurationAdmin mockConfigurationAdmin = getMockConfigurationAdmin(mockConfiguration);
        doThrow(new IOException()).when(mockConfigurationAdmin).getConfiguration(PID, null);
        ServiceReference<ConfigurationAdmin> mockConfigurationAdminServiceReference = getMockConfigurationAdminServiceReference();
        BundleContext mockBundleContext = getMockBundleContext(mockConfigurationAdmin,
                mockConfigurationAdminServiceReference);
        ConfigurationEvent mockConfigurationEvent = getMockConfigurationEvent(
                mockConfigurationAdminServiceReference, ConfigurationEvent.CM_UPDATED);
        ConfigurationAdminListener configurationAdminListener = new ConfigurationAdminListener(
                mockBundleContext, mockFileHandler);

        // Perform Test
        configurationAdminListener.configurationEvent(mockConfigurationEvent);

        // Verify
        verify(mockFileHandler, times(0)).write(PID, PROPERTIES);
        verify(mockFileHandler, times(0)).delete(PID);
    }

    @Test
    public void testNullConfigurationEvent() throws Exception {
        // Setup
        ConfigurationAdminListener configurationAdminListener = new ConfigurationAdminListener(null,
                mockFileHandler);

        // Perform Test
        configurationAdminListener.configurationEvent(null);
    }

    private Configuration getMockConfiguration(Dictionary<String, Object> properties) {
        Configuration mockConfiguration = mock(Configuration.class);
        when(mockConfiguration.getProperties()).thenReturn(properties);
        return mockConfiguration;
    }

    private ConfigurationAdmin getMockConfigurationAdmin(Configuration mockConfiguration)
            throws Exception {
        ConfigurationAdmin mockConfigurationAdmin = mock(ConfigurationAdmin.class);
        when(mockConfigurationAdmin.getConfiguration(PID, null)).thenReturn(mockConfiguration);
        return mockConfigurationAdmin;
    }

    private BundleContext getMockBundleContext(ConfigurationAdmin mockConfigurationAdmin,
            ServiceReference<ConfigurationAdmin> mockConfigurationAdminServiceReference) {
        BundleContext mockBundleContext = mock(BundleContext.class);
        when(mockBundleContext.getService(mockConfigurationAdminServiceReference))
                .thenReturn(mockConfigurationAdmin);
        return mockBundleContext;
    }

    private ServiceReference<ConfigurationAdmin> getMockConfigurationAdminServiceReference() {
        @SuppressWarnings("unchecked")
        ServiceReference<ConfigurationAdmin> mockConfigurationAdminServiceReference = mock(
                ServiceReference.class);
        return mockConfigurationAdminServiceReference;
    }

    private ConfigurationEvent getMockConfigurationEvent(
            ServiceReference<ConfigurationAdmin> mockConfigurationAdminServiceReference,
            int configurationEvent) {
        ConfigurationEvent mockConfigurationEvent = mock(ConfigurationEvent.class);
        when(mockConfigurationEvent.getPid()).thenReturn(PID);
        when(mockConfigurationEvent.getFactoryPid()).thenReturn(null);
        when(mockConfigurationEvent.getType()).thenReturn(configurationEvent);
        when(mockConfigurationEvent.getReference())
                .thenReturn(mockConfigurationAdminServiceReference);
        return mockConfigurationEvent;
    }
}
