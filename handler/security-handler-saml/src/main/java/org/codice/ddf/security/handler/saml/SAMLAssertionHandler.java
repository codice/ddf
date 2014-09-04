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
package org.codice.ddf.security.handler.saml;

import ddf.security.SecurityConstants;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.rs.security.saml.DeflateEncoderDecoder;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.codice.ddf.security.common.HttpUtils;
import org.codice.ddf.security.handler.api.AuthenticationHandler;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.SAMLAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.zip.DataFormatException;

/**
 * Checks for a SAML assertion that has been returned to us in the ddf security cookie. If it exists, it
 * is retrieved and converted into a SecurityToken.
 */
public class SAMLAssertionHandler implements AuthenticationHandler {
    /**
     * SAML type to use when configuring context policy.
     */
    public static final String AUTH_TYPE = "SAML";

    protected String realm = BaseAuthenticationToken.DEFAULT_REALM;


    private static final Logger LOGGER = LoggerFactory
      .getLogger(SAMLAssertionHandler.class);

    @Override
    public String getAuthenticationType() {
        return AUTH_TYPE;
    }

    @Override
    public HandlerResult getNormalizedToken(ServletRequest request, ServletResponse response,
      FilterChain chain, boolean resolve) {
        HandlerResult handlerResult = new HandlerResult();

        SecurityToken securityToken = null;
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        Map<String, Cookie> cookies = HttpUtils.getCookieMap(httpRequest);

        // check for full SAML assertions coming in (federated requests, etc.)
        Cookie samlCookie = cookies.get(SecurityConstants.SAML_COOKIE_NAME);
        if (samlCookie != null) {
            String cookieValue = samlCookie.getValue();
            LOGGER.trace("Cookie retrieved");
            try {
                byte[] cv = Base64Utility.decode(cookieValue);
                DeflateEncoderDecoder decoder = new DeflateEncoderDecoder();
                InputStream is = decoder.inflateToken(cv);
                String tokenString = IOUtils.toString(is, "UTF-8");
                LOGGER.trace("Cookie value: {}", tokenString);
                securityToken = new SecurityToken();
                Element thisToken = StaxUtils.read(new StringReader(tokenString))
                  .getDocumentElement();
                securityToken.setToken(thisToken);
                SAMLAuthenticationToken samlToken = new SAMLAuthenticationToken(null, securityToken, realm);
                handlerResult.setToken(samlToken);
                handlerResult.setStatus(HandlerResult.Status.COMPLETED);
            } catch (DataFormatException e) {
                LOGGER.warn("Unexpected error deflating cookie value - proceeding without SAML token.", e);
            } catch (Base64Exception e) {
                LOGGER.warn("Unexpected error un-encoding the cookie value - proceeding without SAML token.", e);
            } catch (IOException e) {
                LOGGER.warn("Unexpected error converting cookie value to string - proceeding without SAML token.", e);
            } catch (XMLStreamException e) {
                LOGGER.warn("Unexpected error converting XML string to element - proceeding without SAML token.", e);
            }
            return handlerResult;
        }

        HttpSession session = httpRequest.getSession(false);
        if(session != null) {
            SecurityToken savedToken = (SecurityToken) session.getAttribute(
                    SecurityConstants.SAML_ASSERTION);
            if (savedToken != null) {
                LOGGER.trace("Creating SAML authentication token with session.");
                SAMLAuthenticationToken samlToken = new SAMLAuthenticationToken(null, session.getId(),
                        realm);
                handlerResult.setToken(samlToken);
                handlerResult.setStatus(HandlerResult.Status.COMPLETED);
                return handlerResult;
            } else {
                LOGGER.trace("No SAML cookie located - returning with no results");
            }
        } else {
            LOGGER.trace("No HTTP Session - returning with no results");
        }

        return handlerResult;
    }

    /**
     * If an error occured during the processing of the request, this method will get called. Since
     * SAML handling is typically processed first, then we can assume that there was an error with
     * the presented SAML assertion - either it was invalid, or the reference didn't match a
     * cached assertion, etc. In order not to get stuck in a processing loop, we will remove the
     * existing SAML assertion cookies - that will allow handling to progress moving forward.
     * @param servletRequest http servlet request
     * @param servletResponse http servlet response
     * @param chain rest of the request chain to be invoked after security handling
     * @return result containing the potential credentials and status
     * @throws ServletException
     */
    @Override
    public HandlerResult handleError(ServletRequest servletRequest, ServletResponse servletResponse,
      FilterChain chain) throws ServletException {
        HandlerResult result = new HandlerResult();

        HttpServletRequest httpRequest = servletRequest instanceof HttpServletRequest ? (HttpServletRequest) servletRequest : null;
        HttpServletResponse httpResponse = servletResponse instanceof HttpServletResponse ? (HttpServletResponse) servletResponse : null;
        if (httpRequest == null || httpResponse == null) {
            return result;
        }

        LOGGER.debug("In error handler for saml - clearing cookies and returning no action taken.");

        // we tried to process an invalid or missing SAML assertion
        deleteCookie(SecurityConstants.SAML_COOKIE_NAME, httpRequest, httpResponse);
        deleteCookie(SecurityConstants.SAML_COOKIE_REF, httpRequest, httpResponse);

        result.setStatus(HandlerResult.Status.NO_ACTION);
        return result;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public void deleteCookie(String cookieName, HttpServletRequest request, HttpServletResponse response) {
        //remove session cookie
        try {
            LOGGER.debug("Removing cookie {}", cookieName);
            response.setContentType("text/html");
            Cookie cookie = new Cookie(cookieName, "");
            URL url = null;
            url = new URL(request.getRequestURL().toString());
            cookie.setDomain(url.getHost());
            cookie.setMaxAge(0);
            cookie.setPath("/");
            cookie.setComment("EXPIRING COOKIE at " + System.currentTimeMillis());
            response.addCookie(cookie);
        } catch (MalformedURLException e) {
            LOGGER.warn("Unable to delete cookie {}", cookieName, e);
        }
    }

}
