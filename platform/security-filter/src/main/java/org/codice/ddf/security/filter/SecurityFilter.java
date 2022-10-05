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
package org.codice.ddf.security.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.karaf.jaas.boot.principal.ClientPrincipal;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityFilter implements Filter {

  private static final Logger LOGGER = LoggerFactory.getLogger(SecurityFilter.class);

  public static final String AUTHORIZATION_HEADER = "Authorization";

  public static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";

  public static final String KARAF_SUBJECT_RUN_AS = "karaf.subject.runas";

  public static final String DDF_REALM = "ddf";

  // Applications, such as Hawtio, are specifically looking for this property. Do not change it.
  public static final String JAVA_SUBJECT = "subject";

  public static final String JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE =
      "javax.servlet.request.X509Certificate";

  // The default inactive HTTP session limit is unlimited
  private final int sessionTimeout =
      Integer.parseInt(System.getProperty("http.session.inactive.limit", "0").trim());

  private LoginContextFactory loginContextFactory = new LoginContextFactory();

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
      throws IOException, ServletException {
    if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;

      ClassLoader tccl = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(SecurityFilter.class.getClassLoader());
      try {
        addCachingHeaders(httpRequest, httpResponse);
        addSecurityHeaders(httpResponse);
        login(httpRequest, httpResponse, filterChain);
      } finally {
        Thread.currentThread().setContextClassLoader(tccl);
      }
    } else {
      filterChain.doFilter(request, response);
    }
  }

  @Override
  public void destroy() {}

  private void addCachingHeaders(HttpServletRequest request, HttpServletResponse response) {
    String requestURI = request.getRequestURI();
    if (requestURI.endsWith("/")
        || requestURI.endsWith(".html")
        || requestURI.endsWith(".htm")
        || requestURI.endsWith(".xhtml")) {
      response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    } else {
      response.setHeader("Cache-Control", "private, max-age=604800, immutable");
    }
  }

  private void addSecurityHeaders(HttpServletResponse response) {
    response.setHeader("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains");
    response.setHeader(
        "Content-Security-Policy",
        "default-src 'none'; connect-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'");
    response.setHeader("X-FRAME-OPTIONS", "SAMEORIGIN");
    response.setHeader("X-XSS-Protection", "1; mode=block");
    response.setHeader("Referrer-Policy", "nosniff");
    response.setHeader("X-Content-Type-Options", "origin-when-cross-origin");
  }

  private void login(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException {

    Subject subject = getCurrentSubject(request);

    if (subject == null) {
      subject = clientCertLogin(request);
    }
    if (subject == null) {
      subject = basicLogin(request);
    }

    if (subject != null) {
      try {
        Subject.doAs(
            subject,
            (PrivilegedExceptionAction<Void>)
                () -> {
                  filterChain.doFilter(request, response);
                  return null;
                });
      } catch (PrivilegedActionException e) {
        LOGGER.debug("Unable to complete filter chain as user", e);
        throw new ServletException("Unable to complete request as given user.");
      }
    } else {
      requireAuthentication(response);
    }
  }

  private Subject clientCertLogin(HttpServletRequest request) {

    X509Certificate[] certs =
        (X509Certificate[]) request.getAttribute(JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);

    if (certs == null || certs.length == 0 || certs[0].getSubjectX500Principal() == null) {
      return null;
    }

    String username = certs[0].getSubjectX500Principal().getName();
    PublicKey key = certs[0].getPublicKey();

    return loginToSession(request, username, key);
  }

  private Subject basicLogin(HttpServletRequest request) {

    String authHeader = request.getHeader(AUTHORIZATION_HEADER);

    if (authHeader == null || authHeader.length() == 0) {
      return null;
    }

    String[] parts = authHeader.trim().split(" ");

    if (parts.length != 2) {
      return null;
    }

    String authType = parts[0];
    if (!HttpServletRequest.BASIC_AUTH.equalsIgnoreCase(authType)) {
      return null;
    }

    String authInfo = parts[1];
    byte[] decode = Base64.getDecoder().decode(authInfo);
    if (decode == null) {
      return null;
    }

    String userPass = new String(decode, StandardCharsets.UTF_8);
    String[] authComponents = userPass.split(":");
    if (authComponents.length != 2) {
      return null;
    }

    String username = authComponents[0];
    String password = authComponents[1];

    return loginToSession(request, username, password);
  }

  private Subject loginToSession(
      HttpServletRequest request, String username, Object identityProof) {
    String address = request.getRemoteHost() + ":" + request.getRemotePort();
    Subject subject = authenticate(address, username, identityProof);

    if (subject == null) {
      return null;
    }

    request.setAttribute(HttpContext.AUTHENTICATION_TYPE, HttpServletRequest.BASIC_AUTH);
    request.setAttribute(HttpContext.REMOTE_USER, username);
    request.setAttribute(JAVA_SUBJECT, subject);
    try {
      HttpSession session = request.getSession(true);
      session.setMaxInactiveInterval(sessionTimeout);
      session.setAttribute(KARAF_SUBJECT_RUN_AS, subject);
      return subject;
    } catch (Throwable t) {
      LOGGER.debug("Unable to create HTTP session.", t);
      return null;
    }
  }

  private static Subject getCurrentSubject(HttpServletRequest request) {
    Subject subject = null;
    try {
      HttpSession session = request.getSession(false);
      if (session != null) {
        subject = (Subject) session.getAttribute(KARAF_SUBJECT_RUN_AS);
      }
    } catch (Throwable t) {
      LOGGER.debug("Unable to get HTTP session.", t);
    }
    return subject;
  }

  private Subject authenticate(String address, String username, Object identityProof) {
    if (address == null || username == null || identityProof == null) {
      return null;
    }
    try {
      Subject subject = new Subject();
      subject.getPrincipals().add(new ClientPrincipal("http", address));

      LoginContext loginContext = loginContextFactory.create(subject, username, identityProof);
      loginContext.login();
      return subject;
    } catch (GeneralSecurityException e) {
      LOGGER.debug("Unable to authenticate", e);
      return null;
    }
  }

  public void setLoginContextFactory(LoginContextFactory factory) {
    this.loginContextFactory = factory;
  }

  private void requireAuthentication(HttpServletResponse response) {
    response.setHeader(WWW_AUTHENTICATE_HEADER, "Basic realm=\"" + DDF_REALM + "\"");
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentLength(0);
    try {
      response.flushBuffer();
    } catch (IOException e) {
      LOGGER.debug("Error flushing after sending authentication required response.", e);
    }
  }
}
