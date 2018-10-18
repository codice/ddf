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
package org.codice.ddf.security.filter.login;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.common.SecurityTokenHolder;
import ddf.security.http.SessionFactory;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.codice.ddf.platform.filter.AuthenticationException;
import org.codice.ddf.platform.filter.AuthenticationFailureException;
import org.codice.ddf.platform.filter.FilterChain;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.SAMLAuthenticationToken;
import org.codice.ddf.security.handler.api.UPAuthenticationToken;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class LoginFilterTest {
  SessionFactory sessionFactory;

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

  @BeforeClass
  public static void init() {
    OpenSAMLUtil.initSamlEngine();
  }

  @Before
  public void setup() {
    sessionFactory = mock(SessionFactory.class);
  }

  @Test
  public void testNoSubject() {
    LoginFilter loginFilter = new LoginFilter();
    loginFilter.setSessionFactory(sessionFactory);
    loginFilter.init();
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    HttpServletResponse servletResponse = mock(HttpServletResponse.class);
    FilterChain filterChain =
        new FilterChain() {
          @Override
          public void doFilter(ServletRequest request, ServletResponse response) {
            fail("Should not have called doFilter without a valid Subject");
          }
        };

    try {
      loginFilter.doFilter(servletRequest, servletResponse, filterChain);
    } catch (IOException e) {
      fail(e.getMessage());
    } catch (AuthenticationException e) {
      fail(e.getMessage());
    }
  }

  /**
   * Test with a bad subject - shouldn't call the filter chain, just returns.
   *
   * @throws IOException
   */
  @Test
  public void testBadSubject() throws IOException, AuthenticationException {
    LoginFilter loginFilter = new LoginFilter();
    loginFilter.setSessionFactory(sessionFactory);
    loginFilter.init();
    HttpServletRequest servletRequest = new TestHttpServletRequest();
    servletRequest.setAttribute("ddf.security.securityToken", mock(SecurityToken.class));
    HttpServletResponse servletResponse = mock(HttpServletResponse.class);
    FilterChain filterChain =
        new FilterChain() {
          @Override
          public void doFilter(ServletRequest request, ServletResponse response) {
            fail("Should not have continued down the filter chain without a valid Subject");
          }
        };

    loginFilter.doFilter(servletRequest, servletResponse, filterChain);
  }

  @Test
  public void testValidEmptySubject() throws IOException, AuthenticationException {
    LoginFilter loginFilter = new LoginFilter();
    loginFilter.setSessionFactory(sessionFactory);
    loginFilter.init();

    HttpServletRequest servletRequest = new TestHttpServletRequest();
    servletRequest.setAttribute("ddf.security.token", mock(HandlerResult.class));
    HttpServletResponse servletResponse = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);

    loginFilter.doFilter(servletRequest, servletResponse, filterChain);

    verify(filterChain, never()).doFilter(servletRequest, servletResponse);
  }

  @Test
  public void testValidUsernameToken()
      throws IOException, ParserConfigurationException, SAXException, SecurityServiceException,
          AuthenticationException {
    LoginFilter loginFilter = new LoginFilter();
    loginFilter.setSessionFactory(sessionFactory);
    ddf.security.service.SecurityManager securityManager =
        mock(ddf.security.service.SecurityManager.class);
    loginFilter.setSecurityManager(securityManager);
    loginFilter.init();

    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    HttpServletResponse servletResponse = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);

    UPAuthenticationToken token = new UPAuthenticationToken("foo", "bar");
    HandlerResult result = new HandlerResult(HandlerResult.Status.COMPLETED, token);
    when(servletRequest.getAttribute("ddf.security.token")).thenReturn(result);

    HttpSession session = mock(HttpSession.class);
    when(servletRequest.getSession(true)).thenReturn(session);
    when(session.getAttribute(SecurityConstants.SAML_ASSERTION))
        .thenReturn(new SecurityTokenHolder());
    when(session.getId()).thenReturn("sessionId");
    when(sessionFactory.getOrCreateSession(servletRequest)).thenReturn(session);

    Subject subject = mock(Subject.class, RETURNS_DEEP_STUBS);
    when(securityManager.getSubject(token)).thenReturn(subject);
    SecurityAssertion assertion = mock(SecurityAssertion.class);
    SecurityToken securityToken = mock(SecurityToken.class);
    when(assertion.getSecurityToken()).thenReturn(securityToken);
    when(subject.getPrincipals().asList()).thenReturn(Arrays.asList(assertion));
    when(securityToken.getToken()).thenReturn(readDocument("/good_saml.xml").getDocumentElement());

    loginFilter.doFilter(servletRequest, servletResponse, filterChain);
  }

  @Test(expected = AuthenticationFailureException.class)
  public void testExpiredSamlCookie()
      throws IOException, ParserConfigurationException, SAXException, AuthenticationException {
    LoginFilter loginFilter = new LoginFilter();
    loginFilter.setSessionFactory(sessionFactory);
    ddf.security.service.SecurityManager securityManager =
        mock(ddf.security.service.SecurityManager.class);
    loginFilter.setSecurityManager(securityManager);
    loginFilter.setSignaturePropertiesFile("signature.properties");
    loginFilter.init();
    HttpServletRequest servletRequest = new TestHttpServletRequest();
    HttpServletResponse servletResponse = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);
    SecurityToken securityToken = new SecurityToken();
    Element thisToken = readDocument("/good_saml.xml").getDocumentElement();
    securityToken.setToken(thisToken);
    SAMLAuthenticationToken samlToken = new SAMLAuthenticationToken(null, securityToken, "karaf");
    HandlerResult result = new HandlerResult(HandlerResult.Status.COMPLETED, samlToken);
    servletRequest.setAttribute("ddf.security.token", result);

    loginFilter.doFilter(servletRequest, servletResponse, filterChain);
  }

  @Test(expected = AuthenticationFailureException.class)
  public void testBadSigSamlCookie()
      throws IOException, ParserConfigurationException, SAXException, AuthenticationException {
    LoginFilter loginFilter = new LoginFilter();
    loginFilter.setSessionFactory(sessionFactory);
    ddf.security.service.SecurityManager securityManager = mock(SecurityManager.class);
    loginFilter.setSecurityManager(securityManager);
    loginFilter.setSignaturePropertiesFile("signature.properties");
    loginFilter.init();
    HttpServletRequest servletRequest = new TestHttpServletRequest();
    HttpServletResponse servletResponse = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);
    SecurityToken securityToken = new SecurityToken();
    Element thisToken = readDocument("/bad_saml.xml").getDocumentElement();
    securityToken.setToken(thisToken);
    SAMLAuthenticationToken samlToken = new SAMLAuthenticationToken(null, securityToken, "karaf");
    HandlerResult result = new HandlerResult(HandlerResult.Status.COMPLETED, samlToken);
    servletRequest.setAttribute("ddf.security.token", result);

    loginFilter.doFilter(servletRequest, servletResponse, filterChain);
  }

  private Document readDocument(String name)
      throws SAXException, IOException, ParserConfigurationException {
    InputStream inStream = getClass().getResourceAsStream(name);
    return readXml(inStream);
  }

  private class TestHttpServletRequest implements HttpServletRequest {

    private Map<Object, Object> attrMap = new HashMap<Object, Object>();

    @Override
    public String getAuthType() {
      return null;
    }

    @Override
    public Cookie[] getCookies() {
      return new Cookie[0];
    }

    @Override
    public long getDateHeader(String name) {
      return 0;
    }

    @Override
    public String getHeader(String name) {
      return null;
    }

    @Override
    public Enumeration getHeaders(String name) {
      return null;
    }

    @Override
    public Enumeration getHeaderNames() {
      return null;
    }

    @Override
    public int getIntHeader(String name) {
      return 0;
    }

    @Override
    public String getMethod() {
      return null;
    }

    @Override
    public String getPathInfo() {
      return null;
    }

    @Override
    public String getPathTranslated() {
      return null;
    }

    @Override
    public String getContextPath() {
      return null;
    }

    @Override
    public String getQueryString() {
      return null;
    }

    @Override
    public String getRemoteUser() {
      return null;
    }

    @Override
    public boolean isUserInRole(String role) {
      return false;
    }

    @Override
    public Principal getUserPrincipal() {
      return null;
    }

    @Override
    public String getRequestedSessionId() {
      return null;
    }

    @Override
    public String getRequestURI() {
      return null;
    }

    @Override
    public StringBuffer getRequestURL() {
      return null;
    }

    @Override
    public String getServletPath() {
      return null;
    }

    @Override
    public HttpSession getSession(boolean create) {
      return null;
    }

    @Override
    public HttpSession getSession() {
      return null;
    }

    @Override
    public String changeSessionId() {
      return null;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
      return false;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
      return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
      return false;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
      return false;
    }

    @Override
    public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException {
      return false;
    }

    @Override
    public void login(String s, String s1) {
      // not needed
    }

    @Override
    public void logout() {
      // not needed
    }

    @Override
    public Collection<Part> getParts() throws IOException {
      return null;
    }

    @Override
    public Part getPart(String s) throws IOException {
      return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) throws IOException {
      return null;
    }

    @Override
    public Object getAttribute(String name) {
      return attrMap.get(name);
    }

    @Override
    public Enumeration getAttributeNames() {
      return null;
    }

    @Override
    public String getCharacterEncoding() {
      return null;
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {}

    @Override
    public int getContentLength() {
      return 0;
    }

    @Override
    public long getContentLengthLong() {
      return 0;
    }

    @Override
    public String getContentType() {
      return null;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      return null;
    }

    @Override
    public String getParameter(String name) {
      return null;
    }

    @Override
    public Enumeration getParameterNames() {
      return null;
    }

    @Override
    public String[] getParameterValues(String name) {
      return new String[0];
    }

    @Override
    public Map getParameterMap() {
      return null;
    }

    @Override
    public String getProtocol() {
      return null;
    }

    @Override
    public String getScheme() {
      return null;
    }

    @Override
    public String getServerName() {
      return null;
    }

    @Override
    public int getServerPort() {
      return 0;
    }

    @Override
    public BufferedReader getReader() throws IOException {
      return null;
    }

    @Override
    public String getRemoteAddr() {
      return null;
    }

    @Override
    public String getRemoteHost() {
      return null;
    }

    @Override
    public void setAttribute(String name, Object o) {
      attrMap.put(name, o);
    }

    @Override
    public void removeAttribute(String name) {}

    @Override
    public Locale getLocale() {
      return null;
    }

    @Override
    public Enumeration getLocales() {
      return null;
    }

    @Override
    public boolean isSecure() {
      return false;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
      return null;
    }

    @Override
    public String getRealPath(String path) {
      return null;
    }

    @Override
    public int getRemotePort() {
      return 0;
    }

    @Override
    public String getLocalName() {
      return null;
    }

    @Override
    public String getLocalAddr() {
      return null;
    }

    @Override
    public int getLocalPort() {
      return 0;
    }

    @Override
    public ServletContext getServletContext() {
      return null;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
      return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
        throws IllegalStateException {
      return null;
    }

    @Override
    public boolean isAsyncStarted() {
      return false;
    }

    @Override
    public boolean isAsyncSupported() {
      return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
      return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
      return null;
    }
  }
}
