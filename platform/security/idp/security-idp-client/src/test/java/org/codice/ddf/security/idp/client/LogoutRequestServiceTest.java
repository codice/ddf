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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;

import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.impl.LogoutRequestBuilder;
import org.opensaml.saml.saml2.core.impl.LogoutResponseBuilder;
import org.opensaml.xmlsec.signature.SignableXMLObject;

import ddf.security.SecurityConstants;
import ddf.security.common.util.SecurityTokenHolder;
import ddf.security.encryption.EncryptionService;
import ddf.security.http.SessionFactory;
import ddf.security.samlp.LogoutMessage;
import ddf.security.samlp.SamlProtocol;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.ValidationException;
import ddf.security.samlp.impl.RelayStates;

public class LogoutRequestServiceTest {

    private LogoutRequestService logoutRequestService;

    private String nameId = "nameId";

    private Long time = System.currentTimeMillis();

    private String logoutUrl = "https://www.logout.url/logout";

    private String redirectLogoutUrl = "https://www.redirectlogout.location.com/logout";

    private String postLogoutUrl = "https://www.postlogout.location.com/logout";

    private RelayStates<String> relayStates;

    private SessionFactory sessionFactory;

    private HttpServletRequest request;

    private LogoutMessage logoutMessage;

    private EncryptionService encryptionService;

    private IdpMetadata idpMetadata;

    private SystemBaseUrl systemBaseUrl;

    private SimpleSign simpleSign;

    private HttpSession session;

    private SecurityTokenHolder securityTokenHolder;

    private static final long LOGOUT_PAGE_TIMEOUT = TimeUnit.HOURS.toMillis(1);

    private class MockLogoutRequestService extends LogoutRequestService {

        public MockLogoutRequestService(SimpleSign simpleSign, IdpMetadata idpMetadata,
                SystemBaseUrl systemBaseUrl, RelayStates<String> relayStates) {
            super(simpleSign, idpMetadata, systemBaseUrl, relayStates);
        }

        @Override
        protected void buildAndValidateSaml(String samlRequest, String relayState,
                String signatureAlgorithm, String signature, SignableXMLObject xmlObject)
                throws ValidationException {
            return;
        }
    }

    @Before
    public void setup() {
        simpleSign = mock(SimpleSign.class);
        idpMetadata = mock(IdpMetadata.class);
        systemBaseUrl = new SystemBaseUrl();
        relayStates = mock(RelayStates.class);
        sessionFactory = mock(SessionFactory.class);
        request = mock(HttpServletRequest.class);
        logoutMessage = mock(LogoutMessage.class);
        encryptionService = mock(EncryptionService.class);
        session = mock(HttpSession.class);
        securityTokenHolder = mock(SecurityTokenHolder.class);

        logoutRequestService = new MockLogoutRequestService(simpleSign,
                idpMetadata,
                systemBaseUrl,
                relayStates);
        logoutRequestService.setEncryptionService(encryptionService);
        logoutRequestService.setLogOutPageTimeOut(LOGOUT_PAGE_TIMEOUT);
        logoutRequestService.setLogoutMessage(logoutMessage);
        logoutRequestService.setRequest(request);
        logoutRequestService.setSessionFactory(sessionFactory);

        logoutRequestService.init();

        when(sessionFactory.getOrCreateSession(request)).thenReturn(session);
        when(session.getAttribute(eq(SecurityConstants.SAML_ASSERTION))).thenReturn(
                securityTokenHolder);
        when(request.getRequestURL()).thenReturn(new StringBuffer("www.url.com/url"));
        when(idpMetadata.getSigningCertificate()).thenReturn("signingCertificate");
        when(idpMetadata.getSingleLogoutBinding()).thenReturn(SamlProtocol.REDIRECT_BINDING);
        when(idpMetadata.getSingleLogoutLocation()).thenReturn(redirectLogoutUrl);

    }

    @Test
    public void testSendLogoutRequestGetRedirectRequest() throws Exception {
        String encryptedNameIdWithTime = nameId + "\n" + time;
        when(encryptionService.decrypt(any(String.class))).thenReturn(nameId + "\n" + time);
        when(logoutMessage.signSamlGetRequest(any(LogoutRequest.class),
                any(URI.class),
                anyString())).thenReturn(new URI(logoutUrl));
        Response response = logoutRequestService.sendLogoutRequest(encryptedNameIdWithTime);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue("Expected logout url of " + logoutUrl,
                response.getEntity()
                        .toString()
                        .contains(logoutUrl));

    }

