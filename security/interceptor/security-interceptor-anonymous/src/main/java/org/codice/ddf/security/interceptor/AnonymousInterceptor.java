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

import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.encryption.EncryptionService;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import ddf.security.sts.client.configuration.STSClientConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapBindingConstants;
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
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.EffectivePolicy;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.wss4j.AbstractWSS4JInterceptor;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.neethi.Policy;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSSConfig;
import org.apache.wss4j.dom.WSSecurityEngine;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.apache.wss4j.dom.validate.Validator;
import org.apache.wss4j.policy.SP12Constants;
import org.codice.ddf.security.handler.api.AnonymousAuthenticationToken;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Interceptor for anonymous access to SOAP endpoints.
 */
public class AnonymousInterceptor extends AbstractWSS4JInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnonymousInterceptor.class);

    //Token Assertions
    private static final String TOKEN_SAML20 = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";

    private EncryptionService encryptionService;

    private SecurityManager securityManager;

    private STSClientConfiguration stsClientConfiguration;

    private ContextPolicyManager contextPolicyManager;

    private boolean anonymousAccessDenied = false;
    private boolean overrideEndpointPolicies = false;

    private final Object lock = new Object();

    public AnonymousInterceptor(SecurityManager securityManager, ContextPolicyManager contextPolicyManager) {
        super();
        this.securityManager = securityManager;
        this.contextPolicyManager = contextPolicyManager;
        setPhase(Phase.PRE_PROTOCOL);
        //make sure this interceptor runs before the WSS4J one in the same Phase, otherwise it won't work
        Set<String> before = getBefore();
        before.add(WSS4JInInterceptor.class.getName());
        before.add(PolicyBasedWSS4JInInterceptor.class.getName());
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault {

        if (anonymousAccessDenied) {
            LOGGER.debug("AnonymousAccess not enabled - no message checking performed.");
            return;
        }

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
                LOGGER.debug("Current request has no security header, continuing with AnonymousInterceptor");

                AssertionInfoMap assertionInfoMap = message.get(AssertionInfoMap.class);

                // if there is a policy we need to follow or we are ignoring policies, prepare the SOAP message
                if ((assertionInfoMap != null) || overrideEndpointPolicies) {
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
                        return;  // can't add anything if we can't create it
                    }
                    if (soapFactory != null) {
                        //Create security header
                        try {
                            securityHeader = soapFactory.createElement(WSConstants.WSSE_LN,
                                                                       WSConstants.WSSE_PREFIX,
                                                                       WSConstants.WSSE_NS);
                            securityHeader.addAttribute(new QName(WSConstants.URI_SOAP11_ENV,
                                                                  WSConstants.ATTR_MUST_UNDERSTAND), "1");
                        } catch (SOAPException e) {
                            LOGGER.error("Unable to create security header for anonymous user.", e);
                            return;  // can't create the security - just return
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
                            effectivePolicy = policyEngine.getEffectiveClientResponsePolicy(endpointInfo, bindingOperationInfo, message);
                        } else {
                            effectivePolicy = policyEngine.getEffectiveServerRequestPolicy(endpointInfo, bindingOperationInfo, message);
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
                QName secBindingAssertion = null;

                //Supporting Token Assertions
                QName supportingTokenAssertion = null;
                boolean policyRequirementsSupported = false;

                // if there is a policy, try to follow it as closely as possible
                if (effectivePolicy != null) {
                    Policy policy = effectivePolicy.getPolicy();
                    if (policy != null) {
                        AssertionInfoMap infoMap = new AssertionInfoMap(policy);
                        Set<Map.Entry<QName, Collection<AssertionInfo>>> entries = infoMap.entrySet();
                        for (Map.Entry<QName, Collection<AssertionInfo>> entry : entries) {
                            Collection<AssertionInfo> assetInfoList = entry.getValue();
                            for (AssertionInfo info : assetInfoList) {
                                LOGGER.debug("Assertion Name: {}", info.getAssertion().getName().getLocalPart());
                                QName qName = info.getAssertion().getName();
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
                                    if (writer != null) {
                                        info.getAssertion().serialize(writer);
                                        writer.flush();
                                    }
                                } catch (XMLStreamException e) {
                                    LOGGER.debug("Error with XMLStream", e);
                                } finally {
                                    if (writer != null) {
                                        try {
                                            writer.close();
                                        } catch (XMLStreamException ignore) {
                                            //ignore
                                        }
                                    }
                                }
                                LOGGER.debug("Assertion XML: {}", out.toString());
                                String xml = out.toString();

                                if (qName.equals(SP12Constants.TRANSPORT_BINDING)) {
                                    secBindingAssertion = qName;
                                } else if (qName.equals(SP12Constants.INCLUDE_TIMESTAMP)) {
                                    createIncludeTimestamp(soapFactory, securityHeader);
                                } else if (qName.equals(SP12Constants.LAYOUT)) {
                                    String xpathLax = "/Layout/Policy/Lax";
                                    String xpathStrict = "/Layout/Policy/Strict";
                                    String xpathLaxTimestampFirst = "/Layout/Policy/LaxTimestampFirst";
                                    String xpathLaxTimestampLast = "/Layout/Policy/LaxTimestampLast";
                                    layoutLax = evaluateExpression(xml, xpathLax);
                                    layoutStrict = evaluateExpression(xml, xpathStrict);
                                    layoutLaxTimestampFirst = evaluateExpression(xml, xpathLaxTimestampFirst);
                                    layoutLaxTimestampLast = evaluateExpression(xml, xpathLaxTimestampLast);

                                } else if (qName.equals(SP12Constants.TRANSPORT_TOKEN)) {
                                } else if (qName.equals(SP12Constants.HTTPS_TOKEN)) {
                                    String xpath = "/HttpsToken/Policy/RequireClientCertificate";
                                    requireClientCert = evaluateExpression(xml, xpath);

                                } else if (qName.equals(SP12Constants.SIGNED_SUPPORTING_TOKENS)) {
                                    String xpath = "/SignedSupportingTokens/Policy//IssuedToken/RequestSecurityTokenTemplate/TokenType";
                                    tokenType = retrieveXmlValue(xml, xpath);
                                    supportingTokenAssertion = qName;

                                } else if (qName.equals(SP12Constants.SUPPORTING_TOKENS)) {
                                    String xpath = "/SupportingTokens/Policy//IssuedToken/RequestSecurityTokenTemplate/TokenType";
                                    tokenType = retrieveXmlValue(xml, xpath);
                                    supportingTokenAssertion = qName;

                                } else if (qName.equals(org.apache.cxf.ws.addressing.policy.MetadataConstants.ADDRESSING_ASSERTION_QNAME)) {
                                    createAddressing(message, soapMessage, soapFactory);

                                } else if (qName.equals(SP12Constants.TRUST_13)) {

                                } else if (qName.equals(SP12Constants.ISSUED_TOKEN)) {
                                    //Check Token Assertion
                                    String xpath = "/IssuedToken/@IncludeToken";
                                    tokenAssertion = retrieveXmlValue(xml, xpath);

                                } else if (qName.equals(SP12Constants.WSS11)) {

                                }
                            }
                        }

                        //Check security and token policies
                        if (tokenAssertion != null && tokenType != null &&
                            tokenAssertion.trim().equals(SP12Constants.INCLUDE_ALWAYS_TO_RECIPIENT) &&
                            tokenType.trim().equals(TOKEN_SAML20)) {
                            policyRequirementsSupported = true;
                        } else {
                            LOGGER.warn("AnonymousInterceptor does not support the policies presented by the endpoint.");
                        }

                    } else {
                        if (overrideEndpointPolicies) {
                            LOGGER.debug("WS Policy is null, override is true - an anonymous assertion will be generated");
                        } else {
                            LOGGER.warn("WS Policy is null, override flag is false - no anonymous assertion will be generated.");
                        }
                    }
                } else {
                    if (overrideEndpointPolicies) {
                        LOGGER.debug("Effective WS Policy is null, override is true - an anonymous assertion will be generated");
                    } else {
                        LOGGER.warn("Effective WS Policy is null, override flag is false - no anonymous assertion will be generated.");
                    }
                }

                if (policyRequirementsSupported || overrideEndpointPolicies) {
                    LOGGER.debug("Creating anonymous security token.");
                    if (soapFactory != null) {
                        createSecurityToken(version, soapFactory, securityHeader);
                        try {
                            // Add security header to SOAP message
                            soapMessage.getSOAPHeader().addChildElement(securityHeader);
                        } catch (SOAPException e) {
                            LOGGER.error("Issue when adding security header to SOAP message:" + e.getMessage());
                        }
                    } else {
                        LOGGER.debug("Security Header was null so not creating a SAML Assertion");
                    }
                }
            } else {
                LOGGER.debug("SOAP message contains security header, no action taken by the AnonymousInterceptor.");
            }
            if (LOGGER.isTraceEnabled()) {
                try {
                    LOGGER.trace("SOAP request after anonymous interceptor: {}", SecurityLogger.getFormattedXml(soapMessage.getSOAPHeader().getParentNode()));
                } catch (SOAPException e) {
                    //ignore
                }
            }
        } else {
            LOGGER.error("Incoming SOAP message is null - anonymous interceptor makes no sense.");
        }
    }

    private void createSecurityToken(SoapVersion version, SOAPFactory soapFactory, SOAPElement securityHeader) {
        AnonymousAuthenticationToken token = new AnonymousAuthenticationToken(BaseAuthenticationToken.DEFAULT_REALM);

        //synchronize the step of requesting the assertion, it is not thread safe
        Subject subject = null;
        synchronized (lock) {
            try {
                subject = securityManager.getSubject(token);
            } catch (SecurityServiceException sse) {
                LOGGER.warn("Unable to request subject for anonymous user.", sse);
            }

        }
        if (subject != null) {
            PrincipalCollection principals = subject.getPrincipals();
            if (principals != null) {
                SecurityAssertion securityAssertion = principals.oneByType(SecurityAssertion.class);
                if (securityAssertion != null) {
                    SecurityToken securityToken = securityAssertion.getSecurityToken();
                    Element samlElement = securityToken.getToken();
                    SOAPElement samlAssertion = null;
                    try {
                        samlAssertion = soapFactory.createElement(samlElement);
                        securityHeader.addChildElement(samlAssertion);

                    } catch (SOAPException e) {
                        LOGGER.error("Unable to convert SecurityToken to SOAPElement.", e);
                    }
                } else {
                    LOGGER.warn("Subject did not contain a security assertion, could not assertion"
                            + "to security header.");
                }
            } else {
                LOGGER.warn("Subject did not contain any principals, could not create element.");
            }
        }
    }

    private void createIncludeTimestamp(SOAPFactory soapFactory, SOAPElement securityHeader) {
        SOAPElement timestamp = null;
        try {
            timestamp = soapFactory
                    .createElement(WSConstants.TIMESTAMP_TOKEN_LN, WSConstants.WSU_PREFIX,
                            WSConstants.WSU_NS);
            SOAPElement created = soapFactory
                    .createElement(WSConstants.CREATED_LN, WSConstants.WSU_PREFIX,
                            WSConstants.WSU_NS);
            DateTime dateTime = new DateTime();
            created.addTextNode(dateTime.toString());
            SOAPElement expires = soapFactory
                    .createElement(WSConstants.EXPIRES_LN, WSConstants.WSU_PREFIX,
                            WSConstants.WSU_NS);
            expires.addTextNode(dateTime.plusMinutes(5).toString());
            timestamp.addChildElement(created);
            timestamp.addChildElement(expires);
            securityHeader.addChildElement(timestamp);
        } catch (SOAPException e) {
            LOGGER.error("Unable to create security timestamp.", e);
        }
    }

    private void createAddressing(SoapMessage message, SOAPMessage soapMessage, SOAPFactory soapFactory) {

        String addressingProperty = org.apache.cxf.ws.addressing.JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES_INBOUND;
        AddressingProperties addressingProperties = new AddressingProperties();
        SOAPElement action = null;

        try {
            action = soapFactory
                    .createElement(org.apache.cxf.ws.addressing.Names.WSA_ACTION_NAME, org.apache.cxf.ws.addressing.JAXWSAConstants.WSA_PREFIX,
                            org.apache.cxf.ws.security.wss4j.DefaultCryptoCoverageChecker.WSA_NS);
            action.addTextNode((String) message.get(org.apache.cxf.message.Message.REQUEST_URL));
            AttributedURIType attributedString = new AttributedURIType();
            String actionValue = StringUtils.defaultIfEmpty((String) message.get(SoapBindingConstants.SOAP_ACTION), "");
            attributedString.setValue(actionValue);
            addressingProperties.setAction(attributedString);
        } catch (SOAPException e) {
            LOGGER.error("Unable to add addressing action.", e);
        }

        SOAPElement messageId = null;
        try {
            messageId = soapFactory
                    .createElement(org.apache.cxf.ws.addressing.Names.WSA_MESSAGEID_NAME, org.apache.cxf.ws.addressing.JAXWSAConstants.WSA_PREFIX,
                            org.apache.cxf.ws.security.wss4j.DefaultCryptoCoverageChecker.WSA_NS);
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
            to = soapFactory.createElement(org.apache.cxf.ws.addressing.Names.WSA_TO_NAME, org.apache.cxf.ws.addressing.JAXWSAConstants.WSA_PREFIX,
                    org.apache.cxf.ws.security.wss4j.DefaultCryptoCoverageChecker.WSA_NS);
            to.addTextNode((String) message.get(org.apache.cxf.message.Message.REQUEST_URL));
            EndpointReferenceType endpointReferenceType = new EndpointReferenceType();
            AttributedURIType attributedString = new AttributedURIType();
            attributedString.setValue((String) message.get(org.apache.cxf.message.Message.REQUEST_URL));
            endpointReferenceType.setAddress(attributedString);
            addressingProperties.setTo(endpointReferenceType);
        } catch (SOAPException e) {
            LOGGER.error("Unable to add addressing action.", e);
        }

        SOAPElement replyTo = null;
        try {
            replyTo = soapFactory
                    .createElement(org.apache.cxf.ws.addressing.Names.WSA_REPLYTO_NAME, org.apache.cxf.ws.addressing.JAXWSAConstants.WSA_PREFIX,
                            org.apache.cxf.ws.security.wss4j.DefaultCryptoCoverageChecker.WSA_NS);
            SOAPElement address = soapFactory
                    .createElement(org.apache.cxf.ws.addressing.Names.WSA_ADDRESS_NAME, org.apache.cxf.ws.addressing.JAXWSAConstants.WSA_PREFIX,
                            org.apache.cxf.ws.security.wss4j.DefaultCryptoCoverageChecker.WSA_NS);
            address.addTextNode(org.apache.cxf.ws.addressing.Names.WSA_ANONYMOUS_ADDRESS);
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
            result = (String) expr.evaluate(document, XPathConstants.STRING);
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
            result = (Boolean) expr.evaluate(document, XPathConstants.BOOLEAN);
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

    public void setAnonymousAccessDenied(boolean deny) {
        anonymousAccessDenied = deny;
    }

    public boolean isAnonymousAccessDenied() {
        return anonymousAccessDenied;
    }

    public void setOverrideEndpointPolicies(boolean override) {
        overrideEndpointPolicies = override;
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
                    throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, t.getMessage(), t);
                }
            }
            return super.getValidator(qName);
        }
    }
}
