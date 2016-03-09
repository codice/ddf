/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package ddf.security.common.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import org.apache.karaf.jaas.boot.principal.RolePrincipal;
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
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;

public class Security {

    private static final RolePrincipal ADMIN_ROLE = new RolePrincipal("admin");

    private static final Logger LOGGER = LoggerFactory.getLogger(Security.class);

    private static Subject cachedSystemSubject;

    public static Subject getSubject(String username, String password) {
        UPAuthenticationToken token = new UPAuthenticationToken(username, password);
        SecurityManager securityManager = getSecurityManager();
        if (securityManager != null) {
            try {
                return securityManager.getSubject(token);
            } catch (Exception sse) {
                LOGGER.error("Unable to request subject for {} user.", username, sse);
            }
        }
        return null;
    }

    /**
     * If the subjects security token is going to expire in the next minute return true otherwise
     * returns false
     *
     * @param subject
     * @return
     */
    public static boolean tokenAboutToExpire(Subject subject) {
        return !((null != subject) && (null != subject.getPrincipals()) && (null
                != subject.getPrincipals()
                .oneByType(SecurityAssertion.class)) && (!subject.getPrincipals()
                .oneByType(SecurityAssertion.class)
                .getSecurityToken()
                .isAboutToExpire(TimeUnit.MINUTES.toSeconds(1))));
    }

    /**
     * Returns true if the java subject exists and has the admin role. Returns false otherwise.
     *
     * @return
     */
    public static boolean javaSubjectHasAdminRole() {
        javax.security.auth.Subject subject = javax.security.auth.Subject.getSubject(
                AccessController.getContext());
        if (subject != null) {
            return subject.getPrincipals()
                    .contains(ADMIN_ROLE);
        }
        return false;
    }

    /**
     * Returns the subject associated with this system. Uses a cached subject since the subject
     * will not change between calls.
     *
     * @return
     */
    public static synchronized Subject getSystemSubject() {

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

    public static SecurityManager getSecurityManager() {
        BundleContext context = getBundleContext();
        if (context != null) {
            ServiceReference securityManagerRef =
                    context.getServiceReference(SecurityManager.class);
            return (SecurityManager) context.getService(securityManagerRef);
        }
        LOGGER.warn("Unable to get Security Manager");
        return null;
    }

    public static BundleContext getBundleContext() {
        Bundle bundle = FrameworkUtil.getBundle(Security.class);
        if (bundle != null) {
            return bundle.getBundleContext();
        }
        return null;
    }

    public static PKIAuthenticationTokenFactory createPKITokenFactory() {
        PKIAuthenticationTokenFactory pkiTokenFactory = new PKIAuthenticationTokenFactory();
        pkiTokenFactory.init();
        return pkiTokenFactory;
    }

    private static String getCertificateAlias() {
        return System.getProperty("org.codice.ddf.system.hostname");
    }

    private static KeyStore getSystemKeyStore() {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(System.getProperty("javax.net.ssl.keyStoreType"));

        } catch (KeyStoreException e) {
            LOGGER.error("Unable to create keystore instance of type {}",
                    System.getProperty("javax.net.ssl.keyStoreType"),
                    e);
            return null;
        }

        String propValue = System.getProperty("javax.net.ssl.keyStore");
        Path keyStoreFile = new File(propValue).toPath();

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
