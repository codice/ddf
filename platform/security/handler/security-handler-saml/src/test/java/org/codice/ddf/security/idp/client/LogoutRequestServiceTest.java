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

import static org.codice.ddf.security.idp.client.LogoutRequestService.UNABLE_TO_PARSE_LOGOUT_REQUEST;
import static org.codice.ddf.security.idp.client.LogoutRequestService.UNABLE_TO_VALIDATE_LOGOUT_REQUEST;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.security.SecurityConstants;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.common.PrincipalHolder;
import ddf.security.encryption.EncryptionService;
import ddf.security.http.SessionFactory;
import ddf.security.samlp.LogoutSecurityException;
import ddf.security.samlp.LogoutWrapper;
import ddf.security.samlp.SignatureException;
import ddf.security.samlp.impl.LogoutMessageImpl;
import ddf.security.samlp.impl.LogoutWrapperImpl;
import ddf.security.samlp.impl.RelayStates;
import ddf.security.samlp.impl.SamlProtocol;
import ddf.security.samlp.impl.SimpleSign;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.codice.ddf.platform.session.api.HttpSessionInvalidator;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.codice.ddf.security.jaxrs.impl.SamlSecurity;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.SessionIndex;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.impl.LogoutRequestBuilder;
import org.opensaml.saml.saml2.core.impl.LogoutResponseBuilder;
import org.opensaml.xmlsec.signature.Signature;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class LogoutRequestServiceTest {

  private static final String UNENCODED_SAML_RESPONSE = "TEST-SAML-RESPONSE";
  private static final String UNENCODED_SAML_REQUEST = "TEST-SAML-REQUEST";
  private static final String SIGNATURE = "signature";
  private static final String SIGNATURE_ALGORITHM = "sha1";
  private static final long LOGOUT_PAGE_TIMEOUT = TimeUnit.HOURS.toMillis(1);
  private static final String SESSION_INDEX = "123";
  private static final String ID = "TEST-ID";
  private LogoutRequestService logoutRequestService;
  private final String nameId = "nameId";
  private final Long time = System.currentTimeMillis();
  private final String redirectLogoutUrl = "https://www.redirectlogout.location.com/logout";
  private final String postLogoutUrl = "https://www.postlogout.location.com/logout";
  private RelayStates<String> relayStates;
  private SessionFactory sessionFactory;
  private HttpServletRequest request;
  private LogoutMessageImpl logoutMessage;
  private EncryptionService encryptionService;
  private IdpMetadata idpMetadata;
  private SimpleSign simpleSign;
  private final String relayState = UUID.randomUUID().toString();

  public static Document readSamlAssertion()
      throws SAXException, IOException, ParserConfigurationException {
    return getDocument("/SAMLAssertion.xml");
  }

  private static Document getDocument(String resourceName)
      throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    dbf.setValidating(false);
    dbf.setIgnoringComments(false);
    dbf.setIgnoringElementContentWhitespace(true);
    dbf.setNamespaceAware(true);

    DocumentBuilder db = dbf.newDocumentBuilder();
    db.setEntityResolver(new DOMUtils.NullResolver());

    return db.parse(LogoutRequestServiceTest.class.getResourceAsStream(resourceName));
  }

  @Before
  public void setup() throws ParserConfigurationException, SAXException, IOException {
    simpleSign = mock(SimpleSign.class);
    idpMetadata = mock(IdpMetadata.class);
    relayStates = mock(RelayStates.class);
    sessionFactory = mock(SessionFactory.class);
    request = mock(HttpServletRequest.class);
    logoutMessage = mock(LogoutMessageImpl.class);
    UuidGenerator uuidGenerator = mock(UuidGenerator.class);
    doReturn(UUID.randomUUID().toString()).when(uuidGenerator).generateUuid();
    doReturn(uuidGenerator).when(logoutMessage).getUuidGenerator();
    encryptionService = mock(EncryptionService.class);
    Element issuedAssertion = readSamlAssertion().getDocumentElement();
    SimplePrincipalCollection principalCollection = new SimplePrincipalCollection();
    SecurityAssertion securityAssertion = mock(SecurityAssertion.class);
    principalCollection.add(securityAssertion, "default");
    when(securityAssertion.getToken()).thenReturn(issuedAssertion);
    PrincipalHolder principalHolder = mock(PrincipalHolder.class);
    when(principalHolder.getPrincipals()).thenReturn(principalCollection);
    initializeLogoutRequestService();
    HttpSession session = mock(HttpSession.class);
    when(sessionFactory.getOrCreateSession(request)).thenReturn(session);
    when(session.getAttribute(eq(SecurityConstants.SECURITY_TOKEN_KEY)))
        .thenReturn(principalHolder);
    when(request.getRequestURL()).thenReturn(new StringBuffer("https://www.url.com/url"));
    when(idpMetadata.getSigningCertificate()).thenReturn("signingCertificate");
    when(idpMetadata.getSingleLogoutBinding()).thenReturn(SamlProtocol.REDIRECT_BINDING);
    when(idpMetadata.getSingleLogoutLocation()).thenReturn(redirectLogoutUrl);
    System.setProperty("security.audit.roles", "none");
  }

  private void initializeLogoutRequestService() {
    logoutRequestService = new LogoutRequestService(simpleSign, idpMetadata, relayStates);
    logoutRequestService.setEncryptionService(encryptionService);
    logoutRequestService.setLogOutPageTimeOut(LOGOUT_PAGE_TIMEOUT);
    logoutRequestService.setRequest(request);
    logoutRequestService.setSessionFactory(sessionFactory);
    logoutRequestService.setSamlSecurity(new SamlSecurity());
    logoutRequestService.init();
  }

  @Test
  public void testSendLogoutRequestGetRedirectRequest() throws Exception {
    String encryptedNameIdWithTime = nameId + "\n" + time;
    when(encryptionService.decrypt(any(String.class))).thenReturn(nameId + "\n" + time);
    LogoutRequest logoutRequest = new LogoutRequestBuilder().buildObject();
    LogoutWrapper<LogoutRequest> requestLogoutWrapper = new LogoutWrapperImpl<>(logoutRequest);
    when(logoutMessage.buildLogoutRequest(eq(nameId), anyString(), anyList()))
        .thenReturn(requestLogoutWrapper);
    String logoutUrl = "https://www.logout.url/logout";
    when(logoutMessage.signSamlGetRequest(any(LogoutWrapper.class), any(URI.class), anyString()))
        .thenReturn(new URI(logoutUrl));
    logoutRequestService.setLogoutMessage(logoutMessage);
    when(relayStates.encode(nameId)).thenReturn("token");
    Response response = logoutRequestService.sendLogoutRequest(encryptedNameIdWithTime);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertTrue(
        "Expected logout url of " + logoutUrl, response.getEntity().toString().contains(logoutUrl));
  }

  @Test
  public void testSendLogoutRequestGetPostRequest() {
    String encryptedNameIdWithTime = nameId + "\n" + time;
    when(encryptionService.decrypt(any(String.class))).thenReturn(nameId + "\n" + time);
    when(idpMetadata.getSingleLogoutBinding()).thenReturn(SamlProtocol.POST_BINDING);
    when(idpMetadata.getSingleLogoutLocation()).thenReturn(postLogoutUrl);
    LogoutRequest logoutRequest = new LogoutRequestBuilder().buildObject();
    LogoutWrapper<LogoutRequest> requestLogoutWrapper = new LogoutWrapperImpl<>(logoutRequest);
    when(logoutMessage.buildLogoutRequest(eq(nameId), anyString(), anyList()))
        .thenReturn(requestLogoutWrapper);
    logoutRequestService.setLogoutMessage(logoutMessage);
    Response response = logoutRequestService.sendLogoutRequest(encryptedNameIdWithTime);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertTrue(
        "Expected logout url of " + postLogoutUrl,
        response.getEntity().toString().contains(postLogoutUrl));
  }

  @Test
  public void testSendLogoutRequestTimeout() {
    long badTime = (time - TimeUnit.DAYS.toMillis(1));
    String encryptedNameIdWithTime = nameId + "\n" + badTime;
    when(encryptionService.decrypt(any(String.class))).thenReturn(nameId + "\n" + badTime);
    logoutRequestService.setLogoutMessage(logoutMessage);
    Response response = logoutRequestService.sendLogoutRequest(encryptedNameIdWithTime);
    assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
    String msg =
        String.format(
                "Logout request was older than %sms old so it was rejected. Please refresh page and request again.",
                LOGOUT_PAGE_TIMEOUT)
            .replaceAll(" ", "+");
    assertTrue(
        "Expected message containing " + msg, response.getLocation().getQuery().contains(msg));
  }

  @Test
  public void testSendLogoutRequestInvalidNumberOfParams() throws Exception {
    String encryptedNameIdWithTime = nameId + "\n" + time;
    when(encryptionService.decrypt(any(String.class))).thenReturn(nameId);
    LogoutRequest logoutRequest = mock(LogoutRequest.class);
    when(logoutRequest.getIssueInstant()).thenReturn(DateTime.now());
    SessionIndex sessionIndex = mock(SessionIndex.class);
    when(sessionIndex.getSessionIndex()).thenReturn(SESSION_INDEX);
    when(logoutRequest.getSessionIndexes()).thenReturn(Collections.singletonList(sessionIndex));
    logoutRequestService.setLogoutMessage(logoutMessage);
    Response response = logoutRequestService.sendLogoutRequest(encryptedNameIdWithTime);
    assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
    insertLogoutRequest();
    String msg = LogoutRequestService.UNABLE_TO_DECRYPT_LOGOUT_REQUEST.replaceAll(" ", "+");
    assertTrue(
        "Expected message containing " + msg, response.getLocation().getQuery().contains(msg));
  }

  @Test
  public void testSendLogoutRequestNoSupportedBindings() {
    String encryptedNameIdWithTime = nameId + "\n" + time;
    when(encryptionService.decrypt(any(String.class))).thenReturn(nameId + "\n" + time);
    when(idpMetadata.getSingleLogoutBinding())
        .thenReturn("urn:oasis:names:tc:SAML:2.0:bindings:SOAP");
    logoutRequestService.setLogoutMessage(logoutMessage);
    Response response = logoutRequestService.sendLogoutRequest(encryptedNameIdWithTime);
    assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
    String msg =
        "The identity provider does not support either POST or Redirect bindings."
            .replaceAll(" ", "+");
    assertTrue(
        "Expected message containing " + msg, response.getLocation().getQuery().contains(msg));
  }

  @Test
  public void testSendLogoutRequestNullLogoutMessage() {
    final String encryptedNameIdTime = nameId + "\n" + time;
    when(encryptionService.decrypt(any(String.class))).thenReturn(encryptedNameIdTime);
    final Response response = logoutRequestService.sendLogoutRequest(encryptedNameIdTime);
    assertThat(response.getStatus(), is(Response.Status.SEE_OTHER.getStatusCode()));
    final String msg = "Failed to create logout request".replaceAll(" ", "+");
    assertThat(response.getLocation().getQuery(), containsString(msg));
  }

  @Test
  public void testPostLogoutRequest() throws Exception {
    String encodedSamlRequest = "encodedSamlRequest";
    String issuerStr = "issuer";
    LogoutRequest logoutRequest = mock(LogoutRequest.class);
    when(logoutRequest.getIssueInstant()).thenReturn(DateTime.now());
    SessionIndex sessionIndex = mock(SessionIndex.class);
    when(sessionIndex.getSessionIndex()).thenReturn(SESSION_INDEX);
    when(logoutRequest.getSessionIndexes()).thenReturn(Collections.singletonList(sessionIndex));
    LogoutWrapper<LogoutRequest> requestLogoutWrapper = new LogoutWrapperImpl<>(logoutRequest);
    when(logoutMessage.extractSamlLogoutRequest(any(String.class)))
        .thenReturn(requestLogoutWrapper);
    Issuer issuer = mock(Issuer.class);
    OpenSAMLUtil.initSamlEngine();
    LogoutResponse logoutResponse = new LogoutResponseBuilder().buildObject();
    when(logoutRequest.getIssuer()).thenReturn(issuer);
    when(logoutRequest.getIssueInstant()).thenReturn(new DateTime());
    when(logoutRequest.getVersion()).thenReturn(SAMLVersion.VERSION_20);
    when(logoutRequest.getID()).thenReturn("id");
    when(issuer.getValue()).thenReturn(issuerStr);
    LogoutWrapper<LogoutResponse> responseLogoutWrapper = new LogoutWrapperImpl<>(logoutResponse);
    when(logoutMessage.buildLogoutResponse(eq(issuerStr), eq(StatusCode.SUCCESS), anyString()))
        .thenReturn(responseLogoutWrapper);
    logoutRequestService.setLogoutMessage(logoutMessage);
    when(idpMetadata.getSingleLogoutBinding()).thenReturn(SamlProtocol.POST_BINDING);
    when(idpMetadata.getSingleLogoutLocation()).thenReturn(postLogoutUrl);
    Response response =
        logoutRequestService.postLogoutRequest(encodedSamlRequest, null, relayState);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertTrue(
        "Expected logout url of " + postLogoutUrl,
        response.getEntity().toString().contains(postLogoutUrl));
  }

  @Test
  public void testSoapLogoutRequestIssueInstantOld() {
    HttpSessionInvalidator httpSessionInvalidator = mock(HttpSessionInvalidator.class);
    logoutRequestService.setHttpSessionInvalidator(httpSessionInvalidator);
    logoutRequestService.setLogoutMessage(logoutMessage);

    InputStream requestStream =
        LogoutRequestServiceTest.class.getResourceAsStream("/SAMLSoapLogoutRequest-good.xml");
    Response response = logoutRequestService.soapLogoutRequest(requestStream, null);
    assertThat(response.getStatus(), is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    verify(httpSessionInvalidator, times(0)).invalidateSession(anyString(), any());
  }

  @Test
  public void testSoapLogoutRequestBadSignature() throws Exception {
    HttpSessionInvalidator httpSessionInvalidator = mock(HttpSessionInvalidator.class);
    logoutRequestService.setHttpSessionInvalidator(httpSessionInvalidator);

    LogoutResponse logoutResponse = mock(LogoutResponse.class);
    LogoutWrapper logoutResponseWrapper = mock(LogoutWrapper.class);
    doReturn(logoutResponse).when(logoutResponseWrapper).getMessage();
    doReturn(logoutResponseWrapper)
        .when(logoutMessage)
        .buildLogoutResponse(anyString(), anyString(), anyString());
    logoutRequestService.setLogoutMessage(logoutMessage);
    doThrow(SignatureException.class)
        .when(simpleSign)
        .validateSignature(any(Signature.class), any(Document.class));

    InputStream requestStream =
        LogoutRequestServiceTest.class.getResourceAsStream("/SAMLSoapLogoutRequest-good.xml");
    Response response = logoutRequestService.soapLogoutRequest(requestStream, null);
    assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
    verify(httpSessionInvalidator, times(0)).invalidateSession(anyString(), any());
  }

  @Test
  public void testSoapLogoutRequestNotALogout() {
    HttpSessionInvalidator httpSessionInvalidator = mock(HttpSessionInvalidator.class);
    logoutRequestService.setHttpSessionInvalidator(httpSessionInvalidator);
    logoutRequestService.setLogoutMessage(logoutMessage);

    InputStream requestStream =
        LogoutRequestServiceTest.class.getResourceAsStream("/SAMLSoapLogoutRequest-bad.xml");
    Response response = logoutRequestService.soapLogoutRequest(requestStream, null);
    assertThat(response.getStatus(), is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    verify(httpSessionInvalidator, times(0)).invalidateSession(anyString(), any());
  }

  @Test
  public void testSoapLogoutRequestNullLogoutMessage() throws Exception {
    try (final InputStream requestStream =
            LogoutRequestServiceTest.class.getResourceAsStream("/SAMLSoapLogoutRequest-good.xml");
        final Response response = logoutRequestService.soapLogoutRequest(requestStream, null)) {
      assertThat(response.getStatus(), is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }
  }

  @Test
  public void testPostLogoutRequestNotParsable() throws Exception {
    String encodedSamlRequest = "encodedSamlRequest";
    LogoutRequest logoutRequest = mock(LogoutRequest.class);
    when(logoutRequest.getIssueInstant()).thenReturn(DateTime.now());
    SessionIndex sessionIndex = mock(SessionIndex.class);
    when(sessionIndex.getSessionIndex()).thenReturn(SESSION_INDEX);
    when(logoutRequest.getSessionIndexes()).thenReturn(Collections.singletonList(sessionIndex));
    insertLogoutRequest();
    logoutRequestService.setLogoutMessage(logoutMessage);
    Response response =
        logoutRequestService.postLogoutRequest(encodedSamlRequest, null, relayState);
    assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
    String msg = UNABLE_TO_PARSE_LOGOUT_REQUEST.replaceAll(" ", "+");
    assertTrue(
        "Expected message containing " + msg, response.getLocation().getQuery().contains(msg));
  }

  @Test
  public void testPostLogoutRequestNullRequestAndResponse() {
    logoutRequestService.setLogoutMessage(logoutMessage);
    try (final Response response = logoutRequestService.postLogoutRequest(null, null, relayState)) {
      assertThat(response.getStatus(), is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
      assertThat(
          response.getEntity().toString(),
          containsString("System did not receive a SAMLRequest or a SAMLResponse to process"));
    }
  }

  @Test
  public void testPostLogoutRequestResponse() throws Exception {
    String encodedSamlResponse = "encodedSamlRequest";
    String issuerStr = "issuer";
    Issuer issuer = mock(Issuer.class);
    LogoutResponse logoutResponse = mock(LogoutResponse.class);
    logoutResponse.setIssuer(issuer);
    LogoutWrapper<LogoutResponse> responseLogoutWrapper = new LogoutWrapperImpl<>(logoutResponse);
    when(logoutMessage.extractSamlLogoutResponse(any(String.class)))
        .thenReturn(responseLogoutWrapper);
    logoutRequestService.setLogoutMessage(logoutMessage);
    when(logoutResponse.getIssuer()).thenReturn(issuer);
    when(logoutResponse.getIssueInstant()).thenReturn(new DateTime());
    when(logoutResponse.getVersion()).thenReturn(SAMLVersion.VERSION_20);
    when(logoutResponse.getID()).thenReturn("id");
    when(issuer.getValue()).thenReturn(issuerStr);
    when(idpMetadata.getSingleLogoutBinding()).thenReturn(SamlProtocol.POST_BINDING);
    when(idpMetadata.getSingleLogoutLocation()).thenReturn(postLogoutUrl);
    Response response =
        logoutRequestService.postLogoutRequest(null, encodedSamlResponse, relayState);
    assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
    assertTrue(
        "Expected a successful logout message",
        response.getLocation().toString().contains("logged+out+successfully."));
  }

  @Test
  public void testPostLogoutRequestResponseNotParsable() throws Exception {
    LogoutRequest logoutRequest = mock(LogoutRequest.class);
    when(logoutRequest.getIssueInstant()).thenReturn(DateTime.now());
    SessionIndex sessionIndex = mock(SessionIndex.class);
    when(sessionIndex.getSessionIndex()).thenReturn(SESSION_INDEX);
    when(logoutRequest.getSessionIndexes()).thenReturn(Collections.singletonList(sessionIndex));
    String encodedSamlResponse = "encodedSamlRequest";
    when(logoutMessage.extractSamlLogoutResponse(any(String.class))).thenReturn(null);
    logoutRequestService.setLogoutMessage(logoutMessage);
    insertLogoutRequest();
    Response response =
        logoutRequestService.postLogoutRequest(null, encodedSamlResponse, relayState);
    assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
    String msg = LogoutRequestService.UNABLE_TO_PARSE_LOGOUT_RESPONSE.replaceAll(" ", "+");
    assertTrue(
        "Expected message containing " + msg, response.getLocation().getQuery().contains(msg));
  }

  @Test
  public void testGetLogoutRequest() throws Exception {
    SamlSecurity samlSecurity = new SamlSecurity();
    String deflatedSamlRequest = samlSecurity.deflateAndBase64Encode(UNENCODED_SAML_REQUEST);
    doReturn(true).when(simpleSign).validateSignature(anyString(), anyString(), anyString(), any());
    initializeLogoutRequestService();
    insertLogoutRequest();
    LogoutResponse logoutResponse = mock(LogoutResponse.class);
    LogoutWrapper<LogoutResponse> responseLogoutWrapper = new LogoutWrapperImpl<>(logoutResponse);
    when(logoutMessage.buildLogoutResponse(anyString(), anyString(), anyString()))
        .thenReturn(responseLogoutWrapper);
    when(logoutMessage.signSamlGetResponse(any(LogoutWrapper.class), any(URI.class), anyString()))
        .thenReturn(new URI(redirectLogoutUrl));
    logoutRequestService.setLogoutMessage(logoutMessage);
    Response response =
        logoutRequestService.getLogoutRequest(
            deflatedSamlRequest, null, relayState, SIGNATURE_ALGORITHM, SIGNATURE);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertTrue(
        "Expected logout url of " + redirectLogoutUrl,
        response.getEntity().toString().contains(redirectLogoutUrl));
  }

  private void insertLogoutRequest() throws XMLStreamException, LogoutSecurityException {
    LogoutRequest logoutRequest = mock(LogoutRequest.class);
    LogoutWrapper logoutRequestWrapper = mock(LogoutWrapper.class);
    doReturn(logoutRequest).when(logoutRequestWrapper).getMessage();
    SessionIndex sessionIndex = mock(SessionIndex.class);
    doReturn(SESSION_INDEX).when(sessionIndex).getSessionIndex();
    doReturn((Collections.singletonList(sessionIndex))).when(logoutRequest).getSessionIndexes();
    doReturn(DateTime.now()).when(logoutRequest).getIssueInstant();
    doReturn(SAMLVersion.VERSION_20).when(logoutRequest).getVersion();
    doReturn(ID).when(logoutRequest).getID();
    doReturn(logoutRequestWrapper)
        .when(logoutMessage)
        .extractSamlLogoutRequest(eq(UNENCODED_SAML_REQUEST));
  }

  @Test
  public void testGetLogoutRequestNotParsable() throws Exception {
    SamlSecurity samlSecurity = new SamlSecurity();
    String deflatedSamlRequest = samlSecurity.deflateAndBase64Encode(UNENCODED_SAML_REQUEST);
    when(logoutMessage.extractSamlLogoutRequest(eq(UNENCODED_SAML_REQUEST))).thenReturn(null);
    logoutRequestService.setLogoutMessage(logoutMessage);
    Response response =
        logoutRequestService.getLogoutRequest(
            deflatedSamlRequest, null, relayState, SIGNATURE_ALGORITHM, SIGNATURE);
    assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
    String msg = LogoutRequestService.UNABLE_TO_PARSE_LOGOUT_REQUEST.replaceAll(" ", "+");
    assertTrue(
        "Expected message containing " + msg, response.getLocation().getQuery().contains(msg));
  }

  @Test
  public void testGetLogoutRequestInvalidSignature() throws Exception {
    SamlSecurity samlSecurity = new SamlSecurity();
    String deflatedSamlRequest = samlSecurity.deflateAndBase64Encode(UNENCODED_SAML_REQUEST);
    LogoutRequest logoutRequest = mock(LogoutRequest.class);
    LogoutWrapper<LogoutRequest> requestLogoutWrapper = new LogoutWrapperImpl<>(logoutRequest);
    when(logoutMessage.extractSamlLogoutRequest(eq(UNENCODED_SAML_REQUEST)))
        .thenReturn(requestLogoutWrapper);
    logoutRequestService.setLogoutMessage(logoutMessage);

    LogoutRequestService lrs = new LogoutRequestService(simpleSign, idpMetadata, relayStates);

    lrs.setEncryptionService(encryptionService);
    lrs.setLogOutPageTimeOut(LOGOUT_PAGE_TIMEOUT);
    lrs.setLogoutMessage(logoutMessage);
    lrs.setRequest(request);
    lrs.setSessionFactory(sessionFactory);
    lrs.setSamlSecurity(samlSecurity);
    lrs.init();
    doReturn(new URI(redirectLogoutUrl))
        .when(logoutMessage)
        .signSamlGetResponse(any(LogoutWrapper.class), any(URI.class), anyString());
    insertLogoutRequest();
    Response response =
        lrs.getLogoutRequest(deflatedSamlRequest, null, relayState, SIGNATURE_ALGORITHM, SIGNATURE);
    assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
    String msg = UNABLE_TO_VALIDATE_LOGOUT_REQUEST.replaceAll(" ", "+");

    assertTrue(
        "Expected message containing " + msg, response.getLocation().getQuery().contains(msg));
  }

  @Test
  public void testGetLogoutRequestResponse() throws Exception {
    SamlSecurity samlSecurity = new SamlSecurity();
    String deflatedSamlResponse = samlSecurity.deflateAndBase64Encode(UNENCODED_SAML_RESPONSE);
    LogoutResponse logoutResponse = mock(LogoutResponse.class);
    when(logoutResponse.getIssueInstant()).thenReturn(new DateTime());
    when(logoutResponse.getVersion()).thenReturn(SAMLVersion.VERSION_20);
    when(logoutResponse.getID()).thenReturn("id");
    LogoutWrapper<LogoutResponse> responseLogoutWrapper = new LogoutWrapperImpl<>(logoutResponse);
    when(logoutMessage.extractSamlLogoutResponse(eq(UNENCODED_SAML_RESPONSE)))
        .thenReturn(responseLogoutWrapper);
    logoutRequestService.setLogoutMessage(logoutMessage);

    doReturn(true)
        .when(simpleSign)
        .validateSignature(anyString(), anyString(), anyString(), anyString());
    Response response =
        logoutRequestService.getLogoutRequest(
            null, deflatedSamlResponse, relayState, SIGNATURE_ALGORITHM, SIGNATURE);
    initializeLogoutRequestService();
    assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
    assertTrue(
        "Expected a successful logout message",
        response.getLocation().toString().contains("logged+out+successfully."));
  }

  @Test
  public void testGetLogoutRequestResponseNotParsable() throws Exception {
    insertLogoutRequest();
    SamlSecurity samlSecurity = new SamlSecurity();
    String deflatedSamlResponse = samlSecurity.deflateAndBase64Encode(UNENCODED_SAML_RESPONSE);
    when(logoutMessage.extractSamlLogoutResponse(eq(UNENCODED_SAML_RESPONSE))).thenReturn(null);
    logoutRequestService.setLogoutMessage(logoutMessage);
    insertLogoutRequest();
    Response response =
        logoutRequestService.getLogoutRequest(
            null, deflatedSamlResponse, relayState, SIGNATURE_ALGORITHM, SIGNATURE);
    assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
    String msg = LogoutRequestService.UNABLE_TO_PARSE_LOGOUT_RESPONSE.replaceAll(" ", "+");
    assertTrue(
        "Expected message containing " + msg, response.getLocation().getQuery().contains(msg));
  }

  @Test
  public void testGetLogoutRequestNoSessionIndex() throws Exception {
    SamlSecurity samlSecurity = new SamlSecurity();
    String deflatedSamlRequest = samlSecurity.deflateAndBase64Encode(UNENCODED_SAML_REQUEST);
    doReturn(true).when(simpleSign).validateSignature(anyString(), anyString(), anyString(), any());
    initializeLogoutRequestService();
    LogoutRequest logoutRequest = mock(LogoutRequest.class);
    LogoutWrapper logoutRequestWrapper = mock(LogoutWrapper.class);
    doReturn(logoutRequest).when(logoutRequestWrapper).getMessage();

    // No session index
    doReturn(Collections.EMPTY_LIST).when(logoutRequest).getSessionIndexes();

    doReturn(DateTime.now()).when(logoutRequest).getIssueInstant();
    doReturn(SAMLVersion.VERSION_20).when(logoutRequest).getVersion();
    doReturn(ID).when(logoutRequest).getID();
    doReturn(logoutRequestWrapper)
        .when(logoutMessage)
        .extractSamlLogoutRequest(eq(UNENCODED_SAML_REQUEST));
    LogoutResponse logoutResponse = mock(LogoutResponse.class);
    LogoutWrapper<LogoutResponse> responseLogoutWrapper = new LogoutWrapperImpl<>(logoutResponse);
    when(logoutMessage.buildLogoutResponse(anyString(), anyString(), anyString()))
        .thenReturn(responseLogoutWrapper);
    when(logoutMessage.signSamlGetResponse(any(LogoutWrapper.class), any(URI.class), anyString()))
        .thenReturn(new URI(redirectLogoutUrl));
    logoutRequestService.setLogoutMessage(logoutMessage);
    Response response =
        logoutRequestService.getLogoutRequest(
            deflatedSamlRequest, null, relayState, SIGNATURE_ALGORITHM, SIGNATURE);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertTrue(
        "Expected logout url of " + redirectLogoutUrl,
        response.getEntity().toString().contains(redirectLogoutUrl));
  }

  @Test
  public void testGetLogoutRequestResponseInvalidSignature() throws Exception {
    SamlSecurity samlSecurity = new SamlSecurity();
    String deflatedSamlResponse = samlSecurity.deflateAndBase64Encode(UNENCODED_SAML_RESPONSE);
    LogoutResponse logoutResponse = mock(LogoutResponse.class);
    LogoutWrapper<LogoutResponse> responseLogoutWrapper = new LogoutWrapperImpl<>(logoutResponse);
    when(logoutMessage.extractSamlLogoutResponse(eq(UNENCODED_SAML_RESPONSE)))
        .thenReturn(responseLogoutWrapper);
    logoutRequestService.setLogoutMessage(logoutMessage);

    LogoutRequestService lrs = new LogoutRequestService(simpleSign, idpMetadata, relayStates);

    lrs.setEncryptionService(encryptionService);
    lrs.setLogOutPageTimeOut(LOGOUT_PAGE_TIMEOUT);
    lrs.setLogoutMessage(logoutMessage);
    lrs.setRequest(request);
    lrs.setSessionFactory(sessionFactory);
    lrs.setSamlSecurity(samlSecurity);
    lrs.init();
    Response response =
        lrs.getLogoutRequest(
            null, deflatedSamlResponse, relayState, SIGNATURE_ALGORITHM, SIGNATURE);
    assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
    String msg = "Unable to validate".replaceAll(" ", "+");
    assertTrue(
        "Expected message containing " + msg, response.getLocation().getQuery().contains(msg));
  }
}
