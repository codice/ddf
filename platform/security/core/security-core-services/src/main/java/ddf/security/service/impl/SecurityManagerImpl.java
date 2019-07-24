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
package ddf.security.service.impl;

import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.impl.SubjectImpl;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.pam.AbstractAuthenticationStrategy;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.codice.ddf.security.handler.api.SessionToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityManagerImpl implements SecurityManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(SecurityManagerImpl.class);

  private DefaultSecurityManager internalManager;

  /** Creates a new security manager with the collection of given realms. */
  public SecurityManagerImpl() {
    // create the new security manager
    internalManager = new DefaultSecurityManager();
    ((ModularRealmAuthenticator) internalManager.getAuthenticator())
        .setAuthenticationStrategy(new AllSuccessfulStrategy());
  }

  /** @param realms The realms used for the backing authZ and authN operations. */
  public void setRealms(Collection<Realm> realms) {
    // update the default manager with current realm list
    LOGGER.debug("Updating manager with {} realms.", realms.size());
    internalManager.setRealms(realms);
  }

  public Subject getSubject(Object token) throws SecurityServiceException {
    AuthenticationToken authenticationToken = null;
    if (token instanceof SessionToken) {
      return new SubjectImpl(
          ((PrincipalCollection) ((SessionToken) token).getCredentials()),
          true,
          new SimpleSession(UUID.randomUUID().toString()),
          internalManager);
    } else if (token instanceof AuthenticationToken) {
      authenticationToken = (AuthenticationToken) token;
    }

    if (authenticationToken != null) {
      return getSubject(authenticationToken);
    } else {
      throw new SecurityServiceException(
          "Incoming token object NOT supported by security manager implementation. Currently supported types are AuthenticationToken and SecurityToken");
    }
  }

  /**
   * Creates a new subject based on an incoming AuthenticationToken
   *
   * @param token AuthenticationToken that should be used to authenticate the user and use as the
   *     basis for the new subject.
   * @return new subject
   * @throws SecurityServiceException
   */
  private Subject getSubject(AuthenticationToken token) throws SecurityServiceException {
    if (token.getCredentials() == null) {
      throw new SecurityServiceException(
          "CANNOT AUTHENTICATE USER: Authentication token did not contain any credentials. "
              + "This is generally due to an error on the authentication server.");
    }
    AuthenticationInfo info = internalManager.authenticate(token);
    Collection<SecurityAssertion> securityAssertions =
        info.getPrincipals().byType(SecurityAssertion.class);
    Iterator<SecurityAssertion> iterator = securityAssertions.iterator();
    boolean userAuth = false;
    while (iterator.hasNext()) {
      SecurityAssertion assertion = iterator.next();
      if (SecurityAssertion.IDP_AUTH_WEIGHT == assertion.getWeight()
          || SecurityAssertion.LOCAL_AUTH_WEIGHT == assertion.getWeight()) {
        userAuth = true;
      }
    }
    try {
      return new SubjectImpl(
          info.getPrincipals(),
          userAuth,
          new SimpleSession(UUID.randomUUID().toString()),
          internalManager);
    } catch (Exception e) {
      throw new SecurityServiceException("Could not create a new subject", e);
    }
  }

  private static class AllSuccessfulStrategy extends AbstractAuthenticationStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllSuccessfulStrategy.class);

    public AuthenticationInfo afterAttempt(
        Realm realm,
        AuthenticationToken token,
        AuthenticationInfo info,
        AuthenticationInfo aggregate,
        Throwable t)
        throws AuthenticationException {
      if (t != null) {
        if (t instanceof AuthenticationException) {
          // propagate:
          throw ((AuthenticationException) t);
        } else {
          String msg =
              "Unable to acquire account data from realm ["
                  + realm
                  + "].  The ["
                  + getClass().getName()
                  + " implementation requires all configured realm(s) to operate successfully "
                  + "for a successful authentication.";
          throw new AuthenticationException(msg, t);
        }
      }
      if (info == null) {
        String msg =
            "Realm ["
                + realm
                + "] could not find any associated account data for the submitted "
                + "AuthenticationToken ["
                + token
                + "].  The ["
                + getClass().getName()
                + "] implementation requires "
                + "all configured realm(s) to acquire valid account data for a submitted token during the "
                + "log-in process.";
        throw new UnknownAccountException(msg);
      }

      LOGGER.debug("Account successfully authenticated using realm [{}]", realm);

      // If non-null account is returned, then the realm was able to authenticate the
      // user - so merge the account with any accumulated before:
      merge(info, aggregate);

      return aggregate;
    }
  }
}
