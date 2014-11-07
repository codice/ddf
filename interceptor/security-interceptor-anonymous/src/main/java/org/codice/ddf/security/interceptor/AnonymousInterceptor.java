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
package org.codice.ddf.security.interceptor;

import ddf.security.encryption.EncryptionService;
import ddf.security.sts.client.configuration.STSClientConfiguration;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.impl.AddressingPropertiesImpl;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.EffectivePolicy;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.wss4j.AbstractWSS4JInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.neethi.Policy;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.realm.Realm;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityEngine;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.util.WSSecurityUtil;
import org.apache.ws.security.validate.Validator;
import org.codice.ddf.security.handler.api.AnonymousAuthenticationToken;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Interceptor for anonymous access to SOAP endpoints.
 */
public class AnonymousInterceptor extends AbstractWSS4JInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnonymousInterceptor.class);
    private static final String TRANSPORT_BINDING = "TransportBinding";
    private static final String ASYMMETRIC_BINDING = "AsymmetricBinding";
    private static final String SYMMETRIC_BINDING = "SymmetricBinding";
    private static final String INCLUDE_TIMESTAMP = "IncludeTimestamp";
    private static final String LAYOUT = "Layout";
    private static final String TRANSPORT_TOKEN = "TransportToken";
    private static final String HTTPS_TOKEN = "HttpsToken";
    private static final String SIGNED_SUPPORTING_TOKENS = "SignedSupportingTokens";
    private static final String WSS11 = "wss11";
    private static final String ADDRESSING = "Addressing";
    private static final String TRUST13 = "Trust13";
    private static final String ISSUED_TOKEN = "IssuedToken";
    
    //Token Assertions
    private static final String TOKEN_ASSERTION_NEVER = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/Never";
    private static final String TOKEN_ASSERTION_ONCE = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/Once";
    private static final String TOKEN_ASSERTION_ALWAYSTORECIP = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/AlwaysToRecipient";
    private static final String TOKEN_ASSERTION_ALWAYS = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/Always";
    
    private static final String TOKEN_SAML20 = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";
    
    private final List<Realm> realms;

    private EncryptionService encryptionService;

    private STSClientConfiguration stsClientConfiguration;

    private ContextPolicyManager contextPolicyManager;

    private final Object lock = new Object();

    public AnonymousInterceptor(List<Realm> realms, ContextPolicyManager contextPolicyManager) {
        super();
        this.realms = realms;
        this.contextPolicyManager = contextPolicyManager;
        setPhase(Phase.PRE_PROTOCOL);
        //make sure this interceptor runs before the WSS4J one in the same Phase, otherwise it won't work
        Set<String> before = getBefore();
        before.add(WSS4JInInterceptor.class.getName());
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        if (message != null) {
            SoapVersion version = message.getVersion();
            SOAPMessage soapMessage = getSOAPMessage(message);
            SOAPFactory soapFactory = null;
            SOAPElement securityHeader = null;
            
            //Check if security header exists; if not, execute AnonymousInterceptor logic
            String actor = (String) getOption(WSHandlerConstants.ACTOR);
            if (actor == null) {
                actor = (String) message.getContextualProperty(SecurityConstants.ACTOR);
            } 
    
            Element existingSecurityHeader = null;
            try {
                existingSecurityHeader = WSSecurityUtil.getSecurityHeader(soapMessage.getSOAPPart(), actor);
            } catch (WSSecurityException e1) {
                LOGGER.debug("Issue with getting security header", e1);
            }
            if (existingSecurityHeader == null) {
                LOGGER.debug("Security Header returned null, execute AnonymousInterceptor");
          
                
                
                AssertionInfoMap assertionInfoMap = message.get(AssertionInfoMap.class);
                
                //if there is no policy then we don't need to do anything anyways
                if (assertionInfoMap != null) {
                    RequestData reqData = new CXFRequestData();
        
                    WSSConfig config = (WSSConfig) message.getContextualProperty(WSSConfig.class.getName());
                    WSSecurityEngine engine = null;
                    if (config != null) {
                        engine = new WSSecurityEngine();
                        engine.setWssConfig(config);
                    }
                    if (engine == null) {
                        engine = new WSSecurityEngine();
                        config = engine.getWssConfig();
                    }
        
                    reqData.setWssConfig(config);
                    
                    try {
                        soapFactory = SOAPFactory.newInstance();
                    } catch (SOAPException e) {
                        LOGGER.error("Could not create a SOAPFactory.", e);
                    }
                    if (soapFactory != null) {
                        //Create security header
                        try {
                            securityHeader = soapFactory.createElement("Security", "wsse",
                                    "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");
                            securityHeader.addAttribute(new QName("http://schemas.xmlsoap.org/soap/envelope/", "mustUnderstand"), "1");
                        } catch (SOAPException e) {
                            LOGGER.error("Unable to create security header for anonymous user.", e);
                        }
                    }
                }
                
                EffectivePolicy effectivePolicy = message.get(EffectivePolicy.class);
                Exchange exchange = message.getExchange();
                BindingOperationInfo bindingOperationInfo = exchange.getBindingOperationInfo();
                Endpoint endpoint = exchange.get(Endpoint.class);
                if (null == endpoint) {
                    return;
                }
                EndpointInfo endpointInfo = endpoint.getEndpointInfo();
                       
                Bus bus = exchange.get(Bus.class);
                PolicyEngine policyEngine = bus.getExtension(PolicyEngine.class);
                
                if (effectivePolicy == null) {
                        if (policyEngine != null) {
                                if (MessageUtils.isRequestor(message)) {
                                    effectivePolicy = policyEngine.getEffectiveClientResponsePolicy(endpointInfo, bindingOperationInfo);
                                } else {
                                    effectivePolicy = policyEngine.getEffectiveServerRequestPolicy(endpointInfo, bindingOperationInfo);
                                }
                            }
                }        
                
                //Auto analyze endpoint policies
                
                //Token Assertions
                String tokenAssertion = null;
                String tokenType = null;
                
                //Security Binding Assertions
                boolean layoutLax = false;
                boolean layoutStrict = false;
                boolean layoutLaxTimestampFirst = false;
                boolean layoutLaxTimestampLast = false;
                boolean requireClientCert = false;
                String secBindingAssertion = null;
                
                //Supporting Token Assertions
                String supportingTokenAssertion = null;
                
                Policy policy = effectivePolicy.getPolicy();
                    if (policy != null) {
                    AssertionInfoMap infoMap = new AssertionInfoMap(policy);
                    Set<QName> qnames = infoMap.keySet();
                    for (QName entryName : qnames) {
                        Collection<AssertionInfo> assetInfoList = infoMap.get(entryName);
                        for (AssertionInfo info : assetInfoList) {
                            LOGGER.debug("Assertion Name: {}", info.getAssertion().getName().getLocalPart());
                            String localName = info.getAssertion().getName().getLocalPart();
                            StringWriter out = new StringWriter();
                            XMLStreamWriter writer = null;
                            try {
                                writer = XMLOutputFactory.newInstance().createXMLStreamWriter(out);
                            } catch (XMLStreamException e) {
                                LOGGER.debug("Error with XMLStreamWriter", e);
                            } catch (FactoryConfigurationError e) {
                                LOGGER.debug("Error with FactoryConfiguration", e);
                            }
                            try {
                                info.getAssertion().serialize(writer);
                                writer.flush();
                            } catch (XMLStreamException e) {
                                LOGGER.debug("Error with XMLStream", e);
                            }
                            LOGGER.debug("Assertion XML: {}", out.toString());
                            String xml = out.toString();
                            if (localName.equals(TRANSPORT_BINDING)) {
                                secBindingAssertion = TRANSPORT_BINDING;
                            } else if (localName.equals(INCLUDE_TIMESTAMP)) {
                                createIncludeTimestamp(soapFactory, securityHeader);
                            } else if (localName.equals(LAYOUT)) {
                                String xpathLax = "/Layout/Policy/Lax";
                                String xpathStrict = "/Layout/Policy/Strict";
                                String xpathLaxTimestampFirst = "/Layout/Policy/LaxTimestampFirst";
                                String xpathLaxTimestampLast = "/Layout/Policy/LaxTimestampLast";
                                layoutLax = evaluateExpression(xml, xpathLax);
                                layoutStrict = evaluateExpression(xml, xpathStrict);
                                layoutLaxTimestampFirst = evaluateExpression(xml, xpathLaxTimestampFirst);
                                layoutLaxTimestampLast = evaluateExpression(xml, xpathLaxTimestampLast);
                                
                            } else if (localName.equals(TRANSPORT_TOKEN)) {
                                
                            } else if (localName.equals(HTTPS_TOKEN)) {
                                String xpath = "/HttpsToken/Policy/RequireClientCertificate";
                                requireClientCert = evaluateExpression(xml, xpath);
                                
                            } else if (localName.equals(SIGNED_SUPPORTING_TOKENS)) {
                                String xpath = "/SignedSupportingTokens/Policy/IssuedToken/RequestSecurityTokenTemplate/TokenType";
                                tokenType = retrieveXmlValue(xml, xpath);
                                supportingTokenAssertion = SIGNED_SUPPORTING_TOKENS;
                                
                            } else if (localName.equals(ADDRESSING)) {
                                createAddressing(message, soapMessage, soapFactory);
                                
                            } else if (localName.equals(TRUST13)) {
                            } else if (localName.equals(ISSUED_TOKEN)) {
                                //Check Token Assertion
                                String xpath = "/IssuedToken/@IncludeToken";
                                tokenAssertion = retrieveXmlValue(xml, xpath);
                                
                            }  else if (localName.equals(WSS11)) {
                                
                            }
                        }
                    }
                    
                    //Check security and token policies
                    if (tokenAssertion.trim().equals(TOKEN_ASSERTION_ALWAYSTORECIP) &&
                            tokenType.trim().equals(TOKEN_SAML20) &&
                            layoutLax &&
                            requireClientCert &&
                            secBindingAssertion.trim().equals(TRANSPORT_BINDING)
                            ) {
                        createSecurityToken(version, soapFactory, securityHeader);
                    } else {
                        LOGGER.warn("AnonymousInterceptor does not support the policies presented by the endpoint.");
                    }
                    
                    try {
                        //Add security header to SOAP message
                        soapMessage.getSOAPHeader().addChildElement(securityHeader);
                    } catch (SOAPException e) {
                        LOGGER.error("Issue when adding security header to SOAP message.");
                    }
                } else {
                    LOGGER.warn("Policy is null");
                }
            } else {
                LOGGER.warn("SOAP message contains security header, ignore AnonymousInterceptor.");
            }
        }  else {
            LOGGER.error("Incoming SOAP message is null.");
        }
    }
    
    private void createSecurityToken(SoapVersion version, SOAPFactory soapFactory, SOAPElement securityHeader) {
        AnonymousAuthenticationToken token = new AnonymousAuthenticationToken("DDF");

        //synchronize the step of requesting the assertion, it is not thread safe
        AuthenticationInfo authenticationInfo = null;
        synchronized (lock) {
            if (realms != null && !realms.isEmpty()) {
                for (Realm realm : realms) {
                    try {
                        authenticationInfo = realm.getAuthenticationInfo(token);
                        if (authenticationInfo != null) {
                            break;
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Unable to request auth info for anonymous user.", e);
                    }
                }
            }
        }
        if (authenticationInfo != null) {
            SecurityToken securityToken = null;
            for (Object principal : authenticationInfo.getPrincipals()) {
                if (principal instanceof SecurityToken) {
                    securityToken = (SecurityToken) principal;
                }
            }
            if (securityToken != null) {
                Element samlElement = securityToken.getToken();
                SOAPElement samlAssertion = null;
                try {
                    samlAssertion = soapFactory.createElement(samlElement);
                    securityHeader.addChildElement(samlAssertion);
                    
                } catch (SOAPException e) {
                    LOGGER.error("Unable to convert SecurityToken to SOAPElement.", e);
                }
            }
        }
    }
    
    private void createIncludeTimestamp(SOAPFactory soapFactory, SOAPElement securityHeader) {
        SOAPElement timestamp = null;
        try {
            timestamp = soapFactory.createElement("Timestamp", "wsu", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");
            SOAPElement created = soapFactory.createElement("Created", "wsu", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");
            DateTime dateTime = new DateTime();
            created.addTextNode(dateTime.toString());
            SOAPElement expires = soapFactory.createElement("Expires", "wsu", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");
            expires.addTextNode(dateTime.plusMinutes(5).toString());
            timestamp.addChildElement(created);
            timestamp.addChildElement(expires);
            securityHeader.addChildElement(timestamp);
        } catch (SOAPException e) {
            LOGGER.error("Unable to create security timestamp.", e);
        }
    }
    
    private void createAddressing(SoapMessage message, SOAPMessage soapMessage, SOAPFactory soapFactory) {
        
        String addressingProperty = "javax.xml.ws.addressing.context.inbound";
        AddressingPropertiesImpl addressingProperties = new AddressingPropertiesImpl();
        SOAPElement action = null;
        
        try {
            action = soapFactory.createElement("Action", "wsa", "http://www.w3.org/2005/08/addressing");
            action.addTextNode((String) message.get("org.apache.cxf.request.url"));
            AttributedURIType attributedString = new AttributedURIType();
            attributedString.setValue((String) message.get("org.apache.cxf.request.url"));
            addressingProperties.setAction(attributedString);
        } catch (SOAPException e) {
            LOGGER.error("Unable to add addressing action.", e);
        }

        SOAPElement messageId = null;
        try {
            messageId = soapFactory.createElement("MessageID", "wsa", "http://www.w3.org/2005/08/addressing");
            String uuid = "urn:uuid:" + UUID.randomUUID().toString();
            messageId.addTextNode(uuid);
            AttributedURIType attributedString = new AttributedURIType();
            attributedString.setValue(uuid);
            addressingProperties.setMessageID(attributedString);
        } catch (SOAPException e) {
            LOGGER.error("Unable to add addressing action.", e);
        }

        SOAPElement to = null;
        try {
            to = soapFactory.createElement("To", "wsa", "http://www.w3.org/2005/08/addressing");
            to.addTextNode((String) message.get("org.apache.cxf.request.url"));
            EndpointReferenceType endpointReferenceType = new EndpointReferenceType();
            AttributedURIType attributedString = new AttributedURIType();
            attributedString.setValue((String) message.get("org.apache.cxf.request.url"));
            endpointReferenceType.setAddress(attributedString);
            addressingProperties.setTo(endpointReferenceType);
        } catch (SOAPException e) {
            LOGGER.error("Unable to add addressing action.", e);
        }

        SOAPElement replyTo = null;
        try {
            replyTo = soapFactory.createElement("ReplyTo", "wsa", "http://www.w3.org/2005/08/addressing");
            SOAPElement address = soapFactory.createElement("Address", "wsa", "http://www.w3.org/2005/08/addressing");
            address.addTextNode("http://www.w3.org/2005/08/addressing/anonymous");
            replyTo.addChildElement(address);
            
            soapMessage.getSOAPHeader().addChildElement(messageId);
            soapMessage.getSOAPHeader().addChildElement(action);
            soapMessage.getSOAPHeader().addChildElement(to);
            soapMessage.getSOAPHeader().addChildElement(replyTo);
            message.put(addressingProperty, addressingProperties);
        } catch (SOAPException e) {
            LOGGER.error("Unable to add addressing action.", e);
        }
    }
    
    private SoapFault createSoapFault(SoapVersion version, WSSecurityException e) {
        SoapFault fault;
        javax.xml.namespace.QName faultCode = e.getFaultCode();
        if (version.getVersion() == 1.1 && faultCode != null) {
            fault = new SoapFault(e.getMessage(), e, faultCode);
        } else {
            fault = new SoapFault(e.getMessage(), e, version.getSender());
            if (version.getVersion() != 1.1 && faultCode != null) {
                fault.setSubCode(faultCode);
            }
        }
        return fault;
    }
    
    private String retrieveXmlValue(String xml, String xpathStmt) {
        String result = null;
        Document document = createDocument(xml);
        XPathFactory xFactory = XPathFactory.newInstance();
        XPath xpath = xFactory.newXPath();
        
        try {
        
            XPathExpression expr = xpath.compile(xpathStmt);
            result = (String)expr.evaluate(document, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            LOGGER.warn("Error processing XPath statement on policy XML.");
        }
        return result;
    }
    
    private boolean evaluateExpression(String xml, String xpathStmt) {
        Boolean result = null;
        Document document = createDocument(xml);
        
        XPathFactory xFactory = XPathFactory.newInstance();
        XPath xpath = xFactory.newXPath();
        
        try {
        
            XPathExpression expr = xpath.compile("boolean(" + xpathStmt + ")");
            result = (Boolean)expr.evaluate(document, XPathConstants.BOOLEAN);
        } catch (XPathExpressionException e) {
            LOGGER.warn("Error processing XPath statement on policy XML.", e);
        }
        return result;
    }
    
    private Document createDocument(String xml) {
        InputSource source = new InputSource(new StringReader(xml));
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        DocumentBuilder db = null;
        Document document = null;
        try {
            db = dbf.newDocumentBuilder();
            document = db.parse(source);
        } catch (ParserConfigurationException e1) {
            LOGGER.warn("Error creating new document builder.", e1);
        } catch (SAXException e1) {
            LOGGER.warn("Error parsing policy XML.", e1);
        } catch (IOException e1) {
            LOGGER.warn("Error creating Document for XPath parsing.", e1);
        }
        return document;
    }

    private SOAPMessage getSOAPMessage(SoapMessage msg) {
        SAAJInInterceptor.INSTANCE.handleMessage(msg);
        return msg.getContent(SOAPMessage.class);
    }

    public EncryptionService getEncryptionService() {
        return encryptionService;
    }

    public void setEncryptionService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    public STSClientConfiguration getStsClientConfiguration() {
        return stsClientConfiguration;
    }

    public void setStsClientConfiguration(STSClientConfiguration stsClientConfiguration) {
        this.stsClientConfiguration = stsClientConfiguration;
    }

    public ContextPolicyManager getContextPolicyManager() {
        return contextPolicyManager;
    }

    public void setContextPolicyManager(ContextPolicyManager contextPolicyManager) {
        this.contextPolicyManager = contextPolicyManager;
    }

    static class CXFRequestData extends RequestData {
        public CXFRequestData() {
        }

        public Validator getValidator(QName qName) throws WSSecurityException {
            String key = null;
            if (WSSecurityEngine.SAML_TOKEN.equals(qName)) {
                key = SecurityConstants.SAML1_TOKEN_VALIDATOR;
            } else if (WSSecurityEngine.SAML2_TOKEN.equals(qName)) {
                key = SecurityConstants.SAML2_TOKEN_VALIDATOR;
            } else if (WSSecurityEngine.USERNAME_TOKEN.equals(qName)) {
                key = SecurityConstants.USERNAME_TOKEN_VALIDATOR;
            } else if (WSSecurityEngine.SIGNATURE.equals(qName)) {
                key = SecurityConstants.SIGNATURE_TOKEN_VALIDATOR;
            } else if (WSSecurityEngine.TIMESTAMP.equals(qName)) {
                key = SecurityConstants.TIMESTAMP_TOKEN_VALIDATOR;
            } else if (WSSecurityEngine.BINARY_TOKEN.equals(qName)) {
                key = SecurityConstants.BST_TOKEN_VALIDATOR;
            } else if (WSSecurityEngine.SECURITY_CONTEXT_TOKEN_05_02.equals(qName) || WSSecurityEngine.SECURITY_CONTEXT_TOKEN_05_12.equals(qName)) {
                key = SecurityConstants.SCT_TOKEN_VALIDATOR;
            }
            if (key != null) {
                Object o = ((SoapMessage) this.getMsgContext()).getContextualProperty(key);
                try {
                    if (o instanceof Validator) {
                        return (Validator) o;
                    } else if (o instanceof Class) {
                        return (Validator) ((Class<?>) o).newInstance();
                    } else if (o instanceof String) {
                        return (Validator) ClassLoaderUtils.loadClass(o.toString(), WSS4JInInterceptor.class).newInstance();
                    }
                } catch (RuntimeException t) {
                    throw t;
                } catch (Throwable t) {
                    throw new WSSecurityException(t.getMessage(), t);
                }
            }
            return super.getValidator(qName);
        }
    }
}
