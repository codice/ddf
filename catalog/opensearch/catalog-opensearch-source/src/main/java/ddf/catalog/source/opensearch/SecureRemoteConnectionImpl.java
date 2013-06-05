/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.source.opensearch;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.BinaryContentImpl;

/**
 * Makes a SSL Socket connection to a remote entity.
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 *
 */
public class SecureRemoteConnectionImpl implements SecureRemoteConnection {

    private static final Logger LOGGER = Logger.getLogger(OpenSearchSiteUtil.class);
    
    private static final MimeType DEFAULT_MIMETYPE = createDefaultMimeType();

    private SSLSocketFactory socketFactory;

    private String trustStore;

    private String trustStorePass;

    private String keyStore;

    private String keyStorePass;
    
    private static MimeType createDefaultMimeType() {
        try {
            return new MimeType("application/octet-stream");
        } catch (MimeTypeParseException e) {
            LOGGER.debug("Problem creating default mime type.");
        }
        return new MimeType();
    }
    

    @Override
    public BinaryContent getData(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        URLConnection conn = url.openConnection();
        if (conn instanceof HttpsURLConnection) {
            try {
                ((HttpsURLConnection) conn)
                        .setSSLSocketFactory(getSocketFactory());
            } catch (Exception e) {
                throw new IOException("Error in creating SSL socket.", e);
            }
        }
        conn.connect();
        MimeType mimeType = DEFAULT_MIMETYPE;
        try {
            mimeType = new MimeType(conn.getContentType());
        } catch (MimeTypeParseException e) {
            LOGGER.debug("Error creating mime type with input ["
                    + conn.getContentType() + "], defaulting to "
                    + DEFAULT_MIMETYPE.toString());
        }
        return new BinaryContentImpl(conn.getInputStream(), mimeType);
    }

    /**
     * Creates a new SSLSocketFactory from a truststore and keystore. This is
     * used during SSL communications with the server.
     * 
     * @param trustStoreLoc
     *            File path to the truststore.
     * @param trustStorePass
     *            Password to the truststore.
     * @param keyStoreLoc
     *            File path to the keystore.
     * @param keyStorePass
     *            Password to the keystore.
     * @return new SSLSocketFactory instance containing the trust and key
     *         stores.
     * @throws KeyStoreException
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     * @throws KeyManagementException
     */
    public SSLSocketFactory createSocket(String trustStoreLoc,
            String trustStorePass, String keyStoreLoc, String keyStorePass)
            throws KeyStoreException, NoSuchAlgorithmException,
            CertificateException, IOException, UnrecoverableKeyException,
            KeyManagementException {
        String methodName = "createSocket";
        LOGGER.debug("ENTERING: " + methodName);

        LOGGER.debug("trustStoreLoc = " + trustStoreLoc);
        FileInputStream trustFIS = new FileInputStream(trustStoreLoc);
        LOGGER.debug("keyStoreLoc = " + keyStoreLoc);
        FileInputStream keyFIS = new FileInputStream(keyStoreLoc);

        // truststore stuff
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try {
            LOGGER.debug("Loading trustStore");
            trustStore.load(trustFIS, trustStorePass.toCharArray());
        } finally {
            IOUtils.closeQuietly(trustFIS);
        }

        TrustManagerFactory tmf = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        LOGGER.debug("trust manager factory initialized");

        // keystore stuff
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try {
            LOGGER.debug("Loading keyStore");
            keyStore.load(keyFIS, keyStorePass.toCharArray());
        } finally {
            IOUtils.closeQuietly(keyFIS);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory
                .getDefaultAlgorithm());
        kmf.init(keyStore, keyStorePass.toCharArray());
        LOGGER.debug("key manager factory initialized");

        // ssl context
        SSLContext sslCtx = SSLContext.getInstance("TLS");
        sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        LOGGER.debug("EXITING: " + methodName);

        return sslCtx.getSocketFactory();
    }

    @Override
    public String getTrustStoreLocation() {
        return this.trustStore;
    }

    @Override
    public void setTrustStoreLocation(String trustStore) {
        resetSocketFactory();
        this.trustStore = trustStore;
    }

    @Override
    public String getKeyStoreLocation() {
        return this.keyStore;
    }

    @Override
    public void setKeyStoreLocation(String keyStore) {
        resetSocketFactory();
        this.keyStore = keyStore;
    }

    @Override
    public String getTrustStorePassword() {
        return this.trustStorePass;
    }

    @Override
    public void setTrustStorePassword(String trustStorePass) {
        resetSocketFactory();
        this.trustStorePass = trustStorePass;
    }

    @Override
    public String getKeyStorePassword() {
        return this.keyStorePass;
    }

    @Override
    public void setKeyStorePassword(String keyStorePass) {
        resetSocketFactory();
        this.keyStorePass = keyStorePass;
    }

    /**
     * Makes sure a new SSLSocketFactory is only created if necessary. Checks to
     * see if the current factory is null (which means either the stores were
     * changed or a factory hasn't been made yet).
     * 
     * @return current SSLSocketFactory
     */
    private SSLSocketFactory getSocketFactory() throws Exception {
        if (socketFactory == null) {
            socketFactory = createSocket(trustStore, trustStorePass, keyStore,
                    keyStorePass);
        }
        return socketFactory;
    }

    private void resetSocketFactory() {
        socketFactory = null;
    }

}
