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
package ddf.security.pep.interceptor;

import java.io.StringWriter;

import javax.xml.namespace.QName;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.handler.MessageContext;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.Names;
import org.apache.shiro.util.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import ddf.security.service.impl.SecurityAssertionStore;

/**
 * Interceptor used to perform service authentication.
 *
 */
public class PEPAuthorizingInterceptor extends AbstractPhaseInterceptor<Message> {

    private Logger logger = LoggerFactory.getLogger(PEPAuthorizingInterceptor.class);

    private SecurityManager securityManager;

    public PEPAuthorizingInterceptor() {
        super(Phase.PRE_INVOKE);
        addAfter(org.apache.cxf.ws.policy.PolicyVerificationInInterceptor.class.getName());
    }

    /**
     * Sets the security manager that will be used to create a subject.
     *
     * @param securityManager
     */
    public void setSecurityManager(SecurityManager securityManager) {
        logger.trace("Setting the security manager");
        this.securityManager = securityManager;
    }

    /**
     * Intercepts a message. Interceptors should NOT invoke handleMessage or handleFault on the next
     * interceptor - the interceptor chain will take care of this.
     *
     * @param message
     */
    @Override
    public void handleMessage(Message message) throws Fault {
        if (message != null) {
            // grab the SAML assertion associated with this Message from the
            // token store
            SecurityAssertion assertion = SecurityAssertionStore.getSecurityAssertion(message);
            SecurityLogger.logSecurityAssertionInfo(message);
            boolean isPermitted = false;

            if ((assertion != null) && (assertion.getSecurityToken() != null)) {
                Subject user = null;
                CollectionPermission action = null;

                String actionURI = getActionUri(message);

                try {
                    user = securityManager.getSubject(assertion.getSecurityToken());
                    if (user == null) {
                        throw new AccessDeniedException("Unauthorized");
                    }
                    if (logger.isTraceEnabled()) {
                        logger.trace(format(assertion.getSecurityToken().getToken()));
                    }

                    logger.debug("Is user authenticated: {}", user.isAuthenticated());

                    logger.debug("Checking for permission");
                    SecurityLogger.logInfo(
                            "Is user [" + user.getPrincipal() + "] authenticated: " + user
                                    .isAuthenticated());

                    if (StringUtils.isEmpty(actionURI)) {
                        logger.info("Denying access : unable to determine action for {}",
                                user.getPrincipal());
                        SecurityLogger.logWarn("Denying access to [" + user.getPrincipal()
                                + "] for unknown action.");
                        throw new AccessDeniedException("Unauthorized");
                    }

                    action = new KeyValueCollectionPermission(actionURI);
                    logger.debug("Permission: {}", action);

                    isPermitted = user.isPermitted(action);

                    logger.debug("Result of permission: {}", isPermitted);
                    SecurityLogger.logInfo(
                            "Is user [" + user.getPrincipal() + "] permitted: " + isPermitted);
                    // store the subject so the DDF framework can use it later
                    ThreadContext.bind(user);
                    message.put(SecurityConstants.SAML_ASSERTION, user);
                    logger.debug("Added assertion information to message at key {}",
                            SecurityConstants.SAML_ASSERTION);
                } catch (SecurityServiceException e) {
                    logger.warn("Caught exception when trying to perform AuthZ.", e);
                    SecurityLogger.logWarn(
                            "Denying access : Caught exception when trying to authenticate user for service ["
                                    + actionURI + "]", e);
                    throw new AccessDeniedException("Unauthorized");
                }
                if (!isPermitted) {
                    if (action != null) {
                        logger.info("Denying access to {} for service {}", user.getPrincipal(),
                                action.getAction());
                        SecurityLogger.logWarn(
                                "Denying access to [" + user.getPrincipal() + "] for service "
                                        + action.getAction());
                    }
                    throw new AccessDeniedException("Unauthorized");
                }
            } else {
                logger.warn(
                        "Unable to retrieve the security assertion associated with the web service call.");
                throw new AccessDeniedException("Unauthorized");
            }
        } else {
            logger.warn(
                    "Unable to retrieve the current message associated with the web service call.");
            throw new AccessDeniedException("Unauthorized");
        }
    }

