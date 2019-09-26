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
package org.codice.ddf.security.session.management.impl;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.assertion.impl.SecurityAssertionDefault;
import ddf.security.assertion.jwt.impl.SecurityAssertionJwt;
import ddf.security.assertion.saml.impl.SecurityAssertionSaml;
import ddf.security.common.SecurityTokenHolder;
import ddf.security.http.SessionFactory;
import ddf.security.principal.GuestPrincipal;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.SAMLAuthenticationToken;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class SessionManagementServiceImplTest {
  private HttpServletRequest request;

  private SecurityTokenHolder tokenHolder;

  private SecurityManager manager;

  private SessionManagementServiceImpl sessionManagementServiceImpl;

  private SimplePrincipalCollection principalCollection;

  private PrincipalCollection refreshedPrincipalCollection;

  private SessionFactory sessionFactory;

  private HttpSession session;

  private void setSecurityAssertions(SecurityAssertion... assertions) {
    Collection<SecurityAssertion> assertionList = Arrays.asList(assertions);
    principalCollection.addAll(assertionList, "realm");
  }

  private SecurityAssertion getGuestAssertion() {
    SecurityAssertion guestAssertion = mock(SecurityAssertionDefault.class);
    GuestPrincipal guestPrincipal = mock(GuestPrincipal.class);
    when(guestAssertion.getWeight()).thenReturn(SecurityAssertion.NO_AUTH_WEIGHT);
    when(guestPrincipal.getName()).thenReturn("guest");
    when(guestAssertion.getPrincipal()).thenReturn(guestPrincipal);
    when(guestAssertion.getNotOnOrAfter())
        .thenReturn(Date.from(Instant.now().plus(Duration.ofHours(4))));
    return guestAssertion;
  }

  private SecurityAssertion getSamlAssertion() throws Exception {
    SecurityToken securityToken = mock(SecurityToken.class);
    SecurityAssertion principal = mock(SecurityAssertion.class);
    when(principal.getToken()).thenReturn(securityToken);
    when(securityToken.getToken())
        .thenReturn(
            readXml(getClass().getClassLoader().getResourceAsStream("saml.xml"))
                .getDocumentElement());
    return new SecurityAssertionSaml(securityToken);
  }

  private SecurityAssertion getOidcAssertion() throws Exception {
    SecurityAssertion oidcAssertion = mock(SecurityAssertionJwt.class);
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("oidcuser");
    when(oidcAssertion.getPrincipal()).thenReturn(principal);
    when(oidcAssertion.getNotOnOrAfter())
        .thenReturn(Date.from(Instant.now().plus(Duration.ofHours(4))));
    return oidcAssertion;
  }

  @Before
  public void setup()
      throws ParserConfigurationException, SAXException, IOException, SecurityServiceException {
    request = mock(HttpServletRequest.class);
    principalCollection = new SimplePrincipalCollection();
    refreshedPrincipalCollection = mock(PrincipalCollection.class);
    tokenHolder = mock(SecurityTokenHolder.class);
    manager = mock(SecurityManager.class);
    sessionFactory = mock(SessionFactory.class);
    session = mock(HttpSession.class);
    Subject subject = mock(Subject.class);

    when(request.getSession(false)).thenReturn(session);
    when(tokenHolder.getPrincipals()).thenReturn(principalCollection);
    when(manager.getSubject(isA(BaseAuthenticationToken.class))).thenReturn(subject);
    when(sessionFactory.getOrCreateSession(any())).thenReturn(session);
    when(session.getAttribute(SecurityConstants.SECURITY_TOKEN_KEY)).thenReturn(tokenHolder);
    when(session.getMaxInactiveInterval()).thenReturn(Integer.MAX_VALUE);
    when(subject.getPrincipals()).thenReturn(refreshedPrincipalCollection);

    sessionManagementServiceImpl = new SessionManagementServiceImpl();
    sessionManagementServiceImpl.setSecurityManager(manager);
    sessionManagementServiceImpl.setSessionFactory(sessionFactory);
    sessionManagementServiceImpl.setClock(
        Clock.fixed(
            Instant.parse("2113-04-24T00:09:54.788Z").minus(Duration.ofHours(5)),
            ZoneId.of("UTC")));
  }

  @Test
  public void testGetExpiryAssertionExpiresFirst() throws Exception {
    setSecurityAssertions(getSamlAssertion());

    String expiryString = sessionManagementServiceImpl.getExpiry(request);
    assertThat(expiryString, is(Long.toString(Duration.ofHours(5).toMillis())));
  }

  @Test
  public void testGetExpirySessionExpiresFirst() throws Exception {
    setSecurityAssertions(getSamlAssertion());
    when(session.getMaxInactiveInterval())
        .thenReturn((int) (Duration.ofHours(1).toMillis() / 1000));

    String expiryString = sessionManagementServiceImpl.getExpiry(request);
    assertThat(expiryString, is(Long.toString(Duration.ofHours(1).toMillis())));
  }

  @Test
  public void testGetExpiryMultipleAssertions() throws Exception {
    SecurityToken soonerToken = mock(SecurityToken.class);
    String saml =
        IOUtils.toString(
            new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("saml.xml"),
                StandardCharsets.UTF_8));
    saml = saml.replace("2113-04-24", "2113-04-25");
    when(soonerToken.getToken())
        .thenReturn(readXml(IOUtils.toInputStream(saml, "UTF-8")).getDocumentElement());
    SecurityToken laterToken = mock(SecurityToken.class);
    saml =
        IOUtils.toString(
            new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("saml.xml"),
                StandardCharsets.UTF_8));
    saml = saml.replace("2113-04-24", "2113-04-26");
    when(laterToken.getToken())
        .thenReturn(readXml(IOUtils.toInputStream(saml, "UTF-8")).getDocumentElement());
    SecurityAssertion securityAssertion = new SecurityAssertionSaml(soonerToken);
    principalCollection.add(securityAssertion, "realm");

    String expiryString = sessionManagementServiceImpl.getExpiry(request);

    assertThat(expiryString, is(Long.toString(Duration.ofHours(29).toMillis())));
  }

  @Test
  public void testGetRenewalGuest() throws Exception {
    setSecurityAssertions(getGuestAssertion());

    String renewalString = sessionManagementServiceImpl.getRenewal(request);
    assertNotNull(renewalString);
    verify(tokenHolder).setPrincipals(refreshedPrincipalCollection);
  }

  @Test
  public void testGetRenewalSamlOnly() throws Exception {
    setSecurityAssertions(getSamlAssertion());

    String renewalString = sessionManagementServiceImpl.getRenewal(request);
    assertNotNull(renewalString);
    verify(tokenHolder).setPrincipals(refreshedPrincipalCollection);
  }

  @Test
  public void testGetRenewalSamlAndGuest() throws Exception {
    setSecurityAssertions(getSamlAssertion(), getGuestAssertion());

    String renewalString = sessionManagementServiceImpl.getRenewal(request);
    assertNotNull(renewalString);
    verify(tokenHolder).setPrincipals(refreshedPrincipalCollection);
  }

  @Test
  public void testGetRenewalNonRenewableAssertion() throws Exception {
    setSecurityAssertions(getOidcAssertion());

    String renewalString = sessionManagementServiceImpl.getRenewal(request);
    assertNotNull(renewalString);
    verify(tokenHolder, never()).setPrincipals(principalCollection);
  }

  @Test
  public void testGetRenewalFails() throws Exception {
    setSecurityAssertions(getSamlAssertion());

    when(manager.getSubject(isA(SAMLAuthenticationToken.class)))
        .thenThrow(new SecurityServiceException());
    String renewalString = sessionManagementServiceImpl.getRenewal(request);
    assertEquals("0", renewalString);
  }

  @Test
  public void testGetInvalidateNoQueryString() throws Exception {
    setSecurityAssertions(getSamlAssertion());

    when(request.getRequestURL())
        .thenReturn(
            new StringBuffer("https://localhost:8993/services/internal/session/invalidate"));
    when(request.getQueryString()).thenReturn(null);
    URI invalidateUri = sessionManagementServiceImpl.getInvalidate(request);
    assertThat(invalidateUri, is(equalTo(URI.create("https://localhost:8993/logout"))));
  }

  @Test
  public void testGetInvalidateWithQueryString() throws Exception {
    setSecurityAssertions(getSamlAssertion());

    when(request.getRequestURL())
        .thenReturn(
            new StringBuffer(
                "https://localhost:8993/services/internal/session/invalidate?service=admin/"));
    when(request.getQueryString()).thenReturn("service=admin/");
    URI invalidateUri = sessionManagementServiceImpl.getInvalidate(request);
    assertThat(
        invalidateUri, is(equalTo(URI.create("https://localhost:8993/logout?service=admin/"))));
  }

  private static Document readXml(InputStream is)
      throws SAXException, IOException, ParserConfigurationException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    dbf.setValidating(false);
    dbf.setIgnoringComments(false);
    dbf.setIgnoringElementContentWhitespace(true);
    dbf.setNamespaceAware(true);

    DocumentBuilder db = dbf.newDocumentBuilder();
    db.setEntityResolver(new DOMUtils.NullResolver());

    return db.parse(is);
  }
}