    @Test
    public void testSendLogoutRequestGetPostRequest() throws Exception {
        String encryptedNameIdWithTime = nameId + "\n" + time;
        when(encryptionService.decrypt(any(String.class))).thenReturn(nameId + "\n" + time);
        when(idpMetadata.getSingleLogoutBinding()).thenReturn(SamlProtocol.POST_BINDING);
        when(idpMetadata.getSingleLogoutLocation()).thenReturn(postLogoutUrl);
        LogoutRequest logoutRequest = new LogoutRequestBuilder().buildObject();
        when(logoutMessage.buildLogoutRequest(eq(nameId), anyString())).thenReturn(logoutRequest);
        Response response = logoutRequestService.sendLogoutRequest(encryptedNameIdWithTime);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue("Expected logout url of " + postLogoutUrl,
                response.getEntity()
                        .toString()
                        .contains(postLogoutUrl));

    }

    @Test
    public void testSendLogoutRequestTimeout() throws Exception {
        Long badTime = (time - TimeUnit.DAYS.toMillis(1));
        String encryptedNameIdWithTime = nameId + "\n" + badTime;
        when(encryptionService.decrypt(any(String.class))).thenReturn(nameId + "\n" + badTime);
        Response response = logoutRequestService.sendLogoutRequest(encryptedNameIdWithTime);
        assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
        String msg = String.format(
                "Logout request was older than %sms old so it was rejected. Please refresh page and request again.",
                LOGOUT_PAGE_TIMEOUT)
                .replaceAll(" ", "+");
        assertTrue("Expected message containing " + msg,
                response.getLocation()
                        .getQuery()
                        .contains(msg));

    }

    @Test
    public void testSendLogoutRequestInvalidNumberOfParams() throws Exception {
        String encryptedNameIdWithTime = nameId + "\n" + time;
        when(encryptionService.decrypt(any(String.class))).thenReturn(nameId);
        Response response = logoutRequestService.sendLogoutRequest(encryptedNameIdWithTime);
        assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
        String msg =
                "Failed to decrypt logout request params. Invalid number of params.".replaceAll(" ",
                        "+");
        assertTrue("Expected message containing " + msg,
                response.getLocation()
                        .getQuery()
                        .contains(msg));

    }

    @Test
    public void testSendLogoutRequestNoSupportedBindings() throws Exception {
        String encryptedNameIdWithTime = nameId + "\n" + time;
        when(encryptionService.decrypt(any(String.class))).thenReturn(nameId + "\n" + time);
        when(idpMetadata.getSingleLogoutBinding()).thenReturn(
                "urn:oasis:names:tc:SAML:2.0:bindings:SOAP");
        Response response = logoutRequestService.sendLogoutRequest(encryptedNameIdWithTime);
        assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
        String msg =
                "The identity provider does not support either POST or Redirect bindings.".replaceAll(
                        " ",
                        "+");
        assertTrue("Expected message containing " + msg,
                response.getLocation()
                        .getQuery()
                        .contains(msg));

    }

    @Test
    public void getPostLogoutRequest() throws Exception {
        String relayState = UUID.randomUUID()
                .toString();
        String encodedSamlRequest = "encodedSamlRequest";
        String issuerStr = "issuer";
        LogoutRequest logoutRequest = mock(LogoutRequest.class);
        Issuer issuer = mock(Issuer.class);
        OpenSAMLUtil.initSamlEngine();
        LogoutResponse logoutResponse = new LogoutResponseBuilder().buildObject();
        when(logoutMessage.extractSamlLogoutRequest(any(String.class))).thenReturn(logoutRequest);
        when(logoutRequest.getIssuer()).thenReturn(issuer);
        when(logoutRequest.getIssueInstant()).thenReturn(new DateTime());
        when(logoutRequest.getVersion()).thenReturn(SAMLVersion.VERSION_20);
        when(logoutRequest.getID()).thenReturn("id");
        when(issuer.getValue()).thenReturn(issuerStr);
        when(logoutMessage.buildLogoutResponse(eq(issuerStr),
                eq(StatusCode.SUCCESS),
                anyString())).thenReturn(logoutResponse);
        when(idpMetadata.getSingleLogoutBinding()).thenReturn(SamlProtocol.POST_BINDING);
        when(idpMetadata.getSingleLogoutLocation()).thenReturn(postLogoutUrl);
        Response response = logoutRequestService.postLogoutRequest(encodedSamlRequest,
                null,
                relayState);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue("Expected logout url of " + postLogoutUrl,
                response.getEntity()
                        .toString()
                        .contains(postLogoutUrl));
    }

