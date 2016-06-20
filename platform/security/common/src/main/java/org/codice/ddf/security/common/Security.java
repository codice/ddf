/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.common;

import static org.apache.commons.lang.Validate.notNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.subject.ExecutionException;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.GuestAuthenticationToken;
import org.codice.ddf.security.handler.api.PKIAuthenticationToken;
import org.codice.ddf.security.handler.api.PKIAuthenticationTokenFactory;
import org.codice.ddf.security.handler.api.UPAuthenticationToken;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;

/**
 * Singleton class that provides common security related utility functions.
 */
public class Security {

    private static final Security INSTANCE = new Security();

    private static final Logger LOGGER = LoggerFactory.getLogger(Security.class);

    private static final String INSUFFICIENT_PERMISSIONS_ERROR =
            "Current user doesn't have sufficient privileges to run this command";

    private static final RolePrincipal ADMIN_ROLE = new RolePrincipal("admin");

    private Subject cachedSystemSubject;

    private Security() {
        // Singleton
    }

    /**
     * @return unique instance of this class. Never {@code null}.
     */
    public static Security getInstance() {
        return INSTANCE;
    }

    /**
     * Gets the {@link Subject} given a user name and password.
     *
     * @param username username
     * @param password password
     * @return {@link Subject} associated with the user name and password provided
     */
    public Subject getSubject(String username, String password) {
        UPAuthenticationToken token = new UPAuthenticationToken(username, password);
        SecurityManager securityManager = getSecurityManager();

        if (securityManager != null) {
            try {
                return securityManager.getSubject(token);
            } catch (SecurityServiceException | RuntimeException e) {
                LOGGER.error("Unable to request subject for {} user.", username, e);
            }
        }
        return null;
    }

    /**
     * Determines if the current Java {@link Subject} has the admin role.
     *
     * @return {@code true} if the Java {@link Subject} exists and has the admin role, {@code false} otherwise
     */
    public boolean javaSubjectHasAdminRole() {
        javax.security.auth.Subject subject = javax.security.auth.Subject.getSubject(
                AccessController.getContext());
        if (subject != null) {
            return subject.getPrincipals()
                    .contains(ADMIN_ROLE);
        }
        return false;
    }

    /**
     * Runs the {@link Callable} in the current thread as the current security framework's
     * {@link Subject}. If the security framework's {@link Subject} is not currently set and
     * the Java Subject contains the admin role, elevates and runs the {@link Callable} as the
     * system {@link Subject}.
     *
     * @param codeToRun code to run
     * @param <T>       type of the returned value
     * @return value returned by the {@link Callable}
     * @throws SecurityServiceException  if the current subject didn' have enough permissions to run
     *                                   the code
     * @throws InvocationTargetException wraps any exception thrown by {@link Callable#call()}.
     *                                   {@link Callable} exception can be retrieved using the
     *                                   {@link InvocationTargetException#getCause()}.
     */
    public <T> T runWithSubjectOrElevate(@NotNull Callable<T> codeToRun)
            throws SecurityServiceException, InvocationTargetException {
        notNull(codeToRun, "Callable cannot be null");

        try {
            try {
                org.apache.shiro.subject.Subject subject =
                        org.apache.shiro.SecurityUtils.getSubject();
                return subject.execute(codeToRun);
            } catch (IllegalStateException | UnavailableSecurityManagerException e) {
                LOGGER.debug(
                        "No shiro subject available for running command, trying with Java Subject");
            }

            Subject subject = getSystemSubject();

            if (subject == null) {
                SecurityLogger.audit(INSUFFICIENT_PERMISSIONS_ERROR);
                throw new SecurityServiceException(INSUFFICIENT_PERMISSIONS_ERROR);
            }

            SecurityLogger.auditWarn("Elevating current user permissions to use System subject");
            return subject.execute(codeToRun);
        } catch (ExecutionException e) {
            throw new InvocationTargetException(e.getCause());
        }
    }

