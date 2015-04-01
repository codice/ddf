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
package ddf.security.assertion.impl;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SecurityAssertionImplTest {

    private static final String ISSUER = "tokenissuer";

    private static final String PRINCIPAL = "CN=client,OU=I4CE,O=Lockheed Martin,L=Goodyear,ST=Arizona,C=US";

    private static final int NUM_ATTRIBUTES = 1;

    private static final int NUM_NAUTH = 1;

    private static final int NUM_AUTHZ = 0;

    @Test
    public void testEmptyAssertion() {
        SecurityAssertionImpl assertion = new SecurityAssertionImpl();
        assertEquals(null, assertion.getIssuer());
        assertEquals(0, assertion.getAttibuteStatements().size());
        assertEquals(0, assertion.getAuthnStatements().size());
        assertEquals(0, assertion.getAuthzDecisionStatements().size());
        assertEquals(null, assertion.getPrincipal());
    }

    @Test
    public void testSampleAssertion() throws Exception {
        Element issuedAssertion = this.readDocument("/saml.xml").getDocumentElement();
        String assertionId = issuedAssertion.getAttributeNodeNS(null, "ID").getNodeValue();
        SecurityToken token = new SecurityToken(assertionId, issuedAssertion, null);
        SecurityAssertionImpl assertion = new SecurityAssertionImpl(token);
        assertNotNull(assertion.getSecurityToken());
        assertEquals(token, assertion.getSecurityToken());
        assertEquals(ISSUER, assertion.getIssuer());
        assertEquals(PRINCIPAL, assertion.getPrincipal().getName());
        assertEquals(PRINCIPAL, assertion.getPrincipal().toString());
        assertEquals(NUM_ATTRIBUTES, assertion.getAttibuteStatements().size());
        assertEquals(NUM_NAUTH, assertion.getAuthnStatements().size());
        //we don't currently parse these
//        assertEquals(NUM_AUTHZ, assertion.getAuthzDecisionStatements().size());
        assertNotNull(assertion.toString());

    }

    /**
     * Reads a classpath resource into a Document.
     * 
     * @param name
     *            the name of the classpath resource
     */
    protected Document readDocument(String name) throws SAXException, IOException,
        ParserConfigurationException {
        InputStream inStream = getClass().getResourceAsStream(name);
        return readXml(inStream);
    }

    public static Document readXml(InputStream is) throws SAXException, IOException,
            ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        dbf.setValidating(false);
        dbf.setIgnoringComments(false);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setNamespaceAware(true);
        // dbf.setCoalescing(true);
        // dbf.setExpandEntityReferences(true);

        DocumentBuilder db = null;
        db = dbf.newDocumentBuilder();
        db.setEntityResolver(new DOMUtils.NullResolver());

        // db.setErrorHandler( new MyErrorHandler());

        return db.parse(is);
    }
}
