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
package ddf.security.common.util;

import ddf.security.SecurityConstants;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

/**
 * Creates a new SSLSocketFactory
 * 
 */
public final class CommonSSLFactory {
    private static Logger LOGGER = LoggerFactory.getLogger(SecurityConstants.SECURITY_LOGGER);

    private static String ENTERING = "ENTERING: {}";
    private static String EXITING = "EXITING: {}";

    public static final String PROTOCOL = "TLS";

    private CommonSSLFactory() {

    }

    /**
     * Creates a new SSLSocketFactory from a truststore and keystore. This is used during SSL
     * communication.
     * 
     * @param trustStoreLoc
     *            File path to the truststore.
     * @param trustStorePass
     *            Password to the truststore.
     * @param keyStoreLoc
     *            File path to the keystore.
     * @param keyStorePass
     *            Password to the keystore.
     * @return new SSLSocketFactory instance containing the trust and key stores.
     * @throws IOException
     */
    public static SSLSocketFactory createSocket(String trustStoreLoc, String trustStorePass,
            String keyStoreLoc, String keyStorePass) throws IOException {
        String methodName = "createSocket";
        LOGGER.debug(ENTERING, methodName);

        try {
            TrustManagerFactory tmf = createTrustManagerFactory(trustStoreLoc, trustStorePass);

            KeyManagerFactory kmf = createKeyManagerFactory(keyStoreLoc, keyStorePass);

            // ssl context
            SSLContext sslCtx = SSLContext.getInstance(PROTOCOL);
            sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            sslCtx.getDefaultSSLParameters().setNeedClientAuth(true);
            sslCtx.getDefaultSSLParameters().setWantClientAuth(true);
            LOGGER.debug(EXITING, methodName);

            return sslCtx.getSocketFactory();
        } catch (KeyManagementException e) {
            LOGGER.debug(EXITING, methodName);
            throw new IOException("Unable to initialize the SSL context.", e);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.debug(EXITING, methodName);
            throw new IOException(
                    "Problems creating SSL socket. Usually this is "
                            + "referring to the certificate sent by the server not being trusted by the client.",
                    e);
        }
    }

    public static KeyManagerFactory createKeyManagerFactory(String keyStoreLoc, String keyStorePass) throws IOException{
        KeyManagerFactory kmf;
        try {
            // keystore stuff
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            LOGGER.debug("keyStoreLoc = {}", keyStoreLoc);
            FileInputStream keyFIS = new FileInputStream(keyStoreLoc);
            try {
                LOGGER.debug("Loading keyStore");
                keyStore.load(keyFIS, keyStorePass.toCharArray());
            } catch (CertificateException e) {
                throw new IOException("Unable to load certificates from keystore. " + keyStoreLoc,
                        e);
            } finally {
                IOUtils.closeQuietly(keyFIS);
            }
            kmf = KeyManagerFactory.getInstance(KeyManagerFactory
                    .getDefaultAlgorithm());
            kmf.init(keyStore, keyStorePass.toCharArray());
            LOGGER.debug("key manager factory initialized");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(
                    "Problems creating SSL socket. Usually this is "
                            + "referring to the certificate sent by the server not being trusted by the client.",
                    e);
        } catch (UnrecoverableKeyException e) {
            throw new IOException("Unable to load keystore. " + keyStoreLoc, e);
        } catch (KeyStoreException e) {
            throw new IOException("Unable to read keystore. " + keyStoreLoc, e);
        }

        return kmf;
    }

    public static TrustManagerFactory createTrustManagerFactory(String trustStoreLoc, String trustStorePass) throws IOException {
        TrustManagerFactory tmf;
        try {
            // truststore stuff
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            LOGGER.debug("trustStoreLoc = {}", trustStoreLoc);
            FileInputStream trustFIS = new FileInputStream(trustStoreLoc);
            try {
                LOGGER.debug("Loading trustStore");
                trustStore.load(trustFIS, trustStorePass.toCharArray());
            } catch (CertificateException e) {
                throw new IOException("Unable to load certificates from truststore. "
                        + trustStoreLoc, e);
            } finally {
                IOUtils.closeQuietly(trustFIS);
            }

            tmf = TrustManagerFactory.getInstance(TrustManagerFactory
                    .getDefaultAlgorithm());
            tmf.init(trustStore);
            LOGGER.debug("trust manager factory initialized");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(
                    "Problems creating SSL socket. Usually this is "
                            + "referring to the certificate sent by the server not being trusted by the client.",
                    e);
        } catch (KeyStoreException e) {
            throw new IOException("Unable to read keystore. " + trustStoreLoc, e);
        }
        return tmf;
    }
}
