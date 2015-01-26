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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import ddf.security.settings.SecuritySettingsService;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.common.util.CollectionUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.https.HttpsURLConnectionFactory;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.trust.STSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.PropertiesLoader;
import ddf.security.sts.client.configuration.STSClientConfiguration;

public abstract class TrustedRemoteSource {
    
    public static final String DISABLE_CN_CHECK_PROPERTY = "disableCnCheck";

    private static final Logger LOGGER = LoggerFactory.getLogger(TrustedRemoteSource.class);

    public static final Integer DEFAULT_CONNECTION_TIMEOUT = 30000;

    public static final Integer DEFAULT_RECEIVE_TIMEOUT = 60000;
    
    protected static final String ADDRESSING_NAMESPACE = "http://www.w3.org/2005/08/addressing";
    
    protected static final int HTTP_STATUS_CODE_OK = 200;

    protected static final int CONNECTION_TIMEOUT_INTERVAL = 3000;

    protected static final String QUESTION_MARK_WSDL = "?wsdl";

    protected static final String DOT_WSDL = ".wsdl";
    
    protected HashMap<String, String> wsdlSuffixMap = new HashMap<String, String>();

    protected SecuritySettingsService securitySettingsService;

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
        if (httpConduit == null) {
            LOGGER.info("HTTPConduit was null for {}. Unable to configure timeouts", client);
            return;
        }
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
        HTTPConduit conduit = config.getHttpConduit();
        if (conduit == null) {
            LOGGER.info("HTTPConduit was null for {}. Unable to disable CN Check", client);
            return;
        }

        TLSClientParameters params = conduit.getTlsClientParameters();

        if (params == null) {
            params = new TLSClientParameters();
            conduit.setTlsClientParameters(params);
        }

        params.setDisableCNCheck(true);
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
        LOGGER.debug("Setting WSDL location (stsAddress) on STSClient: " + stsAddress);
        stsClient.setWsdlLocation(stsAddress);
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

        //DDF-733 LOGGER.debug("Setting callback handler on STSClient");
        //DDF-733 map.put(SecurityConstants.CALLBACK_HANDLER, new CommonCallbackHandler());
        LOGGER.debug("Setting STS TOKEN USE CERT FOR KEY INFO to \"true\"");
        map.put(SecurityConstants.STS_TOKEN_USE_CERT_FOR_KEYINFO, Boolean.TRUE.toString());
        stsClient.setProperties(map);

        if (stsClient.getWsdlLocation()
                .startsWith(HttpsURLConnectionFactory.HTTPS_URL_PROTOCOL_ID)) {
            try {
                LOGGER.debug("Setting up SSL on the STSClient HTTP Conduit");
                HTTPConduit httpConduit = (HTTPConduit) stsClient.getClient().getConduit();
                if (httpConduit == null) {
                    LOGGER.info("HTTPConduit was null for stsClient. Unable to configure keystores for stsClient.");
                } else {
                    if (securitySettingsService != null) {
                        httpConduit.setTlsClientParameters(securitySettingsService.getTLSParameters());
                    } else {
                        LOGGER.debug("Could not get reference to security settings, SSL communications will use system defaults.");
                    }

                }
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

    public void setSecuritySettings(SecuritySettingsService securitySettings) {
        this.securitySettingsService = securitySettings;
    }

    /**
     * Set current system TLS Parameters on incoming client
     * @param client - Client to set the TLS parameters on
     */
    protected void setTlsParameters(Client client) {
        ClientConfiguration clientConfiguration = WebClient.getConfig(client);

        HTTPConduit httpConduit = clientConfiguration.getHttpConduit();
        if (securitySettingsService != null && httpConduit != null) {
            TLSClientParameters origParameters = httpConduit.getTlsClientParameters();
            TLSClientParameters tlsClientParameters = securitySettingsService.getTLSParameters();
            if (origParameters != null) {
                tlsClientParameters.setDisableCNCheck(origParameters.isDisableCNCheck());
            }
            httpConduit.setTlsClientParameters(tlsClientParameters);
        } else {
            LOGGER.debug(
                    "Could not get a reference to security settings service or reference to client http conduit, using system defaults.");
        }

    }

}
