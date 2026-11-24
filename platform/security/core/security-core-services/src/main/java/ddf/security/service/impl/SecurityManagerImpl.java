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
import ddf.security.audit.SecurityLogger;
import ddf.security.impl.SubjectImpl;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
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
import org.codice.ddf.security.handler.SessionToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityManagerImpl implements SecurityManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(SecurityManagerImpl.class);

  private DefaultSecurityManager internalManager;

  private SecurityLogger securityLogger;

  /**
   * The realms handed to Shiro, which keeps and iterates this collection on every authentication
   * and permission check. A {@link CopyOnWriteArrayList} makes that iteration cheap and lock-free;
   * passing the live blueprint reference-list instead made every permission check create an
   * iterator on it, whose cost grows with every iterator created since the last GC.
   */
  private final CopyOnWriteArrayList<Realm> realms = new CopyOnWriteArrayList<>();

  /** Creates a new security manager with the collection of given realms. */
  public SecurityManagerImpl(SecurityLogger securityLogger) {
    this.securityLogger = securityLogger;

    // create the new security manager
    internalManager = new DefaultSecurityManager();
    ((ModularRealmAuthenticator) internalManager.getAuthenticator())
        .setAuthenticationStrategy(new AllSuccessfulStrategy());
  }

  /**
   * Replaces the realms used for the backing authZ and authN operations.
   *
   * @param realms The realms used for the backing authZ and authN operations.
   */
  public synchronized void setRealms(Collection<Realm> realms) {
    List<Realm> current = new ArrayList<>(realms);
    this.realms.retainAll(current);
    this.realms.addAllAbsent(current);
    updateManager();
  }

  /**
   * Blueprint reference-listener bind method: a realm service became available.
   *
   * @param realm the realm to add
   */
  public synchronized void addRealm(Realm realm) {
    if (realm != null && realms.addIfAbsent(realm)) {
      updateManager();
    }
  }

  /**
   * Blueprint reference-listener unbind method: a realm service went away.
   *
   * @param realm the realm to remove
   */
  public synchronized void removeRealm(Realm realm) {
    if (realm != null && realms.remove(realm)) {
      updateManager();
    }
  }

  private void updateManager() {
    LOGGER.debug("Updating manager with {} realms.", realms.size());
    // Shiro rejects an empty realm collection; when the last realm goes away it keeps a reference
    // to this (now empty) list, so authN/authZ fail until a realm is bound again.
    if (!realms.isEmpty()) {
      // re-set so Shiro re-applies its cache manager, event bus and permission resolvers
      internalManager.setRealms(realms);
    }
  }

  @Override
  public Subject getSubject(Object token) throws SecurityServiceException {
    AuthenticationToken authenticationToken = null;
    if (token instanceof SessionToken) {
      SimpleSession session = new SimpleSession();
      session.setId((String) ((SessionToken) token).getCredentials());
      return new SubjectImpl(
          ((PrincipalCollection) ((SessionToken) token).getPrincipal()),
          true,
          session,
          internalManager);
    } else if (token instanceof AuthenticationToken) {
      authenticationToken = (AuthenticationToken) token;
    }

    if (authenticationToken != null) {
      Subject subject = getSubject(authenticationToken);
      securityLogger.audit("Logged in", subject);
      return subject;
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

    @Override
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
