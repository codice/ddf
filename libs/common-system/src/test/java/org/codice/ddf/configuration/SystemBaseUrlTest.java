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
package org.codice.ddf.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import org.junit.Before;
import org.junit.Test;

public class SystemBaseUrlTest {

    @Before
    public void setUp() throws Exception {
        System.setProperty("org.codice.ddf.system.protocol", "https://");
        System.setProperty("org.codice.ddf.system.externalHostname", "exthost");
        System.setProperty("org.codice.ddf.system.externalHttpsPort", "7993");
        System.setProperty("org.codice.ddf.system.externalHttpPort", "7181");
        System.setProperty("org.codice.ddf.system.hostname", "localhost");
        System.setProperty("org.codice.ddf.system.httpsPort", "8993");
        System.setProperty("org.codice.ddf.system.httpPort", "8181");
    }

    @Test
    public void testPropertyRead() {
        SystemBaseUrl sbu = new SystemBaseUrl();
        assertThat(sbu.getProtocol(), equalTo("https://"));
        assertThat(sbu.getHost(), equalTo("localhost"));
        assertThat(sbu.getHttpPort(), equalTo("8181"));
        assertThat(sbu.getHttpsPort(), equalTo("8993"));
        assertThat(sbu.getExternalHost(), equalTo("exthost"));
        assertThat(sbu.getExternalHttpPort(), equalTo("7181"));
        assertThat(sbu.getExternalHttpsPort(), equalTo("7993"));
    }

    @Test
    public void testGetPortHttps() throws Exception {
        SystemBaseUrl sbu = new SystemBaseUrl();
        assertThat(sbu.getPort(), equalTo("8993"));
    }

    @Test
    public void testGetPortHttp() throws Exception {
        System.setProperty("org.codice.ddf.system.protocol", "http://");
        SystemBaseUrl sbu = new SystemBaseUrl();
        assertThat(sbu.getPort(), equalTo("8181"));
    }

    @Test
    public void testGetPortNoProtocol() throws Exception {
        System.clearProperty("org.codice.ddf.system.protocol");
        SystemBaseUrl sbu = new SystemBaseUrl();
        assertThat(sbu.getPort(), equalTo("8993"));
    }

    @Test
    public void testGetExternalPortHttps() throws Exception {
        SystemBaseUrl sbu = new SystemBaseUrl();
        assertThat(sbu.getExternalPort(), equalTo("7993"));
    }

    @Test
    public void testGetExternalPortHttp() throws Exception {
        System.setProperty("org.codice.ddf.system.protocol", "http://");
        SystemBaseUrl sbu = new SystemBaseUrl();
        assertThat(sbu.getExternalPort(), equalTo("7181"));
    }

    @Test
    public void testGetPortExternalNoProtocol() throws Exception {
        System.clearProperty("org.codice.ddf.system.protocol");
        SystemBaseUrl sbu = new SystemBaseUrl();
        assertThat(sbu.getExternalPort(), equalTo("7993"));
    }

    @Test
    public void testGetBaseUrl() throws Exception {
        SystemBaseUrl sbu = new SystemBaseUrl();
        assertThat(sbu.getBaseUrl(), equalTo("https://localhost:8993"));
        assertThat(sbu.getBaseUrl("http://", false), equalTo("http://localhost:8181"));
        assertThat(sbu.getBaseUrl("http", false), equalTo("http://localhost:8181"));
        assertThat(sbu.getBaseUrl(true), equalTo("https://exthost:7993"));
        assertThat(sbu.getBaseUrl("http://", true), equalTo("http://exthost:7181"));
        assertThat(sbu.getBaseUrl("http", true), equalTo("http://exthost:7181"));
    }

    @Test
    public void testConstructUrl() throws Exception {
        SystemBaseUrl sbu = new SystemBaseUrl();
        assertThat(sbu.constructUrl(null, "/some/path", false),
                equalTo("https://localhost:8993/some/path"));
        assertThat(sbu.constructUrl(null, "some/path", false),
                equalTo("https://localhost:8993/some/path"));
        assertThat(sbu.constructUrl("https", "/some/path", false),
                equalTo("https://localhost:8993/some/path"));
        assertThat(sbu.constructUrl("http", "/some/path", false),
                equalTo("http://localhost:8181/some/path"));
        assertThat(sbu.constructUrl(null, null, false), equalTo("https://localhost:8993"));
        assertThat(sbu.constructUrl("http", null, false), equalTo("http://localhost:8181"));

        assertThat(sbu.constructUrl(null, "/some/path", true),
                equalTo("https://exthost:7993/some/path"));
        assertThat(sbu.constructUrl(null, "some/path", true),
                equalTo("https://exthost:7993/some/path"));
        assertThat(sbu.constructUrl("https", "/some/path", true),
                equalTo("https://exthost:7993/some/path"));
        assertThat(sbu.constructUrl("http", "/some/path", true),
                equalTo("http://exthost:7181/some/path"));
        assertThat(sbu.constructUrl(null, null, true), equalTo("https://exthost:7993"));
        assertThat(sbu.constructUrl("http", null, true), equalTo("http://exthost:7181"));
    }

}