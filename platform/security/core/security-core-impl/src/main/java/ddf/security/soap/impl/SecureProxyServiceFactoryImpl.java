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
package ddf.security.soap.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.cxf.ws.security.wss4j.WSS4JUtils;
import org.apache.shiro.subject.PrincipalCollection;
import org.codice.ddf.platform.util.http.UnavailableUrls;
import org.codice.ddf.security.common.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.PropertiesLoader;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.sts.client.configuration.STSClientConfiguration;
import ddf.security.ws.proxy.ProxyServiceFactory;

/**
 * Factory that uses the JaxWsProxyFactoryBean to create a service proxy for the specified service.
 * This factory object handles both secure (using a STS and SAML assertions) and non-secure proxies and
 * provides the code to configure each appropriately.
 */
public class SecureProxyServiceFactoryImpl implements ProxyServiceFactory {

    protected static final String ADDRESSING_NAMESPACE = "http://www.w3.org/2005/08/addressing";

    private static final Logger LOGGER = LoggerFactory
            .getLogger(SecureProxyServiceFactoryImpl.class);

    private final UnavailableUrls unavailableWsdls = new UnavailableUrls();

    // STS Client
    private STSClientConfiguration stsClientConfig;

    public SecureProxyServiceFactoryImpl(STSClientConfiguration stsClientConfig) {
        this.stsClientConfig = stsClientConfig;
    }

    // For when you need to support a SOAP service that doesn't like following standards.
    protected String appendWsdlExtension(String url) {
        return url + "?wsdl";
    }

    /**
     * Creates a service proxy object that implements the specified Service Endpoing Interface.
     * This accepts a boolean indicating whether the proxy should be configured to communicate
     * securely using an STS and SAML assertions.
     *
     * @param requiresCredentials Indicates that security should be configured for this service proxy
     * @param serviceClass        The Java class object representing the interface to be proxied
     * @param serviceName         The name of the service being proxied
     * @param endpointName        The name corresponding to the endpoint
     * @param endpointAddress     The url for the service being proxied
     * @return the proxy that implements the specified SEI
     */
    @Override
    public <ProxyServiceType> ProxyServiceType create(boolean requiresCredentials,
            Class<ProxyServiceType> serviceClass, QName serviceName, QName endpointName,
            String endpointAddress, Serializable securityAssertion)
            throws UnsupportedOperationException {

        LOGGER.debug("Creating proxy service");

        WebServiceProperties<ProxyServiceType> wsp = new WebServiceProperties<>(serviceClass,
                serviceName, endpointName, endpointAddress);
        SecurityToken securityToken = getSecurityToken(wsp, securityAssertion);
        ProxyServiceType proxyServiceType = createSecureClientFactory(wsp, securityToken);

        LOGGER.debug("Finished creating proxy service");

        return proxyServiceType;
    }

    private SecurityToken getSecurityToken(WebServiceProperties wsp,
            Serializable securityAssertion) {

        SecurityToken securityToken = null;
        if (securityAssertion != null) {
            if (securityAssertion instanceof SecurityAssertion) {
                securityToken = ((SecurityAssertion) securityAssertion).getSecurityToken();
            } else if (securityAssertion instanceof Subject) {
                PrincipalCollection principals = ((Subject) securityAssertion).getPrincipals();
                if (principals != null) {
                    SecurityAssertion assertion = principals.oneByType(SecurityAssertion.class);
                    if (assertion != null) {
                        securityToken = assertion.getSecurityToken();
                    }
                }
            }
        }
        if (securityToken != null) {
            if (securityToken.getProperties() == null) {
                securityToken.setProperties(new Properties());
            }

            //setting security token to point back to itself as the token to use to access this
            //endpoint address
            //this is basically a hack to get around CXF trying to go to the STS for a new token
            securityToken.getProperties().put(wsp.endpointAddress, securityToken.getId());
        }
        return securityToken;
    }

