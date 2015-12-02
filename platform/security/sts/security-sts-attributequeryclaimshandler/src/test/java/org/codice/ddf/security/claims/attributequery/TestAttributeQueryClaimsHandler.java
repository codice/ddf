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
package org.codice.ddf.security.claims.attributequery;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Response;
import org.opensaml.xml.XMLObject;
import org.w3c.dom.Document;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import ddf.security.encryption.EncryptionService;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;

public class TestAttributeQueryClaimsHandler {

    private static final String DESTINATION = "testDestination";

    private static final String ISSUER = "testIssuer";

    private SystemBaseUrl systemBaseUrl;

    private String username = "CN=testCN, OU=testOU, O=testO, L=testL, ST=testST, C=testC";

    private String responseState = "ValidResponse";

    private AttributeQueryClaimsHandlerTest attributeQueryClaimsHandler;

    private List<String> supportedClaims;

    private EncryptionService encryptionService;

    private SystemCrypto systemCrypto;

    private SimpleSign simpleSign;

    private String cannedResponse;

    class AttributeQueryClaimsHandlerTest extends AttributeQueryClaimsHandler {

        @Override
        protected AttributeQueryClient createAttributeQueryClient(SimpleSign simpleSign,
                String externalAttributeStoreUrl, String issuer, String destination) {

            AttributeQueryClient attributeQueryClient = null;
            Document responseDoc;
            try {
                responseDoc = StaxUtils.read(new ByteArrayInputStream(cannedResponse.getBytes()));
                XMLObject responseXmlObject =
                        OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
                Response response = (Response) responseXmlObject;
                // Extract Assertion from response.
                Assertion assertion = response.getAssertions()
                        .get(0);

                // Check that response only has one assertion.
                assertThat(response.getAssertions()
                        .size(), is(equalTo(1)));

                attributeQueryClient = mock(AttributeQueryClient.class);
                if (responseState.equalsIgnoreCase("ValidResponse")) {
                    when(attributeQueryClient.retrieveResponse(anyString())).thenReturn(assertion);
                } else if (responseState.equalsIgnoreCase("NullResponse")) {
                    when(attributeQueryClient.retrieveResponse(anyString())).thenReturn(null);
                } else {
                    when(attributeQueryClient.retrieveResponse(anyString())).thenThrow(new AttributeQueryException(
                            "Invalid Response"));
                }

                return attributeQueryClient;
            } catch (Exception e) {
                fail("Could not create mock AttributeQueryClient.");
            }
            return attributeQueryClient;
        }
    }

    @BeforeClass
    public static void init() {
        OpenSAMLUtil.initSamlEngine();
    }

    @Before
    public void setUp() throws IOException {
        systemBaseUrl = new SystemBaseUrl();
        encryptionService = mock(EncryptionService.class);
        systemCrypto = new SystemCrypto("encryption.properties",
                "signature.properties",
                encryptionService);
        simpleSign = new SimpleSign(systemCrypto);

        supportedClaims = new ArrayList<>();
        supportedClaims.add("Role");
        supportedClaims.add("NameIdentifier");
        supportedClaims.add("Email");

        attributeQueryClaimsHandler = new AttributeQueryClaimsHandlerTest();
        attributeQueryClaimsHandler.setSimpleSign(simpleSign);
        attributeQueryClaimsHandler.setSupportedClaims(supportedClaims);
        attributeQueryClaimsHandler.setExternalAttributeStoreUrl(systemBaseUrl.getBaseUrl());
        attributeQueryClaimsHandler.setIssuer(ISSUER);
        attributeQueryClaimsHandler.setDestination(DESTINATION);
        attributeQueryClaimsHandler.setAttributeMapLocation(TestAttributeQueryClaimsHandler.class.getClassLoader()
                .getResource("attributeMap.properties")
                .getPath());

        cannedResponse = Resources.toString(Resources.getResource(getClass(), "/SAMLResponse.xml"),
                Charsets.UTF_8);
    }

    @Test
    public void testRetrieveClaimValues() throws Exception {
        ProcessedClaimCollection processedClaimCollection = retrieveClaimValues();

        // Test that the claims were created and mapped correctly.
        assertThat(processedClaimCollection.size(), is(equalTo(3)));

        assertThat(processedClaimCollection.get(0)
                .getClaimType()
                .toString(), is(equalTo("Role")));
        assertThat(processedClaimCollection.get(0)
                .getValues()
                .get(0), is(equalTo("Guest-hasMapping")));

        assertThat(processedClaimCollection.get(1)
                .getClaimType()
                .toString(), is(equalTo("NameIdentifier")));
        assertThat(processedClaimCollection.get(1)
                .getValues()
                .get(0), is(equalTo("Name-hasMapping")));

        // Does not have an attribute mapping.
        assertThat(processedClaimCollection.get(2)
                .getClaimType()
                .toString(), is(equalTo("Email")));
        assertThat(processedClaimCollection.get(2)
                .getValues()
                .get(0), is(equalTo("email")));
    }

    @Test
    public void testRetrieveClaimValuesWithBadURI() throws Exception {
        cannedResponse = Resources.toString(Resources.getResource(getClass(),
                "/SAMLResponseBadAttribute.xml"), Charsets.UTF_8);
        supportedClaims.add("Bad: URI");

        assertThat(retrieveClaimValues().size(), is(equalTo(0)));
    }

    @Test
    public void testRetrieveClaimValuesNullResponse() {
        responseState = "NullResponse";

        assertThat(retrieveClaimValues().size(), is(equalTo(0)));
    }

    @Test
    public void testRetrieveClaimValuesInvalidResponseException() {
        responseState = "Exception";

        assertThat(retrieveClaimValues().size(), is(equalTo(0)));
    }

    @Test
    public void testSupportedClaimsTypes() {
        List<URI> supportedClaimTypes = attributeQueryClaimsHandler.getSupportedClaimTypes();

        assertThat(supportedClaimTypes.size(), is(equalTo(3)));
        assertThat(supportedClaimTypes.get(0)
                .toString(), is(equalTo("Role")));
        assertThat(supportedClaimTypes.get(1)
                .toString(), is(equalTo("NameIdentifier")));
        assertThat(supportedClaimTypes.get(2)
                .toString(), is(equalTo("Email")));
    }

    @Test
    public void testSupportedClaimsTypesWithBadURI() {
        supportedClaims.add("Bad: URI");

        assertThat(attributeQueryClaimsHandler.getSupportedClaimTypes()
                .size(), is(equalTo(3)));
    }

    private ProcessedClaimCollection retrieveClaimValues() {
        ClaimCollection claimCollection = new ClaimCollection();
        Claim claim = new Claim();
        try {
            claim.setClaimType(new URI(
                    "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier"));
        } catch (URISyntaxException e) {
            fail("Could not create URI.");
        }
        claimCollection.add(claim);
        ClaimsParameters claimsParameters = mock(ClaimsParameters.class);
        Principal principal = mock(Principal.class);

        when(principal.getName()).thenReturn(username);
        when(claimsParameters.getPrincipal()).thenReturn(principal);

        return attributeQueryClaimsHandler.retrieveClaimValues(claimCollection, claimsParameters);
    }
}
