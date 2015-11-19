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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.session.RelayStates;
import org.junit.Before;
import org.junit.Test;

import ddf.security.encryption.EncryptionService;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;

public class IdpHandlerTest {

    IdpHandler idpHandler;

    private RelayStates<String> relayStates;

    private SystemBaseUrl baseUrl;

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
        systemCrypto = new SystemCrypto("encryption.properties", "signature.properties",
                encryptionService);
        simpleSign = new SimpleSign(systemCrypto);
        idpMetadata = new IdpMetadata();
        baseUrl = new SystemBaseUrl();
        relayStates = (RelayStates<String>) mock(RelayStates.class);
        when(relayStates.encode(anyString())).thenReturn(RELAY_STATE_VAL);
        when(relayStates.decode(RELAY_STATE_VAL)).thenReturn(LOCATION);
        httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("https://localhost:8993"));
        httpResponse = mock(HttpServletResponse.class);

        idpHandler = new IdpHandler(simpleSign, idpMetadata, baseUrl, relayStates);

        StringWriter writer = new StringWriter();
        InputStream inputStream = this.getClass()
                .getResourceAsStream("/IDPmetadata.xml");
        IOUtils.copy(inputStream, writer, "UTF-8");
        metadata = writer.toString();
        idpMetadata.setMetadata(metadata);
    }

    @Test
    public void testGetNormalizedToken() throws Exception {
        HandlerResult handlerResult = idpHandler.getNormalizedToken(httpRequest, httpResponse, null,
                false);
        assertThat("Expected a non null handlerRequest", handlerResult,
                is(notNullValue(HandlerResult.class)));
        assertThat(handlerResult.getStatus(), equalTo(HandlerResult.Status.REDIRECTED));
    }

    @Test
    public void testGetNormalizedTokenNoRedirect() throws Exception {

        when(httpResponse.getWriter()).thenReturn(mock(PrintWriter.class));
        idpMetadata.setMetadata(metadata.replace("HTTP-Redirect", "HTTP-POST"));
        HandlerResult handlerResult = idpHandler.getNormalizedToken(httpRequest, httpResponse, null,
                false);
        assertThat("Expected a non null handlerRequest", handlerResult,
                is(notNullValue(HandlerResult.class)));
        assertThat(handlerResult.getStatus(), equalTo(HandlerResult.Status.REDIRECTED));

    }

    @Test
    public void testHandleError() throws Exception {
        HandlerResult handlerResult = idpHandler.handleError(httpRequest, httpResponse, null);
        assertThat("Expected a non null handlerRequest", handlerResult,
                is(notNullValue(HandlerResult.class)));
        assertThat(handlerResult.getStatus(), equalTo(HandlerResult.Status.NO_ACTION));

    }
}