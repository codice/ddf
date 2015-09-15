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
import java.util.Dictionary;
import java.util.concurrent.TimeUnit;

import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.codice.ddf.security.handler.api.PKIAuthenticationToken;
import org.codice.ddf.security.handler.api.PKIAuthenticationTokenFactory;
import org.codice.ddf.security.handler.api.UPAuthenticationToken;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;

public class SecurityUtils {

    private static final RolePrincipal ADMIN_ROLE = new RolePrincipal("admin");

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityUtils.class);

    private static Subject cachedSystemSubject;

    private static Object lock = new Object();

    public SecurityUtils() {

    }

    public Subject getSubject(String username, String password) {
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

    public boolean tokenAboutToExpire(Subject subject) {
        boolean tokenAboutToExpire = true;
        if ((null != subject) && (null != subject.getPrincipals()) && (null != subject
                .getPrincipals().oneByType(SecurityAssertion.class)) && (!subject.getPrincipals()
                .oneByType(SecurityAssertion.class).getSecurityToken()
                .isAboutToExpire(TimeUnit.MINUTES.toSeconds(1)))) {
            tokenAboutToExpire = false;
        }
        return tokenAboutToExpire;
    }

    public boolean javaSubjectHasAdminRole() {
        javax.security.auth.Subject subject = javax.security.auth.Subject
                .getSubject(AccessController.getContext());
        if (subject != null) {
            return subject.getPrincipals().contains(ADMIN_ROLE);
        }
        return false;
    }

    public Subject getSystemSubject() {

        if (!tokenAboutToExpire(cachedSystemSubject)) {
            return cachedSystemSubject;
        }

        KeyStore keyStore = getSystemKeyStore();
        String alias = null;
        Certificate cert;
        try {
            if (keyStore.size() == 1) {
                alias = keyStore.aliases().nextElement();
            } else if (keyStore.size() > 1) {
                alias = getCertificatAlias();
            }
            cert = keyStore.getCertificate(alias);
        } catch (KeyStoreException e) {
            LOGGER.error("Unable to get certificate for alias [{}]", alias, e);
            return null;
        }

        if (cert == null) {
            LOGGER.error("Unable to get certificate for alias [{}]", alias);
            return null;
        }

        PKIAuthenticationTokenFactory pkiTokenFactory = createPKITokenFactory();
        PKIAuthenticationToken pkiToken = pkiTokenFactory
                .getTokenFromCerts(new X509Certificate[] {(X509Certificate) cert},
                        PKIAuthenticationToken.DEFAULT_REALM);
        if (pkiToken != null) {
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                synchronized (lock) {
                    try {
                        cachedSystemSubject = securityManager.getSubject(pkiToken);
                    } catch (SecurityServiceException sse) {
                        LOGGER.error("Unable to request subject for system user.", sse);
                    }
                }
            }
        }
        return cachedSystemSubject;
    }

    public SecurityManager getSecurityManager() {
        BundleContext context = getBundleContext();
        if (context != null) {
            ServiceReference securityManagerRef = context
                    .getServiceReference(SecurityManager.class);
            return (SecurityManager) context.getService(securityManagerRef);
        }
        LOGGER.warn("Unable to get Security Manager");
        return null;
    }

    public BundleContext getBundleContext() {
        Bundle bundle = FrameworkUtil.getBundle(SecurityUtils.class);
        if (bundle != null) {
            return bundle.getBundleContext();
        }
        return null;
    }

    public PKIAuthenticationTokenFactory createPKITokenFactory() {
        PKIAuthenticationTokenFactory pkiTokenFactory = new PKIAuthenticationTokenFactory();
        pkiTokenFactory.init();
        return pkiTokenFactory;
    }

    private String getCertificatAlias() {
        BundleContext context = getBundleContext();
        if (context != null) {
            ServiceReference configAdminRef = context.getServiceReference(ConfigurationAdmin.class);
            ConfigurationAdmin configAdmin = (ConfigurationAdmin) context
                    .getService(configAdminRef);
            Configuration config;
            try {
                config = configAdmin.getConfiguration("ddf.platform.config", null);
            } catch (IOException e) {
                LOGGER.error("Could not get reference to configuration admin. ", e);
                return null;
            }
            Dictionary<String, Object> properties = config.getProperties();
            return (String) properties.get("host");
        }
        LOGGER.warn("Unable to get system certificate alias");
        return null;
    }

    private KeyStore getSystemKeyStore() {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(System.getProperty("javax.net.ssl.keyStoreType"));

        } catch (KeyStoreException e) {
            LOGGER.error("Unable to create keystore instance of type {}",
                    System.getProperty("javax.net.ssl.keyStoreType"), e);
            return null;
        }
        Path keyStoreFile = Paths.get(System.getProperty("javax.net.ssl.keyStore"));

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
