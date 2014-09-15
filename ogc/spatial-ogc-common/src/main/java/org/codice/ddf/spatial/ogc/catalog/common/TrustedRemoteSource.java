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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.core.Cookie;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.CollectionUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.FiltersType;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.saml.DeflateEncoderDecoder;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.https.HttpsURLConnectionFactory;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.PropertiesLoader;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.common.callback.CommonCallbackHandler;
import ddf.security.sts.client.configuration.STSClientConfiguration;

public abstract class TrustedRemoteSource {
    
    public static final String DISABLE_CN_CHECK_PROPERTY = "disableCnCheck";

    protected static final String[] SSL_ALLOWED_ALGORITHMS =
            {
                    ".*_WITH_AES_.*"
            };

    protected static final String[] SSL_DISALLOWED_ALGORITHMS =
            {
                    ".*_WITH_NULL_.*", ".*_DH_anon_.*"
            };

    protected static final String SAML_COOKIE_NAME = "org.codice.websso.saml.token";

    private static final Logger LOGGER = LoggerFactory.getLogger(TrustedRemoteSource.class);

    public static final Integer DEFAULT_CONNECTION_TIMEOUT = 30000;

    public static final Integer DEFAULT_RECEIVE_TIMEOUT = 60000;
    
    protected static final String ADDRESSING_NAMESPACE = "http://www.w3.org/2005/08/addressing";
    
    protected static final int HTTP_STATUS_CODE_OK = 200;

    protected static final int CONNECTION_TIMEOUT_INTERVAL = 3000;

    protected static final String QUESTION_MARK_WSDL = "?wsdl";

    protected static final String DOT_WSDL = ".wsdl";
    
    protected HashMap<String, String> wsdlSuffixMap = new HashMap<String, String>();

    // SSL keystores and passwords
    protected String trustStorePath;

    protected String keyStorePath;

    protected String trustStorePassword;

    protected String keyStorePassword;

