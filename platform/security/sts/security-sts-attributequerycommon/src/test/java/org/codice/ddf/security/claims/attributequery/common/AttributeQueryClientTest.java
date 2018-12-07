/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.claims.attributequery.common;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import ddf.security.encryption.EncryptionService;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensaml.core.config.InitializationException;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml2.core.Assertion;

public class AttributeQueryClientTest {

  private static final String SAML2_SUCCESS = "urn:oasis:names:tc:SAML:2.0:status:Success";

  private static final String SAML2_UNKNOWN_ATTR_PROFILE =
      "urn:oasis:names:tc:SAML:2.0:status:UnknownAttrProfile";

  private static final String SAML2_INVALID_ATTR_NAME_VALUE =
      "urn:oasis:names:tc:SAML:2.0:status:InvalidAttrNameOrValue";

  private static final String SAML2_UNKNOWN_PRINCIPAL =
      "urn:oasis:names:tc:SAML:2.0:status:UnknownPrincipal";

  private static final String DESTINATION = "testDestination";

  private static final String ISSUER = "testIssuer";

  private static final String USERNAME = "admin";

  private static final String EXTERNAL_ATTRIBUTE_STORE = SystemBaseUrl.INTERNAL.getBaseUrl();

  private Dispatch<StreamSource> dispatch;

  private AttributeQueryClient attributeQueryClient;

  private EncryptionService encryptionService;

  private SystemCrypto systemCrypto;

  private SimpleSign spySimpleSign;

  private String cannedResponse;

  @BeforeClass
  public static void init() throws InitializationException {
    OpenSAMLUtil.initSamlEngine();
  }

  @Before
  public void setUp() throws IOException {
    dispatch = mock(Dispatch.class);
    encryptionService = mock(EncryptionService.class);
    systemCrypto =
        new SystemCrypto("encryption.properties", "signature.properties", encryptionService);
    SimpleSign simpleSign = new SimpleSign(systemCrypto);
    spySimpleSign = spy(simpleSign);
    attributeQueryClient =
        new AttributeQueryClient(
            dispatch, spySimpleSign, EXTERNAL_ATTRIBUTE_STORE, ISSUER, DESTINATION);
    attributeQueryClient.setDispatch(dispatch);
    attributeQueryClient.setSimpleSign(spySimpleSign);
    attributeQueryClient.setExternalAttributeStoreUrl(EXTERNAL_ATTRIBUTE_STORE);
    attributeQueryClient.setIssuer(ISSUER);
    attributeQueryClient.setDestination(DESTINATION);

    cannedResponse =
        Resources.toString(Resources.getResource(getClass(), "/SAMLResponse.xml"), Charsets.UTF_8);
  }

  @Test
  public void testRetrieveResponse() {
    setResponse(cannedResponse, false);

    Assertion assertion = attributeQueryClient.query(USERNAME);
    assertThat(assertion, is(notNullValue()));
    assertThat(assertion.getIssuer().getValue(), is(equalTo("localhost")));
    assertThat(assertion.getSubject().getNameID().getValue(), is(equalTo("admin")));
    assertThat(assertion.getAttributeStatements(), is(notNullValue()));
  }

  @Test
  public void testRetrieveResponseUnknownAttrProfile() throws IOException {
    setResponse(
        Resources.toString(Resources.getResource(getClass(), "/SAMLResponse.xml"), Charsets.UTF_8)
            .replaceAll(SAML2_SUCCESS, SAML2_UNKNOWN_ATTR_PROFILE),
        false);

    assertThat(attributeQueryClient.query(USERNAME), is(nullValue()));
  }

  @Test
  public void testRetrieveResponseInvalidAttrNameOrValue() throws IOException {
    setResponse(
        Resources.toString(Resources.getResource(getClass(), "/SAMLResponse.xml"), Charsets.UTF_8)
            .replaceAll(SAML2_SUCCESS, SAML2_INVALID_ATTR_NAME_VALUE),
        false);

    assertThat(attributeQueryClient.query(USERNAME), is(nullValue()));
  }

  @Test
  public void testRetrieveResponseUnknownPrincipal() throws IOException {
    setResponse(
        Resources.toString(Resources.getResource(getClass(), "/SAMLResponse.xml"), Charsets.UTF_8)
            .replaceAll(SAML2_SUCCESS, SAML2_UNKNOWN_PRINCIPAL),
        false);

    assertThat(attributeQueryClient.query(USERNAME), is(nullValue()));
  }

  @Test
  public void testRetrieveResponseInvalidResponse() throws IOException {
    setResponse(
        Resources.toString(Resources.getResource(getClass(), "/SAMLResponse.xml"), Charsets.UTF_8)
            .replaceAll(SAML2_SUCCESS, "Invalid Response"),
        false);

    assertThat(attributeQueryClient.query(USERNAME), is(nullValue()));
  }

  @Test
  public void testRetrieveResponseEmptyResponse() throws IOException {
    setResponse("", false);

    assertThat(attributeQueryClient.query(USERNAME), is(nullValue()));
  }

  @Test(expected = AttributeQueryException.class)
  public void testRetrieveResponseMalformedResponse() {
    setResponse("<test>test<test/", false);

    attributeQueryClient.query(USERNAME);
  }

  @Test(expected = AttributeQueryException.class)
  public void testRetrieveResponseDispatchException() {
    setResponse(cannedResponse, true);

    attributeQueryClient.query(USERNAME);
  }

  @Test(expected = AttributeQueryException.class)
  public void testRetrieveResponseSimpleSignSignatureException()
      throws SimpleSign.SignatureException {
    doThrow(new SimpleSign.SignatureException())
        .when(spySimpleSign)
        .signSamlObject(any(SignableSAMLObject.class));

    attributeQueryClient.query(USERNAME);
  }

  private void setResponse(String response, boolean throwException) {
    InputStream inputStream = new ByteArrayInputStream(response.getBytes());
    StreamSource streamSource = new StreamSource(inputStream);

    if (!throwException) {
      when(dispatch.invoke(any(StreamSource.class))).thenReturn(streamSource);
    } else {
      when(dispatch.invoke(any(StreamSource.class))).thenThrow(RuntimeException.class);
    }
  }
}
