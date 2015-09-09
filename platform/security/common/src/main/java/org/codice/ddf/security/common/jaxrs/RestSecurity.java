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
package org.codice.ddf.security.common.jaxrs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;

/**
 * Provides methods that help with securing RESTful (jaxrs) communications.
 */
public final class RestSecurity {

    public static final String SAML_HEADER_PREFIX = "SAML ";

    public static final String SAML_HEADER_NAME = "Authorization";

    private static final Logger LOGGER = LoggerFactory.getLogger(RestSecurity.class);

    /**
     * Parses the incoming subject for a saml assertion and sets that as a cookie on the client.
     *
     * @param subject Subject containing a SAML-based security token.
     * @param client  Non-null client to set the cookie on.
     * @throws NullPointerException if client is null
     */
    public static void setSubjectOnClient(Subject subject, Client client) {
        if (client != null && subject != null && "https"
                .equalsIgnoreCase(client.getCurrentURI().getScheme())) {
            String encodedSamlHeader = createSamlHeader(subject);
            if (encodedSamlHeader == null) {
                LOGGER.debug("SAML Header was null. Unable to set the header for the client.");
                return;
            }
            client.header(SAML_HEADER_NAME, encodedSamlHeader);
        }
    }

    /**
     * Creates an authorization header to be returned to the browser if the token was successfully
     * exchanged for a SAML assertion
     *
     * @param subject - {@link ddf.security.Subject} to create the header from
     */
    private static String createSamlHeader(Subject subject) {
        String encodedSamlHeader = null;
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
                BigDecimal maxAge = null;
                if (expires == null) {
                    //default to 10 minutes
                    maxAge = new BigDecimal(600);
                } else {
                    maxAge = new BigDecimal((expires.getTime() - new Date().getTime()) / 1000);
                }
                SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlToken);
                String saml = assertion.assertionToString();
                encodedSamlHeader = SAML_HEADER_PREFIX + encodeSaml(saml);
            }
        } catch (WSSecurityException | ArithmeticException e) {
            LOGGER.error("Unable to parse SAML assertion from subject.", e);
        }
        return encodedSamlHeader;
    }

    /**
     * Encodes the SAML assertion as a deflated Base64 String so that it can be used as a Cookie.
     *
     * @param token SAML assertion as a token
     * @return String
     * @throws WSSecurityException if the assertion in the token cannot be converted
     */
    public static String encodeSaml(String token) throws WSSecurityException {
        ByteArrayOutputStream tokenBytes = new ByteArrayOutputStream();
        try (OutputStream tokenStream = new DeflaterOutputStream(tokenBytes,
                new Deflater(Deflater.DEFAULT_COMPRESSION, false))) {
            IOUtils.copy(new ByteArrayInputStream(token.getBytes(StandardCharsets.UTF_8)),
                    tokenStream);
            tokenStream.close();

            return new String(Base64.encodeBase64(tokenBytes.toByteArray()));
        } catch (IOException e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e);
        }
    }

    public static String decodeSaml(String encodedToken) throws IOException {
        byte[] deflatedToken = Base64.decodeBase64(encodedToken);
        InputStream is = new InflaterInputStream(new ByteArrayInputStream(deflatedToken),
                new Inflater(false));
        return IOUtils.toString(is, "UTF-8");
    }

}
