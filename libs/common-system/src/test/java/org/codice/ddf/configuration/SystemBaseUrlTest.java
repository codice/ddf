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
        System.setProperty("org.codice.ddf.system.hostname", "localhost");
        System.setProperty("org.codice.ddf.system.httpsPort", "8993");
        System.setProperty("org.codice.ddf.system.httpPort", "8181");
    }

    @Test
    public void testPropertyRead() {
        assertThat(SystemBaseUrl.getProtocol(), equalTo("https://"));
        assertThat(SystemBaseUrl.getHost(), equalTo("localhost"));
        assertThat(SystemBaseUrl.getHttpPort(), equalTo("8181"));
        assertThat(SystemBaseUrl.getHttpsPort(), equalTo("8993"));
    }

    @Test
    public void testGetPortHttps() throws Exception {
        assertThat(SystemBaseUrl.getPort(), equalTo("8993"));
    }

    @Test
    public void testGetPortHttp() throws Exception {
        System.setProperty("org.codice.ddf.system.protocol", "http://");
        assertThat(SystemBaseUrl.getPort(), equalTo("8181"));
    }

    @Test
    public void testGetPortHttpsProtoHttp() throws Exception {
        assertThat(SystemBaseUrl.getPort("http"), equalTo("8181"));
    }

    @Test
    public void testGetPortHttpsBadProto() throws Exception {
        assertThat(SystemBaseUrl.getPort("asdf"), equalTo("8181"));
    }

    @Test
    public void testGetPortHttpsNullProto() throws Exception {
        assertThat(SystemBaseUrl.getPort(null), equalTo("8993"));
    }

    @Test
    public void testGetPortNoProtocol() throws Exception {
        System.clearProperty("org.codice.ddf.system.protocol");
        assertThat(SystemBaseUrl.getPort(), equalTo("8993"));
    }

    @Test
    public void testGetBaseUrl() throws Exception {
        assertThat(SystemBaseUrl.getBaseUrl(), equalTo("https://localhost:8993"));
        assertThat(SystemBaseUrl.getBaseUrl("http://"), equalTo("http://localhost:8181"));
        assertThat(SystemBaseUrl.getBaseUrl("http"), equalTo("http://localhost:8181"));
    }

    @Test
    public void testConstructUrl() throws Exception {
        assertThat(SystemBaseUrl.constructUrl("/some/path"),
                equalTo("https://localhost:8993/some/path"));
        assertThat(SystemBaseUrl.constructUrl(null, "/some/path"),
                equalTo("https://localhost:8993/some/path"));
        assertThat(SystemBaseUrl.constructUrl(null, "some/path"),
                equalTo("https://localhost:8993/some/path"));
        assertThat(SystemBaseUrl.constructUrl("https", "/some/path"),
                equalTo("https://localhost:8993/some/path"));
        assertThat(SystemBaseUrl.constructUrl("http", "/some/path"),
                equalTo("http://localhost:8181/some/path"));
        assertThat(SystemBaseUrl.constructUrl(null, null), equalTo("https://localhost:8993"));
        assertThat(SystemBaseUrl.constructUrl("http", null), equalTo("http://localhost:8181"));
    }

    @Test
    public void testRootContext() throws Exception {
        assertThat(SystemBaseUrl.getRootContext(), equalTo(""));
        System.setProperty("org.codice.ddf.system.rootContext", "/services");
        assertThat(SystemBaseUrl.getRootContext(), equalTo("/services"));

        assertThat(SystemBaseUrl.constructUrl("/some/path", true),
                equalTo("https://localhost:8993/services/some/path"));
        System.setProperty("org.codice.ddf.system.rootContext", "services");
        assertThat(SystemBaseUrl.constructUrl("/some/path", true),
                equalTo("https://localhost:8993/services/some/path"));
    }

}