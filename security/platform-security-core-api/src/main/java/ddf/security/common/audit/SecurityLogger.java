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
package ddf.security.common.audit;

import java.security.Principal;
import java.util.List;

import ddf.security.SecurityConstants;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreFactory;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.ws.security.SAMLTokenPrincipal;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import javax.servlet.http.HttpServletRequest;

/**
 * Class that contains utility methods for logging common security messages.
 * 
 */
public final class SecurityLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConstants.SECURITY_LOGGER);

    private SecurityLogger() {

    }

    private static String requestIpAndPortMessage(Message message) {
        if (message != null) {
            HttpServletRequest servletRequest = (HttpServletRequest) message
                    .get(AbstractHTTPDestination.HTTP_REQUEST);
            // pull out the ip and port of the incoming connection so we know
            // who is trying to get access
            if (servletRequest != null) {
                return " Request IP: " + servletRequest.getRemoteAddr() + ", Port: "
                        + servletRequest.getRemotePort();
            }
        }
        return "";
    }

    /**
     * Log all of the information associated with the security assertion for this message
     * 
     * @param message
     *            CXF Message containing the SAML assertion.
     */
    public static void logSecurityAssertionInfo(Message message) {
        if (message != null) {
            SecurityToken token = getToken(message);
            String requestLogInfo = requestIpAndPortMessage(message);

            // grab the SAML assertion associated with this Message from the
            // token store
            if (token != null) {
                LOGGER.info("SAML assertion successfully extracted from incoming Message.{}",
                        requestLogInfo);
                logSecurityAssertionInfo(token);
            } else {
                LOGGER.info("No SAML assertion exists on the incoming Message.{}", requestLogInfo);
            }
        }
    }

    public static void logSecurityAssertionInfo(SecurityToken token) {
        if (LOGGER.isDebugEnabled() && token != null) {
            LOGGER.debug(getFormattedXml(token.getToken()));
        }
    }

    /**
     * Transform into formatted XML.
     */
    public static String getFormattedXml(Node node) {
        Document document = node.getOwnerDocument().getImplementation()
                .createDocument("", "fake", null);
        Element copy = (Element) document.importNode(node, true);
        document.importNode(node, false);
        document.removeChild(document.getDocumentElement());
        document.appendChild(copy);
        DOMImplementation domImpl = document.getImplementation();
        DOMImplementationLS domImplLs = (DOMImplementationLS) domImpl.getFeature("LS", "3.0");
        LSSerializer serializer = domImplLs.createLSSerializer();
        serializer.getDomConfig().setParameter("format-pretty-print", true);
        return serializer.writeToString(document);
    }

    public static void logTrace(String log, Throwable throwable) {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        LOGGER.trace("{}{}", log, requestIpAndPortMessage(message), throwable);
    }

    public static void logTrace(String log) {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        LOGGER.trace("{}{}", log, requestIpAndPortMessage(message));
    }

    public static void logDebug(String log, Throwable throwable) {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        LOGGER.debug("{}{}", log, requestIpAndPortMessage(message), throwable);
    }

    public static void logDebug(String log) {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        LOGGER.debug("{}{}", log, requestIpAndPortMessage(message));
    }

    public static void logInfo(String log, Throwable throwable) {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        LOGGER.info("{}{}", log, requestIpAndPortMessage(message), throwable);
    }

    public static void logInfo(String log) {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        LOGGER.info("{}{}", log, requestIpAndPortMessage(message));
    }

    public static void logWarn(String log, Throwable throwable) {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        LOGGER.warn("{}{}", log, requestIpAndPortMessage(message), throwable);
    }

    public static void logWarn(String log) {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        LOGGER.warn("{}{}", log, requestIpAndPortMessage(message));
    }

    public static void logError(String log, Throwable throwable) {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        LOGGER.error("{}{}", log, requestIpAndPortMessage(message), throwable);
    }

    public static void logError(String log) {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        LOGGER.error("{}{}", log, requestIpAndPortMessage(message));
    }

    public static boolean isDebugEnabled() {
        return LOGGER.isDebugEnabled();
    }

    public static boolean isInfoEnabled() {
        return LOGGER.isInfoEnabled();
    }

    private static SecurityToken getToken(Message message) {
        SecurityToken token = null;
        if (message != null) {
            TokenStore tokenStore = getTokenStore(message);
            Principal principal = (Principal) message.get(WSS4JInInterceptor.PRINCIPAL_RESULT);
            if (!(principal instanceof SAMLTokenPrincipal)) {
                // Try to find the SAMLTokenPrincipal if it exists
                List<?> wsResults = List.class.cast(message.get(WSHandlerConstants.RECV_RESULTS));
                for (Object wsResult : wsResults) {
                    if (wsResult instanceof WSHandlerResult) {
                        List<WSSecurityEngineResult> wsseResults = ((WSHandlerResult) wsResult)
                                .getResults();
                        for (WSSecurityEngineResult wsseResult : wsseResults) {
                            Object principalResult = wsseResult
                                    .get(WSSecurityEngineResult.TAG_PRINCIPAL);
                            if (principalResult instanceof SAMLTokenPrincipal) {
                                principal = (SAMLTokenPrincipal) principalResult;
                                break;
                            }
                        }
                    }
                }
            }
            if (tokenStore != null && principal != null && principal instanceof SAMLTokenPrincipal) {
                token = tokenStore.getToken(((SAMLTokenPrincipal) principal).getId());
            }
        }
        return token;
    }

    private static TokenStore getTokenStore(Message message) {
        EndpointInfo info = message.getExchange().get(Endpoint.class).getEndpointInfo();
        synchronized (info) {
            TokenStore tokenStore = (TokenStore) message
                    .getContextualProperty(org.apache.cxf.ws.security.SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
            if (tokenStore == null) {
                tokenStore = (TokenStore) info
                        .getProperty(org.apache.cxf.ws.security.SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
            }
            if (tokenStore == null) {
                TokenStoreFactory tokenStoreFactory = TokenStoreFactory.newInstance();
                tokenStore = tokenStoreFactory.newTokenStore(
                        org.apache.cxf.ws.security.SecurityConstants.TOKEN_STORE_CACHE_INSTANCE,
                        message);
                info.setProperty(
                        org.apache.cxf.ws.security.SecurityConstants.TOKEN_STORE_CACHE_INSTANCE,
                        tokenStore);
            }
            return tokenStore;
        }
    }

}
