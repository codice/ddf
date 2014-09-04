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

import org.codice.proxy.http.HttpProxyService;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    
    @Test
    public void testTargetUrlWmsServerOnceNonBlankWmsServerBecomesBlank() throws Exception {
        
        // Setup
        HttpProxyService mockHttpProxyService = mock(HttpProxyService.class);
        when(mockHttpProxyService.start(anyString(), anyString(), anyInt())).thenReturn(ENDPOINT_NAME);
        BundleContext mockBundleContext = mock(BundleContext.class);
        Bundle mockBundle = mock(Bundle.class);
        when(mockBundleContext.getBundle()).thenReturn(mockBundle);
        when(mockBundle.getSymbolicName()).thenReturn(BUNDLE_SYMBOLIC_NAME);
    
        //Populate with non blank WMS Server
        ConfigurationStore configurationStore = ConfigurationStore.getInstance();
        configurationStore.setHttpProxy(mockHttpProxyService);
        configurationStore.setBundleContext(mockBundleContext);
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(WMS_SERVER_PROPERTY, WMS_SERVER);
        properties.put(TIMEOUT_PROPERTY, TIMEOUT);

        configurationStore.update(properties);
        
        //Now return back to default and make WMS Server blank
        Map<String, Object> defaultProperties = new HashMap<String, Object>();
        defaultProperties.put(WMS_SERVER_PROPERTY, EMPTY_STRING);
        configurationStore.update(defaultProperties);
        
        // Verify
        verify(mockHttpProxyService, times(1)).start(anyString(), eq(WMS_SERVER), eq(TIMEOUT));
        
        //Ensure that targetUrl and wmsServer variables are blank so that default map is returned
        assertEquals("", configurationStore.getTargetUrl());
        assertEquals("", configurationStore.getWmsServer());
       
    }

    @Test
    public void testContentTypeMappings() throws Exception {
        // Setup
        ConfigurationStore configurationStore = ConfigurationStore.getInstance();
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("typeNameMapping", Arrays.asList("foo=bar,foo=baz", "foo=qux",
                "alpha=beta, alpha = omega ", "=,=,", "bad,input", "name=,=type").toArray());

        // Perform Test
        configurationStore.update(properties);

        // Verify
        assertThat(configurationStore.getTypeNameMapping().size(), is(2));
        assertThat(configurationStore.getTypeNameMapping(), hasEntry("foo", Sets.newSet("bar",
                "baz", "qux")));
        assertThat(configurationStore.getTypeNameMapping(), hasEntry("alpha", Sets.newSet("beta",
                "omega")));
    }
    
}
