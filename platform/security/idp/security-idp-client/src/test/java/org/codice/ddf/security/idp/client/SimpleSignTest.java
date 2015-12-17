/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.idp.client;

import static org.mockito.Mockito.mock;
import static junit.framework.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;

import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.impl.UriBuilderImpl;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.util.DOM2Writer;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensaml.xml.XMLObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import ddf.security.encryption.EncryptionService;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;

public class SimpleSignTest {

    private String cannedResponse;

    private EncryptionService encryptionService;

    private SystemCrypto systemCrypto;

    private SimpleSign simpleSign;

    private static final String SAML_RESPONSE = "SAMLResponse";

    private static final String RELAY_STATE = "RelayState";

    private static final String SIG_ALG = "SigAlg";

    private static final String RELAY_STATE_VAL = "b0b4e449-7f69-413f-a844-61fe2256de19";

    @BeforeClass
    public static void init() {
        OpenSAMLUtil.initSamlEngine();
    }

    @Before
    public void setUp() throws Exception {

        encryptionService = mock(EncryptionService.class);
        systemCrypto = new SystemCrypto("encryption.properties",
                "signature.properties",
                encryptionService);
        simpleSign = new SimpleSign(systemCrypto);

        cannedResponse = Resources.toString(Resources.getResource(getClass(), "/SAMLResponse.xml"),
                Charsets.UTF_8);

    }

    @Test
    public void testSignSamlObject() throws Exception {

        Document responseDoc = StaxUtils.read(new ByteArrayInputStream(cannedResponse.getBytes()));
        XMLObject responseXmlObject = OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
        org.opensaml.saml2.core.Response response =
                (org.opensaml.saml2.core.Response) responseXmlObject;
        simpleSign.signSamlObject(response);

        Document doc = DOMUtils.createDocument();
        Element requestElement = OpenSAMLUtil.toDom(response, doc);
        String responseMessage = DOM2Writer.nodeToString(requestElement);
        responseDoc = StaxUtils.read(new ByteArrayInputStream(responseMessage.getBytes()));
        responseXmlObject = OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
        response = (org.opensaml.saml2.core.Response) responseXmlObject;
        simpleSign.validateSignature(response.getSignature(),
                response.getDOM()
                        .getOwnerDocument());
    }

    @Test(expected = SimpleSign.SignatureException.class)
    public void testSignSamlObjectThenModify() throws Exception {

        Document responseDoc = StaxUtils.read(new ByteArrayInputStream(cannedResponse.getBytes()));
        XMLObject responseXmlObject = OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
        org.opensaml.saml2.core.Response response =
                (org.opensaml.saml2.core.Response) responseXmlObject;
        simpleSign.signSamlObject(response);

        Document doc = DOMUtils.createDocument();
        Element requestElement = OpenSAMLUtil.toDom(response, doc);
        requestElement.setAttribute("oops", "changedit");
        String responseMessage = DOM2Writer.nodeToString(requestElement);
        responseDoc = StaxUtils.read(new ByteArrayInputStream(responseMessage.getBytes()));
        responseXmlObject = OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
        response = (org.opensaml.saml2.core.Response) responseXmlObject;
        simpleSign.validateSignature(response.getSignature(),
                response.getDOM()
                        .getOwnerDocument());
    }

    @Test
    public void testSignUriStringWithDsa() throws Exception {

        systemCrypto = new SystemCrypto("dsa-encryption.properties",
                "dsa-signature.properties",
                encryptionService);
        simpleSign = new SimpleSign(systemCrypto);
        IdpMetadata idpMetadata = new IdpMetadata();
        String metadata = Resources.toString(Resources.getResource(getClass(),
                "/dsa-IDPmetadata.xml"), Charsets.UTF_8);
        idpMetadata.setMetadata(metadata);
        String deflatedSamlResponse = RestSecurity.deflateAndBase64Encode(cannedResponse);

        String queryParams = String.format("SAMLResponse=%s&RelayState=%s",
                URLEncoder.encode(deflatedSamlResponse, "UTF-8"),
                URLEncoder.encode(RELAY_STATE_VAL, "UTF-8"));
        String idpRequest = idpMetadata.getSingleSignOnLocation() + "?" + queryParams;
        UriBuilder idpUri = new UriBuilderImpl(new URI(idpRequest));
        simpleSign.signUriString(queryParams, idpUri);

        String signatureAlgorithm = URLEncodedUtils.parse(idpUri.build(), "UTF-8")
                .get(2)
                .getValue();
        String signatureString = URLEncodedUtils.parse(idpUri.build(), "UTF-8")
                .get(3)
                .getValue();

        String signedMessage = String.format("%s=%s&%s=%s&%s=%s",
                SAML_RESPONSE,
                URLEncoder.encode(deflatedSamlResponse, "UTF-8"),
                RELAY_STATE,
                URLEncoder.encode(RELAY_STATE_VAL, "UTF-8"),
                SIG_ALG,
                URLEncoder.encode(signatureAlgorithm, "UTF-8"));
        boolean valid = simpleSign.validateSignature(signedMessage,
                signatureString,
                idpMetadata.getSigningCertificate());
        assertTrue("Signature was expected to be valid", valid);

    }

    @Test(expected = SimpleSign.SignatureException.class)
    public void testSignUriStringAndModifyWithDsa() throws Exception {

        systemCrypto = new SystemCrypto("dsa-encryption.properties",
                "dsa-signature.properties",
                encryptionService);
        simpleSign = new SimpleSign(systemCrypto);
        IdpMetadata idpMetadata = new IdpMetadata();
        String metadata = Resources.toString(Resources.getResource(getClass(),
                "/dsa-IDPmetadata.xml"), Charsets.UTF_8);
        idpMetadata.setMetadata(metadata);
        String deflatedSamlResponse = RestSecurity.deflateAndBase64Encode(cannedResponse);

        String queryParams = String.format("SAMLResponse=%s&RelayState=%s",
                URLEncoder.encode(deflatedSamlResponse, "UTF-8"),
                URLEncoder.encode(RELAY_STATE_VAL, "UTF-8"));
        String idpRequest = idpMetadata.getSingleSignOnLocation() + "?" + queryParams;
        UriBuilder idpUri = new UriBuilderImpl(new URI(idpRequest));
        simpleSign.signUriString(queryParams, idpUri);
        idpUri.queryParam("RelayState", "changedit");

        String signatureAlgorithm = URLEncodedUtils.parse(idpUri.build(), "UTF-8")
                .get(2)
                .getValue();
        String signatureString = URLEncodedUtils.parse(idpUri.build(), "UTF-8")
                .get(3)
                .getValue();

        String signedMessage = String.format("%s=%s&%s=%s&%s=%s",
                SAML_RESPONSE,
                URLEncoder.encode(deflatedSamlResponse, "UTF-8"),
                RELAY_STATE,
                URLEncoder.encode(RELAY_STATE_VAL, "UTF-8"),
                SIG_ALG,
                URLEncoder.encode(signatureAlgorithm, "UTF-8"));
        simpleSign.validateSignature(signedMessage,
                signatureString,
                idpMetadata.getSigningCertificate());

    }

}