    /**
     * This method is an implementation of the WSA-M and WSA-W specs for determining the action URI.<br>
     * <ul>
     * <li>http://www.w3.org/TR/ws-addr-metadata/#actioninwsdl</li>
     * <li>http://www.w3.org/TR/ws-addr-wsdl/#actioninwsdl</li>
     * </ul>
     * Adapted from {@link org.apache.cxf.ws.addressing.impl.MAPAggregatorImpl} and
     * {@link org.apache.cxf.ws.addressing.impl.InternalContextUtils}
     *
     * @param message
     * @return
     */
    private String getActionUri(Message message) {
        String actionURI = null;

        /**
         * See if the action is explicitly defined in the WSDL message service model. Retrieves one
         * of the Action attribute in the wsdl:input message.
         */
        MessageInfo msgInfo = (MessageInfo) message.get(MessageInfo.class.getName());
        if (msgInfo != null && msgInfo.getExtensionAttributes() != null) {
            // wsaw:Action
            Object attr = msgInfo.getExtensionAttribute(JAXWSAConstants.WSAW_ACTION_QNAME);
            // wsam:Action
            if (attr == null) {
                attr = msgInfo.getExtensionAttribute(JAXWSAConstants.WSAM_ACTION_QNAME);
            }
            // support for older usages
            if (attr == null) {
                attr = msgInfo.getExtensionAttributes()
                        .get(new QName(JAXWSAConstants.NS_WSA, Names.WSAW_ACTION_NAME));
            }
            if (attr == null) {
                attr = msgInfo.getExtensionAttributes()
                        .get(new QName(Names.WSA_NAMESPACE_WSDL_NAME_OLD, Names.WSAW_ACTION_NAME));
            }
            if (attr instanceof QName) {
                actionURI = ((QName) attr).getLocalPart();
            } else {
                actionURI = attr == null ? null : attr.toString();
            }
        }

        /**
         * See if the action is explicitly defined in the WSDL operation service model. Retrieves
         * the operation soap:soapAction property.
         */
        if (StringUtils.isEmpty(actionURI)) {
            BindingOperationInfo bindingOpInfo = message.getExchange()
                    .get(BindingOperationInfo.class);
            SoapOperationInfo soi = bindingOpInfo.getExtensor(SoapOperationInfo.class);
            if (soi == null && bindingOpInfo.isUnwrapped()) {
                soi = bindingOpInfo.getWrappedOperation().getExtensor(SoapOperationInfo.class);
            }
            actionURI = soi == null ? null : soi.getAction();
            actionURI = StringUtils.isEmpty(actionURI) ? null : actionURI;
        }

        /**
         * If the service model doesn't explicitly defines the action, we'll construct the default
         * URI string.
         */
        if (StringUtils.isEmpty(actionURI)) {
            QName op = (QName) message.get(MessageContext.WSDL_OPERATION);
            QName port = (QName) message.get(MessageContext.WSDL_PORT);
            if (op != null && port != null) {
                actionURI = port.getNamespaceURI();
                actionURI = addPath(actionURI, port.getLocalPart());
                actionURI = addPath(actionURI, op.getLocalPart() + "Request");
            }
        }

        return actionURI;
    }

    private String addPath(String uri, String path) {
        StringBuilder builder = new StringBuilder(uri);
        String delimiter = uri.startsWith("urn") ? ":" : "/";
        if (!uri.endsWith(delimiter) && !path.startsWith(delimiter)) {
            builder.append(delimiter);
        }
        builder.append(path);
        return builder.toString();
    }

    private String format(Element unformattedXml) {
        if (unformattedXml == null) {
            logger.error("Unable to transform xml: null");
            return null;
        }

        StreamResult xmlOutput = new StreamResult(new StringWriter());
        TransformerFactory transformerFactory = TransformerFactory.newInstance();

        Transformer transformer = null;
        String formattedXml = null;

        try {
            transformer = transformerFactory.newTransformer();
            logger.trace("transformer class: {}", transformer.getClass());
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource(unformattedXml), xmlOutput);
            formattedXml = xmlOutput.getWriter().toString();
        } catch (TransformerConfigurationException e) {
            String message =
                    "Unable to transform xml:\n" + unformattedXml + "\nUsing unformatted xml.";
            logger.error(message, e);
        } catch (TransformerException e) {
            String message =
                    "Unable to transform xml:\n" + unformattedXml + "\nUsing unformatted xml.";
            logger.error(message, e);
        }

        return formattedXml;
    }
}
