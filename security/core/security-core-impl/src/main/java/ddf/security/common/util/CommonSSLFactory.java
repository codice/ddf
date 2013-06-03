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
package ddf.security.common.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import ddf.security.SecurityConstants;

/**
 * Creates a new SSLSocketFactory
 * 
 */
public final class CommonSSLFactory
{
    private static Logger logger = Logger.getLogger(SecurityConstants.SECURITY_LOGGER);
    private static String exiting = "EXITING: ";
    
    private CommonSSLFactory()
    {

    }

    /**
     * Creates a new SSLSocketFactory from a truststore and keystore. This is
     * used during SSL communication.
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
     * @throws IOException
     */
    public static SSLSocketFactory createSocket(String trustStoreLoc, String trustStorePass, String keyStoreLoc, String keyStorePass) throws IOException
    {
        String methodName = "createSocket";
        logger.debug("ENTERING: " + methodName);

        try
        {
            logger.debug("trustStoreLoc = " + trustStoreLoc);
            FileInputStream trustFIS = new FileInputStream(trustStoreLoc);
            logger.debug("keyStoreLoc = " + keyStoreLoc);
            FileInputStream keyFIS = new FileInputStream(keyStoreLoc);

            // truststore stuff
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try
            {
                logger.debug("Loading trustStore");
                trustStore.load(trustFIS, trustStorePass.toCharArray());
            }
            catch (CertificateException e)
            {
                throw new IOException("Unable to load certificates from truststore. "+trustStoreLoc, e);
            }
            finally
            {
                IOUtils.closeQuietly(trustFIS);
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            logger.debug("trust manager factory initialized");

            // keystore stuff
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try
            {
                logger.debug("Loading keyStore");
                keyStore.load(keyFIS, keyStorePass.toCharArray());
            }
            catch (CertificateException e)
            {
                throw new IOException("Unable to load certificates from keystore. "+keyStoreLoc, e);
            }
            finally
            {
                IOUtils.closeQuietly(keyFIS);
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, keyStorePass.toCharArray());
            logger.debug("key manager factory initialized");

            // ssl context
            SSLContext sslCtx = SSLContext.getInstance("TLS");
            sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            sslCtx.getDefaultSSLParameters().setNeedClientAuth(true);
            sslCtx.getDefaultSSLParameters().setWantClientAuth(true);
            logger.debug(exiting + methodName);

            return sslCtx.getSocketFactory();
        }
        catch (KeyManagementException e)
        {
            logger.debug(exiting + methodName);
            throw new IOException("Unable to initialize the SSL context.", e);
        }
        catch (NoSuchAlgorithmException e)
        {
            logger.debug(exiting + methodName);
            throw new IOException("Problems creating SSL socket. Usually this is " +
                    "referring to the certificate sent by the server not being trusted by the client.", e);
        }
        catch (UnrecoverableKeyException e)
        {
            logger.debug(exiting + methodName);
            throw new IOException("Unable to load keystore. "+keyStoreLoc, e);
        }
        catch (KeyStoreException e)
        {
            logger.debug(exiting + methodName);
            throw new IOException("Unable to read keystore. "+keyStoreLoc, e);
        }
    }
}
