/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.security.service.impl;

import java.util.Collection;
import java.util.UUID;

import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.Authorizer;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.Subject;
import ddf.security.assertion.impl.SecurityAssertionImpl;
import ddf.security.impl.SubjectImpl;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;

public class SecurityManagerImpl implements SecurityManager {

    private DefaultSecurityManager internalManager;

    private Collection<Realm> realms;

    private Logger logger = LoggerFactory.getLogger(SecurityManagerImpl.class);

    /**
     * Creates a new security manager with the collection of given realms.
     * 
     * @param realms
     *            The realms used for the backing authZ and authN operations.
     */
    public SecurityManagerImpl() {
        // create the new security manager
        internalManager = new DefaultSecurityManager();
    }

    public void setRealms(Collection<Realm> realms) {
        this.realms = realms;
        // update the default manager with current realm list
        logger.debug("Updating manager with {} realms.", realms.size());
        internalManager.setRealms(realms);
    }

    public Subject getSubject(Object token) throws SecurityServiceException {
        if (token instanceof AuthenticationToken) {
            return getSubject((AuthenticationToken) token);
        } else if (token instanceof SecurityToken) {
            return getSubject((SecurityToken) token);
        } else {
            throw new SecurityServiceException(
                    "Incoming token object NOT supported by security manager implementation. Currently supported types are AuthenticationToken and SecurityToken");
        }
    }

    /**
     * Creates a new subject based on an incoming AuthenticationToken
     * 
     * @param token
     *            AuthenticationToken that should be used to authenticate the
     *            user and use as the basis for the new subject.
     * @return new subject
     * @throws SecurityServiceException
     */
    private Subject getSubject(AuthenticationToken token) throws SecurityServiceException {
        if (token.getCredentials() == null) {
            throw new SecurityServiceException(
                    "CANNOT AUTHENTICATE USER: Authentication token did not contain any credentials. "
                            + "This is generally due to an error on the authentication server.");
        }
        // authenticate the token - this will call the stsrealm
        AuthenticationInfo info = internalManager.authenticate(token);
        SecurityToken secToken = info.getPrincipals().oneByType(SecurityToken.class);
        if (secToken == null) {
            throw new SecurityServiceException(
                    "Did not receive a security token back, cannot complete authentication.");
        }
        try {
            // create the subject that will be returned back to the user
            return new SubjectImpl(createPrincipalFromToken(secToken), true, new SimpleSession(UUID
                    .randomUUID().toString()), internalManager);
        } catch (Exception e) {
            throw new SecurityServiceException("Could not create a new subject", e);
        }
    }

    /**
     * Creates a new subject using an incoming SecurityToken.
     * 
     * @param token
     *            Security token that the subject should be populated with
     * @return new subject
     * @throws SecurityServiceException
     */
    private Subject getSubject(SecurityToken token) throws SecurityServiceException {
        try {
            // return the newly created subject
            return new SubjectImpl(createPrincipalFromToken(token), true, new SimpleSession(UUID
                    .randomUUID().toString()), internalManager);
        } catch (Exception e) {
            throw new SecurityServiceException("Could not create a new subject", e);
        }
    }

    /**
     * Creates a new principal object from an incoming security token.
     * 
     * @param token
     *            SecurityToken that contains the principals.
     * @return new SimplePrincipalCollection
     */
    private SimplePrincipalCollection createPrincipalFromToken(SecurityToken token) {
        SimplePrincipalCollection principals = new SimplePrincipalCollection();
        for (Realm curRealm : realms) {
            try {
                logger.debug("Configuring settings for realm name: {} type: {}",
                        curRealm.getName(), curRealm.getClass().toString());
                logger.debug("Is authorizer: {}, is AuthorizingRealm: {}",
                        curRealm instanceof Authorizer, curRealm instanceof AuthorizingRealm);
                principals.add(new AssertionWrapper(token.getToken()).getSaml2().getSubject()
                        .getNameID().getValue(), curRealm.getName());
                principals.add(new SecurityAssertionImpl(token), curRealm.getName());
            } catch (WSSecurityException wsse) {
                logger.warn(
                        "Could not add principal for realm ({}). This may cause an authorization failure when the realm is called.",
                        curRealm.getName());
            }
        }
        return principals;
    }
}
