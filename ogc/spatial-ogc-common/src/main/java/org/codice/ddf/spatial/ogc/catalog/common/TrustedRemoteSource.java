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

package org.codice.ddf.spatial.ogc.catalog.common;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.common.util.CollectionUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.FiltersType;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.HTTPConduit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TrustedRemoteSource {
    
    protected static final String[] SSL_ALLOWED_ALGORITHMS =
            {
                    ".*_WITH_AES_.*"
            };

    protected static final String[] SSL_DISALLOWED_ALGORITHMS =
            {
                    ".*_WITH_NULL_.*", ".*_DH_anon_.*"
            };

    private static final Logger LOGGER = LoggerFactory.getLogger(TrustedRemoteSource.class);



    /**
     * Configures the client keystores. If any of the paramters are null, that keystore will be set
     * to the system default.
     *
     * @param client             Client used for outgoing requests.
     * @param keyStorePath       Path to the keystore that should be used.
     * @param keyStorePassword   Password for the keystore.
     * @param trustStorePath     Path to the truststore that should be used.
     * @param trustStorePassword Password for the truststore.
     */
    protected void configureKeystores(Client client, String keyStorePath, String keyStorePassword,
            String trustStorePath, String trustStorePassword) {
        ClientConfiguration clientConfiguration = WebClient.getConfig(client);

        HTTPConduit httpConduit = clientConfiguration.getHttpConduit();
        TLSClientParameters tlsParams = httpConduit.getTlsClientParameters();

        try {
            if (tlsParams == null) {
                tlsParams = new TLSClientParameters();
                httpConduit.setTlsClientParameters(tlsParams);
            }

            // the default type is JKS
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());

            // add the trustStore if it exists
            if (StringUtils.isNotEmpty(trustStorePath)) {
                File trustStoreFile = new File(trustStorePath);
                if (trustStoreFile.exists() && trustStorePassword != null) {
                    FileInputStream fis = new FileInputStream(trustStoreFile);
                    try {
                        LOGGER.debug("Loading trustStore");
                        if (StringUtils.isNotEmpty(trustStorePassword)) {
                            trustStore.load(fis, trustStorePassword.toCharArray());
                        } else {
                            LOGGER.debug(
                                    "No password found, trying to load trustStore with no password.");
                            trustStore.load(fis, null);
                        }
                    } catch (IOException e) {
                        LOGGER.error("Unable to load truststore. {}", trustStoreFile, e);
                    } catch (CertificateException e) {
                        LOGGER.error("Unable to load certificates from keystore. {}",
                                trustStoreFile, e);
                    } finally {
                        IOUtils.closeQuietly(fis);
                    }
                    TrustManagerFactory trustFactory = TrustManagerFactory
                            .getInstance(TrustManagerFactory
                                    .getDefaultAlgorithm());
                    trustFactory.init(trustStore);
                    LOGGER.debug("trust manager factory initialized");
                    TrustManager[] tm = trustFactory.getTrustManagers();
                    tlsParams.setTrustManagers(tm);
                } else {
                    tlsParams.setTrustManagers(null);
                    LOGGER.debug(
                            "TrustStore file does not exist or no password was set, using system default.");
                }
            } else {
                tlsParams.setTrustManagers(null);
                LOGGER.debug(
                        "TrustStore path was passed in as null or empty, using system default");
            }

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

            // add the keyStore if it exists
            if(StringUtils.isNotEmpty(keyStorePath)) {
                File keyStoreFile = new File(keyStorePath);
                if (keyStoreFile.exists()) {
                    FileInputStream fis = new FileInputStream(keyStoreFile);
                    try {
                        LOGGER.debug("Loading keyStore");
                        if(StringUtils.isNotEmpty(keyStorePassword)) {
                            keyStore.load(fis, keyStorePassword.toCharArray());
                        } else {
                            LOGGER.debug(
                                    "No password found, trying to load keyStore with no password.");
                            keyStore.load(fis, null);
                        }
                    } catch (IOException e) {
                        LOGGER.error("Unable to load keystore. {}", keyStoreFile, e);
                    } catch (CertificateException e) {
                        LOGGER.error("Unable to load certificates from keystore. {}", keyStoreFile,
                                e);
                    } finally {
                        IOUtils.closeQuietly(fis);
                    }
                    KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory
                            .getDefaultAlgorithm());
                    keyFactory.init(keyStore, keyStorePassword.toCharArray());
                    LOGGER.debug("key manager factory initialized");
                    KeyManager[] km = keyFactory.getKeyManagers();
                    tlsParams.setKeyManagers(km);
                } else {
                    LOGGER.debug(
                            "Keystore path or password were not passed in, using system defaults.");
                    tlsParams.setKeyManagers(null);
                }
            } else {
                LOGGER.debug(
                        "Keystore path was passed in as null or empty, using system defaults.");
                tlsParams.setKeyManagers(null);
            }

            // this sets the algorithms that we accept for SSL
            FiltersType filter = new FiltersType();
            filter.getInclude().addAll(Arrays.asList(SSL_ALLOWED_ALGORITHMS));
            filter.getExclude().addAll(Arrays.asList(SSL_DISALLOWED_ALGORITHMS));
            tlsParams.setCipherSuitesFilter(filter);

        } catch (KeyStoreException e) {
            LOGGER.error("Unable to read keystore: ", e);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Problems creating SSL socket. Usually this is "
                            + "referring to the certificate sent by the server not being trusted by the client.",
                    e);
        } catch (FileNotFoundException e) {
            LOGGER.error("Unable to locate one of the SSL stores: {} | {}", trustStorePath,
                    keyStorePath, e);
        } catch (UnrecoverableKeyException e) {
            LOGGER.error("Unable to read keystore: ", e);
        }
    }
    
    /**
     * Creates the JAX-RS client based the information provided.
     * 
     * @param clazz
     *            - the interface this client implements
     * @param url
     *            - the URL of the server to connect to
     * @param username
     *            - username for basic auth (can be null or empty)
     * @param password
     *            - password for basic auth (can be null or empty)
     * @param disableSslCertValidation
     *            - flag to disable CN check when using HTTPS
     * @param providers
     *            - list of providers
     * @param classLoader
     *            - the classloader of the client. Ensures the interface is on the classpath.
     * @return - the client
     */
    protected <T> T createClientBean(Class<T> clazz, String url, String username,
            String password, boolean disableSslCertValidation, List<? extends Object> providers, ClassLoader classLoader) {
        JAXRSClientFactoryBean clientFactoryBean = initClientBean(clazz, url, classLoader,
                providers, username, password);

        T client = clientFactoryBean.create(clazz);
        if (disableSslCertValidation) {
            disableSSLCertValidation(client);
        }

        return client;
    }

    /**
     * Creates the JAX-RS client based the information provided.
     * 
     * @param clazz
     *            - the interface this client implements
     * @param url
     *            - the URL of the server to connect to
     * @param username
     *            - username for basic auth (can be null or empty)
     * @param password
     *            - password for basic auth (can be null or empty)
     * @param disableSslCertValidation
     *            - flag to disable CN check when using HTTPS
     * @param providers
     *            - list of providers
     * @param classLoader
     *            - the classloader of the client. Ensures the interface is on the classpath.
     * @param interceptor
     *            - a custom InInterceptor
     * @return - the client
     */
    protected <T> T createClientBean(Class<T> clazz, String url, String username,
 String password,
            boolean disableSslCertValidation, List<? extends Object> providers,
            ClassLoader classLoader,
            Interceptor<? extends Message> interceptor) {
        JAXRSClientFactoryBean clientFactoryBean = initClientBean(clazz, url, classLoader,
                providers, username, password);

        if (interceptor != null) {
            clientFactoryBean.getInInterceptors().add(interceptor);
        }

        T client = clientFactoryBean.create(clazz);
        if (disableSslCertValidation) {
            disableSSLCertValidation(client);
        }

        return client;
    }

    private JAXRSClientFactoryBean initClientBean(Class clazz, String url, ClassLoader classLoader,
            List<? extends Object> providers, String username, String password) {
        if (StringUtils.isEmpty(url)) {
            final String errMsg = TrustedRemoteSource.class.getSimpleName()
                    + " was called without a valid URL. "
                    + TrustedRemoteSource.class.getSimpleName() + " will not be able to connect.";
            LOGGER.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        JAXRSClientFactoryBean clientFactoryBean = new JAXRSClientFactoryBean();
        clientFactoryBean.setServiceClass(clazz);
        clientFactoryBean.setAddress(url);
        clientFactoryBean.setClassLoader(classLoader);
        clientFactoryBean.getInInterceptors().add(new LoggingInInterceptor());
        clientFactoryBean.getOutInterceptors().add(new LoggingOutInterceptor());

        if (!CollectionUtils.isEmpty(providers)) {
            clientFactoryBean.setProviders(providers);
        }
        if ((StringUtils.isNotEmpty(username)) && (StringUtils.isNotEmpty(password))) {
            clientFactoryBean.setUsername(username);
            clientFactoryBean.setPassword(password);
        }
        return clientFactoryBean;
    }

    private void disableSSLCertValidation(Object client) {
        ClientConfiguration config = WebClient.getConfig(client);
        HTTPConduit conduit = (HTTPConduit) config.getConduit();

        TLSClientParameters params = conduit.getTlsClientParameters();

        if (params == null) {
            params = new TLSClientParameters();
            conduit.setTlsClientParameters(params);
        }

        params.setTrustManagers(new TrustManager[] {new X509TrustManager() {

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

        }});
        params.setDisableCNCheck(true);

    }
}
