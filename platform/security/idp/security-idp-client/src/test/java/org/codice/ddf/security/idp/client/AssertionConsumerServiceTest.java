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
package org.codice.ddf.security.idp.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.xml.HasXPath.hasXPath;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import ddf.security.encryption.EncryptionService;
import ddf.security.http.SessionFactory;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;
import ddf.security.samlp.impl.RelayStates;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.http.HttpStatus;
import org.codice.ddf.platform.filter.AuthenticationFailureException;
import org.codice.ddf.platform.filter.FilterChain;
import org.codice.ddf.platform.filter.SecurityFilter;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opensaml.saml.saml2.core.StatusCode;
import org.w3c.dom.Document;

public class AssertionConsumerServiceTest {

  private AssertionConsumerService assertionConsumerService;

  private SimpleSign simpleSign;

  private IdpMetadata idpMetadata;

  private SystemCrypto systemCrypto;

  private RelayStates<String> relayStates;

  private SecurityFilter loginFilter;

  private SessionFactory sessionFactory;

  private EncryptionService encryptionService;

  private String cannedResponse;

  private HttpServletRequest httpRequest;

  private static final String RELAY_STATE_VAL = "b0b4e449-7f69-413f-a844-61fe2256de19";

  private static final String LOCATION = "test";

  private static final String SIG_ALG_VAL = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";

  private static final String SIGNATURE_VAL =
      "Xwl1U2xqGx9Q/mWvBSsvv3oolPODCTvs920soROMh54fNNRYA+3hEF1oIoC4IaR4IhZfpGTTwuVyoVBpWOGd/8jh515/056CAGezFsG/sD1E5h36XHnV+Xdnlua2wl+c9TfedynfIexRgL+IGTsTm6SPyM0W9B9ddjg96g8shEY=";

  private static String deflatedSamlResponse;

  @Before
  public void setUp() throws Exception {

    encryptionService = mock(EncryptionService.class);
    systemCrypto =
        new SystemCrypto("encryption.properties", "signature.properties", encryptionService);
    simpleSign = new SimpleSign(systemCrypto);
    relayStates = (RelayStates<String>) mock(RelayStates.class);
    when(relayStates.encode("fubar")).thenReturn(RELAY_STATE_VAL);
    when(relayStates.decode(RELAY_STATE_VAL)).thenReturn(LOCATION);
    loginFilter = mock(SecurityFilter.class);
    sessionFactory = mock(SessionFactory.class);
    httpRequest = mock(HttpServletRequest.class);
    when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("fubar"));
    when(httpRequest.isSecure()).thenReturn(true);
    idpMetadata = new IdpMetadata();

    assertionConsumerService =
        new AssertionConsumerService(simpleSign, idpMetadata, systemCrypto, relayStates);
    assertionConsumerService.setRequest(httpRequest);
    assertionConsumerService.setLoginFilter(loginFilter);
    assertionConsumerService.setSessionFactory(sessionFactory);
    cannedResponse =
        Resources.toString(Resources.getResource(getClass(), "/SAMLResponse.xml"), Charsets.UTF_8);

    String metadata =
        Resources.toString(Resources.getResource(getClass(), "/IDPmetadata.xml"), Charsets.UTF_8);

