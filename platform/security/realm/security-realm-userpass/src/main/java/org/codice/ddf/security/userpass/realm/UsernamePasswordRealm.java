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
package org.codice.ddf.security.userpass.realm;

import ddf.security.assertion.Attribute;
import ddf.security.assertion.AttributeStatement;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.assertion.impl.AttributeDefault;
import ddf.security.assertion.impl.AttributeStatementDefault;
import ddf.security.assertion.impl.DefaultSecurityAssertionBuilder;
import ddf.security.claims.Claim;
import ddf.security.claims.ClaimsCollection;
import ddf.security.claims.ClaimsHandler;
import ddf.security.claims.impl.ClaimsParametersImpl;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.wss4j.common.NamePasswordCallbackHandler;
import org.codice.ddf.security.handler.api.AuthenticationTokenType;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsernamePasswordRealm extends AuthenticatingRealm {

  public static final Logger LOGGER = LoggerFactory.getLogger(UsernamePasswordRealm.class);

  protected final List<JaasRealm> realmList = new CopyOnWriteArrayList<>();

  private List<ClaimsHandler> claimsHandlers = new ArrayList<>();

  private Duration fourHours = Duration.ofHours(4);

  /** Determine if the supplied token is supported by this realm. */
  @Override
  public boolean supports(AuthenticationToken token) {
    if (!(token instanceof BaseAuthenticationToken)) {
      LOGGER.debug(
          "The supplied authentication token is not an instance of BaseAuthenticationToken. Sending back not supported.");
      return false;
    }

    BaseAuthenticationToken authToken = (BaseAuthenticationToken) token;

    Object credentials = authToken.getCredentials();

    if (credentials == null || authToken.getType() != AuthenticationTokenType.USERNAME) {
      LOGGER.debug(
          "The supplied authentication token has null/empty credentials. Sending back not supported.");
      return false;
    }

    if (credentials instanceof String) {
      LOGGER.debug(
          "Token {} is supported by {}.", token.getClass(), UsernamePasswordRealm.class.getName());
      return true;
    }

    return false;
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token)
      throws AuthenticationException {
    String credentials = (String) token.getCredentials();
    String[] userpass = credentials.split(":");
    if (userpass.length != 2) {
      throw new AuthenticationException("Credentials were not in the correct format.");
    }
    String user = new String(Base64.getDecoder().decode(userpass[0]), StandardCharsets.UTF_8);
    String pass = new String(Base64.getDecoder().decode(userpass[1]), StandardCharsets.UTF_8);
    Subject subject = null;
    for (JaasRealm jaasRealm : realmList) {
      try {
        subject = login(user, pass, jaasRealm.getName());
        LOGGER.trace("Login succeeded for {} against realm {}", user, jaasRealm.getName());
        break;
      } catch (LoginException e) {
        LOGGER.trace("Login failed for {} against realm {}", user, jaasRealm.getName());
      }
    }
    if (subject != null) {
      SimpleAuthenticationInfo simpleAuthenticationInfo = new SimpleAuthenticationInfo();
      SimplePrincipalCollection principalCollection = createPrincipalCollectionFromSubject(subject);
      simpleAuthenticationInfo.setPrincipals(principalCollection);
      simpleAuthenticationInfo.setCredentials(credentials);
      return simpleAuthenticationInfo;
    }
    throw new AuthenticationException("Login failed for user: " + user);
  }

  private SimplePrincipalCollection createPrincipalCollectionFromSubject(Subject subject) {
    SimplePrincipalCollection principals = new SimplePrincipalCollection();
    DefaultSecurityAssertionBuilder assertionBuilder = new DefaultSecurityAssertionBuilder();

    AttributeStatement attributeStatement = new AttributeStatementDefault();
    Principal userPrincipal =
        subject.getPrincipals().stream()
            .filter(p -> p instanceof UserPrincipal)
            .findFirst()
            .orElseThrow(AuthenticationException::new);
    Set<Principal> rolePrincipals =
        subject.getPrincipals().stream()
            .filter(p -> p instanceof RolePrincipal)
            .collect(Collectors.toSet());
    for (ClaimsHandler claimsHandler : claimsHandlers) {
      ClaimsCollection claims =
          claimsHandler.retrieveClaims(
              new ClaimsParametersImpl(userPrincipal, rolePrincipals, new HashMap<>()));
      mergeClaimsToAttributes(attributeStatement, claims);
    }
    final Instant now = Instant.now();

    assertionBuilder
        .addAttributeStatement(attributeStatement)
        .userPrincipal(userPrincipal)
        .weight(SecurityAssertion.LOCAL_AUTH_WEIGHT)
        .issuer("DDF")
        .notBefore(Date.from(now))
        .notOnOrAfter(Date.from(now.plus(fourHours)));

    for (Principal principal : rolePrincipals) {
      assertionBuilder.addPrincipal(principal);
    }

    SecurityAssertion assertion = assertionBuilder.build();

    principals.add(assertion, "UP");

    return principals;
  }

  private void mergeClaimsToAttributes(
      AttributeStatement attributeStatement, ClaimsCollection claims) {
    for (Claim claim : claims) {
      Attribute newAttr = new AttributeDefault();
      newAttr.setName(claim.getName());
      newAttr.setValues(claim.getValues());
      boolean found = false;
      for (Attribute attribute : attributeStatement.getAttributes()) {
        if (attribute.getName().equals(newAttr.getName())) {
          found = true;
          for (String value : newAttr.getValues()) {
            attribute.addValue(value);
          }
        }
      }
      if (!found) {
        attributeStatement.addAttribute(newAttr);
      }
    }
  }

  public void addRealm(ServiceReference<JaasRealm> serviceReference) {
    Bundle bundle = FrameworkUtil.getBundle(UsernamePasswordRealm.class);
    if (null != bundle) {
      JaasRealm realm = bundle.getBundleContext().getService(serviceReference);
      LOGGER.trace("Adding validator for JaasRealm {}", realm.getName());
      realmList.add(realm);
    }
  }

  public void removeRealm(ServiceReference<JaasRealm> serviceReference) {
    Bundle bundle = FrameworkUtil.getBundle(UsernamePasswordRealm.class);
    if (null != bundle) {
      JaasRealm realm = bundle.getBundleContext().getService(serviceReference);
      LOGGER.trace("Removing validator for JaasRealm {}", realm.getName());
      realmList.remove(realm);
    }
  }

  protected Subject login(String username, String password, String realmName)
      throws LoginException {
    CallbackHandler handler = getCallbackHandler(username, password);
    LoginContext ctx = new LoginContext(realmName, handler);
    ctx.login();
    return ctx.getSubject();
  }

  private CallbackHandler getCallbackHandler(String name, String password) {
    return new NamePasswordCallbackHandler(name, password);
  }

  public void setClaimsHandlers(List<ClaimsHandler> claimsHandlers) {
    this.claimsHandlers = claimsHandlers;
  }
}
