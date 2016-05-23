/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.interceptor;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPMessage;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.policy.MetadataConstants;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.wss4j.AbstractWSS4JInterceptor;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JOutInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.codice.ddf.platform.util.XMLUtils;
import org.codice.ddf.security.common.Security;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.encryption.EncryptionService;
import ddf.security.service.SecurityManager;
import ddf.security.sts.client.configuration.STSClientConfiguration;

/**
 * Interceptor for guest access to SOAP endpoints.
 */
public class GuestInterceptor extends AbstractWSS4JInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuestInterceptor.class);

    private EncryptionService encryptionService;

    private SecurityManager securityManager;

    private STSClientConfiguration stsClientConfiguration;

    private ContextPolicyManager contextPolicyManager;

    private boolean guestAccessDenied = false;

    private static final Cache<String, Subject> CACHED_GUEST_SUBJECT = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(1000)
            .build();

    private Security security;

    private final PolicyBasedWSS4JOutInterceptor.PolicyBasedWSS4JOutInterceptorInternal
            policyBasedWss4jOutInterceptor =
            new PolicyBasedWSS4JOutInterceptor().createEndingInterceptor();

    public GuestInterceptor(SecurityManager securityManager,
            ContextPolicyManager contextPolicyManager, Security security) {
        super();
        LOGGER.trace("Constructing GuestInterceptor");
        this.securityManager = securityManager;
        this.contextPolicyManager = contextPolicyManager;
        this.security = security;
        setPhase(Phase.PRE_PROTOCOL);
        //make sure this interceptor runs before the WSS4J one in the same Phase, otherwise it won't work
        Set<String> before = getBefore();
        before.add(WSS4JInInterceptor.class.getName());
        before.add(PolicyBasedWSS4JInInterceptor.class.getName());
        LOGGER.trace("Exiting GuestInterceptor constructor.");
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        if (guestAccessDenied) {
            LOGGER.debug("Guest Access not enabled - no message checking performed.");
            return;
        }

        if (message == null) {
            LOGGER.error("Incoming SOAP message is null - guest interceptor makes no sense.");
            return;
        }

        SOAPMessage soapMessage = getSOAPMessage(message);
        internalHandleMessage(message, soapMessage);
        if (LOGGER.isTraceEnabled()) {
            try {
                LOGGER.trace("SOAP request after guest interceptor: {}",
                        XMLUtils.prettyFormat(soapMessage.getSOAPHeader()
                                .getParentNode()));
            } catch (SOAPException e) {
                //ignore
            }
        }

    }

    private void internalHandleMessage(SoapMessage message, SOAPMessage soapMessage) throws Fault {

        //Check if security header exists; if not, execute GuestInterceptor logic
        String actor = (String) getOption(WSHandlerConstants.ACTOR);
        if (actor == null) {
            actor = (String) message.getContextualProperty(SecurityConstants.ACTOR);
        }

        Element existingSecurityHeader = null;
        try {
            LOGGER.debug("Checking for security header.");
            existingSecurityHeader = WSSecurityUtil.getSecurityHeader(soapMessage.getSOAPPart(),
                    actor);
        } catch (WSSecurityException e1) {
            LOGGER.debug("Issue with getting security header", e1);
        }

        if (existingSecurityHeader != null) {
            LOGGER.debug(
                    "SOAP message contains security header, no action taken by the GuestInterceptor.");
            return;
        }

        LOGGER.debug("Current request has no security header, continuing with GuestInterceptor");

        AssertionInfoMap assertionInfoMap = message.get(AssertionInfoMap.class);

        boolean hasAddressingAssertion = assertionInfoMap.entrySet()
                .stream()
                .flatMap(p -> p.getValue()
                        .stream())
                .filter(info -> MetadataConstants.ADDRESSING_ASSERTION_QNAME.equals(info.getAssertion()
                        .getName()))
                .findFirst()
                .isPresent();
        if (hasAddressingAssertion) {
            createAddressing(message, soapMessage);
        }

        LOGGER.debug("Creating guest security token.");
        HttpServletRequest request =
                (HttpServletRequest) message.get(AbstractHTTPDestination.HTTP_REQUEST);
        SecurityToken securityToken = createSecurityToken(request.getRemoteAddr());
        message.put(SecurityConstants.TOKEN, securityToken);
        if (!MessageUtils.isRequestor(message)) {
            try {
                message.put(Message.REQUESTOR_ROLE, true);
                policyBasedWss4jOutInterceptor.handleMessage(message);
            } finally {
                message.remove(Message.REQUESTOR_ROLE);
            }
        } else {
            policyBasedWss4jOutInterceptor.handleMessage(message);
        }

    }

    private SecurityToken createSecurityToken(String ipAddress) {
        SecurityToken securityToken = null;
        Subject subject = getSubject(ipAddress);

        LOGGER.trace("Attempting to create Security token.");
        if (subject != null) {
            PrincipalCollection principals = subject.getPrincipals();
            if (principals != null) {
                SecurityAssertion securityAssertion = principals.oneByType(SecurityAssertion.class);
                if (securityAssertion != null) {
                    securityToken = securityAssertion.getSecurityToken();
                } else {
                    LOGGER.warn(
                            "Subject did not contain a security assertion, could not add assertion to the security header.");
                }
            } else {
                LOGGER.warn(
                        "Subject did not contain any principals, could not create security token.");
            }
        }
        return securityToken;
    }

    private Subject getSubject(String ipAddress) {
        Subject subject = CACHED_GUEST_SUBJECT.getIfPresent(ipAddress);
        if (security.tokenAboutToExpire(subject)) {
            subject = security.getGuestSubject(ipAddress);
            CACHED_GUEST_SUBJECT.put(ipAddress, subject);
        } else {
            LOGGER.debug("Using cached Guest user token for {}", ipAddress);
        }
        return subject;
    }

    private void createAddressing(SoapMessage message, SOAPMessage soapMessage) {
        SOAPFactory soapFactory;
        try {
            soapFactory = SOAPFactory.newInstance();
        } catch (SOAPException e) {
            LOGGER.error("Could not create a SOAPFactory.", e);
            return;  // can't add anything if we can't create it
        }

        String addressingProperty =
                org.apache.cxf.ws.addressing.JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES_INBOUND;
        AddressingProperties addressingProperties = new AddressingProperties();

        try {
            SOAPElement action =
                    soapFactory.createElement(org.apache.cxf.ws.addressing.Names.WSA_ACTION_NAME,
                            org.apache.cxf.ws.addressing.JAXWSAConstants.WSA_PREFIX,
                            org.apache.cxf.ws.security.wss4j.DefaultCryptoCoverageChecker.WSA_NS);
            action.addTextNode((String) message.get(org.apache.cxf.message.Message.REQUEST_URL));
            AttributedURIType attributedString = new AttributedURIType();
            String actionValue = StringUtils.defaultIfEmpty((String) message.get(
                    SoapBindingConstants.SOAP_ACTION), "");
            attributedString.setValue(actionValue);
            addressingProperties.setAction(attributedString);
            soapMessage.getSOAPHeader()
                    .addChildElement(action);
        } catch (SOAPException e) {
            LOGGER.error("Unable to add addressing action.", e);
        }

        try {
            SOAPElement messageId =
                    soapFactory.createElement(org.apache.cxf.ws.addressing.Names.WSA_MESSAGEID_NAME,
                            org.apache.cxf.ws.addressing.JAXWSAConstants.WSA_PREFIX,
                            org.apache.cxf.ws.security.wss4j.DefaultCryptoCoverageChecker.WSA_NS);
            String uuid = "urn:uuid:" + UUID.randomUUID()
                    .toString();
            messageId.addTextNode(uuid);
            AttributedURIType attributedString = new AttributedURIType();
            attributedString.setValue(uuid);
            addressingProperties.setMessageID(attributedString);
            soapMessage.getSOAPHeader()
                    .addChildElement(messageId);
        } catch (SOAPException e) {
            LOGGER.error("Unable to add addressing messageId.", e);
        }

        try {
            SOAPElement to =
                    soapFactory.createElement(org.apache.cxf.ws.addressing.Names.WSA_TO_NAME,
                            org.apache.cxf.ws.addressing.JAXWSAConstants.WSA_PREFIX,
                            org.apache.cxf.ws.security.wss4j.DefaultCryptoCoverageChecker.WSA_NS);
            to.addTextNode((String) message.get(org.apache.cxf.message.Message.REQUEST_URL));
            EndpointReferenceType endpointReferenceType = new EndpointReferenceType();
            AttributedURIType attributedString = new AttributedURIType();
            attributedString.setValue((String) message.get(org.apache.cxf.message.Message.REQUEST_URL));
            endpointReferenceType.setAddress(attributedString);
            addressingProperties.setTo(endpointReferenceType);
            soapMessage.getSOAPHeader()
                    .addChildElement(to);
        } catch (SOAPException e) {
            LOGGER.error("Unable to add addressing to.", e);
        }

        try {
            SOAPElement replyTo =
                    soapFactory.createElement(org.apache.cxf.ws.addressing.Names.WSA_REPLYTO_NAME,
                            org.apache.cxf.ws.addressing.JAXWSAConstants.WSA_PREFIX,
                            org.apache.cxf.ws.security.wss4j.DefaultCryptoCoverageChecker.WSA_NS);
            SOAPElement address =
                    soapFactory.createElement(org.apache.cxf.ws.addressing.Names.WSA_ADDRESS_NAME,
                            org.apache.cxf.ws.addressing.JAXWSAConstants.WSA_PREFIX,
                            org.apache.cxf.ws.security.wss4j.DefaultCryptoCoverageChecker.WSA_NS);
            address.addTextNode(org.apache.cxf.ws.addressing.Names.WSA_ANONYMOUS_ADDRESS);
            replyTo.addChildElement(address);
            soapMessage.getSOAPHeader()
                    .addChildElement(replyTo);

        } catch (SOAPException e) {
            LOGGER.error("Unable to add addressing replyTo.", e);
        }
        message.put(addressingProperty, addressingProperties);
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

    public boolean isGuestAccessDenied() {
        return guestAccessDenied;
    }

    public void setGuestAccessDenied(boolean deny) {
        guestAccessDenied = deny;
    }

}
