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
package org.codice.ddf.platform.http.proxy;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.codice.proxy.http.HttpProxyService;
import org.codice.proxy.http.HttpProxyServiceImpl;
import org.junit.Test;

public class TestHttpProxy {

    @Test
    public void testStartingAndStoppingProxyWithDifferentConfigurations() throws Exception {
        HttpProxyServiceImpl httpProxyService = mock(HttpProxyServiceImpl.class);
        when(httpProxyService.start(eq("0.0.0.0:8181"), anyString(), eq(1000), eq(true)))
                .thenReturn("endpointName");
        HttpProxy httpProxy = new HttpProxy(httpProxyService) {
            public Properties getProperties() {
                Properties properties = new Properties();
                InputStream propertiesStream = HttpProxy.class.getResourceAsStream("/etc/org.ops4j.pax.web.cfg");
                try {
                    properties.load(propertiesStream);
                } catch (IOException e) {
                    fail(e.getMessage());
                }
                return properties;
            }
        };
        httpProxy.startProxy();
        verify(httpProxyService, times(1)).start(eq("0.0.0.0:8181"), anyString(), eq(1000),
                eq(true));

        httpProxy.stopProxy();
        verify(httpProxyService, times(1)).stop("endpointName");

        httpProxy.setHostname("blah");

        httpProxy.startProxy();
        verify(httpProxyService, times(1)).start(eq("0.0.0.0:8181"), eq("https://blah:8993"), eq(1000), eq(true));

        httpProxy.startProxy();
        verify(httpProxyService, times(3)).stop("endpointName");

        httpProxy = new HttpProxy(httpProxyService) {
            public Properties getProperties() {
                Properties properties = new Properties();
                InputStream propertiesStream = HttpProxy.class.getResourceAsStream("/etc/org.ops4j.pax.web.cfg");
                try {
                    properties.load(propertiesStream);
                } catch (IOException e) {
                    fail(e.getMessage());
                }
                properties.put("org.osgi.service.http.enabled", "true");
                return properties;
            }
        };
        httpProxy.startProxy();
        verify(httpProxyService, times(3)).start(eq("0.0.0.0:8181"), anyString(), eq(1000), eq(true));
    }

    @Test
    public void testStartingWithNoProperties() throws Exception {
        HttpProxyServiceImpl httpProxyService = mock(HttpProxyServiceImpl.class);
        when(httpProxyService.start(anyString(), anyString(), anyInt(), anyBoolean()))
                .thenReturn("endpointName");
        HttpProxy httpProxy = new HttpProxy(httpProxyService) {
            public Properties getProperties() {
                return new Properties();
            }
        };
        httpProxy.startProxy();
        verify(httpProxyService, times(0)).start(anyString(), anyString(), anyInt(), anyBoolean());
    }

    @Test
    public void testLoadProperties() {
        HttpProxyService httpProxyService = mock(HttpProxyService.class);
        HttpProxy httpProxy = new HttpProxy(httpProxyService);
        Properties properties = httpProxy.getProperties();
        assertThat(0, is(properties.stringPropertyNames().size()));
    }
}
