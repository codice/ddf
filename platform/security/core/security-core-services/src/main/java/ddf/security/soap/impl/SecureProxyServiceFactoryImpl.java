/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.security.soap.impl;

import ddf.security.Subject;
import ddf.security.assertion.Attribute;
import ddf.security.assertion.AttributeStatement;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.assertion.saml.impl.SecurityAssertionSaml;
import ddf.security.sts.client.configuration.STSClientConfiguration;
import ddf.security.ws.proxy.ProxyServiceFactory;
import ddf.security.ws.proxy.WsdlSuffixRetriever;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.IssuedToken;
import org.codice.ddf.platform.util.http.UnavailableUrls;
import org.codice.ddf.platform.util.properties.PropertiesLoader;
import org.codice.ddf.security.common.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * Factory that uses the JaxWsProxyFactoryBean to create a service proxy for the specified service.
 * This factory object handles both secure (using a STS and SAML assertions) and non-secure proxies
 * and provides the code to configure each appropriately.
 */
public class SecureProxyServiceFactoryImpl implements ProxyServiceFactory {

  protected static final String ADDRESSING_NAMESPACE = "http://www.w3.org/2005/08/addressing";

  private static final Logger LOGGER = LoggerFactory.getLogger(SecureProxyServiceFactoryImpl.class);

  private final UnavailableUrls unavailableWsdls = new UnavailableUrls();

  // STS Client
  private STSClientConfiguration stsClientConfig;

  private WsdlSuffixRetriever wsdlSuffixRetriever;

  public SecureProxyServiceFactoryImpl(STSClientConfiguration stsClientConfig) {
    this.stsClientConfig = stsClientConfig;
    this.wsdlSuffixRetriever = address -> "?wsdl";
  }

  public SecureProxyServiceFactoryImpl(
      STSClientConfiguration stsClientConfig, WsdlSuffixRetriever wsdlSuffixRetriever) {
    this.stsClientConfig = stsClientConfig;
    this.wsdlSuffixRetriever = wsdlSuffixRetriever;
  }

  /**
   * Creates a service proxy object that implements the specified Service Endpoing Interface. This
   * accepts a boolean indicating whether the proxy should be configured to communicate securely
   * using an STS and SAML assertions.
   *
   * @param requiresCredentials Indicates that security should be configured for this service proxy
   * @param serviceClass The Java class object representing the interface to be proxied
   * @param serviceName The name of the service being proxied
   * @param endpointName The name corresponding to the endpoint
   * @param endpointAddress The url for the service being proxied
   * @return the proxy that implements the specified SEI
   */
  @Override
  public <ProxyServiceType> ProxyServiceType create(
      boolean requiresCredentials,
      Class<ProxyServiceType> serviceClass,
      QName serviceName,
      QName endpointName,
      String endpointAddress,
      Serializable securityAssertion)
      throws UnsupportedOperationException {

    LOGGER.debug("Creating proxy service");

    WebServiceProperties<ProxyServiceType> wsp =
        new WebServiceProperties<>(serviceClass, serviceName, endpointName, endpointAddress);
    SecurityToken securityToken = getSecurityToken(wsp, securityAssertion);
    ProxyServiceType proxyServiceType = createSecureClientFactory(wsp, securityToken);

    LOGGER.debug("Finished creating proxy service");

    return proxyServiceType;
  }