    @Test
    public void getPostLogoutRequestNotParsable() throws Exception {
        String relayState = UUID.randomUUID()
                .toString();
        String encodedSamlRequest = "encodedSamlRequest";
        Response response = logoutRequestService.postLogoutRequest(encodedSamlRequest,
                null,
                relayState);
        assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
        String msg = "Unable to parse logout request.".replaceAll(" ", "+");
        assertTrue("Expected message containing " + msg,
                response.getLocation()
                        .getQuery()
                        .contains(msg));
    }

    @Test
    public void testPostLogoutRequestResponse() throws Exception {
        String relayState = UUID.randomUUID()
                .toString();
        String encodedSamlResponse = "encodedSamlRequest";
        String issuerStr = "issuer";
        Issuer issuer = mock(Issuer.class);
        LogoutResponse logoutResponse = mock(LogoutResponse.class);
        logoutResponse.setIssuer(issuer);
        when(logoutMessage.extractSamlLogoutResponse(any(String.class))).thenReturn(logoutResponse);
        when(request.getRequestURL()).thenReturn(new StringBuffer("www.url.com/url"));
        when(logoutResponse.getIssuer()).thenReturn(issuer);
        when(logoutResponse.getIssueInstant()).thenReturn(new DateTime());
        when(logoutResponse.getVersion()).thenReturn(SAMLVersion.VERSION_20);
        when(logoutResponse.getID()).thenReturn("id");
        when(issuer.getValue()).thenReturn(issuerStr);
        when(idpMetadata.getSingleLogoutBinding()).thenReturn(SamlProtocol.POST_BINDING);
        when(idpMetadata.getSingleLogoutLocation()).thenReturn(postLogoutUrl);
        Response response = logoutRequestService.postLogoutRequest(null,
                encodedSamlResponse,
                relayState);
        assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
        assertTrue("Expected a successful logout message",
                response.getLocation()
                        .toString()
                        .contains("logged+out+successfully."));

    }

    @Test
    public void testPostLogoutRequestResponseNotParsable() throws Exception {
        String relayState = UUID.randomUUID()
                .toString();
        String encodedSamlResponse = "encodedSamlRequest";
        when(logoutMessage.extractSamlLogoutResponse(any(String.class))).thenReturn(null);
        Response response = logoutRequestService.postLogoutRequest(null,
                encodedSamlResponse,
                relayState);
        assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
        String msg = "Unable to parse logout response.".replaceAll(" ", "+");
        assertTrue("Expected message containing " + msg,
                response.getLocation()
                        .getQuery()
                        .contains(msg));

    }

    @Test
    public void testGetLogoutRequest() throws Exception {
        String signature = "signature";
        String signatureAlgorithm = "sha1";
        String relayState = UUID.randomUUID()
                .toString();
        String deflatedSamlRequest = RestSecurity.deflateAndBase64Encode("deflatedSamlRequest");
        LogoutRequest logoutRequest = mock(LogoutRequest.class);
        when(logoutMessage.extractSamlLogoutRequest(eq("deflatedSamlRequest"))).thenReturn(
                logoutRequest);
        when(logoutMessage.signSamlGetResponse(any(LogoutRequest.class),
                any(URI.class),
                anyString())).thenReturn(new URI(redirectLogoutUrl));
        Response response = logoutRequestService.getLogoutRequest(deflatedSamlRequest,
                null,
                relayState,
                signatureAlgorithm,
                signature);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue("Expected logout url of " + redirectLogoutUrl,
                response.getEntity()
                        .toString()
                        .contains(redirectLogoutUrl));
    }

    @Test
    public void testGetLogoutRequestNotParsable() throws Exception {
        String signature = "signature";
        String signatureAlgorithm = "sha1";
        String relayState = UUID.randomUUID()
                .toString();
        String deflatedSamlRequest = RestSecurity.deflateAndBase64Encode("deflatedSamlRequest");
        when(logoutMessage.extractSamlLogoutRequest(eq("deflatedSamlRequest"))).thenReturn(null);
        Response response = logoutRequestService.getLogoutRequest(deflatedSamlRequest,
                null,
                relayState,
                signatureAlgorithm,
                signature);
        assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
        String msg = "Unable to parse logout request.".replaceAll(" ", "+");
        assertTrue("Expected message containing " + msg,
                response.getLocation()
                        .getQuery()
                        .contains(msg));
    }

