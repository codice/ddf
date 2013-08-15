/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.security.common.audit;


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
import org.apache.log4j.Logger;
import org.apache.ws.security.SAMLTokenPrincipal;
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
public final class SecurityLogger
{
    private static final Logger SECURITY_LOGGER = Logger.getLogger(SecurityConstants.SECURITY_LOGGER);

    private SecurityLogger()
    {

    }

    private static String requestIpAndPortMessage(Message message)
    {
        if(message != null)
        {
            HttpServletRequest servletRequest = (HttpServletRequest) message.get(AbstractHTTPDestination.HTTP_REQUEST);
            // pull out the ip and port of the incoming connection so we know
            // who is trying to get access
            if (servletRequest != null)
            {
                return " Request IP: " + servletRequest.getRemoteAddr() + ", Port: "
                        + servletRequest.getRemotePort();
            }
        }
        return "";
    }

    /**
     * Log all of the information associated with the security assertion for
     * this message
     * 
     * @param message CXF Message containing the SAML assertion.
     */
    public static void logSecurityAssertionInfo( Message message )
    {
        if (message != null)
        {
            SecurityToken token = getToken(message);
            String requestLogInfo = requestIpAndPortMessage(message);

            // grab the SAML assertion associated with this Message from the
            // token store
            if (token != null)
            {
                String logMessage = "SAML assertion successfully extracted from incoming Message.";
                logMessage += requestLogInfo;
                SECURITY_LOGGER.info(logMessage);
                logSecurityAssertionInfo(token);
            }
            else
            {
                String logMessage = "No SAML assertion exists on the incoming Message.";
                logMessage += requestLogInfo;
                SECURITY_LOGGER.info(logMessage);
            }
        }
    }
    
    public static void logSecurityAssertionInfo( SecurityToken token )
    {
        if(SECURITY_LOGGER.isDebugEnabled() && token != null)
        {
            SECURITY_LOGGER.debug(getFormattedXml(token.getToken()));
        }
    }

    /**
     * Transform into formatted XML.
     */
    public static String getFormattedXml( Node node )
    {
        Document document = node.getOwnerDocument().getImplementation().createDocument("", "fake", null);
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

    public static void logTrace(String log, Throwable throwable)
    {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        SECURITY_LOGGER.trace(log + requestIpAndPortMessage(message), throwable);
    }

    public static void logTrace(String log)
    {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        SECURITY_LOGGER.trace(log+requestIpAndPortMessage(message));
    }

    public static void logDebug(String log, Throwable throwable)
    {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        SECURITY_LOGGER.debug(log + requestIpAndPortMessage(message), throwable);
    }

    public static void logDebug(String log)
    {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        SECURITY_LOGGER.debug(log+requestIpAndPortMessage(message));
    }

    public static void logInfo(String log, Throwable throwable)
    {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        SECURITY_LOGGER.info(log+requestIpAndPortMessage(message), throwable);
    }

    public static void logInfo(String log)
    {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        SECURITY_LOGGER.info(log+requestIpAndPortMessage(message));
    }

    public static void logWarn(String log, Throwable throwable)
    {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        SECURITY_LOGGER.warn(log + requestIpAndPortMessage(message), throwable);
    }

    public static void logWarn(String log)
    {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        SECURITY_LOGGER.warn(log+requestIpAndPortMessage(message));
    }

    public static void logError(String log, Throwable throwable)
    {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        SECURITY_LOGGER.error(log + requestIpAndPortMessage(message), throwable);
    }

    public static void logError(String log)
    {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        SECURITY_LOGGER.error(log+requestIpAndPortMessage(message));
    }
    
    private static SecurityToken getToken(Message message)
    {
        TokenStore tokenStore = getTokenStore(message);
        SAMLTokenPrincipal principal = (SAMLTokenPrincipal) message.get(WSS4JInInterceptor.PRINCIPAL_RESULT);
        if(tokenStore != null && principal != null)
        {
            return tokenStore.getToken(principal.getId());
        }
        else
        {
            return null;
        }
    }
    
    private static TokenStore getTokenStore(Message message)
    {
        EndpointInfo info = message.getExchange().get(Endpoint.class).getEndpointInfo();
        synchronized (info)
        {
            TokenStore tokenStore = (TokenStore) message.getContextualProperty(org.apache.cxf.ws.security.SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
            if (tokenStore == null)
            {
                tokenStore = (TokenStore) info.getProperty(org.apache.cxf.ws.security.SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
            }
            if (tokenStore == null)
            {
                TokenStoreFactory tokenStoreFactory = TokenStoreFactory.newInstance();
                tokenStore = tokenStoreFactory.newTokenStore(org.apache.cxf.ws.security.SecurityConstants.TOKEN_STORE_CACHE_INSTANCE, message);
                info.setProperty(org.apache.cxf.ws.security.SecurityConstants.TOKEN_STORE_CACHE_INSTANCE, tokenStore);
            }
            return tokenStore;
        }
    }

}
