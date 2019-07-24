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

import static ddf.security.SecurityConstants.AUTHENTICATION_TOKEN_KEY;

import com.google.common.hash.Hashing;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.assertion.saml.impl.SecurityAssertionSaml;
import ddf.security.common.SecurityTokenHolder;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.http.SessionFactory;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivilegedExceptionAction;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.subject.PrincipalCollection;
import org.codice.ddf.platform.filter.AuthenticationException;
import org.codice.ddf.platform.filter.FilterChain;
import org.codice.ddf.platform.filter.SecurityFilter;
import org.codice.ddf.platform.util.XMLUtils;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.SAMLAuthenticationToken;
import org.codice.ddf.security.handler.api.SessionToken;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A filter that exchanges all incoming tokens for a security assertion. */
public class LoginFilter implements SecurityFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoginFilter.class);

  private static final ThreadLocal<DocumentBuilder> BUILDER =
      new ThreadLocal<DocumentBuilder>() {
        @Override
        protected DocumentBuilder initialValue() {
          try {
            return XML_UTILS.getSecureDocumentBuilder(true);
          } catch (ParserConfigurationException ex) {
            // This exception should not happen
            throw new IllegalArgumentException("Unable to create new DocumentBuilder", ex);
          }
        }
      };

  private static final XMLUtils XML_UTILS = XMLUtils.getInstance();

  private SecurityManager securityManager;

  private SessionFactory sessionFactory;

  private ContextPolicyManager contextPolicyManager;

  private int expirationTime;

  public LoginFilter() {
    super();
  }

  @Override
  public void init() {
    LOGGER.debug("Starting LoginFilter.");
  }

  /**
   * Gets token, resolves token references, and calls the security manager to get a Subject
   *
   * @param request
   * @param response
   * @param chain
   * @throws IOException
   * @throws ServletException
   */
  @Override
  public void doFilter(
      final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws IOException, AuthenticationException {
    LOGGER.debug("Performing doFilter() on LoginFilter");
    HttpServletRequest httpRequest = (HttpServletRequest) request;

    // Skip filter if no authentication policy
    if (request.getAttribute(ContextPolicy.NO_AUTH_POLICY) != null) {
      LOGGER.debug("NO_AUTH_POLICY header was found, skipping login filter.");
      chain.doFilter(request, response);
      return;
    }

    // grab token from httpRequest
    BaseAuthenticationToken token;
    Object ddfAuthToken = httpRequest.getAttribute(AUTHENTICATION_TOKEN_KEY);
    if (ddfAuthToken instanceof HandlerResult) {
      token = ((HandlerResult) ddfAuthToken).getToken();
    } else {
      LOGGER.debug("Could not attach subject to http request.");
      return;
    }

    token.setX509Certs(
        (X509Certificate[]) httpRequest.getAttribute("javax.servlet.request.X509Certificate"));
    token.setRequestURI(httpRequest.getRequestURI());

    // TODO - temporary method call should be removed once the STS is removed
    token = checkSessionTokenExpiration(token);

    // get subject from the token
    Subject subject;
    try {
      subject = securityManager.getSubject(token);
    } catch (SecurityServiceException e) {
      LOGGER.debug("Error getting subject from a Shiro realm", e);
      return;
    }

    // check that security manager was able to resolve a subject
    if (subject == null) {
      LOGGER.debug("Could not attach subject to http request.");
      return;
    }

    // attach subject to the http session
    if (contextPolicyManager.getSessionAccess()) {
      addToSession(httpRequest, subject);
    }

    // subject is now resolved, perform request as that subject
    httpRequest.setAttribute(SecurityConstants.SECURITY_SUBJECT, subject);
    LOGGER.debug(
        "Now performing request as user {} for {}",
        subject.getPrincipal(),
        StringUtils.isNotBlank(httpRequest.getContextPath())
            ? httpRequest.getContextPath()
            : httpRequest.getServletPath());
    subject.execute(
        () -> {
          PrivilegedExceptionAction<Void> action =
              () -> {
                chain.doFilter(request, response);
                return null;
              };
          Collection<SecurityAssertion> securityAssertions =
              subject.getPrincipals().byType(SecurityAssertion.class);
          if (!securityAssertions.isEmpty()) {
            HashSet emptySet = new HashSet();
            javax.security.auth.Subject javaSubject =
                new javax.security.auth.Subject(
                    true,
                    securityAssertions
                        .stream()
                        .map(SecurityAssertion::getPrincipals)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet()),
                    emptySet,
                    emptySet);
            httpRequest.setAttribute(SecurityConstants.SECURITY_JAVA_SUBJECT, javaSubject);
            javax.security.auth.Subject.doAs(javaSubject, action);
          } else {
            LOGGER.debug("Subject had no security assertion.");
          }
          return null;
        });
  }

  // TODO - temporary method should be removed once the STS is removed
  private BaseAuthenticationToken checkSessionTokenExpiration(BaseAuthenticationToken token) {
    if (token instanceof SessionToken) {
      Collection<SecurityAssertion> securityAssertions =
          ((PrincipalCollection) token.getCredentials()).byType(SecurityAssertion.class);
      SecurityAssertionSaml securityAssertionSaml =
          (SecurityAssertionSaml)
              securityAssertions
                  .stream()
                  .filter(sa -> SecurityAssertionSaml.SAML2_TOKEN_TYPE.equals(sa.getTokenType()))
                  .findFirst()
                  .orElse(null);
      if (securityAssertionSaml != null) {
        SAMLAuthenticationToken authToken =
            new SAMLAuthenticationToken(
                null, (PrincipalCollection) token.getCredentials(), token.getIpAddress());
        authToken.setAllowGuest(token.getAllowGuest());
        return authToken;
      }
    }
    return token;
  }

  /**
   * Attaches a subject to the HttpSession associated with an HttpRequest. If a session does not
   * already exist, one will be created.
   *
   * @param httpRequest HttpRequest associated with an HttpSession to attach the Subject to
   * @param subject Subject to attach to request
   */
  private void addToSession(HttpServletRequest httpRequest, Subject subject) {
    boolean nullSession = httpRequest.getSession(false) == null;
    PrincipalCollection principals = subject.getPrincipals();
    HttpSession session = sessionFactory.getOrCreateSession(httpRequest);
    SecurityTokenHolder holder =
        (SecurityTokenHolder) session.getAttribute(SecurityConstants.SECURITY_TOKEN_KEY);
    PrincipalCollection oldPrincipals = holder.getPrincipals();
    if (!principals.equals(oldPrincipals)) {
      holder.setPrincipals(principals);
    }

    if (nullSession) {
      SecurityLogger.audit(
          "Added token for user [{}] to session [{}]",
          principals.getPrimaryPrincipal(),
          Hashing.sha256().hashString(session.getId(), StandardCharsets.UTF_8).toString());
      session.setMaxInactiveInterval(expirationTime * 60);
    }
  }

  public void setSecurityManager(SecurityManager securityManager) {
    this.securityManager = securityManager;
  }

  public void setExpirationTime(int expirationTime) {
    this.expirationTime = expirationTime;
  }

  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  public void setContextPolicyManager(ContextPolicyManager contextPolicyManager) {
    this.contextPolicyManager = contextPolicyManager;
  }

  @Override
  public void destroy() {
    LOGGER.debug("Destroying log in filter");
    BUILDER.remove();
  }
}