    @Test
    public void testGetLogoutRequestInvalidSignature() throws Exception {
        String signature = "signature";
        String signatureAlgorithm = "sha1";
        String relayState = UUID.randomUUID()
                .toString();
        String deflatedSamlRequest = RestSecurity.deflateAndBase64Encode("deflatedSamlRequest");
        LogoutRequest logoutRequest = mock(LogoutRequest.class);
        when(logoutMessage.extractSamlLogoutRequest(eq("deflatedSamlRequest"))).thenReturn(
                logoutRequest);

        LogoutRequestService lrs = new LogoutRequestService(simpleSign,
                idpMetadata,
                systemBaseUrl,
                relayStates);

        lrs.setEncryptionService(encryptionService);
        lrs.setLogOutPageTimeOut(LOGOUT_PAGE_TIMEOUT);
        lrs.setLogoutMessage(logoutMessage);
        lrs.setRequest(request);
        lrs.setSessionFactory(sessionFactory);
        lrs.init();
        Response response = lrs.getLogoutRequest(deflatedSamlRequest,
                null,
                relayState,
                signatureAlgorithm,
                signature);
        assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
        String msg = "Unable to validate".replaceAll(" ", "+");
        assertTrue("Expected message containing " + msg,
                response.getLocation()
                        .getQuery()
                        .contains(msg));
    }

    @Test
    public void testGetLogoutRequestResponse() throws Exception {
        String signature = "signature";
        String signatureAlgorithm = "sha1";
        String relayState = UUID.randomUUID()
                .toString();
        String deflatedSamlResponse = RestSecurity.deflateAndBase64Encode("deflatedSamlResponse");
        LogoutResponse logoutResponse = mock(LogoutResponse.class);
        when(logoutResponse.getIssueInstant()).thenReturn(new DateTime());
        when(logoutResponse.getVersion()).thenReturn(SAMLVersion.VERSION_20);
        when(logoutResponse.getID()).thenReturn("id");
        when(logoutMessage.extractSamlLogoutResponse(eq("deflatedSamlResponse"))).thenReturn(
                logoutResponse);
        Response response = logoutRequestService.getLogoutRequest(null,
                deflatedSamlResponse,
                relayState,
                signatureAlgorithm,
                signature);
        assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
        assertTrue("Expected a successful logout message",
                response.getLocation()
                        .toString()
                        .contains("logged+out+successfully."));
    }

    @Test
    public void testGetLogoutRequestResponseNotParsable() throws Exception {
        String signature = "signature";
        String signatureAlgorithm = "sha1";
        String relayState = UUID.randomUUID()
                .toString();
        String deflatedSamlResponse = RestSecurity.deflateAndBase64Encode("deflatedSamlResponse");
        when(logoutMessage.extractSamlLogoutResponse(eq("deflatedSamlResponse"))).thenReturn(null);
        Response response = logoutRequestService.getLogoutRequest(null,
                deflatedSamlResponse,
                relayState,
                signatureAlgorithm,
                signature);
        assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
        String msg = "Unable to parse logout response.".replaceAll(" ", "+");
        assertTrue("Expected message containing " + msg,
                response.getLocation()
                        .getQuery()
                        .contains(msg));
    }

    @Test
    public void testGetLogoutRequestResponseInvalidSignature() throws Exception {
        String signature = "signature";
        String signatureAlgorithm = "sha1";
        String relayState = UUID.randomUUID()
                .toString();
        String deflatedSamlResponse = RestSecurity.deflateAndBase64Encode("deflatedSamlResponse");
        LogoutResponse logoutResponse = mock(LogoutResponse.class);
        when(logoutMessage.extractSamlLogoutResponse(eq("deflatedSamlResponse"))).thenReturn(
                logoutResponse);
        LogoutRequestService lrs = new LogoutRequestService(simpleSign,
                idpMetadata,
                systemBaseUrl,
                relayStates);

        lrs.setEncryptionService(encryptionService);
        lrs.setLogOutPageTimeOut(LOGOUT_PAGE_TIMEOUT);
        lrs.setLogoutMessage(logoutMessage);
        lrs.setRequest(request);
        lrs.setSessionFactory(sessionFactory);
        lrs.init();
        Response response = lrs.getLogoutRequest(null,
                deflatedSamlResponse,
                relayState,
                signatureAlgorithm,
                signature);
        assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
        String msg = "Unable to validate".replaceAll(" ", "+");
        assertTrue("Expected message containing " + msg,
                response.getLocation()
                        .getQuery()
                        .contains(msg));
    }
}