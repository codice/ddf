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

import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.rs.security.saml.DeflateEncoderDecoder;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
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
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
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

    protected static final String SAML_COOKIE_NAME = "org.codice.websso.saml.token";

    protected String realm = BaseAuthenticationToken.DEFAULT_REALM;


    private static final transient Logger LOGGER = LoggerFactory
      .getLogger(SAMLAssertionHandler.class);

    /**
     * Returns a mapping of cookies from the incoming request. Key is the cookie name, while the
     * value is the Cookie object itself.
     *
     * @param req Servlet request for this call
     * @return map of Cookie objects present in the current request - always returns a map
     */
    public static Map<String, Cookie> getCookieMap(HttpServletRequest req) {
        HashMap<String, Cookie> map = new HashMap<String, Cookie>();

        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie != null) {
                    map.put(cookie.getName(), cookie);
                }
            }
        }

        return map;
    }

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
        Map<String, Cookie> cookies = getCookieMap(httpRequest);

        Cookie samlCookie = cookies.get(SAML_COOKIE_NAME);
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
        } else {
            LOGGER.trace("No SAML cookie located - returning with no results");
        }

        return handlerResult;
    }

    @Override
    public HandlerResult handleError(ServletRequest servletRequest, ServletResponse servletResponse,
      FilterChain chain) throws ServletException {
        HandlerResult result = new HandlerResult();
        LOGGER.debug("In error handler for saml - returning no action taken.");
        result.setStatus(HandlerResult.Status.NO_ACTION);
        return result;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }
}
