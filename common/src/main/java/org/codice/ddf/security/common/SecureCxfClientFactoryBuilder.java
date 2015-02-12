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
package org.codice.ddf.security.common;

import ddf.security.PropertiesLoader;
import ddf.security.service.SecurityServiceException;
import ddf.security.settings.SecuritySettingsService;
import ddf.security.sts.client.configuration.STSClientConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.common.util.CollectionUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.https.HttpsURLConnectionFactory;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import javax.ws.rs.core.Cookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class SecureCxfClientFactoryBuilder {

    protected static final String ADDRESSING_NAMESPACE = "http://www.w3.org/2005/08/addressing";

    private static final transient Logger LOGGER = LoggerFactory
            .getLogger(SecureCxfClientFactoryBuilder.class);

    private final SecuritySettingsService securitySettingsService;

    private final STSClientConfiguration stsClientConfig;

    /**
     * Blueprint constructor.
     */
    public SecureCxfClientFactoryBuilder(SecuritySettingsService securitySettingsService,
            STSClientConfiguration stsClientConfig) {
        this.securitySettingsService = securitySettingsService;
        this.stsClientConfig = stsClientConfig;
    }

    /**
     * @see SecureCxfClientFactoryBuilder#buildFactory(String, Class, String, String, java.util.List, boolean)
     */
    public <T> SecureCxfClientFactory<T> buildFactory(String endpointUrl, Class<T> interfaceClass)
            throws SecurityServiceException {
        return buildFactory(endpointUrl, interfaceClass, null, null);
    }

    /**
     * @see SecureCxfClientFactoryBuilder#buildFactory(String, Class, String, String, java.util.List, boolean)
     */
    public <T> SecureCxfClientFactory<T> buildFactory(String endpointUrl, Class<T> interfaceClass,
            String username, String password) throws SecurityServiceException {
        return buildFactory(endpointUrl, interfaceClass, username, password, null, false);
    }

    /**
     * Creates a factory that will return security-aware clients.
     *
     * @see org.codice.ddf.security.common.SecureCxfClientFactoryBuilder.SecureCxfClientFactory
     */
    public <T> SecureCxfClientFactory<T> buildFactory(String endpointUrl, Class<T> interfaceClass,
            String username, String password, List<?> providers, boolean disableCnCheck)
            throws SecurityServiceException {
        if (StringUtils.isEmpty(endpointUrl) || interfaceClass == null) {
            throw new IllegalArgumentException(
                    "Called without a valid URL, will not be able to connect.");
        }

        JAXRSClientFactoryBean jaxrsClientFactoryBean = new JAXRSClientFactoryBean();
        jaxrsClientFactoryBean.setServiceClass(interfaceClass);
        jaxrsClientFactoryBean.setAddress(endpointUrl);
        jaxrsClientFactoryBean.setClassLoader(interfaceClass.getClassLoader());
        jaxrsClientFactoryBean.getInInterceptors().add(new LoggingInInterceptor());
        jaxrsClientFactoryBean.getOutInterceptors().add(new LoggingOutInterceptor());

        if (!CollectionUtils.isEmpty(providers)) {
            jaxrsClientFactoryBean.setProviders(providers);
        }

        Client cxfClient = WebClient.client(jaxrsClientFactoryBean.create(interfaceClass));
        ClientConfiguration clientConfig = WebClient.getConfig(cxfClient);
        initSecurity(clientConfig, cxfClient, username, password);

        if (disableCnCheck) {
            disableCnCheck(clientConfig);
        }

        return new SecureCxfClientFactory<>(cxfClient);
    }

    private void disableCnCheck(ClientConfiguration clientConfig) throws SecurityServiceException {
        HTTPConduit httpConduit = clientConfig.getHttpConduit();
        if (httpConduit == null) {
            throw new SecurityServiceException(
                    "HTTPConduit was null for " + this + ". Unable to disable CN Check");
        }

        TLSClientParameters tlsParams = httpConduit.getTlsClientParameters();

        if (tlsParams == null) {
            tlsParams = new TLSClientParameters();
            httpConduit.setTlsClientParameters(tlsParams);
        }

        tlsParams.setDisableCNCheck(true);
    }

    /*
     * Add TLS and Basic Auth credentials to the underlying {@link org.apache.cxf.transport.http.HTTPConduit}
     * This includes two-way ssl assuming that the platform keystores are configured correctly
     */
    private void initSecurity(ClientConfiguration clientConfig, Client cxfClient, String username,
            String password) throws SecurityServiceException {

        HTTPConduit httpConduit = clientConfig.getHttpConduit();
        if (httpConduit == null) {
            throw new SecurityServiceException(
                    "HTTPConduit was null for " + this + ". Unable to configure security.");
        }

        if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
            if (!StringUtils.startsWithIgnoreCase(httpConduit.getAddress(), "https")) {
                throw new SecurityServiceException(
                        "Cannot perform basic auth over non-https connection " + httpConduit
                                .getAddress());
            }
            if (httpConduit.getAuthorization() != null) {
                httpConduit.getAuthorization().setUserName(username);
                httpConduit.getAuthorization().setPassword(password);
            }
        }

        TLSClientParameters tlsParams = securitySettingsService.getTLSParameters();
        httpConduit.setTlsClientParameters(tlsParams);

        initSamlAssertion(clientConfig, cxfClient);
    }

    private void initSamlAssertion(ClientConfiguration clientConfig, Client cxfClient)
            throws SecurityServiceException {
        if (stsClientConfig == null || StringUtils.isBlank(stsClientConfig.getAddress())) {
            LOGGER.debug(
                    "STSClientConfiguration is either null or its address is blank - assuming no STS Client is configured, so no SAML assertion will get generated.");
            return;
        }
        Bus clientBus = clientConfig.getBus();
        STSClient stsClient = configureSTSClient(clientBus, stsClientConfig);
        try {
            SecurityToken securityToken = stsClient
                    .requestSecurityToken(stsClientConfig.getAddress());
            Element samlToken = securityToken.getToken();
            if (samlToken != null) {
                Cookie cookie = new Cookie(RestSecurity.SECURITY_COOKIE_NAME,
                        RestSecurity.encodeSaml(samlToken));
                cxfClient.reset();
                cxfClient.cookie(cookie);
            } else {
                LOGGER.debug(
                        "Attempt to retrieve SAML token resulted in null token - could not add token to request");
            }
        } catch (Exception e) {
            throw new SecurityServiceException("Exception trying to get SAML assertion", e);
        }
    }

    /**
     * Returns a new STSClient object configured with the properties that have
     * been set.
     *
     * @param bus - CXF bus to initialize STSClient with
     * @return STSClient
     */
    private STSClient configureSTSClient(Bus bus, STSClientConfiguration stsClientConfig)
            throws SecurityServiceException {

        String stsAddress = stsClientConfig.getAddress();
        String stsServiceName = stsClientConfig.getServiceName();
        String stsEndpointName = stsClientConfig.getEndpointName();
        String signaturePropertiesPath = stsClientConfig.getSignatureProperties();
        String encryptionPropertiesPath = stsClientConfig.getEncryptionProperties();
        String stsPropertiesPath = stsClientConfig.getTokenProperties();

        STSClient stsClient = new STSClient(bus);
        if (StringUtils.isBlank(stsAddress)) {
            LOGGER.debug("STS address is null, unable to create STS Client");
            return stsClient;
        }
        LOGGER.debug("Setting WSDL location (stsAddress) on STSClient: {}", stsAddress);
        stsClient.setWsdlLocation(stsAddress);
        LOGGER.debug("Setting service name on STSClient: {}", stsServiceName);
        stsClient.setServiceName(stsServiceName);
        LOGGER.debug("Setting endpoint name on STSClient: {}", stsEndpointName);
        stsClient.setEndpointName(stsEndpointName);
        LOGGER.debug("Setting addressing namespace on STSClient: {}", ADDRESSING_NAMESPACE);
        stsClient.setAddressingNamespace(ADDRESSING_NAMESPACE);

        Map<String, Object> newStsProperties = new HashMap<>();

        // Properties loader should be able to find the properties file
        // no matter where it is
        if (signaturePropertiesPath != null && !signaturePropertiesPath.isEmpty()) {
            LOGGER.debug("Setting signature properties on STSClient: {}", signaturePropertiesPath);
            Properties signatureProperties = PropertiesLoader
                    .loadProperties(signaturePropertiesPath);
            newStsProperties.put(SecurityConstants.SIGNATURE_PROPERTIES, signatureProperties);
        }
        if (encryptionPropertiesPath != null && !encryptionPropertiesPath.isEmpty()) {
            LOGGER.debug("Setting encryption properties on STSClient: {}",
                    encryptionPropertiesPath);
            Properties encryptionProperties = PropertiesLoader
                    .loadProperties(encryptionPropertiesPath);
            newStsProperties.put(SecurityConstants.ENCRYPT_PROPERTIES, encryptionProperties);
        }
        if (stsPropertiesPath != null && !stsPropertiesPath.isEmpty()) {
            LOGGER.debug("Setting sts properties on STSClient: {}", stsPropertiesPath);
            Properties stsProperties = PropertiesLoader.loadProperties(stsPropertiesPath);
            newStsProperties.put(SecurityConstants.STS_TOKEN_PROPERTIES, stsProperties);
        }

        LOGGER.debug("Setting STS TOKEN USE CERT FOR KEY INFO to \"true\"");
        newStsProperties
                .put(SecurityConstants.STS_TOKEN_USE_CERT_FOR_KEYINFO, Boolean.TRUE.toString());
        stsClient.setProperties(newStsProperties);

        if (stsClient.getWsdlLocation()
                .startsWith(HttpsURLConnectionFactory.HTTPS_URL_PROTOCOL_ID)) {
            try {
                LOGGER.debug("Setting up SSL on the STSClient HTTP Conduit");
                HTTPConduit httpConduit = (HTTPConduit) stsClient.getClient().getConduit();
                if (httpConduit == null) {
                    LOGGER.info(
                            "HTTPConduit was null for stsClient. Unable to configure keystores for stsClient.");
                } else {
                    if (securitySettingsService != null) {
                        httpConduit
                                .setTlsClientParameters(securitySettingsService.getTLSParameters());
                    } else {
                        LOGGER.debug(
                                "Could not get reference to security settings, SSL communications will use system defaults.");
                    }

                }
            } catch (BusException e) {
                throw new SecurityServiceException("Unable to create sts client.", e);
            } catch (EndpointException e) {
                throw new SecurityServiceException("Unable to create sts client endpoint.", e);
            }
        }

        stsClient.setTokenType(stsClientConfig.getAssertionType());
        stsClient.setKeyType(stsClientConfig.getKeyType());
        stsClient.setKeySize(Integer.valueOf(stsClientConfig.getKeySize()));

        return stsClient;
    }

    public class SecureCxfClientFactory<T> {

        private Client cxfClient;

        private SecureCxfClientFactory(Client cxfClient) {
            this.cxfClient = cxfClient;
        }

        /**
         * Clients produced by this method will be secured with basic authentication
         * (if a username and password were provided), two-way ssl,
         * and the security subject from the current thread.
         * <p/>
         * The returned client should NOT be reused between threads!
         * This method should be called for each new request thread in order to ensure
         * that the security subject is up-to-date each time.
         *
         * @see org.codice.ddf.security.common.SecureCxfClientFactoryBuilder
         */
        public T getClient() throws SecurityServiceException {
            if (cxfClient == null) {
                throw new SecurityServiceException("Factory must be initialized first!");
            }

            WebClient newClient = WebClient.fromClient(cxfClient);

            Subject subject = SecurityUtils.getSubject();
            if (subject instanceof ddf.security.Subject) {
                RestSecurity.setSubjectOnClient((ddf.security.Subject) subject, newClient);
            }

            return (T) newClient;
        }
    }
}