  private SecurityToken getSecurityToken(WebServiceProperties wsp, Serializable securityAssertion) {

    SecurityToken securityToken = null;
    if (securityAssertion != null) {
      if (securityAssertion instanceof SecurityAssertion
          && ((SecurityAssertion) securityAssertion).getToken() instanceof SecurityToken) {
        securityToken = (SecurityToken) ((SecurityAssertion) securityAssertion).getToken();
      } else if (securityAssertion instanceof Subject) {
        PrincipalCollection principals = ((Subject) securityAssertion).getPrincipals();
        if (principals != null) {
          Collection<SecurityAssertion> assertions = principals.byType(SecurityAssertion.class);
          securityToken =
              (SecurityToken)
                  assertions
                      .stream()
                      .filter(assertion -> assertion.getToken() instanceof SecurityToken)
                      .map(SecurityAssertion::getToken)
                      .findFirst()
                      .orElse(null);
        }
      }
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
    clientFactory
        .getClientFactoryBean()
        .getServiceFactory()
        .setPopulateFromClass(populateFromClass);

    LOGGER.debug("Configuring client proxy properties");
    configureProxyFactoryProperties(clientFactory, token, wsp);
    clientFactory.getOutInterceptors().add(new TokenPassThroughInterceptor());

    return AccessController.doPrivileged(
        (PrivilegedAction<ProxyServiceType>)
            () -> {
              try {
                return clientFactory.create(wsp.serviceClass);
              } catch (ServiceConstructionException e) {
                LOGGER.debug(
                    "Unable to use WSDL to build client. Attempting to use service class.", e);
                unavailableWsdls.add(wsp.endpointWsdlURL);
                clientFactory.getClientFactoryBean().getServiceFactory().setPopulateFromClass(true);
                return clientFactory.create(wsp.serviceClass);
              }
            });
  }

  /**
   * Returns a new STSClient object configured with the properties that have been set.
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

    return AccessController.doPrivileged(
        (PrivilegedAction<STSClient>)
            () -> {
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

                Map<String, Object> map = new HashMap<>();

                // Properties loader should be able to find the properties file no
                // matter where it is
                PropertiesLoader propertiesLoader = PropertiesLoader.getInstance();
                if (signaturePropertiesPath != null && !signaturePropertiesPath.isEmpty()) {
                  LOGGER.debug(
                      "Setting signature properties on STSClient: {}", signaturePropertiesPath);
                  Properties signatureProperties =
                      propertiesLoader.loadProperties(signaturePropertiesPath);
                  map.put(SecurityConstants.SIGNATURE_PROPERTIES, signatureProperties);
                }
                if (encryptionPropertiesPath != null && !encryptionPropertiesPath.isEmpty()) {
                  LOGGER.debug(
                      "Setting encryption properties on STSClient: {}", encryptionPropertiesPath);
                  Properties encryptionProperties =
                      propertiesLoader.loadProperties(encryptionPropertiesPath);
                  map.put(SecurityConstants.ENCRYPT_PROPERTIES, encryptionProperties);
                }
                if (stsPropertiesPath != null && !stsPropertiesPath.isEmpty()) {
                  LOGGER.debug("Setting sts properties on STSClient: {}", stsPropertiesPath);
                  Properties stsProperties = propertiesLoader.loadProperties(stsPropertiesPath);
                  map.put(SecurityConstants.STS_TOKEN_PROPERTIES, stsProperties);
                }

                LOGGER.debug("Setting STS TOKEN USE CERT FOR KEY INFO to \"true\"");
                map.put(SecurityConstants.STS_TOKEN_USE_CERT_FOR_KEYINFO, Boolean.TRUE.toString());
                map.put(
                    SecurityConstants.DISABLE_STS_CLIENT_WSMEX_CALL_USING_EPR_ADDRESS,
                    Boolean.TRUE.toString());
                stsClient.setProperties(map);
              } else {
                LOGGER.debug("STS address is null, unable to create STS Client");
              }
              return stsClient;
            });
  }

  /**
   * Configures the JaxWsProxyFactoryBean with the properties that have been set for the particular
   * source.
   */
  protected void configureProxyFactoryProperties(
      JaxWsProxyFactoryBean clientFactory, SecurityToken token, WebServiceProperties wsp) {
    String signaturePropertiesPath = stsClientConfig.getSignatureProperties();
    String encryptionPropertiesPath = stsClientConfig.getEncryptionProperties();
    String stsPropertiesPath = stsClientConfig.getTokenProperties();

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

      STSClient stsClient = configureSTSClient(bus);
      LOGGER.debug("Setting STSClient");
      properties.put(SecurityConstants.STS_CLIENT, stsClient);

      if (token != null && token.getToken() != null) {
        LOGGER.debug("Setting incoming SAML assertion to outgoing federated client");
        properties.put(SecurityConstants.TOKEN, token);
      }
      PropertiesLoader propertiesLoader = PropertiesLoader.getInstance();
      if (signaturePropertiesPath != null && !signaturePropertiesPath.isEmpty()) {
        LOGGER.debug("Setting signature properties: {}", signaturePropertiesPath);
        Properties signatureProperties = propertiesLoader.loadProperties(signaturePropertiesPath);
        properties.put(SecurityConstants.SIGNATURE_PROPERTIES, signatureProperties);
      }
      if (stsPropertiesPath != null && !stsPropertiesPath.isEmpty()) {
        LOGGER.debug("Setting sts properties: {}", stsPropertiesPath);
        Properties stsProperties = propertiesLoader.loadProperties(stsPropertiesPath);
        properties.put(SecurityConstants.STS_TOKEN_PROPERTIES, stsProperties);
      }
      if (encryptionPropertiesPath != null && !encryptionPropertiesPath.isEmpty()) {
        LOGGER.debug("Setting encryption properties: {}", encryptionPropertiesPath);
        Properties encryptionProperties = propertiesLoader.loadProperties(encryptionPropertiesPath);
        properties.put(SecurityConstants.ENCRYPT_PROPERTIES, encryptionProperties);
      }
      properties.put(
          SecurityConstants.DISABLE_STS_CLIENT_WSMEX_CALL_USING_EPR_ADDRESS,
          Boolean.TRUE.toString());
      clientFactory.setProperties(properties);
    }
    LOGGER.debug("Finished configuring proxy factory properties");
  }

