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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.codice.ddf.platform.filter.FilterChain;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.junit.Test;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class BasicAuthenticationHandlerTest {

  private static final String USERNAME = "admin";

  private static final String PASSWORD = "password";

  private static final String USERNAME_ATTR = "Username";

  private static final String PASSWORD_ATTR = "Password";

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

    HandlerResult result = handler.getNormalizedToken(request, response, chain, true);

    assertNotNull(result);
    assertEquals(HandlerResult.Status.REDIRECTED, result.getStatus());
    // confirm that the proper responses were sent through the HttpResponse
    Mockito.verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    Mockito.verify(response).setContentLength(0);
    Mockito.verify(response).flushBuffer();
  }

  /**
   * This test case handles the scenario in which the credentials should be obtained (i.e. resolve
   * flag is set) - both requests without and with the credentials are tested.
   */
  @Test
  public void testGetNormalizedTokenResolveWithCredentials() throws Exception {
    BasicAuthenticationHandler handler = new BasicAuthenticationHandler();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getHeader(HttpHeaders.AUTHORIZATION))
        .thenReturn("Basic " + Base64.getEncoder().encodeToString(CREDENTIALS.getBytes()));

    HandlerResult result = handler.getNormalizedToken(request, response, chain, true);

    assertNotNull(result);
    assertEquals(HandlerResult.Status.COMPLETED, result.getStatus());
    assertEquals("admin", getXmlAttributeValue(result.getToken(), USERNAME_ATTR));
    assertEquals("password", getXmlAttributeValue(result.getToken(), PASSWORD_ATTR));

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
  public void testGetNormalizedTokenResolveCompleted() throws Exception {
    BasicAuthenticationHandler handler = new BasicAuthenticationHandler();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getHeader(HttpHeaders.AUTHORIZATION))
        .thenReturn("Basic " + Base64.getEncoder().encodeToString(CREDENTIALS.getBytes()));

    HandlerResult result = handler.getNormalizedToken(request, response, chain, true);

    assertNotNull(result);
    assertEquals(HandlerResult.Status.COMPLETED, result.getStatus());
    assertEquals("admin", getXmlAttributeValue(result.getToken(), USERNAME_ATTR));
  }

  /**
   * This test case handles the scenario in which the credentials are not to be obtained (i.e.
   * resolve flag is not set) and the UsernameTokenType was successfully created from the HTTP
   * request.
   */
  @Test
  public void testGetNormalizedTokenNoResolveCompleted() throws Exception {
    BasicAuthenticationHandler handler = new BasicAuthenticationHandler();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getHeader(HttpHeaders.AUTHORIZATION))
        .thenReturn("Basic " + Base64.getEncoder().encodeToString(CREDENTIALS.getBytes()));

    HandlerResult result = handler.getNormalizedToken(request, response, chain, false);

    assertNotNull(result);
    assertEquals(HandlerResult.Status.COMPLETED, result.getStatus());
    assertEquals("admin", getXmlAttributeValue(result.getToken(), USERNAME_ATTR));
  }

  @Test
  public void testExtractAuthInfo() throws Exception {
    BasicAuthenticationHandler handler = new BasicAuthenticationHandler();
    BaseAuthenticationToken result =
        handler.extractAuthInfo(
            "Basic " + Base64.getEncoder().encodeToString(CREDENTIALS.getBytes()), "127.0.0.1");
    assertNotNull(result);
    assertEquals("admin", getXmlAttributeValue(result, USERNAME_ATTR));
    assertEquals("password", getXmlAttributeValue(result, PASSWORD_ATTR));

    handler = new BasicAuthenticationHandler();
    result =
        handler.extractAuthInfo(
            "Basic " + Base64.getEncoder().encodeToString(CREDENTIALS.getBytes()), "127.0.0.1");
    assertNotNull(result);

    result =
        handler.extractAuthInfo(
            "Basic " + Base64.getEncoder().encodeToString(":password".getBytes()), "127.0.0.1");
    assertNotNull(result);
    assertEquals("", getXmlAttributeValue(result, USERNAME_ATTR));
    assertEquals("password", getXmlAttributeValue(result, PASSWORD_ATTR));

    result =
        handler.extractAuthInfo(
            "Basic " + Base64.getEncoder().encodeToString("user:".getBytes()), "127.0.0.1");
    assertNotNull(result);
    assertEquals("user", getXmlAttributeValue(result, USERNAME_ATTR));
    assertEquals("", getXmlAttributeValue(result, PASSWORD_ATTR));

    result =
        handler.extractAuthInfo(
            "Basic " + Base64.getEncoder().encodeToString("user/password".getBytes()), "127.0.0.1");
    assertNull(result);

    result =
        handler.extractAuthInfo(
            "Basic " + Base64.getEncoder().encodeToString("".getBytes()), "127.0.0.1");
    assertNull(result);
  }

  @Test
  public void testExtractAuthenticationInfo() throws Exception {
    // only test valid authorization header and missing header - invalid values are tested in
    // textExtractAuthInfo
    BasicAuthenticationHandler handler = new BasicAuthenticationHandler();
    HttpServletRequest request = mock(HttpServletRequest.class);

    when(request.getHeader(HttpHeaders.AUTHORIZATION))
        .thenReturn("Basic " + Base64.getEncoder().encodeToString(CREDENTIALS.getBytes()));

    BaseAuthenticationToken result = handler.extractAuthenticationInfo(request);
    assertNotNull(result);
    assertEquals("admin", getXmlAttributeValue(result, USERNAME_ATTR));
    assertEquals("password", getXmlAttributeValue(result, PASSWORD_ATTR));

    result = handler.extractAuthenticationInfo(request);
    assertNotNull(result);
    assertEquals("admin", getXmlAttributeValue(result, USERNAME_ATTR));
    assertEquals("password", getXmlAttributeValue(result, PASSWORD_ATTR));

    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
    result = handler.extractAuthenticationInfo(request);
    assertNull(result);
  }

  private String getXmlAttributeValue(BaseAuthenticationToken token, String attribute)
      throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    ByteArrayInputStream input =
        new ByteArrayInputStream(token.getCredentialsAsString().getBytes("UTF-8"));
    Document doc = builder.parse(input);
    NodeList children = doc.getDocumentElement().getChildNodes();

    int childIndex = 0;
    while (childIndex < children.getLength()) {
      Node child = children.item(childIndex);
      if (child.getNodeName().contains(attribute)) {
        if (child.getFirstChild() != null) {
          return child.getFirstChild().getNodeValue();
        } else {
          return "";
        }
      }
      childIndex++;
    }
    return "";
  }
}
