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
package org.codice.ddf.security.handler.saml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.security.SecurityConstants;
import ddf.security.common.SecurityTokenHolder;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.codice.ddf.platform.filter.FilterChain;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class SAMLAssertionHandlerTest {

  public static Document readXml(InputStream is)
      throws SAXException, IOException, ParserConfigurationException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    dbf.setValidating(false);
    dbf.setIgnoringComments(false);
    dbf.setIgnoringElementContentWhitespace(true);
    dbf.setNamespaceAware(true);
    // dbf.setCoalescing(true);
    // dbf.setExpandEntityReferences(true);

    DocumentBuilder db = null;
    db = dbf.newDocumentBuilder();
    db.setEntityResolver(new DOMUtils.NullResolver());

    // db.setErrorHandler( new MyErrorHandler());

    return db.parse(is);
  }

  /**
   * This test ensures the proper functionality of SAMLAssertionHandler's method,
   * getNormalizedToken(), when given a valid HttpServletRequest.
   */
  @Test
  public void testGetNormalizedTokenSuccessWithHeader() throws Exception {
    SAMLAssertionHandler handler = new SAMLAssertionHandler();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    Element assertion = readDocument("/saml.xml").getDocumentElement();
    String assertionId = assertion.getAttributeNodeNS(null, "ID").getNodeValue();
    SecurityToken samlToken = new SecurityToken(assertionId, assertion, null);
    SamlAssertionWrapper wrappedAssertion = new SamlAssertionWrapper(samlToken.getToken());
    String saml = wrappedAssertion.assertionToString();

    doReturn("SAML " + RestSecurity.deflateAndBase64Encode(saml))
        .when(request)
        .getHeader(SecurityConstants.SAML_HEADER_NAME);

    HandlerResult result = handler.getNormalizedToken(request, response, chain, true);

    assertNotNull(result);
    assertEquals(HandlerResult.Status.COMPLETED, result.getStatus());
  }

  /**
   * This test ensures the proper functionality of SAMLAssertionHandler's method,
   * getNormalizedToken(), when given an invalid HttpServletRequest.
   */
  @Test
  public void testGetNormalizedTokenFailureWithHeader() {
    SAMLAssertionHandler handler = new SAMLAssertionHandler();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    doReturn(null).when(request).getHeader(SecurityConstants.SAML_HEADER_NAME);

    HandlerResult result = handler.getNormalizedToken(request, response, chain, true);

    assertNotNull(result);
    assertEquals(HandlerResult.Status.NO_ACTION, result.getStatus());
  }

  /**
   * This test ensures the proper functionality of SAMLAssertionHandler's method,
   * getNormalizedToken(), when given a valid HttpServletRequest. Uses legacy SAML cookie
   */
  @Test
  public void testGetNormalizedTokenSuccessWithCookie() throws Exception {
    SAMLAssertionHandler handler = new SAMLAssertionHandler();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    Element assertion = readDocument("/saml.xml").getDocumentElement();
    String assertionId = assertion.getAttributeNodeNS(null, "ID").getNodeValue();
    SecurityToken samlToken = new SecurityToken(assertionId, assertion, null);
    SamlAssertionWrapper wrappedAssertion = new SamlAssertionWrapper(samlToken.getToken());
    String saml = wrappedAssertion.assertionToString();
    Cookie cookie =
        new Cookie(SecurityConstants.SAML_COOKIE_NAME, RestSecurity.deflateAndBase64Encode(saml));
    when(request.getCookies()).thenReturn(new Cookie[] {cookie});

    HandlerResult result = handler.getNormalizedToken(request, response, chain, true);

    assertNotNull(result);
    assertEquals(HandlerResult.Status.COMPLETED, result.getStatus());
  }

  /**
   * This test ensures the proper functionality of SAMLAssertionHandler's method,
   * getNormalizedToken(), when given an invalid HttpServletRequest. Uses legacy SAML cookie
   */
  @Test
  public void testGetNormalizedTokenFailurewithCookie() {
    SAMLAssertionHandler handler = new SAMLAssertionHandler();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getCookies()).thenReturn(null);

    HandlerResult result = handler.getNormalizedToken(request, response, chain, true);

    assertNotNull(result);
    assertEquals(HandlerResult.Status.NO_ACTION, result.getStatus());
  }

  @Test
  public void testGetNormalizedTokenFromSession() throws Exception {
    SAMLAssertionHandler handler = new SAMLAssertionHandler();

    HttpServletRequest request = mock(HttpServletRequest.class);

    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getCookies()).thenReturn(null);
    HttpSession session = mock(HttpSession.class);
    when(request.getSession(false)).thenReturn(session);
    when(request.getAttribute(ContextPolicy.ACTIVE_REALM)).thenReturn("foo");
    SecurityTokenHolder tokenHolder = mock(SecurityTokenHolder.class);
    when(session.getAttribute(SecurityConstants.SAML_ASSERTION)).thenReturn(tokenHolder);
    SecurityToken securityToken = mock(SecurityToken.class);
    when(tokenHolder.getSecurityToken("foo")).thenReturn(securityToken);
    when(securityToken.getToken()).thenReturn(readDocument("/saml.xml").getDocumentElement());

    HandlerResult result = handler.getNormalizedToken(request, response, chain, true);

    assertNotNull(result);
    assertEquals(HandlerResult.Status.COMPLETED, result.getStatus());
  }

  /**
   * Reads a classpath resource into a Document.
   *
   * @param name the name of the classpath resource
   */
  private Document readDocument(String name)
      throws SAXException, IOException, ParserConfigurationException {
    InputStream inStream = getClass().getResourceAsStream(name);
    return readXml(inStream);
  }
}