    deflatedSamlResponse =
        Resources.toString(
            Resources.getResource(getClass(), "/DeflatedSAMLResponse.txt"), Charsets.UTF_8);
    idpMetadata.setMetadata(metadata);
    System.setProperty("org.codice.ddf.system.rootContext", "/services");
  }

  @Test
  public void testPostSamlResponse() throws Exception {
    Response response =
        assertionConsumerService.postSamlResponse(
            Base64.getEncoder().encodeToString(this.cannedResponse.getBytes()), RELAY_STATE_VAL);
    assertThat(
        "The http response was not 307 TEMPORARY REDIRECT",
        response.getStatus(),
        is(HttpStatus.SC_TEMPORARY_REDIRECT));
    assertThat(
        "Response LOCATION was " + response.getLocation() + " expected " + LOCATION,
        response.getLocation().toString(),
        equalTo(LOCATION));
  }

  @Test
  public void testPostSamlResponseNotSecure() throws Exception {
    when(httpRequest.isSecure()).thenReturn(false);
    Response response =
        assertionConsumerService.postSamlResponse(
            Base64.getEncoder().encodeToString(this.cannedResponse.getBytes()), RELAY_STATE_VAL);
    assertThat(
        "The http response was not 500 ERROR",
        response.getStatus(),
        is(HttpStatus.SC_INTERNAL_SERVER_ERROR));
  }

  @Ignore
  @Test
  public void testPostSamlResponseDoubleSignature() throws Exception {
    cannedResponse =
        Resources.toString(
            Resources.getResource(getClass(), "/DoubleSignedSAMLResponse.txt"), Charsets.UTF_8);
    String relayStateValue = "a0552c29-8b2b-492c-87fb-17a20d22f887";

    when(relayStates.encode("fubar")).thenReturn(relayStateValue);
    when(relayStates.decode(relayStateValue)).thenReturn(LOCATION);

    Response response =
        assertionConsumerService.postSamlResponse(
            new String(this.cannedResponse.getBytes()), relayStateValue);
    assertThat(
        "The http response was not 307 TEMPORARY REDIRECT",
        response.getStatus(),
        is(HttpStatus.SC_TEMPORARY_REDIRECT));
    assertThat(
        "Response LOCATION was " + response.getLocation() + " expected " + LOCATION,
        response.getLocation().toString(),
        equalTo(LOCATION));
  }

  @Test
  public void testGetSamlResponse() throws Exception {

    Response response =
        assertionConsumerService.getSamlResponse(
            deflatedSamlResponse, RELAY_STATE_VAL, SIG_ALG_VAL, SIGNATURE_VAL);
    assertThat(
        "The http response was not 307 TEMPORARY REDIRECT",
        response.getStatus(),
        is(HttpStatus.SC_TEMPORARY_REDIRECT));
    assertThat(
        "Response LOCATION was " + response.getLocation() + " expected " + LOCATION,
        response.getLocation().toString(),
        equalTo(LOCATION));
  }

  @Test
  public void testGetSamlResponseNullSamlResponse() throws Exception {
    Response response =
        assertionConsumerService.getSamlResponse(null, RELAY_STATE_VAL, SIG_ALG_VAL, SIGNATURE_VAL);
    assertThat(
        "The http response was not 500 SERVER ERROR",
        response.getStatus(),
        is(HttpStatus.SC_INTERNAL_SERVER_ERROR));
  }

  @Test
  public void testGetSamlResponseInvalidSignature() throws Exception {
    Response response =
        assertionConsumerService.getSamlResponse(
            deflatedSamlResponse, RELAY_STATE_VAL, SIG_ALG_VAL, SIGNATURE_VAL.replace('z', 'x'));
    assertThat(
        "The http response was not 500 SERVER ERROR",
        response.getStatus(),
        is(HttpStatus.SC_INTERNAL_SERVER_ERROR));
  }

  @Test
  public void testGetSamlResponseNoSignature() throws Exception {
    Response response =
        assertionConsumerService.getSamlResponse(
            deflatedSamlResponse, RELAY_STATE_VAL, SIG_ALG_VAL, null);
    assertThat(
        "The http response was not 307 TEMPORARY REDIRECT",
        response.getStatus(),
        is(HttpStatus.SC_TEMPORARY_REDIRECT));
    assertThat(
        "Response LOCATION was " + response.getLocation() + " expected " + LOCATION,
        response.getLocation().toString(),
        equalTo(LOCATION));
  }

  @Test
  public void testGetSamlResponseNoSignatureAlgorithm() throws Exception {
    Response response =
        assertionConsumerService.getSamlResponse(
            deflatedSamlResponse, RELAY_STATE_VAL, null, SIGNATURE_VAL);
    assertThat(
        "The http response was not 500 SERVER ERROR",
        response.getStatus(),
        is(HttpStatus.SC_INTERNAL_SERVER_ERROR));
  }

  @Test
  public void testProcessBadSamlResponse() throws Exception {
    String badRequest =
        Resources.toString(Resources.getResource(getClass(), "/SAMLRequest.xml"), Charsets.UTF_8);

    Response response =
        assertionConsumerService.processSamlResponse(badRequest, RELAY_STATE_VAL, true);
    assertThat(
        "The http response was not 500 SEVER ERROR",
        response.getStatus(),
        is(HttpStatus.SC_INTERNAL_SERVER_ERROR));
  }

  @Test
  public void testProcessSamlResponseAgainstLoginPage() throws Exception {
    when(relayStates.decode(RELAY_STATE_VAL)).thenReturn("https://test/login?prevurl=/newurl");

    Response response =
        assertionConsumerService.processSamlResponse(cannedResponse, RELAY_STATE_VAL, true);
    assertThat(
        "The http response was not 307 TEMPORARY REDIRECT",
        response.getStatus(),
        is(HttpStatus.SC_TEMPORARY_REDIRECT));
    assertThat(
        "The response did not redirect to the correct location.",
        response.getLocation().getPath(),
        is("/newurl"));
  }

  @Test
  public void testProcessSamlResponseAgainstLoginPage1() throws Exception {
    when(relayStates.decode(RELAY_STATE_VAL)).thenReturn("https://test/login/?prevurl=/newurl");

    Response response =
        assertionConsumerService.processSamlResponse(cannedResponse, RELAY_STATE_VAL, false);
    assertThat(
        "The http response was not 307 TEMPORARY REDIRECT",
        response.getStatus(),
        is(HttpStatus.SC_TEMPORARY_REDIRECT));
    assertThat(
        "The response did not redirect to the correct location.",
        response.getLocation().getPath(),
        is("/newurl"));
  }

  @Test
  public void testProcessSamlResponseAgainstLoginPageBadQuery() throws Exception {
    when(relayStates.decode(RELAY_STATE_VAL)).thenReturn("https://test/login?blah=/newurl");

    Response response =
        assertionConsumerService.processSamlResponse(cannedResponse, RELAY_STATE_VAL, false);
    assertThat(
        "The http response was not 307 TEMPORARY REDIRECT",
        response.getStatus(),
        is(HttpStatus.SC_TEMPORARY_REDIRECT));
    assertThat(
        "The response did not redirect to the correct location.",
        response.getLocation().getPath(),
        is("/login"));
  }

  @Test
  public void testProcessSamlResponseAuthnFailure() throws Exception {
    String failureRequest = cannedResponse.replace(StatusCode.SUCCESS, StatusCode.AUTHN_FAILED);
    Response response =
        assertionConsumerService.processSamlResponse(failureRequest, RELAY_STATE_VAL, false);
    assertThat(
        "The http response was not 500 SEVER ERROR",
        response.getStatus(),
        is(HttpStatus.SC_INTERNAL_SERVER_ERROR));
  }

  @Test
  public void testProcessSamlResponseRequestDenied() throws Exception {
    String failureRequest = cannedResponse.replace(StatusCode.SUCCESS, StatusCode.REQUEST_DENIED);
    Response response =
        assertionConsumerService.processSamlResponse(failureRequest, RELAY_STATE_VAL, false);
    assertThat(
        "The http response was not 500 SEVER ERROR",
        response.getStatus(),
        is(HttpStatus.SC_INTERNAL_SERVER_ERROR));
  }

  @Test
  public void testProcessSamlResponseRequestUnsupported() throws Exception {
    String failureRequest =
        cannedResponse.replace(StatusCode.SUCCESS, StatusCode.REQUEST_UNSUPPORTED);
    Response response =
        assertionConsumerService.processSamlResponse(failureRequest, RELAY_STATE_VAL, false);
    assertThat(
        "The http response was not 500 SEVER ERROR",
        response.getStatus(),
        is(HttpStatus.SC_INTERNAL_SERVER_ERROR));
  }

  @Test
  public void testProcessSamlResponseUnsupportedBinding() throws Exception {
    String failureRequest =
        cannedResponse.replace(StatusCode.SUCCESS, StatusCode.UNSUPPORTED_BINDING);
    Response response =
        assertionConsumerService.processSamlResponse(failureRequest, RELAY_STATE_VAL, false);
    assertThat(
        "The http response was not 500 SEVER ERROR",
        response.getStatus(),
        is(HttpStatus.SC_INTERNAL_SERVER_ERROR));
  }

  @Test
  public void testProcessSamlResponseNoAssertion() throws Exception {
    String failureRequest =
        Resources.toString(
            Resources.getResource(getClass(), "/SAMLResponse-noAssertion.xml"), Charsets.UTF_8);

    Response response =
        assertionConsumerService.processSamlResponse(failureRequest, RELAY_STATE_VAL, false);
    assertThat(
        "The http response was not 500 SEVER ERROR",
        response.getStatus(),
        is(HttpStatus.SC_INTERNAL_SERVER_ERROR));
  }

  @Test
  public void testProcessSamlResponseNullAssertion() throws Exception {
    String failureRequest =
        Resources.toString(
            Resources.getResource(getClass(), "/SAMLResponse-nullAssertion.xml"), Charsets.UTF_8);

    Response response =
        assertionConsumerService.processSamlResponse(failureRequest, RELAY_STATE_VAL, false);
    assertThat(
        "The http response was not 307 TEMPORARY REDIRECT",
        response.getStatus(),
        is(HttpStatus.SC_TEMPORARY_REDIRECT));
    assertThat(
        "Response LOCATION was " + response.getLocation() + " expected " + LOCATION,
        response.getLocation().toString(),
        equalTo(LOCATION));
  }

  @Test
  public void testProcessSamlResponseMalformedAssertion() throws Exception {
    String failureRequest =
        Resources.toString(
            Resources.getResource(getClass(), "/SAMLResponse-malformedAssertion.xml"),
            Charsets.UTF_8);

    Response response =
        assertionConsumerService.processSamlResponse(failureRequest, RELAY_STATE_VAL, false);
    assertThat(
        "The http response was not 500 SEVER ERROR",
        response.getStatus(),
        is(HttpStatus.SC_INTERNAL_SERVER_ERROR));
  }

  @Test
  public void testProcessSamlResponseMultipleAssertion() throws Exception {
    String multipleAssertions =
        Resources.toString(
            Resources.getResource(getClass(), "/SAMLResponse-multipleAssertions.xml"),
            Charsets.UTF_8);

    Response response =
        assertionConsumerService.processSamlResponse(multipleAssertions, RELAY_STATE_VAL, false);
    assertThat(
        "The http response was not 307 TEMPORARY REDIRECT",
        response.getStatus(),
        is(HttpStatus.SC_TEMPORARY_REDIRECT));
    assertThat(
        "Response LOCATION was " + response.getLocation() + " expected " + LOCATION,
        response.getLocation().toString(),
        equalTo(LOCATION));
  }

  @Test
  public void testProcessSamlResponseEmptySamlResponse() throws Exception {
    Response response = assertionConsumerService.processSamlResponse("", RELAY_STATE_VAL, false);
    assertThat(
        "The http response was not 500 SEVER ERROR",
        response.getStatus(),
        is(HttpStatus.SC_INTERNAL_SERVER_ERROR));
  }

  @Test
  public void testProcessSamlResponseLoginFail() throws Exception {
    doThrow(AuthenticationFailureException.class)
        .when(loginFilter)
        .doFilter(any(ServletRequest.class), isNull(ServletResponse.class), any(FilterChain.class));
    Response response =
        assertionConsumerService.processSamlResponse(this.cannedResponse, RELAY_STATE_VAL, false);
    assertThat(
        "The http response was not 500 SEVER ERROR",
        response.getStatus(),
        is(HttpStatus.SC_INTERNAL_SERVER_ERROR));
  }

  @Test
  public void testProcessSamlResponseEmptyRelayState() throws Exception {
    Response response =
        assertionConsumerService.processSamlResponse(this.cannedResponse, "", false);
    assertThat(
        "The http response was not 500 SEVER ERROR",
        response.getStatus(),
        is(HttpStatus.SC_INTERNAL_SERVER_ERROR));
  }

  /*
  We cannot assume the presence of the SingleLogout Service
  DDF-1605
   */
  @Ignore
  @Test
  public void testRetrieveMetadata() throws Exception {
    Response response = assertionConsumerService.retrieveMetadata();

    Document document = parse(response.getEntity().toString());
    assertThat(
        "SingleLogoutService Binding attribute was not the expected HTTP-Redirect",
        document,
        hasXPath(
            "//urn:oasis:names:tc:SAML:2.0:metadata:SingleLogoutService/@Binding",
            is(equalTo("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect"))));
    assertThat(
        "SingleLogoutService Binding attribute was not the expected HTTP-Redirect",
        document,
        hasXPath(
            "//urn:oasis:names:tc:SAML:2.0:metadata:SingleLogoutService/@Location",
            is(equalTo("https://localhost:8993/logout"))));
    assertThat("The http response was not 200 OK", response.getStatus(), is(HttpStatus.SC_OK));
    assertThat("Response entity was null", response.getEntity(), notNullValue());
  }

  private static Document parse(String xml) {
    try {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      documentBuilderFactory.setNamespaceAware(true);
      DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
      return documentBuilder.parse(new ByteArrayInputStream(xml.getBytes()));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  public void testGetLoginFilter() throws Exception {
    SecurityFilter filter = assertionConsumerService.getLoginFilter();
    assertThat(
        "Returned login filter was not the same as the one set", filter, equalTo(loginFilter));
  }
}
