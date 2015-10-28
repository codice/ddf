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
package org.codice.ddf.cxf;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.service.SecurityServiceException;

public class SecureCxfClientFactory<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecureCxfClientFactory.class);

    private final JAXRSClientFactoryBean clientFactory;

    private final boolean disableCnCheck;

    private final Class<T> interfaceClass;

    private static final Integer DEFAULT_CONNECTION_TIMEOUT = 30000;

    private static final Integer DEFAULT_RECEIVE_TIMEOUT = 60000;

    private Integer connectionTimeout;

    private Integer receiveTimeout;

    /**
     * @see #SecureCxfClientFactory(String, Class, java.util.List, Interceptor, boolean)
     */
    public SecureCxfClientFactory(String endpointUrl, Class<T> interfaceClass) {
        this(endpointUrl, interfaceClass, null, null, false);
    }

    /**
     * Constructs a factory that will return security-aware cxf clients. Once constructed,
     * use the getClient* methods to retrieve a fresh client  with the same configuration.
     * <p>
     * This factory can and should be cached. The clients it constructs should not be.
     *
     * @param endpointUrl    the remote url to connect to
     * @param interfaceClass an interface representing the resource at the remote url
     * @param providers      optional list of providers to further configure the client
     * @param interceptor    optional message interceptor for the client
     * @param disableCnCheck disable ssl check for common name / host name match
     */
    public SecureCxfClientFactory(String endpointUrl, Class<T> interfaceClass, List<?> providers,
            Interceptor<? extends Message> interceptor, boolean disableCnCheck) {
        if (StringUtils.isEmpty(endpointUrl) || interfaceClass == null) {
            throw new IllegalArgumentException(
                    "Called without a valid URL, will not be able to connect.");
        }

        this.interfaceClass = interfaceClass;
        this.disableCnCheck = disableCnCheck;

        JAXRSClientFactoryBean jaxrsClientFactoryBean = new JAXRSClientFactoryBean();
        jaxrsClientFactoryBean.setServiceClass(interfaceClass);
        jaxrsClientFactoryBean.setAddress(endpointUrl);
        jaxrsClientFactoryBean.setClassLoader(interfaceClass.getClassLoader());
        jaxrsClientFactoryBean.getInInterceptors().add(new LoggingInInterceptor());
        jaxrsClientFactoryBean.getOutInterceptors().add(new LoggingOutInterceptor());

        if (CollectionUtils.isNotEmpty(providers)) {
            jaxrsClientFactoryBean.setProviders(providers);
        }

        if (interceptor != null) {
            jaxrsClientFactoryBean.getInInterceptors().add(interceptor);
        }

        this.clientFactory = jaxrsClientFactoryBean;
    }

    /**
     * Constructs a factory that will return security-aware cxf clients. Once constructed,
     * use the getClient* methods to retrieve a fresh client  with the same configuration.
     * <p>
     * This factory can and should be cached. The clients it constructs should not be.
     *
     * @param endpointUrl       the remote url to connect to
     * @param interfaceClass    an interface representing the resource at the remote url
     * @param providers         optional list of providers to further configure the client
     * @param interceptor       optional message interceptor for the client
     * @param disableCnCheck    disable ssl check for common name / host name match
     * @param connectionTimeout timeout for the connection
     * @param receiveTimeout    timeout for receiving responses
     */
    public SecureCxfClientFactory(String endpointUrl, Class<T> interfaceClass, List<?> providers,
            Interceptor<? extends Message> interceptor, boolean disableCnCheck,
            Integer connectionTimeout, Integer receiveTimeout) {

        this(endpointUrl, interfaceClass, providers, interceptor, disableCnCheck);

        this.connectionTimeout = connectionTimeout;

        this.receiveTimeout = receiveTimeout;
    }

    /**
     * Constructs a factory that will return security-aware cxf clients. Once constructed,
     * use the getClient* methods to retrieve a fresh client  with the same configuration.
     * <p>
     * This factory can and should be cached. The clients it constructs should not be.
     * <p>
     * This constructor represents a quick fix only.
     *
     * @param endpointUrl       the remote url to connect to
     * @param interfaceClass    an interface representing the resource at the remote url
     * @param providers         optional list of providers to further configure the client
     * @param interceptor       optional message interceptor for the client
     * @param disableCnCheck    disable ssl check for common name / host name match
     * @param connectionTimeout timeout for the connection
     * @param receiveTimeout    timeout for receiving responses
     * @param username          a String representing the username
     * @param password          a String representing a password
     */
    public SecureCxfClientFactory(String endpointUrl, Class<T> interfaceClass, List<?> providers,
            Interceptor<? extends Message> interceptor, boolean disableCnCheck,
            Integer connectionTimeout, Integer receiveTimeout, String username, String password) {

        this(endpointUrl, interfaceClass, providers, interceptor, disableCnCheck, connectionTimeout,
                receiveTimeout);

        this.clientFactory.setPassword(password);
        this.clientFactory.setUsername(username);
    }

    /**
     * Clients produced by this method will be secured with two-way ssl
     * and the provided security subject.
     * <p>
     * The returned client should NOT be reused between requests!
     * This method should be called for each new request in order to ensure
     * that the security token is up-to-date each time.
     */
    public T getClientForSubject(Subject subject) throws SecurityServiceException {
        String asciiString = clientFactory.getAddress();

        T newClient = getNewClient();

        if (StringUtils.startsWithIgnoreCase(asciiString, "https")) {
            if (subject instanceof ddf.security.Subject) {
                RestSecurity.setSubjectOnClient((ddf.security.Subject) subject,
                        WebClient.client(newClient));
            } else {
                throw new SecurityServiceException("Not a ddf subject " + subject);
            }
        }

        return newClient;
    }

    /**
     * Convenience method to get a {@link WebClient} instead of a {@link org.apache.cxf.jaxrs.client.ClientProxyImpl ClientProxyImpl}.
     *
     * @see #getClientForSubject(Subject subject)
     */
    public WebClient getWebClientForSubject(Subject subject) throws SecurityServiceException {
        return getWebClient(getClientForSubject(subject));
    }

    /**
     * Clients produced by this method will be secured with two-way ssl
     * and basic authentication.
     * <p>
     * The returned client should NOT be reused between requests!
     * This method should be called for each new request in order to ensure
     * that the security token is up-to-date each time.
     */
    public T getClientForBasicAuth(String username, String password)
            throws SecurityServiceException {
        T client = getNewClient();
        RestSecurity.setUserOnClient(username, password, WebClient.client(client));
        return client;
    }

    /**
     * Convenience method to get a {@link WebClient} instead of a {@link org.apache.cxf.jaxrs.client.ClientProxyImpl ClientProxyImpl}.
     *
     * @see #getClientForBasicAuth(String, String)
     */
    public WebClient getWebClientForBasicAuth(String username, String password)
            throws SecurityServiceException {
        return getWebClient(getClientForBasicAuth(username, password));
    }

    private WebClient getWebClient(Object client) {
        return WebClient.fromClient(WebClient.client(client), true);
    }

    /**
     * Clients produced by this method will be completely unsecured.
     * <p>
     * Since there is no security information to expire, this client may be reused.
     */
    public T getUnsecuredClient() throws SecurityServiceException {
        return getNewClient();
    }

    /**
     * Convenience method to get a {@link WebClient} instead of a {@link org.apache.cxf.jaxrs.client.ClientProxyImpl ClientProxyImpl}.
     *
     * @see #getUnsecuredClient()
     */
    public WebClient getUnsecuredWebClient() throws SecurityServiceException {
        return WebClient.fromClientObject(getUnsecuredClient());
    }

    private T getNewClient() throws SecurityServiceException {
        T clientImpl = JAXRSClientFactory.fromClient(clientFactory.create(), interfaceClass);
        if (clientImpl == null) {
            throw new SecurityServiceException("Could not construct base client");
        }
        ClientConfiguration clientConfig = WebClient.getConfig(clientImpl);
        clientConfig.getRequestContext().put(Message.MAINTAIN_SESSION, Boolean.TRUE);

        String endpointUrl = clientFactory.getAddress();
        configureCnCheck(clientConfig);
        configureTimeouts(clientConfig, connectionTimeout, receiveTimeout);
        return clientImpl;
    }

    /*
     * Add TLS and Basic Auth credentials to the underlying {@link org.apache.cxf.transport.http.HTTPConduit}
     * This includes two-way ssl assuming that the platform keystores are configured correctly
     */
    private void configureCnCheck(ClientConfiguration clientConfig)
            throws SecurityServiceException {
        HTTPConduit httpConduit = clientConfig.getHttpConduit();
        if (httpConduit == null) {
            throw new SecurityServiceException(
                    "HTTPConduit was null for " + this + ". Unable to configure security.");
        }

        if (disableCnCheck) {
            TLSClientParameters tlsParams = httpConduit.getTlsClientParameters();
            if (tlsParams == null) {
                tlsParams = new TLSClientParameters();
            }
            tlsParams.setDisableCNCheck(true);
            httpConduit.setTlsClientParameters(tlsParams);
        }
    }

    /**
     * Configures the connection and receive timeouts. If any of the parameters are null, the timeouts
     * will be set to the system default.
     *
     * @param clientConfiguration Client configuration used for outgoing requests.
     * @param connectionTimeout   Connection timeout in milliseconds.
     * @param receiveTimeout      Receive timeout in milliseconds.
     */
    protected void configureTimeouts(ClientConfiguration clientConfiguration,
            Integer connectionTimeout, Integer receiveTimeout) {

        HTTPConduit httpConduit = clientConfiguration.getHttpConduit();
        if (httpConduit == null) {
            LOGGER.info("HTTPConduit was null for {}. Unable to configure timeouts", this);
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

}
