/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
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
    System.setProperty("org.codice.ddf.system.rootContext", "/services");
    System.setProperty("org.codice.ddf.external.protocol", "https://");
    System.setProperty("org.codice.ddf.external.hostname", "not_localhost");
    System.setProperty("org.codice.ddf.external.httpsPort", "8994");
    System.setProperty("org.codice.ddf.external.httpPort", "8282");
    System.setProperty("org.codice.ddf.external.context", "/ddf");
  }

  @Test
  public void testPropertyRead() {
    assertThat(SystemBaseUrl.INTERNAL.getProtocol(), equalTo("https://"));
    assertThat(SystemBaseUrl.INTERNAL.getHost(), equalTo("localhost"));
    assertThat(SystemBaseUrl.INTERNAL.getHttpPort(), equalTo("8181"));
    assertThat(SystemBaseUrl.INTERNAL.getHttpsPort(), equalTo("8993"));
    assertThat(SystemBaseUrl.INTERNAL.getRootContext(), equalTo("/services"));
    assertThat(SystemBaseUrl.EXTERNAL.getProtocol(), equalTo("https://"));
    assertThat(SystemBaseUrl.EXTERNAL.getHost(), equalTo("not_localhost"));
    assertThat(SystemBaseUrl.EXTERNAL.getHttpPort(), equalTo("8282"));
    assertThat(SystemBaseUrl.EXTERNAL.getHttpsPort(), equalTo("8994"));
    assertThat(SystemBaseUrl.EXTERNAL.getRootContext(), equalTo("/ddf"));
  }

  @Test
  public void testGetPortHttps() {
    assertThat(SystemBaseUrl.INTERNAL.getPort(), equalTo("8993"));
    assertThat(SystemBaseUrl.EXTERNAL.getPort(), equalTo("8994"));
  }

  @Test
  public void testGetPortHttp() {
    System.setProperty("org.codice.ddf.system.protocol", "http://");
    System.setProperty("org.codice.ddf.external.protocol", "http://");
    assertThat(SystemBaseUrl.INTERNAL.getPort(), equalTo("8181"));
    assertThat(SystemBaseUrl.EXTERNAL.getPort(), equalTo("8282"));
  }

  @Test
  public void testGetPortHttpsProtoHttp() {
    assertThat(SystemBaseUrl.INTERNAL.getPort("http"), equalTo("8181"));
    assertThat(SystemBaseUrl.EXTERNAL.getPort("http"), equalTo("8282"));
  }

  @Test
  public void testGetPortHttpsBadProto() {
    assertThat(SystemBaseUrl.INTERNAL.getPort("asdf"), equalTo("8181"));
    assertThat(SystemBaseUrl.EXTERNAL.getPort("asdf"), equalTo("8282"));
  }

  @Test
  public void testGetPortHttpsNullProto() {
    assertThat(SystemBaseUrl.INTERNAL.getPort(null), equalTo("8993"));
    assertThat(SystemBaseUrl.EXTERNAL.getPort(null), equalTo("8994"));
  }

  @Test
  public void testGetPortNoProtocol() {
    System.clearProperty("org.codice.ddf.system.protocol");
    System.clearProperty("org.codice.ddf.external.protocol");
    assertThat(SystemBaseUrl.INTERNAL.getPort(), equalTo("8993"));
    assertThat(SystemBaseUrl.EXTERNAL.getPort(), equalTo("8994"));
  }

  @Test
  public void testGetBaseUrl() {
    assertThat(SystemBaseUrl.INTERNAL.getBaseUrl(), equalTo("https://localhost:8993"));
    assertThat(SystemBaseUrl.INTERNAL.getBaseUrl("http://"), equalTo("http://localhost:8181"));
    assertThat(SystemBaseUrl.INTERNAL.getBaseUrl("http"), equalTo("http://localhost:8181"));

    assertThat(SystemBaseUrl.EXTERNAL.getBaseUrl(), equalTo("https://not_localhost:8994/ddf"));
    assertThat(
        SystemBaseUrl.EXTERNAL.getBaseUrl("http://"), equalTo("http://not_localhost:8282/ddf"));
    assertThat(SystemBaseUrl.EXTERNAL.getBaseUrl("http"), equalTo("http://not_localhost:8282/ddf"));
  }

  @Test
  public void testConstructUrl() {
    assertThat(
        SystemBaseUrl.INTERNAL.constructUrl("/some/path"),
        equalTo("https://localhost:8993/some/path"));
    assertThat(
        SystemBaseUrl.INTERNAL.constructUrl(null, "/some/path"),
        equalTo("https://localhost:8993/some/path"));
    assertThat(
        SystemBaseUrl.INTERNAL.constructUrl(null, "some/path"),
        equalTo("https://localhost:8993/some/path"));
    assertThat(
        SystemBaseUrl.INTERNAL.constructUrl("https", "/some/path"),
        equalTo("https://localhost:8993/some/path"));
    assertThat(
        SystemBaseUrl.INTERNAL.constructUrl("http", "/some/path"),
        equalTo("http://localhost:8181/some/path"));
    assertThat(SystemBaseUrl.INTERNAL.constructUrl(null, null), equalTo("https://localhost:8993"));
    assertThat(SystemBaseUrl.INTERNAL.constructUrl("http", null), equalTo("http://localhost:8181"));

    assertThat(
        SystemBaseUrl.EXTERNAL.constructUrl("/some/path"),
        equalTo("https://not_localhost:8994/ddf/some/path"));
    assertThat(
        SystemBaseUrl.EXTERNAL.constructUrl(null, "/some/path"),
        equalTo("https://not_localhost:8994/ddf/some/path"));
    assertThat(
        SystemBaseUrl.EXTERNAL.constructUrl(null, "some/path"),
        equalTo("https://not_localhost:8994/ddf/some/path"));
    assertThat(
        SystemBaseUrl.EXTERNAL.constructUrl("https", "/some/path"),
        equalTo("https://not_localhost:8994/ddf/some/path"));
    assertThat(
        SystemBaseUrl.EXTERNAL.constructUrl("http", "/some/path"),
        equalTo("http://not_localhost:8282/ddf/some/path"));
    assertThat(
        SystemBaseUrl.EXTERNAL.constructUrl(null, null), equalTo("https://not_localhost:8994/ddf"));
    assertThat(
        SystemBaseUrl.EXTERNAL.constructUrl("http", null),
        equalTo("http://not_localhost:8282/ddf"));
  }

  @Test
  public void testRootContext() {
    System.clearProperty("org.codice.ddf.system.rootContext");
    assertThat(SystemBaseUrl.INTERNAL.getRootContext(), equalTo(""));
    System.setProperty("org.codice.ddf.system.rootContext", "/services");
    assertThat(SystemBaseUrl.INTERNAL.getRootContext(), equalTo("/services"));

    assertThat(
        SystemBaseUrl.INTERNAL.constructUrl("/some/path", true),
        equalTo("https://localhost:8993/services/some/path"));
    System.setProperty("org.codice.ddf.system.rootContext", "services");
    assertThat(
        SystemBaseUrl.INTERNAL.constructUrl("/some/path", true),
        equalTo("https://localhost:8993/services/some/path"));

    System.setProperty("org.codice.ddf.external.context", "/ddf");
    assertThat(SystemBaseUrl.EXTERNAL.getRootContext(), equalTo("/ddf"));
    assertThat(
        SystemBaseUrl.EXTERNAL.constructUrl("/some/path", true),
        equalTo("https://not_localhost:8994/ddf/services/some/path"));
    System.setProperty("org.codice.ddf.external.context", "ddf");
    assertThat(
        SystemBaseUrl.EXTERNAL.constructUrl("/some/path", true),
        equalTo("https://not_localhost:8994/ddf/services/some/path"));
  }
}