    private <ProxyServiceType> ProxyServiceType createSecureClientFactory(
            WebServiceProperties<ProxyServiceType> wsp, SecurityToken token)
            throws UnsupportedOperationException {

        JaxWsProxyFactoryBean clientFactory = new JaxWsProxyFactoryBean();
        boolean populateFromClass = unavailableWsdls.contains(wsp.endpointWsdlURL);
        if (populateFromClass) {
            LOGGER.debug("Using service class to create client rather than WSDL.");
        }
        clientFactory.getClientFactoryBean().getServiceFactory()
                .setPopulateFromClass(populateFromClass);

        LOGGER.debug("Configuring client proxy properties");
        configureProxyFactoryProperties(clientFactory, token, wsp);
        clientFactory.getOutInterceptors().add(new TokenPassThroughInterceptor(token));

        ProxyServiceType proxyServiceType;
        try {
            proxyServiceType = clientFactory.create(wsp.serviceClass);
        } catch (ServiceConstructionException e) {
            LOGGER.debug("Unable to use WSDL to build client. Attempting to use service class.", e);
            unavailableWsdls.add(wsp.endpointWsdlURL);
            clientFactory.getClientFactoryBean().getServiceFactory().setPopulateFromClass(true);
            proxyServiceType = clientFactory.create(wsp.serviceClass);
        }

        return proxyServiceType;
    }

    /**
     * Returns a new STSClient object configured with the properties that have
     * been set.
     *
     * @param bus - CXF bus to initialize STSClient with
     * @return STSClient
     */
    protected STSClient configureSTSClient(Bus bus) {
        LOGGER.debug("Configuring STS client...");

        String stsAddress = stsClientConfig.getAddress();
        String stsServiceName = stsClientConfig.getServiceName();
        String stsEndpointName = stsClientConfig.getEndpointName();
        String signaturePropertiesPath = stsClientConfig.getSignatureProperties();
        String encryptionPropertiesPath = stsClientConfig.getEncryptionProperties();
        String stsPropertiesPath = stsClientConfig.getTokenProperties();

        STSClient stsClient = new STSClient(bus);
        if (stsAddress != null && !stsAddress.isEmpty()) {
            LOGGER.debug("Setting WSDL location on STSClient: {}", stsAddress);
            stsClient.setWsdlLocation(stsAddress);
            LOGGER.debug("Setting service name on STSClient: {}", stsServiceName);
            stsClient.setServiceName(stsServiceName);
            LOGGER.debug("Setting endpoint name on STSClient: {}", stsEndpointName);
            stsClient.setEndpointName(stsEndpointName);
            LOGGER.debug("Setting addressing namespace on STSClient: {}", ADDRESSING_NAMESPACE);
            stsClient.setAddressingNamespace(ADDRESSING_NAMESPACE);

            Map<String, Object> map = new HashMap<String, Object>();

            // Properties loader should be able to find the properties file no
            // matter where it is
            if (signaturePropertiesPath != null && !signaturePropertiesPath.isEmpty()) {
                LOGGER.debug("Setting signature properties on STSClient: {}",
                        signaturePropertiesPath);
                Properties signatureProperties = PropertiesLoader
                        .loadProperties(signaturePropertiesPath);
                map.put(SecurityConstants.SIGNATURE_PROPERTIES, signatureProperties);
            }
            if (encryptionPropertiesPath != null && !encryptionPropertiesPath.isEmpty()) {
                LOGGER.debug("Setting encryption properties on STSClient: {}",
                        encryptionPropertiesPath);
                Properties encryptionProperties = PropertiesLoader
                        .loadProperties(encryptionPropertiesPath);
                map.put(SecurityConstants.ENCRYPT_PROPERTIES, encryptionProperties);
            }
            if (stsPropertiesPath != null && !stsPropertiesPath.isEmpty()) {
                LOGGER.debug("Setting sts properties on STSClient: {}", stsPropertiesPath);
                Properties stsProperties = PropertiesLoader.loadProperties(stsPropertiesPath);
                map.put(SecurityConstants.STS_TOKEN_PROPERTIES, stsProperties);
            }

            LOGGER.debug("Setting STS TOKEN USE CERT FOR KEY INFO to \"true\"");
            map.put(SecurityConstants.STS_TOKEN_USE_CERT_FOR_KEYINFO, Boolean.TRUE.toString());
            map.put(SecurityConstants.DISABLE_STS_CLIENT_WSMEX_CALL_USING_EPR_ADDRESS,
                    Boolean.TRUE.toString());
            stsClient.setProperties(map);
        } else {
            LOGGER.debug("STS address is null, unable to create STS Client");
        }
        LOGGER.debug("Done configuring STS client");
        return stsClient;
    }

