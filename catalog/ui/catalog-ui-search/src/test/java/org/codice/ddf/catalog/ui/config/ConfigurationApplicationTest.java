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
 **/
package org.codice.ddf.catalog.ui.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.codice.proxy.http.HttpProxyService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class ConfigurationApplicationTest {

    private static final String PROXY_SERVER = "http://www.example.com/wms";

    private static final String BUNDLE_SYMBOLIC_NAME = "mySymbolicName";

    private ConfigurationApplication configurationApplication;

    @Before
    public void setUp() {
        configurationApplication = new ConfigurationApplication();
    }

    @Test
    public void testSetImageryProviders() throws Exception {
        // Setup
        HttpProxyService mockHttpProxyService = mock(HttpProxyService.class);
        BundleContext mockBundleContext = mock(BundleContext.class);
        Bundle mockBundle = mock(Bundle.class);
        when(mockBundleContext.getBundle()).thenReturn(mockBundle);
        when(mockBundle.getSymbolicName()).thenReturn(BUNDLE_SYMBOLIC_NAME);
        when(mockHttpProxyService.start(anyString(),
                anyString(),
                anyInt())).thenReturn(PROXY_SERVER);

        configurationApplication.setHttpProxy(mockHttpProxyService);
        configurationApplication.setImageryProviders(IOUtils.toString(getClass().getResourceAsStream(
                "/imagery-providers.json")));

        // Verify
        for (Map<String, Object> provider : configurationApplication.getProxiedImageryProviders()) {
            assertTrue(provider.get(ConfigurationApplication.URL)
                    .toString()
                    .contains(PROXY_SERVER));
        }
    }

    @Test
    public void testSetTerrainProvider() throws Exception {
        // Setup
        HttpProxyService mockHttpProxyService = mock(HttpProxyService.class);
        BundleContext mockBundleContext = mock(BundleContext.class);
        Bundle mockBundle = mock(Bundle.class);
        when(mockBundleContext.getBundle()).thenReturn(mockBundle);
        when(mockBundle.getSymbolicName()).thenReturn(BUNDLE_SYMBOLIC_NAME);
        when(mockHttpProxyService.start(anyString(),
                anyString(),
                anyInt())).thenReturn(PROXY_SERVER);

        configurationApplication.setHttpProxy(mockHttpProxyService);
        configurationApplication.setTerrainProvider(IOUtils.toString(getClass().getResourceAsStream(
                "/terrain-provider.json")));

        // Verify
        assertTrue(configurationApplication.getProxiedTerrainProvider()
                .get(ConfigurationApplication.URL)
                .toString()
                .contains(PROXY_SERVER));
    }

    @Test
    public void testContentTypeMappings() throws Exception {
        // Setup
        configurationApplication.setTypeNameMapping((String[]) Arrays.asList("foo=bar,foo=baz",
                "foo=qux",
                "alpha=beta, alpha = omega ",
                "=,=,",
                "bad,input",
                "name=,=type")
                .toArray());

        // Verify
        assertThat(configurationApplication.getTypeNameMapping()
                .size(), is(2));
        assertThat(configurationApplication.getTypeNameMapping(),
                hasEntry("foo", Sets.newSet("bar", "baz", "qux")));
        assertThat(configurationApplication.getTypeNameMapping(),
                hasEntry("alpha", Sets.newSet("beta", "omega")));
    }

    @Test
    public void testContentTypeMappingsList() throws Exception {
        // Setup
        configurationApplication.setTypeNameMapping(Arrays.asList("foo=bar,foo=baz",
                "foo=qux",
                "alpha=beta, alpha = omega ",
                "=,=,",
                "bad,input",
                "name=,=type"));

        // Verify
        assertThat(configurationApplication.getTypeNameMapping()
                .size(), is(2));
        assertThat(configurationApplication.getTypeNameMapping(),
                hasEntry("foo", Sets.newSet("bar", "baz", "qux")));
        assertThat(configurationApplication.getTypeNameMapping(),
                hasEntry("alpha", Sets.newSet("beta", "omega")));
    }

    @Test
    public void testContentTypeMappingsListString() throws Exception {
        // Setup
        configurationApplication.setTypeNameMapping(
                "foo=bar,foo=baz,foo=qux,alpha=beta, alpha = omega , =,=,bad,input,name=,=type");

        // Verify
        assertThat(configurationApplication.getTypeNameMapping()
                .size(), is(2));
        assertThat(configurationApplication.getTypeNameMapping(),
                hasEntry("foo", Sets.newSet("bar", "baz", "qux")));
        assertThat(configurationApplication.getTypeNameMapping(),
                hasEntry("alpha", Sets.newSet("beta", "omega")));
    }

    @Test
    public void testsetAttributeAliases() throws Exception {
        configurationApplication.setAttributeAliases(Arrays.asList(" a = b ", " x = y "));
        assertThat(configurationApplication.getAttributeAliases(), is(Arrays.asList("a=b", "x=y")));
    }

}