  static final class TokenPassThroughInterceptor extends AbstractPhaseInterceptor<Message> {

    public TokenPassThroughInterceptor() {
      super(Phase.POST_LOGICAL);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
      AssertionInfoMap aim = message.get(AssertionInfoMap.class);
      // extract Assertion information
      if (aim == null) {
        return;
      }

      Collection<AssertionInfo> ais = getAllAssertionsByLocalname(aim, SPConstants.ISSUED_TOKEN);
      if (ais == null) {
        return;
      }

      IssuedToken itok = (IssuedToken) ais.iterator().next().getAssertion();
      SecurityToken token = (SecurityToken) message.getContextualProperty(SecurityConstants.TOKEN);
      boolean shouldRequestNewToken = false;
      if (token == null || itok == null) {
        return;
      }

      SecurityAssertion securityAssertion = new SecurityAssertionSaml(token);
      Element requestSecurityTokenTemplate = itok.getRequestSecurityTokenTemplate();
      List<AttributeStatement> attributeStatements = securityAssertion.getAttributeStatements();

      XMLStreamReader xmlStreamReader =
          StaxUtils.createXMLStreamReader(requestSecurityTokenTemplate);
      try {
        while (xmlStreamReader.hasNext()) {
          int event = xmlStreamReader.next();
          switch (event) {
            case XMLStreamConstants.START_ELEMENT:
              {
                String localName = xmlStreamReader.getLocalName();
                String elementText;
                switch (localName) {
                  case "TokenType":
                    elementText = xmlStreamReader.getElementText();

                    shouldRequestNewToken =
                        shouldRequestNewTokenFromTokenType(securityAssertion, elementText);
                    break;
                  case "KeyType":
                    elementText = xmlStreamReader.getElementText();
                    shouldRequestNewToken =
                        shouldRequestNewTokenFromKeyType(securityAssertion, elementText);
                    break;
                  case "ClaimType":
                    int attributeCount = xmlStreamReader.getAttributeCount();
                    shouldRequestNewToken =
                        shouldRequestNewTokenFromClaimType(
                            attributeStatements, xmlStreamReader, attributeCount);
                    break;
                }
              }
          }
        }
      } catch (XMLStreamException e) {
        throw new Fault(e);
      }
      if (shouldRequestNewToken) {
        message.put(SecurityConstants.TOKEN, null);
        message.put(SecurityConstants.STS_TOKEN_ON_BEHALF_OF, token.getToken());
      }
    }

