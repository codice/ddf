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
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.impl.AddressingPropertiesImpl;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.wss4j.AbstractWSS4JInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.realm.Realm;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityEngine;
import org.apache.ws.security.WSSecurityEngineResult;
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
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPMessage;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Interceptor for anonymous access to SOAP endpoints.
 */
public class AnonymousInterceptor extends AbstractWSS4JInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnonymousInterceptor.class);

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
        SOAPMessage soapMessage = getSOAPMessage(message);

        AssertionInfoMap assertionInfoMap = message.get(AssertionInfoMap.class);

        //TODO: we should really be reading the policy and figuring out which things are actually needed and adding them dynamically
        //right now I'm just adding in stuff that I know is required
        //I'm going to leave this code in here commented out because it will be needed once we go to that model, and this will save someone the
        //trouble of figuring it out

//        EffectivePolicy effectivePolicy = message.get(EffectivePolicy.class);
//
//        Exchange exchange = message.getExchange();
//        BindingOperationInfo bindingOperationInfo = exchange.getBindingOperationInfo();
//        Endpoint endpoint = exchange.get(Endpoint.class);
//        if (null == endpoint) {
//            return;
//        }
//        EndpointInfo endpointInfo = endpoint.getEndpointInfo();
//
//        Bus bus = exchange.get(Bus.class);
//        PolicyEngine policyEngine = bus.getExtension(PolicyEngine.class);
//
//        if (effectivePolicy == null) {
//            if (policyEngine != null) {
//                if (MessageUtils.isRequestor(message)) {
//                    effectivePolicy = policyEngine.getEffectiveClientResponsePolicy(endpointInfo, bindingOperationInfo);
//                } else {
//                    effectivePolicy = policyEngine.getEffectiveServerRequestPolicy(endpointInfo, bindingOperationInfo);
//                }
//            }
//        }

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

            SoapVersion version = message.getVersion();

            String actor = (String) getOption(WSHandlerConstants.ACTOR);
            if (actor == null) {
                actor = (String) message.getContextualProperty(SecurityConstants.ACTOR);
            }

            try {
                Element elem = WSSecurityUtil.getSecurityHeader(soapMessage.getSOAPPart(), actor);

                List<WSSecurityEngineResult> wsResult = engine.processSecurityHeader(elem, reqData);

                //if there is a security header just ignore and go to the next interceptor
                if (wsResult == null || wsResult.isEmpty()) {
                    //no security header, need to inject assertion
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
                            String addressingProperty = "javax.xml.ws.addressing.context.inbound";
                            AddressingPropertiesImpl addressingProperties = new AddressingPropertiesImpl();
                            Element samlElement = securityToken.getToken();
                            SOAPFactory soapFactory = null;
                            try {
                                soapFactory = SOAPFactory.newInstance();
                            } catch (SOAPException e) {
                                LOGGER.error("Could not create a SOAPFactory.", e);
                            }
                            if (soapFactory != null) {
                                SOAPElement securityHeader = null;
                                try {
                                    securityHeader = soapFactory.createElement("Security", "wsse",
                                            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");
                                } catch (SOAPException e) {
                                    LOGGER.error("Unable to create security header for anonymous user.", e);
                                }

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
                                } catch (SOAPException e) {
                                    LOGGER.error("Unable to create security timestamp.", e);
                                }

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
                                } catch (SOAPException e) {
                                    LOGGER.error("Unable to add addressing action.", e);
                                }

                                SOAPElement samlAssertion = null;
                                try {
                                    samlAssertion = soapFactory.createElement(samlElement);
                                } catch (SOAPException e) {
                                    LOGGER.error("Unable to convert SecurityToken to SOAPElement.", e);
                                }

                                try {
                                    if (securityHeader != null) {
                                        securityHeader.addAttribute(new QName("http://schemas.xmlsoap.org/soap/envelope/", "mustUnderstand"), "1");
                                        securityHeader.addChildElement(timestamp);
                                        securityHeader.addChildElement(samlAssertion);
                                        soapMessage.getSOAPHeader().addChildElement(messageId);
                                        soapMessage.getSOAPHeader().addChildElement(action);
                                        soapMessage.getSOAPHeader().addChildElement(to);
                                        soapMessage.getSOAPHeader().addChildElement(replyTo);
                                        soapMessage.getSOAPHeader().addChildElement(securityHeader);
                                        message.put(addressingProperty, addressingProperties);
                                    }
                                } catch (SOAPException e) {
                                    LOGGER.error("Unable to add security header to SOAP message.", e);
                                }
                            }
                        }
                    }
                }
            } catch (WSSecurityException e) {
                LOGGER.warn("", e);
                throw createSoapFault(version, e);
            }
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
