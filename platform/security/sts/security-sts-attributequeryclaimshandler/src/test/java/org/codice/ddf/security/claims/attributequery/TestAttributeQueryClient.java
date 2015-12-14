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
package org.codice.ddf.security.claims.attributequery;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensaml.saml2.core.Assertion;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import ddf.security.encryption.EncryptionService;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;

public class TestAttributeQueryClient {

    class AttributeQueryClientTest extends AttributeQueryClient {

        AttributeQueryClientTest(SimpleSign simpleSign, String externalAttributeStoreUrl,
                String issuer, String destination) {
            super(simpleSign, externalAttributeStoreUrl, issuer, destination);
        }

        @Override
        protected HttpsURLConnection createHttpsUrlConnection(URL url) {
            HttpsURLConnection httpsURLConnection = mock(HttpsURLConnection.class);
            DataOutputStream dataOutputStream = mock(DataOutputStream.class);
            InputStream inputStream = new ByteArrayInputStream(cannedResponse.getBytes());
            try {
                if (!outputStreamIoException) {
                    when(httpsURLConnection.getOutputStream()).thenReturn(dataOutputStream);
                } else {
                    when(httpsURLConnection.getOutputStream()).thenThrow(
                            new IOException("Could not get the connection's OutputStream."));
                }
                when(httpsURLConnection.getResponseCode()).thenReturn(responseCode);

                if (!inputStreamIoException) {
                    when(httpsURLConnection.getInputStream()).thenReturn(inputStream);
                } else {
                    when(httpsURLConnection.getInputStream()).thenThrow(
                            new IOException("Could not get the connection's InputStream."));
                }
            } catch (IOException e) {
                fail("Unable to create HttpsUrlConnection.");
            }
            return httpsURLConnection;
        }
    }

    private static final String SAML2_SUCCESS = "urn:oasis:names:tc:SAML:2.0:status:Success";

    private static final String SAML2_UNKNOWN_ATTR_PROFILE = "urn:oasis:names:tc:SAML:2.0:status:UnknownAttrProfile";

    private static final String SAML2_INVALID_ATTR_NAME_VALUE = "urn:oasis:names:tc:SAML:2.0:status:InvalidAttrNameOrValue";

    private static final String SAML2_UNKNOWN_PRINCIPAL = "urn:oasis:names:tc:SAML:2.0:status:UnknownPrincipal";

    private static final String DESTINATION = "testDestination";

    private static final String ISSUER = "testIssuer";

    private String username = "admin";

    private boolean inputStreamIoException = false;

    private boolean outputStreamIoException = false;

    private int responseCode = 200;

    private SystemBaseUrl systemBaseUrl;

    private AttributeQueryClient attributeQueryClient;

    private EncryptionService encryptionService;

    private SystemCrypto systemCrypto;

    private SimpleSign spySimpleSign;

    private String cannedResponse;

    @BeforeClass
    public static void init() {
        OpenSAMLUtil.initSamlEngine();
    }

    @Before
    public void setUp() throws IOException {
        encryptionService = mock(EncryptionService.class);
        systemCrypto = new SystemCrypto("encryption.properties", "signature.properties",
                encryptionService);
        SimpleSign simpleSign = new SimpleSign(systemCrypto);
        spySimpleSign = spy(simpleSign);
        systemBaseUrl = new SystemBaseUrl();

        attributeQueryClient = new AttributeQueryClientTest(spySimpleSign,
                systemBaseUrl.getBaseUrl(), ISSUER, DESTINATION);
        attributeQueryClient.setSimpleSign(spySimpleSign);
        attributeQueryClient.setExternalAttributeStoreUrl(systemBaseUrl.getBaseUrl());
        attributeQueryClient.setIssuer(ISSUER);
        attributeQueryClient.setDestination(DESTINATION);

        cannedResponse = Resources
                .toString(Resources.getResource(getClass(), "/SAMLResponse.xml"), Charsets.UTF_8);
    }

    @Test
    public void testRetrieveResponse() {
        Assertion assertion = attributeQueryClient.retrieveResponse(username);

        assertThat(assertion, is(notNullValue()));
        assertThat(assertion.getIssuer().getValue(), is(equalTo("localhost")));
        assertThat(assertion.getSubject().getNameID().getValue(), is(equalTo("admin")));
        assertThat(assertion.getAttributeStatements(), is(notNullValue()));
    }

    @Test
    public void testRetrieveResponseUnauthorizedStatusCode() {
        responseCode = 401;

        assertThat(attributeQueryClient.retrieveResponse(username), is(nullValue()));
    }

    @Test
    public void testRetrieveResponseNotFoundStatusCode() {
        responseCode = 404;

        assertThat(attributeQueryClient.retrieveResponse(username), is(nullValue()));
    }

    @Test
    public void testRetrieveResponseUnknownStatusCode() {
        responseCode = -1;

        assertThat(attributeQueryClient.retrieveResponse(username), is(nullValue()));
    }

    @Test
    public void testRetrieveResponseUnknownAttrProfile() throws IOException {
        cannedResponse = Resources
                .toString(Resources.getResource(getClass(), "/SAMLResponse.xml"), Charsets.UTF_8)
                .replaceAll(SAML2_SUCCESS, SAML2_UNKNOWN_ATTR_PROFILE);

        assertThat(attributeQueryClient.retrieveResponse(username), is(nullValue()));
    }

    @Test
    public void testRetrieveResponseInvalidAttrNameOrValue() throws IOException {
        cannedResponse = Resources
                .toString(Resources.getResource(getClass(), "/SAMLResponse.xml"), Charsets.UTF_8)
                .replaceAll(SAML2_SUCCESS, SAML2_INVALID_ATTR_NAME_VALUE);

        assertThat(attributeQueryClient.retrieveResponse(username), is(nullValue()));
    }

    @Test
    public void testRetrieveResponseUnknownPrincipal() throws IOException {
        cannedResponse = Resources
                .toString(Resources.getResource(getClass(), "/SAMLResponse.xml"), Charsets.UTF_8)
                .replaceAll(SAML2_SUCCESS, SAML2_UNKNOWN_PRINCIPAL);

        assertThat(attributeQueryClient.retrieveResponse(username), is(nullValue()));
    }

    @Test
    public void testRetrieveResponseInvalidResponse() throws IOException {
        cannedResponse = Resources
                .toString(Resources.getResource(getClass(), "/SAMLResponse.xml"), Charsets.UTF_8)
                .replaceAll(SAML2_SUCCESS, "Invalid Response");

        assertThat(attributeQueryClient.retrieveResponse(username), is(nullValue()));
    }

    @Test(expected = AttributeQueryException.class)
    public void testRetrieveResponseConnectionGetInputStreamIOException() {
        outputStreamIoException = true;

        attributeQueryClient.retrieveResponse(username);
    }

    @Test(expected = AttributeQueryException.class)
    public void testRetrieveResponseConnectionGetOutputStreamIOException() {
        inputStreamIoException = true;

        attributeQueryClient.retrieveResponse(username);
    }

    @Test(expected = AttributeQueryException.class)
    public void testRetrieveResponseSimpleSignSignatureException()
            throws SimpleSign.SignatureException {
        doThrow(new SimpleSign.SignatureException()).when(spySimpleSign)
                .signSamlObject(anyObject());

        attributeQueryClient.retrieveResponse(username);
    }
}