    private boolean shouldRequestNewTokenFromClaimType(
        List<AttributeStatement> attributeStatements,
        XMLStreamReader xmlStreamReader,
        int attributeCount) {
      boolean shouldRequestNewToken = false;
      boolean foundRequired = true;
      boolean isOptional = true;
      String uri = "";
      for (int i = 0; i < attributeCount; i++) {
        String attrLocalName = xmlStreamReader.getAttributeLocalName(i);
        String attributeValue = xmlStreamReader.getAttributeValue(i);
        if (attrLocalName.equalsIgnoreCase("Optional")) {
          isOptional = Boolean.parseBoolean(attributeValue);
        }
        if (attrLocalName.equalsIgnoreCase("Uri")) {
          uri = attributeValue;
        }
      }
      if (!isOptional) {
        // claim is not optional so make sure that the assertion we have
        // includes it
        foundRequired = false;
        for (AttributeStatement attributeStatement : attributeStatements) {
          for (Attribute attribute : attributeStatement.getAttributes()) {
            if (attribute.getName().equals(uri)) {
              // found the required attribute, so we don't need to do anything
              // else
              foundRequired = true;
            }
          }
        }
      }
      // there is a required attribute that the token doesn't contain so we need
      // to get a new one
      if (!foundRequired) {
        shouldRequestNewToken = true;
      }
      return shouldRequestNewToken;
    }

    private boolean shouldRequestNewTokenFromKeyType(
        SecurityAssertion securityAssertion, String elementText) {
      boolean shouldRequestNewToken = false;
      // bearer only lines up with bearer, so make sure they match
      if (StringUtils.containsIgnoreCase(elementText, "bearer")
          && securityAssertion
              .getSubjectConfirmations()
              .stream()
              .noneMatch(s -> StringUtils.containsIgnoreCase(s, "bearer"))) {
        shouldRequestNewToken = true;
      }
      // either of these key types can line up with either of the key
      // confirmation methods
      if (StringUtils.containsIgnoreCase(elementText, "publickey")
          || StringUtils.containsIgnoreCase(elementText, "symmetrickey")
              && securityAssertion
                  .getSubjectConfirmations()
                  .stream()
                  .noneMatch(
                      s ->
                          StringUtils.containsIgnoreCase(s, "holder-of-key")
                              || StringUtils.containsIgnoreCase(s, "sender-vouches"))) {
        shouldRequestNewToken = true;
      }
      return shouldRequestNewToken;
    }

    private boolean shouldRequestNewTokenFromTokenType(
        SecurityAssertion securityAssertion, String elementText) {
      boolean shouldRequestNewToken = false;
      // check that the token type is the same
      if (elementText == null || !securityAssertion.getTokenType().equals(elementText.trim())) {
        shouldRequestNewToken = true;
      }
      return shouldRequestNewToken;
    }

    static Collection<AssertionInfo> getAllAssertionsByLocalname(
        AssertionInfoMap aim, String localname) {
      Collection<AssertionInfo> sp11Ais = aim.get(new QName(SP11Constants.SP_NS, localname));
      Collection<AssertionInfo> sp12Ais = aim.get(new QName(SP12Constants.SP_NS, localname));

      if ((sp11Ais != null && !sp11Ais.isEmpty()) || (sp12Ais != null && !sp12Ais.isEmpty())) {
        Collection<AssertionInfo> ais = new HashSet<>();
        if (sp11Ais != null) {
          ais.addAll(sp11Ais);
        }
        if (sp12Ais != null) {
          ais.addAll(sp12Ais);
        }
        return ais;
      }

      return Collections.emptySet();
    }
  }

  /**
   * Object to contain web service properties and to pass them around to various methods. Since they
   * are no longer being assigned to a shared SecureProxyServiceFactoryImpl instance, we eliminate
   * any possibility of a race conditions.
   */
  private class WebServiceProperties<ProxyServiceType> {

    public final Class<ProxyServiceType> serviceClass;

    public final QName serviceName;

    public final QName endpointName;

    public final String endpointAddress;

    public final String endpointWsdlURL;

    public WebServiceProperties(
        Class<ProxyServiceType> serviceClass,
        QName serviceName,
        QName endpointName,
        String endpointAddress) {

      this.serviceClass = serviceClass;
      this.serviceName = serviceName;
      this.endpointName = endpointName;
      this.endpointAddress = HttpUtils.stripQueryString(endpointAddress);
      this.endpointWsdlURL = appendWsdlExtension(this.endpointAddress);
    }

    // For when you need to support a SOAP service that doesn't like following standards.
    private String appendWsdlExtension(String url) {
      return url + wsdlSuffixRetriever.retrieveWsdlSuffix(url);
    }
  }
}