    /**
     * Gets the {@link Subject} associated with this system. Uses a cached subject since the subject
     * will not change between calls.
     *
     * @return system's {@link Subject}
     */
    public synchronized Subject getSystemSubject() {

        if (!javaSubjectHasAdminRole()) {
            SecurityLogger.audit("Unable to retrieve system subject.");
            return null;
        }

        if (!tokenAboutToExpire(cachedSystemSubject)) {
            return cachedSystemSubject;
        }

        KeyStore keyStore = getSystemKeyStore();
        String alias = null;
        Certificate cert = null;
        try {
            if (keyStore != null) {
                if (keyStore.size() == 1) {
                    alias = keyStore.aliases()
                            .nextElement();
                } else if (keyStore.size() > 1) {
                    alias = getCertificateAlias();
                }
                cert = keyStore.getCertificate(alias);
            }
        } catch (KeyStoreException e) {
            LOGGER.error("Unable to get certificate for alias [{}]", alias, e);
            return null;
        }

        if (cert == null) {
            LOGGER.error("Unable to get certificate for alias [{}]", alias);
            return null;
        }

        PKIAuthenticationTokenFactory pkiTokenFactory = createPKITokenFactory();
        PKIAuthenticationToken pkiToken = pkiTokenFactory.getTokenFromCerts(new X509Certificate[] {
                (X509Certificate) cert}, PKIAuthenticationToken.DEFAULT_REALM);
        if (pkiToken != null) {
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                try {
                    cachedSystemSubject = securityManager.getSubject(pkiToken);
                } catch (SecurityServiceException sse) {
                    LOGGER.error("Unable to request subject for system user.", sse);
                }
            }
        }
        return cachedSystemSubject;
    }

    /**
     * Gets the guest {@link Subject} associated with the specified IP. Uses a cached subject when possible since the subject
     * will not change between calls.
     *
     * @return system's {@link Subject}
     */
    public Subject getGuestSubject(String ipAddress) {
        Subject subject = null;
        GuestAuthenticationToken token =
                new GuestAuthenticationToken(BaseAuthenticationToken.DEFAULT_REALM, ipAddress);
        LOGGER.debug("Getting new Guest user token for {}", ipAddress);
        try {
            subject = getSecurityManager().getSubject(token);
        } catch (SecurityServiceException sse) {
            LOGGER.warn("Unable to request subject for guest user.", sse);
        }

        return subject;
    }

    /**
     * Determines whether a {@link Subject}'s token is about to expire or not.
     *
     * @param subject subject whose token needs to be checked
     * @return {@code true} only if the {@link Subject}'s token will expire soon
     */
    public boolean tokenAboutToExpire(Subject subject) {
        return !((null != subject) && (null != subject.getPrincipals()) && (null
                != subject.getPrincipals()
                .oneByType(SecurityAssertion.class)) && (!subject.getPrincipals()
                .oneByType(SecurityAssertion.class)
                .getSecurityToken()
                .isAboutToExpire(TimeUnit.MINUTES.toSeconds(1))));
    }

    /**
     * Get the expires time from the {@link Subject}'s token.
     *
     * @param subject subject whose token needs to be checked
     * @return {@code Date} or null if subject doesn't have an expire time.
     */
    public Date getExpires(Subject subject) {
        return ((null != subject) && (null != subject.getPrincipals()) && (null
                != subject.getPrincipals()
                .oneByType(SecurityAssertion.class))) ?
                subject.getPrincipals()
                        .oneByType(SecurityAssertion.class)
                        .getSecurityToken()
                        .getExpires() :
                null;
    }

    /**
     * Gets a reference to the {@link SecurityManager}.
     *
     * @return reference to the {@link SecurityManager}
     */
    public SecurityManager getSecurityManager() {
        BundleContext context = getBundleContext();
        if (context != null) {
            ServiceReference securityManagerRef =
                    context.getServiceReference(SecurityManager.class);
            return (SecurityManager) context.getService(securityManagerRef);
        }
        LOGGER.warn("Unable to get Security Manager");
        return null;
    }

    public static <T> T runAsAdmin(PrivilegedAction<T> action) {
        Set<Principal> principals = new HashSet<>();
        principals.add(new RolePrincipal("admin"));
        javax.security.auth.Subject subject = new javax.security.auth.Subject(true,
                principals,
                new HashSet(),
                new HashSet());

        return javax.security.auth.Subject.doAs(subject, action);
    }

    private BundleContext getBundleContext() {
        Bundle bundle = FrameworkUtil.getBundle(Security.class);
        if (bundle != null) {
            return bundle.getBundleContext();
        }
        return null;
    }

    private PKIAuthenticationTokenFactory createPKITokenFactory() {
        PKIAuthenticationTokenFactory pkiTokenFactory = new PKIAuthenticationTokenFactory();
        pkiTokenFactory.init();
        return pkiTokenFactory;
    }

    private String getCertificateAlias() {
        return System.getProperty("org.codice.ddf.system.hostname");
    }

    private KeyStore getSystemKeyStore() {
        KeyStore keyStore;

        try {
            keyStore = KeyStore.getInstance(System.getProperty("javax.net.ssl.keyStoreType"));

        } catch (KeyStoreException e) {
            LOGGER.error("Unable to create keystore instance of type {}",
                    System.getProperty("javax.net.ssl.keyStoreType"),
                    e);
            return null;
        }

        Path keyStoreFile = new File(System.getProperty("javax.net.ssl.keyStore")).toPath();
        Path ddfHomePath = Paths.get(System.getProperty("ddf.home"));

        if (!keyStoreFile.isAbsolute()) {
            keyStoreFile = Paths.get(ddfHomePath.toString(), keyStoreFile.toString());
        }

        String keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword");

        if (!Files.isReadable(keyStoreFile)) {
            LOGGER.error("Unable to read system key/trust store files: [ {} ] ", keyStoreFile);
            return null;
        }

        try (InputStream kfis = Files.newInputStream(keyStoreFile)) {
            keyStore.load(kfis, keyStorePassword.toCharArray());
        } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
            LOGGER.error("Unable to load system key file.", e);
        }

        return keyStore;
    }
}
