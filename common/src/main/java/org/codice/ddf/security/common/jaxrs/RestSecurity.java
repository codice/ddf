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
package org.codice.ddf.security.common.jaxrs;

import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.rs.security.saml.DeflateEncoderDecoder;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Cookie;

/**
 * Provides methods that help with securing RESTful (jaxrs) communications.
 */
public final class RestSecurity {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestSecurity.class);

    public static final String SECURITY_COOKIE_NAME = "org.codice.websso.saml.token";

    /**
     * Parses the incoming subject for a saml assertion and sets that as a cookie on the client.
     *
     * @param subject Subject containing a SAML-based security token.
     * @param client  Client to set the cookie on.
     */
    public static void setSubjectOnClient(Subject subject, Client client) {
        if (subject != null) {
            javax.ws.rs.core.Cookie cookie = createSamlCookie(subject);
            if (cookie == null) {
                LOGGER.info("SAML Cookie was null. Unable to set the cookie for the client.");
                return;
            }
            client.cookie(cookie);
        }
    }

    /**
     * Creates a cookie to be returned to the browser if the token was successfully exchanged for
     * a SAML assertion.
     *
     * @param subject - {@link ddf.security.Subject} to create the cookie from
     */
    private static Cookie createSamlCookie(Subject subject) {
        Cookie cookie = null;
        org.w3c.dom.Element samlToken = null;
        try {
            for (Object principal : subject.getPrincipals().asList()) {
                if (principal instanceof SecurityAssertion) {
                    samlToken = ((SecurityAssertion) principal).getSecurityToken()
                            .getToken();
                }
            }
            if (samlToken != null) {
                cookie = new Cookie(SECURITY_COOKIE_NAME, encodeSaml(samlToken));
            }
        } catch (WSSecurityException e) {
            LOGGER.error("Unable to parse SAML assertion from subject.", e);
        }
        return cookie;
    }

    /**
     * Encodes the SAML assertion as a deflated Base64 String so that it can be used as a Cookie.
     *
     * @param token SAML assertion as a token
     * @return String
     * @throws WSSecurityException
     */
    public static String encodeSaml(org.w3c.dom.Element token) throws WSSecurityException {
        AssertionWrapper assertion = new AssertionWrapper(token);
        String samlStr = assertion.assertionToString();
        DeflateEncoderDecoder deflateEncoderDecoder = new DeflateEncoderDecoder();
        byte[] deflatedToken = deflateEncoderDecoder.deflateToken(samlStr.getBytes());
        return Base64Utility.encode(deflatedToken);
    }

}
