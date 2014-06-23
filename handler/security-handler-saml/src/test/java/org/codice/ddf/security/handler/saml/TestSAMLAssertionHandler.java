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

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.rs.security.saml.DeflateEncoderDecoder;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.servlet.FilterChain;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestSAMLAssertionHandler {

    /**
     * This test ensures the proper functionality of SAMLAssertionHandler's
     * method, getNormalizedToken(), when given a valid HttpServletRequest.
     */
    @Test
    public void testGetNormalizedTokenSuccess() throws Exception {
        SAMLAssertionHandler handler = new SAMLAssertionHandler();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        Element assertion = readDocument("/saml.xml").getDocumentElement();
        String assertionId = assertion.getAttributeNodeNS(null, "ID").getNodeValue();
        SecurityToken samlToken = new SecurityToken(assertionId, assertion, null);
        Cookie cookie = new Cookie(SAMLAssertionHandler.SAML_COOKIE_NAME, encodeSaml(samlToken.getToken()));
        when(request.getCookies()).thenReturn(new Cookie[] {cookie});

        HandlerResult result = handler.getNormalizedToken(request, response, chain, true);

        assertNotNull(result);
        assertEquals(HandlerResult.Status.COMPLETED, result.getStatus());
    }

    /**
     * This test ensures the proper functionality of SAMLAssertionHandler's
     * method, getNormalizedToken(), when given an invalid HttpServletRequest.
     */
    @Test
    public void testGetNormalizedTokenFailure() {
        SAMLAssertionHandler handler = new SAMLAssertionHandler();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getCookies()).thenReturn(null);

        HandlerResult result = handler.getNormalizedToken(request, response, chain, true);

        assertNotNull(result);
        assertEquals(HandlerResult.Status.NO_ACTION, result.getStatus());
    }

    /**
     * Reads a classpath resource into a Document.
     *
     * @param name
     *            the name of the classpath resource
     */
    private Document readDocument(String name) throws SAXException, IOException,
            ParserConfigurationException {
        InputStream inStream = getClass().getResourceAsStream(name);
        return DOMUtils.readXml(inStream);
    }

    /**
     * Encodes the SAML assertion as a deflated Base64 String so that it can be used as a Cookie.
     *
     * @param token
     * @return String
     * @throws WSSecurityException
     */
    private String encodeSaml(org.w3c.dom.Element token) throws WSSecurityException {
        AssertionWrapper assertion = new AssertionWrapper(token);
        String samlStr = assertion.assertionToString();
        DeflateEncoderDecoder deflateEncoderDecoder = new DeflateEncoderDecoder();
        byte[] deflatedToken = deflateEncoderDecoder.deflateToken(samlStr.getBytes());
        return Base64Utility.encode(deflatedToken);
    }
}