    /**
     * Configures the connection and receive timeouts. If any of the parameters are null, the timeouts
     * will be set to the system default.
     *
     * @param client             Client used for outgoing requests.
     * @param connectionTimeout  Connection timeout in milliseconds.
     * @param receiveTimeout     Receive timeout in milliseconds.
     */
    protected void configureTimeouts(Client client, Integer connectionTimeout, Integer receiveTimeout) {
        ClientConfiguration clientConfiguration = WebClient.getConfig(client);

        HTTPConduit httpConduit = clientConfiguration.getHttpConduit();
        HTTPClientPolicy httpClientPolicy = httpConduit.getClient();

        if (httpClientPolicy == null) {
            httpClientPolicy = new HTTPClientPolicy();
        }

        if (connectionTimeout != null) {
            httpClientPolicy.setConnectionTimeout(connectionTimeout);
        } else {
            httpClientPolicy.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
        }

        if (receiveTimeout != null) {
            httpClientPolicy.setReceiveTimeout(receiveTimeout);
        } else {
            httpClientPolicy.setReceiveTimeout(DEFAULT_RECEIVE_TIMEOUT);
        }

        if (httpClientPolicy.isSetConnectionTimeout()) {
            LOGGER.debug("Connection timeout has been set.");
        } else {
            LOGGER.error("Connection timeout has NOT been set.");
        }
        if (httpClientPolicy.isSetReceiveTimeout()) {
            LOGGER.debug("Receive timeout has been set.");
        } else {
            LOGGER.error("Receive timeout has NOT been set.");
        }

        httpConduit.setClient(httpClientPolicy);
    }



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
        configureKeystores(httpConduit, keyStorePath, keyStorePassword, trustStorePath, trustStorePassword);
    }
    
    private void configureKeystores(HTTPConduit httpConduit, String keyStorePath, String keyStorePassword,
            String trustStorePath, String trustStorePassword) {

        LOGGER.debug("Setting keyStore/trustStore paths/passwords");
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
        this.trustStorePath = trustStorePath;
        this.trustStorePassword = trustStorePassword;

        TLSClientParameters tlsParams = httpConduit.getTlsClientParameters();

        try {
            if (tlsParams == null) {
                tlsParams = new TLSClientParameters();
            }
            
            tlsParams.setDisableCNCheck(true);

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
            
            httpConduit.setTlsClientParameters(tlsParams);

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
     * @param disableCnCheck
     *            - flag to disable CN check when using HTTPS (Should only be used for testing)
     * @param providers
     *            - list of providers
     * @param classLoader
     *            - the classloader of the client. Ensures the interface is on the classpath.
     * @return - the client
     */
    protected <T> T createClientBean(Class<T> clazz, String url, String username, String password,
            boolean disableCnCheck, List<? extends Object> providers, ClassLoader classLoader) {
        JAXRSClientFactoryBean clientFactoryBean = initClientBean(clazz, url, classLoader,
                providers, username, password);

        T client = clientFactoryBean.create(clazz);
        if (disableCnCheck) {
            disableCnCheck(client);
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
     * @param disableCnCheck
     *            - flag to disable CN check when using HTTPS (Should only be used for testing)
     * @param providers
     *            - list of providers
     * @param classLoader
     *            - the classloader of the client. Ensures the interface is on the classpath.
     * @param interceptor
     *            - a custom InInterceptor
     * @return - the client
     */
    protected <T> T createClientBean(Class<T> clazz, String url, String username, String password,
            boolean disableCnCheck, List<? extends Object> providers, ClassLoader classLoader,
            Interceptor<? extends Message> interceptor) {
        JAXRSClientFactoryBean clientFactoryBean = initClientBean(clazz, url, classLoader,
                providers, username, password);

        if (interceptor != null) {
            clientFactoryBean.getInInterceptors().add(interceptor);
        }

        T client = clientFactoryBean.create(clazz);
        if (disableCnCheck) {
            disableCnCheck(client);
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

    private void disableCnCheck(Object client) {
        ClientConfiguration config = WebClient.getConfig(client);
        HTTPConduit conduit = (HTTPConduit) config.getConduit();

        TLSClientParameters params = conduit.getTlsClientParameters();

        if (params == null) {
            params = new TLSClientParameters();
            conduit.setTlsClientParameters(params);
        }

        params.setDisableCNCheck(true);
    }

    /**
     * Sets a subject cookie on a {@link org.apache.cxf.jaxrs.client.WebClient} and returns the
     * resulting client.
     * @param client - {@link org.apache.cxf.jaxrs.client.Client} to update
     * @param subject - {@link ddf.security.Subject} to inject
     * @return {@link org.apache.cxf.jaxrs.client.WebClient}
     */
    protected void setSubjectOnRequest(Client client, Subject subject) {
        if(subject != null) {
            Cookie cookie = createSamlCookie(subject);
            client.reset();
            client.cookie(cookie);
        }
    }

    /**
     * Creates a cookie to be returned to the browser if the token was successfully exchanged for
     * a SAML assertion.
     *
     * @param subject - {@link ddf.security.Subject} to create the cookie from
     */
    private Cookie createSamlCookie(Subject subject) {
        Cookie cookie = null;
        org.w3c.dom.Element samlToken = null;
        try {
            for (Object principal : subject.getPrincipals().asList()) {
                if (principal instanceof SecurityAssertion) {
                    samlToken = ((SecurityAssertion) principal).getSecurityToken()
                            .getToken();
                }
            }
            if (samlToken != null) {
                cookie = new Cookie(SAML_COOKIE_NAME, encodeSaml(samlToken));
            }
        } catch (WSSecurityException e) {
            LOGGER.error("Unable to parse SAML assertion from subject.", e);
        }
        return cookie;
    }

    /**
     * Encodes the SAML assertion as a deflated Base64 String so that it can be used as a Cookie.
     *
     * @param token
     * @return String
     * @throws WSSecurityException
     */
    protected  String encodeSaml(org.w3c.dom.Element token) throws WSSecurityException {
        AssertionWrapper assertion = new AssertionWrapper(token);
        String samlStr = assertion.assertionToString();
        DeflateEncoderDecoder deflateEncoderDecoder = new DeflateEncoderDecoder();
        byte[] deflatedToken = deflateEncoderDecoder.deflateToken(samlStr.getBytes());
        return Base64Utility.encode(deflatedToken);
    }
    
    public void setSAMLAssertion(Client client, STSClientConfiguration stsClientConfig) {
        LOGGER.debug("ENTERING: setSAMLAssertion()");
        if (stsClientConfig == null || StringUtils.isBlank(stsClientConfig.getAddress())) {
            LOGGER.debug("STSClientConfiguration is either null or its address is blank - assuming no STS Client is configured, so no SAML assertion will get generated.");
            return;
        }
        ClientConfiguration clientConfig = WebClient.getConfig(client);
        Bus bus = clientConfig.getBus();
        STSClient stsClient = configureSTSClient(bus, stsClientConfig);
        stsClient.setTokenType(stsClientConfig.getAssertionType());
        stsClient.setKeyType(stsClientConfig.getKeyType());
        stsClient.setKeySize(Integer.valueOf(stsClientConfig.getKeySize()));
        try {
            SecurityToken securityToken = stsClient.requestSecurityToken(stsClientConfig.getAddress());
            org.w3c.dom.Element samlToken = securityToken.getToken();
            if (samlToken != null) {
                Cookie cookie = new Cookie(SAML_COOKIE_NAME, encodeSaml(samlToken));
                client.reset();
                client.cookie(cookie);
            }
        } catch (Exception e) {
            LOGGER.warn("Exception trying to get SAML assertion", e);
        }
        LOGGER.debug("EXITING: setSAMLAssertion()");
    }
    
    /**
     * Returns a new STSClient object configured with the properties that have
     * been set.
     *
     * @param bus - CXF bus to initialize STSClient with
     * @return STSClient
     */
    protected STSClient configureSTSClient(Bus bus, STSClientConfiguration stsClientConfig) {
        final String methodName = "configureSTSClient";
        LOGGER.debug("ENTERING: {}", methodName);

        String stsAddress = stsClientConfig.getAddress();
        String stsServiceName = stsClientConfig.getServiceName();
        String stsEndpointName = stsClientConfig.getEndpointName();
        String signaturePropertiesPath = stsClientConfig.getSignatureProperties();
        String encryptionPropertiesPath = stsClientConfig.getEncryptionProperties();
        String stsPropertiesPath = stsClientConfig.getTokenProperties();

        STSClient stsClient = new STSClient(bus);
        if (StringUtils.isBlank(stsAddress)) {
            LOGGER.debug("STS address is null, unable to create STS Client");
            LOGGER.debug("EXITING: {}", methodName);
            return stsClient;
        }
        String wsdlExt = retrieveWsdlSuffix(stsAddress);
        LOGGER.debug("Setting WSDL location on STSClient: " + stsAddress + wsdlExt);
        stsClient.setWsdlLocation(stsAddress + wsdlExt);
        LOGGER.debug("Setting service name on STSClient: " + stsServiceName);
        stsClient.setServiceName(stsServiceName);
        LOGGER.debug("Setting endpoint name on STSClient: " + stsEndpointName);
        stsClient.setEndpointName(stsEndpointName);
        LOGGER.debug("Setting addressing namespace on STSClient: " + ADDRESSING_NAMESPACE);
        stsClient.setAddressingNamespace(ADDRESSING_NAMESPACE);

        Map<String, Object> map = new HashMap<String, Object>();

        // Properties loader should be able to find the properties file no
        // matter where it is
        if (signaturePropertiesPath != null && !signaturePropertiesPath.isEmpty()) {
            LOGGER.debug(
                    "Setting signature properties on STSClient: " + signaturePropertiesPath);
            Properties signatureProperties = PropertiesLoader
                    .loadProperties(signaturePropertiesPath);
            map.put(SecurityConstants.SIGNATURE_PROPERTIES, signatureProperties);
        }
        if (encryptionPropertiesPath != null && !encryptionPropertiesPath.isEmpty()) {
            LOGGER.debug(
                    "Setting encryption properties on STSClient: " + encryptionPropertiesPath);
            Properties encryptionProperties = PropertiesLoader
                    .loadProperties(encryptionPropertiesPath);
            map.put(SecurityConstants.ENCRYPT_PROPERTIES, encryptionProperties);
        }
        if (stsPropertiesPath != null && !stsPropertiesPath.isEmpty()) {
            LOGGER.debug("Setting sts properties on STSClient: " + stsPropertiesPath);
            Properties stsProperties = PropertiesLoader.loadProperties(stsPropertiesPath);
            map.put(SecurityConstants.STS_TOKEN_PROPERTIES, stsProperties);
        }

        LOGGER.debug("Setting callback handler on STSClient");
        map.put(SecurityConstants.CALLBACK_HANDLER, new CommonCallbackHandler());
        LOGGER.debug("Setting STS TOKEN USE CERT FOR KEY INFO to \"true\"");
        map.put(SecurityConstants.STS_TOKEN_USE_CERT_FOR_KEYINFO, Boolean.TRUE.toString());
        stsClient.setProperties(map);

        if (stsClient.getWsdlLocation()
                .startsWith(HttpsURLConnectionFactory.HTTPS_URL_PROTOCOL_ID)) {
            try {
                LOGGER.debug("Setting up SSL on the STSClient HTTP Conduit");
                HTTPConduit httpConduit = (HTTPConduit) stsClient.getClient().getConduit();
                this.configureKeystores(httpConduit, keyStorePath, keyStorePassword,
                        trustStorePath, trustStorePassword);
            } catch (BusException e) {
                LOGGER.error("Unable to create sts client.", e);
            } catch (EndpointException e) {
                LOGGER.error("Unable to create sts client endpoint.", e);
            }
        }
        
        LOGGER.debug("EXITING: {}", methodName);
        return stsClient;
    }

    /**
     * This method attempts to figure out which wsdl suffix should be appended to the addresses.
     * If it can find the right address using a simulated "ping," then it uses that.
     * Otherwise, it uses .wsdl  by default.
     *
     * @param address
     * @return
     */
    protected String retrieveWsdlSuffix(String address) {
        if (address != null && !address.isEmpty()) {
            if (!wsdlSuffixMap.containsKey(address)) {
                String url = address + DOT_WSDL;
                try {
                    final HttpURLConnection connection = (HttpURLConnection) new URL(url)
                            .openConnection();
                    connection.setConnectTimeout(CONNECTION_TIMEOUT_INTERVAL);
                    connection.connect();

                    if (connection.getResponseCode() == HTTP_STATUS_CODE_OK) {
                        wsdlSuffixMap.put(address, DOT_WSDL);
                        return DOT_WSDL;
                    }
                } catch (final MalformedURLException e) {
                    LOGGER.info("Bad URL: " + url, e);
                } catch (final IOException e) {
                    LOGGER.info("Service " + url + " is not available.", e);
                }

                url = address + QUESTION_MARK_WSDL;
                try {
                    final HttpURLConnection connection = (HttpURLConnection) new URL(url)
                            .openConnection();
                    connection.setConnectTimeout(CONNECTION_TIMEOUT_INTERVAL);
                    connection.connect();

                    if (connection.getResponseCode() == HTTP_STATUS_CODE_OK) {
                        wsdlSuffixMap.put(address, QUESTION_MARK_WSDL);
                        return QUESTION_MARK_WSDL;
                    }
                } catch (final MalformedURLException e) {
                    LOGGER.info("Bad URL: " + url, e);
                } catch (final IOException e) {
                    LOGGER.info("Service " + url + " is not available.", e);
                }

                // if we can recognize that this is a DDF address, then we know it uses ?wsdl
                if (url.contains("/services")) {
                    wsdlSuffixMap.put(address, QUESTION_MARK_WSDL);
                    return QUESTION_MARK_WSDL;
                }
            } else {
                return wsdlSuffixMap.get(address);
            }
        }
        return QUESTION_MARK_WSDL;
    }

}
