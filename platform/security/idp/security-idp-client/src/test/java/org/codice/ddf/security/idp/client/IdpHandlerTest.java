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

import static org.codice.ddf.security.idp.client.IdpHandler.ECP_NS;
import static org.codice.ddf.security.idp.client.IdpHandler.HTTPS;
import static org.codice.ddf.security.idp.client.IdpHandler.PAOS;
import static org.codice.ddf.security.idp.client.IdpHandler.PAOS_MIME;
import static org.codice.ddf.security.idp.client.IdpHandler.PAOS_NS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.security.encryption.EncryptionService;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;
import ddf.security.samlp.impl.RelayStates;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.HandlerResult.Status;
import org.junit.Before;
import org.junit.Test;

public class IdpHandlerTest {

  public static final String BROWSER_USER_AGENT =
      "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko; compatible; Googlebot/2.1; +http://www.google.com/bot.html) Safari/537.36.";

  IdpHandler idpHandler;

  private RelayStates<String> relayStates;

  private IdpMetadata idpMetadata;

  private SimpleSign simpleSign;

  private EncryptionService encryptionService;

  private SystemCrypto systemCrypto;

  private HttpServletRequest httpRequest;

  private HttpServletResponse httpResponse;

  private String metadata;

  private static final String RELAY_STATE_VAL = "b0b4e449-7f69-413f-a844-61fe2256de19";

  private static final String LOCATION = "test";

  @Before
  public void setUp() throws Exception {
    encryptionService = mock(EncryptionService.class);
    systemCrypto =
        new SystemCrypto("encryption.properties", "signature.properties", encryptionService);
    simpleSign = new SimpleSign(systemCrypto);
    idpMetadata = new IdpMetadata();
    relayStates = (RelayStates<String>) mock(RelayStates.class);
    when(relayStates.encode(anyString())).thenReturn(RELAY_STATE_VAL);
    when(relayStates.decode(RELAY_STATE_VAL)).thenReturn(LOCATION);
    httpRequest = mock(HttpServletRequest.class);
    when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("https://localhost:8993"));
    when(httpRequest.getMethod()).thenReturn("GET");
    httpResponse = mock(HttpServletResponse.class);

    idpHandler = new IdpHandler(simpleSign, idpMetadata, relayStates);
    idpHandler.setAuthContextClasses(
        Arrays.asList(
            "urn:oasis:names:tc:SAML:2.0:ac:classes:Password",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:X509",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:TLSClient"));

    StringWriter writer = new StringWriter();
    InputStream inputStream = this.getClass().getResourceAsStream("/IDPmetadata.xml");
    IOUtils.copy(inputStream, writer, "UTF-8");
    metadata = writer.toString();
    idpMetadata.setMetadata(metadata);
  }

  @Test
  public void testGetNormalizedToken() throws Exception {
    when(httpRequest.getHeader("User-Agent")).thenReturn(BROWSER_USER_AGENT);
    HandlerResult handlerResult =
        idpHandler.getNormalizedToken(httpRequest, httpResponse, null, false);
    assertThat(
        "Expected a non null handlerRequest", handlerResult, is(notNullValue(HandlerResult.class)));
    assertThat(handlerResult.getStatus(), equalTo(HandlerResult.Status.REDIRECTED));
  }

  @Test
  public void testGetNormalizedTokenNoRedirect() throws Exception {
    when(httpRequest.getHeader("User-Agent")).thenReturn(BROWSER_USER_AGENT);
    when(httpResponse.getWriter()).thenReturn(mock(PrintWriter.class));
    idpMetadata.setMetadata(metadata.replace("HTTP-Redirect", "HTTP-POST"));
    HandlerResult handlerResult =
        idpHandler.getNormalizedToken(httpRequest, httpResponse, null, false);
    assertThat(
        "Expected a non null handlerRequest", handlerResult, is(notNullValue(HandlerResult.class)));
    assertThat(handlerResult.getStatus(), equalTo(HandlerResult.Status.REDIRECTED));
  }

  @Test
  public void testHandleError() throws Exception {
    when(httpRequest.getHeader("User-Agent")).thenReturn(BROWSER_USER_AGENT);
    HandlerResult handlerResult = idpHandler.handleError(httpRequest, httpResponse, null);
    assertThat(
        "Expected a non null handlerRequest", handlerResult, is(notNullValue(HandlerResult.class)));
    assertThat(handlerResult.getStatus(), equalTo(HandlerResult.Status.NO_ACTION));
  }

  @Test
  public void testGetNormalizedTokenShouldUseECP() throws Exception {
    when(httpRequest.getHeader(HttpHeaders.ACCEPT)).thenReturn(PAOS_MIME);
    when(httpRequest.getHeader(PAOS)).thenReturn(PAOS_NS + ECP_NS);
    when(httpRequest.getScheme()).thenReturn(HTTPS);
    when(httpResponse.getOutputStream()).thenReturn(mock(ServletOutputStream.class));
    HandlerResult handlerResult =
        idpHandler.getNormalizedToken(httpRequest, httpResponse, null, false);
    assertThat(
        "Expected a non null handlerRequest", handlerResult, is(notNullValue(HandlerResult.class)));
    assertThat(handlerResult.getStatus(), equalTo(HandlerResult.Status.REDIRECTED));
  }

  @Test
  public void testGetNormalizedTokenWithNoTlsShouldNotUseECP() throws Exception {
    when(httpRequest.getHeader(HttpHeaders.ACCEPT)).thenReturn(PAOS_MIME);
    when(httpRequest.getHeader(PAOS)).thenReturn(PAOS_NS + ECP_NS);
    when(httpRequest.getScheme()).thenReturn("http");
    when(httpResponse.getOutputStream()).thenReturn(mock(ServletOutputStream.class));
    HandlerResult handlerResult =
        idpHandler.getNormalizedToken(httpRequest, httpResponse, null, false);
    assertThat(
        "Expected a non null handlerRequest", handlerResult, is(notNullValue(HandlerResult.class)));
    assertThat(handlerResult.getStatus(), equalTo(Status.NO_ACTION));
  }

  @Test
  public void testGetNormalizedTokenLegacyClient() throws Exception {
    HandlerResult handlerResult =
        idpHandler.getNormalizedToken(httpRequest, httpResponse, null, false);
    assertThat(
        "Expected a non null handlerRequest", handlerResult, is(notNullValue(HandlerResult.class)));
    assertThat(handlerResult.getStatus(), equalTo(HandlerResult.Status.NO_ACTION));
  }
}
