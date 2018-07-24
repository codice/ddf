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
package org.codice.ddf.security.handler.cas;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.security.handler.cas.filter.FilteredRequest;
import org.junit.Before;
import org.junit.Test;

public class FilteredRequestTest {

  private HttpServletRequest httpServletRequest;

  private String https = "https://";
  private String internalHost = "localhost";
  private String internalPort = "8993";
  private String internalBaseUrl = String.format("https://%s:%s", internalHost, internalPort);
  private String externalHost = "reverse.proxy";
  private String externalPort = "443";
  private String externalBaseUrl = String.format("https://%s:%s", externalHost, externalPort);
  private String externalContext = "/ddf";
  private String servicePath = "/path/to/service";

  public void defaultConfig() {
    System.setProperty(SystemBaseUrl.INTERNAL_PROTOCOL, https);
    System.setProperty(SystemBaseUrl.INTERNAL_HOST, internalHost);
    System.setProperty(SystemBaseUrl.INTERNAL_PORT, internalPort);
    System.setProperty(SystemBaseUrl.EXTERNAL_PROTOCOL, https);
    System.setProperty(SystemBaseUrl.EXTERNAL_HOST, internalHost);
    System.setProperty(SystemBaseUrl.EXTERNAL_PORT, internalPort);
    System.setProperty(SystemBaseUrl.EXTERNAL_CONTEXT, "");
  }

  public void reverseProxyConfig() {
    System.setProperty(SystemBaseUrl.INTERNAL_PROTOCOL, https);
    System.setProperty(SystemBaseUrl.INTERNAL_HOST, internalHost);
    System.setProperty(SystemBaseUrl.INTERNAL_PORT, internalPort);
    System.setProperty(SystemBaseUrl.EXTERNAL_PROTOCOL, https);
    System.setProperty(SystemBaseUrl.EXTERNAL_HOST, externalHost);
    System.setProperty(SystemBaseUrl.EXTERNAL_PORT, externalPort);
    System.setProperty(SystemBaseUrl.EXTERNAL_CONTEXT, externalContext);
  }

  @Before
  public void initialize() {
    httpServletRequest = mock(HttpServletRequest.class);
    when(httpServletRequest.getRequestURI()).thenReturn(servicePath);
  }

  @Test
  public void testDefaultConfig() {
    defaultConfig();
    FilteredRequest filteredRequest = new FilteredRequest(httpServletRequest, internalBaseUrl);

    assertEquals(filteredRequest.getContextPath(), "");
    assertEquals(filteredRequest.getRequestURI(), servicePath);
    assertEquals(filteredRequest.getRequestURL().toString(), internalBaseUrl + servicePath);
  }

  @Test
  public void testProxyConfigUnmatchedServerName() {
    reverseProxyConfig();
    FilteredRequest filteredRequest =
        new FilteredRequest(httpServletRequest, "https://example.com/");

    assertEquals(filteredRequest.getRequestURL().toString(), "https://example.com" + servicePath);
  }

  @Test
  public void testDefaultConfigMalformedServerName() {
    defaultConfig();
    String invalidUri = "not^a^valid^uri";
    FilteredRequest filteredRequest = new FilteredRequest(httpServletRequest, invalidUri);

    assertEquals(filteredRequest.getRequestURL().toString(), invalidUri + servicePath);
  }

  @Test
  public void testProxyConfigInternalRequest() {
    reverseProxyConfig();
    FilteredRequest filteredRequest = new FilteredRequest(httpServletRequest, internalBaseUrl);

    assertEquals(filteredRequest.getContextPath(), "");
    assertEquals(filteredRequest.getRequestURI(), servicePath);
    assertEquals(filteredRequest.getRequestURL().toString(), internalBaseUrl + servicePath);
  }

  @Test
  public void testProxyConfigExternalRequest() {
    reverseProxyConfig();
    FilteredRequest filteredRequest = new FilteredRequest(httpServletRequest, externalBaseUrl);

    assertEquals(filteredRequest.getContextPath(), externalContext);
    assertEquals(filteredRequest.getRequestURI(), externalContext + servicePath);
    assertEquals(
        filteredRequest.getRequestURL().toString(),
        externalBaseUrl + externalContext + servicePath);
  }

  @Test
  public void testProxyConfigNestedWrapping() {
    reverseProxyConfig();
    FilteredRequest filteredRequest = new FilteredRequest(httpServletRequest, externalBaseUrl);
    FilteredRequest filteredRequest1 = new FilteredRequest(filteredRequest, externalBaseUrl);

    assertEquals(filteredRequest1.getContextPath(), externalContext);
    assertEquals(filteredRequest1.getRequestURI(), externalContext + servicePath);
    assertEquals(
        filteredRequest1.getRequestURL().toString(),
        externalBaseUrl + externalContext + servicePath);
  }
}
