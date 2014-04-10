/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package org.codice.ddf.ui.searchui.standard.properties;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.codice.proxy.http.HttpProxyService;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class ConfigurationStoreTest {
    
    private static final String BLANK_STRING = " ";
    
    private static final String EMPTY_STRING = "";
    
    private static final String WMS_SERVER_PROPERTY = "wmsServer";
    
    private static final String WMS_SERVER = "http://www.example.com/wms";
    
    private static final String TIMEOUT_PROPERTY = "timeout";
    
    private static final Integer TIMEOUT = new Integer(5000);
    
    private static final String ENDPOINT_NAME = "myEndpointName";
    
    private static final String BUNDLE_SYMBOLIC_NAME = "mySymbolicName";
    
    private static final String EXPECTED_TARGET_URL = ConfigurationStore.SERVLET_PATH + "/" + ENDPOINT_NAME;

    @Test
    public void testUpdateWithEmptyWmsServer() throws Exception {
        // Setup
        HttpProxyService mockHttpProxyService = mock(HttpProxyService.class);
        BundleContext mockBundleContext = mock(BundleContext.class);
        Bundle mockBundle = mock(Bundle.class);
        when(mockBundleContext.getBundle()).thenReturn(mockBundle);
        when(mockBundle.getSymbolicName()).thenReturn(BUNDLE_SYMBOLIC_NAME);
    
        ConfigurationStore configurationStore = ConfigurationStore.getInstance();
        configurationStore.setHttpProxy(mockHttpProxyService);
        configurationStore.setBundleContext(mockBundleContext);
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(WMS_SERVER_PROPERTY, EMPTY_STRING);

        // Perform Test
        configurationStore.update(properties);
        
        // Verify
        verify(mockHttpProxyService, never()).start(anyString(), anyString(), anyInt());
    }
    
    @Test
    public void testUpdateWithNullWmsServer() throws Exception {
        // Setup
        HttpProxyService mockHttpProxyService = mock(HttpProxyService.class);
        BundleContext mockBundleContext = mock(BundleContext.class);
        Bundle mockBundle = mock(Bundle.class);
        when(mockBundleContext.getBundle()).thenReturn(mockBundle);
        when(mockBundle.getSymbolicName()).thenReturn(BUNDLE_SYMBOLIC_NAME);
    
        ConfigurationStore configurationStore = ConfigurationStore.getInstance();
        configurationStore.setHttpProxy(mockHttpProxyService);
        configurationStore.setBundleContext(mockBundleContext);
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(WMS_SERVER_PROPERTY, null);

        // Perform Test
        configurationStore.update(properties);
        
        // Verify
        verify(mockHttpProxyService, never()).start(anyString(), anyString(), anyInt());
    }
    
    @Test
    public void testUpdateWithBlankWmsServer() throws Exception {
        // Setup
        HttpProxyService mockHttpProxyService = mock(HttpProxyService.class);
        BundleContext mockBundleContext = mock(BundleContext.class);
        Bundle mockBundle = mock(Bundle.class);
        when(mockBundleContext.getBundle()).thenReturn(mockBundle);
        when(mockBundle.getSymbolicName()).thenReturn(BUNDLE_SYMBOLIC_NAME);
    
        ConfigurationStore configurationStore = ConfigurationStore.getInstance();
        configurationStore.setHttpProxy(mockHttpProxyService);
        configurationStore.setBundleContext(mockBundleContext);
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(WMS_SERVER_PROPERTY, BLANK_STRING);

        // Perform Test
        configurationStore.update(properties);
        
        // Verify
        verify(mockHttpProxyService, never()).start(anyString(), anyString(), anyInt());
    }
    
    @Test
    public void testUpdateNonBlankWmsServer() throws Exception {
        // Setup
        HttpProxyService mockHttpProxyService = mock(HttpProxyService.class);
        when(mockHttpProxyService.start(anyString(), anyString(), anyInt())).thenReturn(ENDPOINT_NAME);
        BundleContext mockBundleContext = mock(BundleContext.class);
        Bundle mockBundle = mock(Bundle.class);
        when(mockBundleContext.getBundle()).thenReturn(mockBundle);
        when(mockBundle.getSymbolicName()).thenReturn(BUNDLE_SYMBOLIC_NAME);
    
        ConfigurationStore configurationStore = ConfigurationStore.getInstance();
        configurationStore.setHttpProxy(mockHttpProxyService);
        configurationStore.setBundleContext(mockBundleContext);
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(WMS_SERVER_PROPERTY, WMS_SERVER);
        properties.put(TIMEOUT_PROPERTY, TIMEOUT);

        // Perform Test
        configurationStore.update(properties);
        
        // Verify
        verify(mockHttpProxyService, times(1)).start(anyString(), eq(WMS_SERVER), eq(TIMEOUT));
        assertEquals(EXPECTED_TARGET_URL, configurationStore.getTargetUrl());
    }
}
