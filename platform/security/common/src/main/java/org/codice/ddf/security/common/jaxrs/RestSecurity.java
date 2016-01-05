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
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.common.audit.SecurityLogger;

/**
 * Provides methods that help with securing RESTful (jaxrs) communications.
 */
public final class RestSecurity {

    public static final String SAML_HEADER_PREFIX = "SAML ";

    public static final String BASIC_HEADER_PREFIX = "BASIC ";

    public static final String AUTH_HEADER = "Authorization";

    private static final Logger LOGGER = LoggerFactory.getLogger(RestSecurity.class);

    public static final boolean GZIP_COMPATIBLE = true;

    /**
     * Parses the incoming subject for a saml assertion and sets that as a header on the client.
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
            client.header(AUTH_HEADER, encodedSamlHeader);
        }
    }

    public static void setUserOnClient(String username, String password, Client client)
            throws UnsupportedEncodingException {
        if (client != null && username != null && password != null) {
            if (!StringUtils.startsWithIgnoreCase(client.getCurrentURI().getScheme(), "https")) {
                if (Boolean.valueOf(
                        System.getProperty("org.codice.allowBasicAuthOverHttp", "false"))) {
                    LOGGER.warn(
                            "CAUTION: Passing username & password on an un-encrypted protocol [{}]."
                                    + " This is a security issue. ", client.getCurrentURI());
                    SecurityLogger.logWarn(
                            "Passing username & password on an un-encrypted protocol [" + client
                                    .getCurrentURI() + "].");
                } else {
                    LOGGER.warn(
                            "Passing username & password is not allowed on an un-encrypted protocol [{}].",
                            client.getCurrentURI());
                    return;
                }
            }
            String basicCredentials = username + ":" + password;
            String encodedHeader = BASIC_HEADER_PREFIX + new String(
                    Base64.encodeBase64(basicCredentials.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
            client.header(AUTH_HEADER, encodedHeader);

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
        try {
            for (Object principal : subject.getPrincipals().asList()) {
                if (principal instanceof SecurityAssertion) {
                    SecurityToken securityToken = ((SecurityAssertion) principal)
                            .getSecurityToken();
                    samlToken = securityToken.getToken();
                }
            }
            if (samlToken != null) {
                SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlToken);
                String saml = assertion.assertionToString();
                encodedSamlHeader = SAML_HEADER_PREFIX + deflateAndBase64Encode(saml);
            }
        } catch (WSSecurityException | ArithmeticException | IOException e) {
            LOGGER.error("Unable to parse SAML assertion from subject.", e);
        }
        return encodedSamlHeader;
    }

    /**
     * Deflates a value and Base64 encodes the result.
     *
     * @param value value to deflate and Base64 encode
     * @return String
     * @throws IOException if the value cannot be converted
     */
    public static String deflateAndBase64Encode(String value) throws IOException {
        ByteArrayOutputStream valueBytes = new ByteArrayOutputStream();
        try (OutputStream tokenStream = new DeflaterOutputStream(valueBytes,
                new Deflater(Deflater.DEFLATED, GZIP_COMPATIBLE))) {
            tokenStream.write(value.getBytes(StandardCharsets.UTF_8));
            tokenStream.close();

            return new String(Base64.encodeBase64(valueBytes.toByteArray()), StandardCharsets.UTF_8.name());
        }
    }

    public static String inflateBase64(String base64EncodedValue) throws IOException {
        byte[] deflatedValue = Base64.decodeBase64(base64EncodedValue);
        InputStream is = new InflaterInputStream(new ByteArrayInputStream(deflatedValue),
                new Inflater(GZIP_COMPATIBLE));
        return IOUtils.toString(is, StandardCharsets.UTF_8.name());
    }

}
