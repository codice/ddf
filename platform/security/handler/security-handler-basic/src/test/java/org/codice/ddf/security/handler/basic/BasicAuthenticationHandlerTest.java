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
package org.codice.ddf.security.handler.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Base64;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.xml.XmlParser;
import org.codice.ddf.platform.filter.FilterChain;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.UPAuthenticationToken;
import org.junit.Test;
import org.mockito.Mockito;

public class BasicAuthenticationHandlerTest {

  private static final String USERNAME = "admin";

  private static final String PASSWORD = "password";

  private static final String CREDENTIALS = USERNAME + ":" + PASSWORD;

  /**
   * This test case handles the scenario in which the credentials should be obtained (i.e. resolve
   * flag is set) - both requests without and with the credentials are tested.
   */
  @Test
  public void testGetNormalizedTokenResolveWithoutCredentials() throws IOException {
    BasicAuthenticationHandler handler = new BasicAuthenticationHandler();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getAttribute(anyString())).thenReturn("karaf");
    HandlerResult result = handler.getNormalizedToken(request, response, chain, true);

    assertNotNull(result);
    assertEquals(HandlerResult.Status.REDIRECTED, result.getStatus());
    // confirm that the proper responses were sent through the HttpResponse
    Mockito.verify(response).setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"karaf\"");
    Mockito.verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    Mockito.verify(response).setContentLength(0);
    Mockito.verify(response).flushBuffer();
  }

  /**
   * This test case handles the scenario in which the credentials should be obtained (i.e. resolve
   * flag is set) - both requests without and with the credentials are tested.
   */
  @Test
  public void testGetNormalizedTokenResolveWithCredentials() throws IOException {
    BasicAuthenticationHandler handler = new BasicAuthenticationHandler();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getAttribute(anyString())).thenReturn("TestRealm");
    when(request.getHeader(HttpHeaders.AUTHORIZATION))
        .thenReturn("Basic " + Base64.getEncoder().encodeToString(CREDENTIALS.getBytes()));

    HandlerResult result = handler.getNormalizedToken(request, response, chain, true);

    assertNotNull(result);
    assertEquals(HandlerResult.Status.COMPLETED, result.getStatus());
    assertEquals("admin", result.getToken().getPrincipal());
    assertEquals("password", result.getToken().getCredentials());
    assertEquals("TestRealm", result.getToken().getRealm());

    // confirm that no responses were sent through the HttpResponse
    Mockito.verify(response, never()).setHeader(anyString(), anyString());
    Mockito.verify(response, never()).setStatus(anyInt());
    Mockito.verify(response, never()).setContentLength(anyInt());
    Mockito.verify(response, never()).flushBuffer();
  }

  /**
   * This test case handles the scenario in which the credentials should be obtained (i.e. resolve
   * flag is set) and UsernameTokenType was created from the HTTP request.
   */
  @Test
  public void testGetNormalizedTokenResolveCompleted() {
    BasicAuthenticationHandler handler = new BasicAuthenticationHandler();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getHeader(HttpHeaders.AUTHORIZATION))
        .thenReturn("Basic " + Base64.getEncoder().encodeToString(CREDENTIALS.getBytes()));

    HandlerResult result = handler.getNormalizedToken(request, response, chain, true);

    assertNotNull(result);
    assertEquals(HandlerResult.Status.COMPLETED, result.getStatus());
    assertEquals("admin", result.getToken().getPrincipal());
  }

  /**
   * This test case handles the scenario in which the credentials are not to be obtained (i.e.
   * resolve flag is not set) and the UsernameTokenType could not be created with the HTTP request.
   */
  @Test
  public void testGetNormalizedTokenNoResolveNoAction() {
    BasicAuthenticationHandler handler = new BasicAuthenticationHandler();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    HandlerResult result = handler.getNormalizedToken(request, response, chain, false);

    assertNotNull(result);
    assertEquals(HandlerResult.Status.NO_ACTION, result.getStatus());
  }

  /**
   * This test case handles the scenario in which the credentials are not to be obtained (i.e.
   * resolve flag is not set) and the UsernameTokenType was successfully created from the HTTP
   * request.
   */
  @Test
  public void testGetNormalizedTokenNoResolveCompleted() {
    BasicAuthenticationHandler handler = new BasicAuthenticationHandler();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getHeader(HttpHeaders.AUTHORIZATION))
        .thenReturn("Basic " + Base64.getEncoder().encodeToString(CREDENTIALS.getBytes()));

    HandlerResult result = handler.getNormalizedToken(request, response, chain, false);

    assertNotNull(result);
    assertEquals(HandlerResult.Status.COMPLETED, result.getStatus());
    assertEquals("admin", result.getToken().getPrincipal());
  }

  @Test
  public void testExtractAuthInfo() {
    Parser parser = new XmlParser();
    BasicAuthenticationHandler handler = new BasicAuthenticationHandler();
    UPAuthenticationToken result =
        (UPAuthenticationToken)
            handler.extractAuthInfo(
                "Basic " + Base64.getEncoder().encodeToString(CREDENTIALS.getBytes()), "TestRealm");
    assertNotNull(result);
    assertEquals("admin", result.getUsername());
    assertEquals("password", result.getPassword());
    assertEquals("TestRealm", result.getRealm());

    WssBasicAuthenticationHandler wssHandler = new WssBasicAuthenticationHandler(parser);
    BaseAuthenticationToken wssResult =
        wssHandler.extractAuthInfo(
            "Basic " + Base64.getEncoder().encodeToString(CREDENTIALS.getBytes()), "TestRealm");
    assertNotNull(wssResult);
    assertEquals("", wssResult.getRealm());

    result =
        (UPAuthenticationToken)
            handler.extractAuthInfo(
                "Basic " + Base64.getEncoder().encodeToString(":password".getBytes()), "TestRealm");
    assertNotNull(result);
    assertEquals("", result.getUsername());
    assertEquals("password", result.getPassword());
    assertEquals("TestRealm", result.getRealm());

    result =
        (UPAuthenticationToken)
            handler.extractAuthInfo(
                "Basic " + Base64.getEncoder().encodeToString("user:".getBytes()), "TestRealm");
    assertNotNull(result);
    assertEquals("user", result.getUsername());
    assertEquals("", result.getPassword());
    assertEquals("TestRealm", result.getRealm());

    result =
        (UPAuthenticationToken)
            handler.extractAuthInfo(
                "Basic " + Base64.getEncoder().encodeToString("user/password".getBytes()),
                "TestRealm");
    assertNull(result);

    result =
        (UPAuthenticationToken)
            handler.extractAuthInfo(
                "Basic " + Base64.getEncoder().encodeToString("".getBytes()), "TestRealm");
    assertNull(result);
  }

  @Test
  public void testExtractAuthenticationInfo() {
    // only test valid authorization header and missing header - invalid values are tested in
    // textExtractAuthInfo
    BasicAuthenticationHandler handler = new BasicAuthenticationHandler();
    HttpServletRequest request = mock(HttpServletRequest.class);

    when(request.getHeader(HttpHeaders.AUTHORIZATION))
        .thenReturn("Basic " + Base64.getEncoder().encodeToString(CREDENTIALS.getBytes()));
    when(request.getAttribute(anyString())).thenReturn("karaf");

    UPAuthenticationToken result =
        (UPAuthenticationToken) handler.extractAuthenticationInfo(request);
    assertNotNull(result);
    assertEquals("admin", result.getUsername());
    assertEquals("password", result.getPassword());
    assertEquals(BaseAuthenticationToken.DEFAULT_REALM, result.getRealm());

    result = (UPAuthenticationToken) handler.extractAuthenticationInfo(request);
    assertNotNull(result);
    assertEquals("admin", result.getUsername());
    assertEquals("password", result.getPassword());
    assertEquals("karaf", result.getRealm());

    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
    result = (UPAuthenticationToken) handler.extractAuthenticationInfo(request);
    assertNull(result);
  }

  @Test(expected = IllegalStateException.class)
  public void testIllegalStateException() {
    BasicAuthenticationHandler handler = new BasicAuthenticationHandler();
    UPAuthenticationToken result =
        (UPAuthenticationToken)
            handler.extractAuthInfo(
                "Basic " + Base64.getEncoder().encodeToString(CREDENTIALS.getBytes()), "TestRealm");
    assertNotNull(result);
    assertEquals("admin", result.getUsername());
    assertEquals("password", result.getPassword());
    assertEquals("TestRealm", result.getRealm());

    WssBasicAuthenticationHandler wssHandler = new WssBasicAuthenticationHandler(null);
    BaseAuthenticationToken wssResult =
        wssHandler.extractAuthInfo(
            "Basic " + Base64.getEncoder().encodeToString(CREDENTIALS.getBytes()), "TestRealm");
  }
}
