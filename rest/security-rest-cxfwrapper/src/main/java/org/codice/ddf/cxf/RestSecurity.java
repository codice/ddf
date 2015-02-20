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
package org.codice.ddf.cxf;

import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.rs.security.saml.DeflateEncoderDecoder;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Provides methods that help with securing RESTful (jaxrs) communications.
 */
public final class RestSecurity {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestSecurity.class);

    /**
     * Parses the incoming subject for a saml assertion and sets that as a cookie on the client.
     *
     * @param subject Subject containing a SAML-based security token.
     * @param client  Non-null client to set the cookie on.
     * @throws NullPointerException if client is null
     */
    public static void setSubjectOnClient(Subject subject, Client client) {
        if (subject != null) {
            javax.ws.rs.core.Cookie cookie = createSamlCookie(subject, true);
            if (cookie == null) {
                LOGGER.debug("SAML Cookie was null. Unable to set the cookie for the client.");
                return;
            }
            client.cookie(cookie);
        }
    }

    /**
     * Sets a saml cookie without requiring ssl on the underlying client. This method
     * only exists for compatibility with legacy or misconfigured systems, and is not
     * recommended for use otherwise.
     *
     * @see #setSubjectOnClient(ddf.security.Subject, org.apache.cxf.jaxrs.client.Client)
     */
    public static void setUnsecuredSubjectOnClient(Subject subject, Client client) {
        if (subject != null) {
            javax.ws.rs.core.Cookie cookie = createSamlCookie(subject, false);
            if (cookie == null) {
                LOGGER.debug("SAML Cookie was null. Unable to set the cookie for the client.");
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
     * @param secure  - whether or not to require SSL for this cookie
     */
    private static Cookie createSamlCookie(Subject subject, boolean secure) {
        Cookie cookie = null;
        org.w3c.dom.Element samlToken = null;
        Date expires = null;
        try {
            for (Object principal : subject.getPrincipals().asList()) {
                if (principal instanceof SecurityAssertion) {
                    SecurityToken securityToken = ((SecurityAssertion) principal)
                            .getSecurityToken();
                    samlToken = securityToken.getToken();
                    expires = securityToken.getExpires();
                }
            }
            if (samlToken != null) {
                cookie = new NewCookie(
                        new Cookie(SecurityConstants.SAML_COOKIE_NAME, encodeSaml(samlToken)), "",
                        // gives us a checked exception for the cast
                        new BigDecimal((expires.getTime() - new Date().getTime()) / 1000)
                                .intValueExact(), secure).toCookie();
            }
        } catch (WSSecurityException | ArithmeticException e) {
            LOGGER.error("Unable to parse SAML assertion from subject.", e);
        }
        return cookie;
    }

    /**
     * Encodes the SAML assertion as a deflated Base64 String so that it can be used as a Cookie.
     *
     * @param token SAML assertion as a token
     * @return String
     * @throws WSSecurityException if the assertion in the token cannot be converted
     */
    public static String encodeSaml(org.w3c.dom.Element token) throws WSSecurityException {
        AssertionWrapper assertion = new AssertionWrapper(token);
        String samlStr = assertion.assertionToString();
        DeflateEncoderDecoder deflateEncoderDecoder = new DeflateEncoderDecoder();
        byte[] deflatedToken = deflateEncoderDecoder
                .deflateToken(samlStr.getBytes(StandardCharsets.UTF_8));
        return Base64Utility.encode(deflatedToken);
    }

}
