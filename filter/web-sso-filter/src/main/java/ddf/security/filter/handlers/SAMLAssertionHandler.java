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
package ddf.security.filter.handlers;

import ddf.security.filter.AuthenticationHandler;
import ddf.security.filter.DeflateEncoderDecoder;
import ddf.security.filter.FilterResult;
import ddf.security.filter.FilterUtils;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Map;
import java.util.zip.DataFormatException;

/**
 * Checks for a SAML assertion that has been returned to us in the ddf security cookie. If it exists, it
 * is retrieved and converted into a SecurityToken.
 */
public class SAMLAssertionHandler implements AuthenticationHandler {
    private static final transient Logger LOGGER = LoggerFactory
      .getLogger(SAMLAssertionHandler.class);

    private static final String SAML_COOKIE_NAME = "org.codice.websso.saml.token";

    @Override
    public FilterResult getNormalizedToken(ServletRequest request, ServletResponse response, FilterChain chain, boolean resolve) {
        FilterResult filterResult = new FilterResult();

        SecurityToken securityToken = null;
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        Map<String, Cookie> cookies = FilterUtils.getCookieMap(httpRequest);

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
                Element thisToken = StaxUtils.read(new StringReader(tokenString)).getDocumentElement();
                securityToken.setToken(thisToken);
                filterResult.setSecurityToken(securityToken);
                filterResult.setStatus(FilterResult.FilterStatus.COMPLETED);
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

        return filterResult;
    }
}