    /**
     * Configures the JaxWsProxyFactoryBean with the properties that have been set for the particular source.
     */
    protected void configureProxyFactoryProperties(JaxWsProxyFactoryBean clientFactory,
            SecurityToken actAsToken, WebServiceProperties wsp) {
        String signaturePropertiesPath = stsClientConfig.getSignatureProperties();
        String encryptionPropertiesPath = stsClientConfig.getEncryptionProperties();

        LOGGER.debug("Configuring proxy factory properties");
        if (wsp.endpointAddress != null) {
            LOGGER.debug("Configuring JaxWsProxyFactoryBean");
            Bus bus = clientFactory.getBus();
            if (bus == null) {
                LOGGER.debug("Getting CXF thread default bus.");
                bus = BusFactory.getThreadDefaultBus();
            }

            clientFactory.setWsdlURL(wsp.endpointWsdlURL);
            clientFactory.setAddress(wsp.endpointAddress);
            clientFactory.setServiceName(wsp.serviceName);
            clientFactory.setEndpointName(wsp.endpointName);
            clientFactory.setServiceClass(wsp.serviceClass);

            LOGGER.debug("Configuring STS Client");
            HashMap<String, Object> properties = new HashMap<>();

            if (actAsToken != null && actAsToken.getToken() != null) {
                LOGGER.debug("Setting incoming SAML assertion to outgoing federated client");
                properties.put(SecurityConstants.STS_TOKEN_ACT_AS, actAsToken.getToken());
            } else {
                STSClient stsClient = configureSTSClient(bus);
                LOGGER.debug("Setting STSClient");
                properties.put(SecurityConstants.STS_CLIENT, stsClient);
            }
            if (signaturePropertiesPath != null && !signaturePropertiesPath.isEmpty()) {
                LOGGER.debug("Setting signature properties: {}", signaturePropertiesPath);
                Properties signatureProperties = PropertiesLoader
                        .loadProperties(signaturePropertiesPath);
                properties.put(SecurityConstants.SIGNATURE_PROPERTIES, signatureProperties);
            }
            if (encryptionPropertiesPath != null && !encryptionPropertiesPath.isEmpty()) {
                LOGGER.debug("Setting encryption properties: {}", encryptionPropertiesPath);
                Properties encryptionProperties = PropertiesLoader
                        .loadProperties(encryptionPropertiesPath);
                properties.put(SecurityConstants.ENCRYPT_PROPERTIES, encryptionProperties);
            }
            properties.put(SecurityConstants.DISABLE_STS_CLIENT_WSMEX_CALL_USING_EPR_ADDRESS,
                    Boolean.TRUE.toString());
            clientFactory.setProperties(properties);
        }
        LOGGER.debug("Finished configuring proxy factory properties");
    }

    static final class TokenPassThroughInterceptor extends AbstractPhaseInterceptor<Message> {

        private final SecurityToken securityToken;

        public TokenPassThroughInterceptor(SecurityToken securityToken) {
            super(Phase.POST_LOGICAL);
            this.securityToken = securityToken;
        }

        @Override
        public void handleMessage(Message message) throws Fault {
            if (securityToken != null) {
                TokenStore tokenStore = WSS4JUtils.getTokenStore(message);
                tokenStore.add(securityToken);
            }
        }
    }

    /**
     * Object to contain web service properties and to pass them around to various methods. Since
     * they are no longer being assigned to a shared SecureProxyServiceFactoryImpl instance, we
     * eliminate any possibility of a race conditions.
     */
    private class WebServiceProperties<ProxyServiceType> {

        public final Class<ProxyServiceType> serviceClass;

        public final QName serviceName;

        public final QName endpointName;

        public final String endpointAddress;

        public final String endpointWsdlURL;

        public WebServiceProperties(Class<ProxyServiceType> serviceClass, QName serviceName,
                QName endpointName, String endpointAddress) {

            this.serviceClass = serviceClass;
            this.serviceName = serviceName;
            this.endpointName = endpointName;
            this.endpointAddress = HttpUtils.stripQueryString(endpointAddress);
            this.endpointWsdlURL = appendWsdlExtension(this.endpointAddress);
        }
    }
}

