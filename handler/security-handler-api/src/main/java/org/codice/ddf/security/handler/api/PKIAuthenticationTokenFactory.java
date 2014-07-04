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
package org.codice.ddf.security.handler.api;

import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.CredentialException;
import org.apache.ws.security.components.crypto.Merlin;
import ddf.security.common.util.PropertiesLoader;
import org.apache.ws.security.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.cert.X509Certificate;

public class PKIAuthenticationTokenFactory {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(PKIAuthenticationTokenFactory.class);

    private Merlin merlin;

    private String signaturePropertiesPath;

    private String realm;

    /**
     * Initializes Merlin crypto object.
     */
    public void init() {
        try {
            merlin = new Merlin(PropertiesLoader.loadProperties(signaturePropertiesPath));
        } catch (CredentialException e) {
            LOGGER.error("Unable to read merlin properties file for crypto operations.", e);
        } catch (IOException e) {
            LOGGER.error("Unable to read merlin properties file.", e);
        }
    }

    public PKIAuthenticationToken getTokenFromString(String certString, boolean isEncoded) {
        PKIAuthenticationToken token = null;
        try {
            byte[] certBytes = isEncoded ? Base64.decode(certString) : certString.getBytes();
            token = getTokenFromBytes(certBytes);
        } catch (WSSecurityException e) {
            LOGGER.error("Unable to decode given string certificate: {}", e.getMessage(), e);
        }
        return token;
    }

    public PKIAuthenticationToken getTokenFromBytes(byte[] certBytes) {
        PKIAuthenticationToken token = null;
        try {
            X509Certificate[] certs = merlin.getCertificatesFromBytes(certBytes);
            token = new PKIAuthenticationToken(certs[0].getSubjectDN(), certBytes, realm);
        } catch (WSSecurityException e) {
            LOGGER.error("Unable to extract certificates from bytes: {}", e.getMessage(), e);
        }
        return token;
    }

    public PKIAuthenticationToken getTokenFromCerts(X509Certificate[] certs) {
        PKIAuthenticationToken token = null;
        if (certs != null && certs.length > 0) {
            byte[] certBytes = null;
            try {
                certBytes = getCertBytes(certs);
            } catch (WSSecurityException e) {
                LOGGER.error("Unable to convert PKI certs to byte array.", e);
            }
            if (certBytes != null) {
                token = new PKIAuthenticationToken(certs[0].getSubjectDN(), certBytes, realm);
            }
        }
        return token;
    }

    /**
     * Returns a byte array representing a certificate chain.
     *
     * @param certs
     * @return byte[]
     * @throws WSSecurityException
     */
    private byte[] getCertBytes(X509Certificate[] certs) throws WSSecurityException {
        byte[] certBytes = null;

        if (merlin != null) {
            certBytes = merlin.getBytesFromCertificates(certs);
        }
        return certBytes;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getSignaturePropertiesPath() {
        return signaturePropertiesPath;
    }

    public void setSignaturePropertiesPath(String path) {
        this.signaturePropertiesPath = path;
    }
}
