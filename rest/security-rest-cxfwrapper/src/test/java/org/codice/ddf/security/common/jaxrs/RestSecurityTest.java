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

import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.codice.ddf.cxf.RestSecurity;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RestSecurityTest {

    @Test
    public void testSetSubjectOnClient() throws Exception {
        Element samlToken = readDocument("/saml.xml").getDocumentElement();
        Subject subject = mock(Subject.class);
        SecurityAssertion assertion = mock(SecurityAssertion.class);
        SecurityToken token = new SecurityToken(UUID.randomUUID().toString(), samlToken, new Date(),
                new Date());
        when(assertion.getSecurityToken()).thenReturn(token);
        when(subject.getPrincipals()).thenReturn(new SimplePrincipalCollection(assertion, "sts"));
        WebClient client = WebClient.create("http://example.org");
        RestSecurity.setSubjectOnClient(subject, client);
        assertNotNull(client.getHeaders().get("Cookie"));
        ArrayList cookies = (ArrayList) client.getHeaders().get("Cookie");
        boolean containsSaml = false;
        for (Object cookie : cookies) {
            if (StringUtils.contains(cookie.toString(), SecurityConstants.SAML_COOKIE_NAME)) {
                containsSaml = true;
            }
        }
        assertTrue(containsSaml);
    }

    /**
     * Reads a classpath resource into a Document.
     *
     * @param name the name of the classpath resource
     */
    private Document readDocument(String name)
            throws SAXException, IOException, ParserConfigurationException {
        InputStream inStream = getClass().getResourceAsStream(name);
        return DOMUtils.readXml(inStream);
    }

}
