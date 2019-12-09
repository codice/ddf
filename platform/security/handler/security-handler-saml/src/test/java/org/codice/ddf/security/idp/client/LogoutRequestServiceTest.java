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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.security.SecurityConstants;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.common.SecurityTokenHolder;
import ddf.security.encryption.EncryptionService;
import ddf.security.http.SessionFactory;
import ddf.security.samlp.SamlProtocol;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SimpleSign.SignatureException;
import ddf.security.samlp.impl.LogoutMessageImpl;
import ddf.security.samlp.impl.RelayStates;
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
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.codice.ddf.platform.session.api.HttpSessionInvalidator;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.opensaml.saml.common.SAMLObject;
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

  public static final String UNENCODED_SAML_RESPONSE = "TEST-SAML-RESPONSE";
  public static final String UNENCODED_SAML_REQUEST = "TEST-SAML-REQUEST";
  public static final String SIGNATURE = "signature";
  public static final String SIGNATURE_ALGORITHM = "sha1";
  private static final long LOGOUT_PAGE_TIMEOUT = TimeUnit.HOURS.toMillis(1);
  private static final String SESSION_INDEX = "123";
  private static final String ID = "TEST-ID";
  private LogoutRequestService logoutRequestService;
  private String nameId = "nameId";
  private Long time = System.currentTimeMillis();
  private String logoutUrl = "https://www.logout.url/logout";
  private String redirectLogoutUrl = "https://www.redirectlogout.location.com/logout";
  private String postLogoutUrl = "https://www.postlogout.location.com/logout";
  private RelayStates<String> relayStates;
  private SessionFactory sessionFactory;
  private HttpServletRequest request;
  private LogoutMessageImpl logoutMessage;
  private EncryptionService encryptionService;
  private IdpMetadata idpMetadata;
  private SimpleSign simpleSign;
  private HttpSession session;
  private SecurityTokenHolder securityTokenHolder;
  private String relayState = UUID.randomUUID().toString();

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
    session = mock(HttpSession.class);
    securityTokenHolder = mock(SecurityTokenHolder.class);
    Element issuedAssertion = readSamlAssertion().getDocumentElement();
    String assertionId = issuedAssertion.getAttributeNodeNS(null, "ID").getNodeValue();
    SecurityToken token = new SecurityToken(assertionId, issuedAssertion, null);
    SimplePrincipalCollection principalCollection = new SimplePrincipalCollection();
    SecurityAssertion securityAssertion = mock(SecurityAssertion.class);
    principalCollection.add(securityAssertion, "default");
    when(securityAssertion.getToken()).thenReturn(token);
    when(securityTokenHolder.getPrincipals()).thenReturn(principalCollection);
    initializeLogutRequestService();
    when(sessionFactory.getOrCreateSession(request)).thenReturn(session);
    when(session.getAttribute(eq(SecurityConstants.SECURITY_TOKEN_KEY)))
        .thenReturn(securityTokenHolder);
    when(request.getRequestURL()).thenReturn(new StringBuffer("www.url.com/url"));
    when(idpMetadata.getSigningCertificate()).thenReturn("signingCertificate");
    when(idpMetadata.getSingleLogoutBinding()).thenReturn(SamlProtocol.REDIRECT_BINDING);
    when(idpMetadata.getSingleLogoutLocation()).thenReturn(redirectLogoutUrl);
    System.setProperty("security.audit.roles", "none");
  }

  private void initializeLogutRequestService() {
    logoutRequestService = new LogoutRequestService(simpleSign, idpMetadata, relayStates);
    logoutRequestService.setEncryptionService(encryptionService);
    logoutRequestService.setLogOutPageTimeOut(LOGOUT_PAGE_TIMEOUT);
    logoutRequestService.setLogoutMessage(logoutMessage);
    logoutRequestService.setRequest(request);
    logoutRequestService.setSessionFactory(sessionFactory);
    logoutRequestService.init();
  }

  @Test
  public void testSendLogoutRequestGetRedirectRequest() throws Exception {
    String encryptedNameIdWithTime = nameId + "\n" + time;
    when(encryptionService.decrypt(any(String.class))).thenReturn(nameId + "\n" + time);
    when(logoutMessage.signSamlGetRequest(any(LogoutRequest.class), any(URI.class), anyString()))
        .thenReturn(new URI(logoutUrl));
    Response response = logoutRequestService.sendLogoutRequest(encryptedNameIdWithTime);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertTrue(
        "Expected logout url of " + logoutUrl, response.getEntity().toString().contains(logoutUrl));
  }

  @Test
  public void testSendLogoutRequestGetPostRequest() throws Exception {
    String encryptedNameIdWithTime = nameId + "\n" + time;
    when(encryptionService.decrypt(any(String.class))).thenReturn(nameId + "\n" + time);
    when(idpMetadata.getSingleLogoutBinding()).thenReturn(SamlProtocol.POST_BINDING);
    when(idpMetadata.getSingleLogoutLocation()).thenReturn(postLogoutUrl);
    LogoutRequest logoutRequest = new LogoutRequestBuilder().buildObject();
    when(logoutMessage.buildLogoutRequest(eq(nameId), anyString(), anyListOf(String.class)))
        .thenReturn(logoutRequest);
    Response response = logoutRequestService.sendLogoutRequest(encryptedNameIdWithTime);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertTrue(
        "Expected logout url of " + postLogoutUrl,
        response.getEntity().toString().contains(postLogoutUrl));
  }

  @Test
  public void testSendLogoutRequestTimeout() throws Exception {
    Long badTime = (time - TimeUnit.DAYS.toMillis(1));
    String encryptedNameIdWithTime = nameId + "\n" + badTime;
    when(encryptionService.decrypt(any(String.class))).thenReturn(nameId + "\n" + badTime);
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
    Response response = logoutRequestService.sendLogoutRequest(encryptedNameIdWithTime);
    assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
    insertLogoutRequest();
    String msg = LogoutRequestService.UNABLE_TO_DECRYPT_LOGOUT_REQUEST.replaceAll(" ", "+");
    assertTrue(
        "Expected message containing " + msg, response.getLocation().getQuery().contains(msg));
  }

  @Test
  public void testSendLogoutRequestNoSupportedBindings() throws Exception {
    String encryptedNameIdWithTime = nameId + "\n" + time;
    when(encryptionService.decrypt(any(String.class))).thenReturn(nameId + "\n" + time);
    when(idpMetadata.getSingleLogoutBinding())
        .thenReturn("urn:oasis:names:tc:SAML:2.0:bindings:SOAP");
    Response response = logoutRequestService.sendLogoutRequest(encryptedNameIdWithTime);
    assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
    String msg =
        "The identity provider does not support either POST or Redirect bindings."
            .replaceAll(" ", "+");
    assertTrue(
        "Expected message containing " + msg, response.getLocation().getQuery().contains(msg));
  }

  @Test
  public void getPostLogoutRequest() throws Exception {
    String encodedSamlRequest = "encodedSamlRequest";
    String issuerStr = "issuer";
    LogoutRequest logoutRequest = mock(LogoutRequest.class);
    when(logoutRequest.getIssueInstant()).thenReturn(DateTime.now());
    SessionIndex sessionIndex = mock(SessionIndex.class);
    when(sessionIndex.getSessionIndex()).thenReturn(SESSION_INDEX);
    when(logoutRequest.getSessionIndexes()).thenReturn(Collections.singletonList(sessionIndex));
    when(logoutMessage.extractSamlLogoutRequest(any(String.class))).thenReturn(logoutRequest);
    Issuer issuer = mock(Issuer.class);
    OpenSAMLUtil.initSamlEngine();
    LogoutResponse logoutResponse = new LogoutResponseBuilder().buildObject();
    when(logoutRequest.getIssuer()).thenReturn(issuer);
    when(logoutRequest.getIssueInstant()).thenReturn(new DateTime());
    when(logoutRequest.getVersion()).thenReturn(SAMLVersion.VERSION_20);
    when(logoutRequest.getID()).thenReturn("id");
    when(issuer.getValue()).thenReturn(issuerStr);
    when(logoutMessage.buildLogoutResponse(eq(issuerStr), eq(StatusCode.SUCCESS), anyString()))
        .thenReturn(logoutResponse);
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
  public void soapLogoutRequestIssueInstantOld() throws Exception {
    HttpSessionInvalidator httpSessionInvalidator = mock(HttpSessionInvalidator.class);
    logoutRequestService.setHttpSessionInvalidator(httpSessionInvalidator);

    InputStream requestStream =
        LogoutRequestServiceTest.class.getResourceAsStream("/SAMLSoapLogoutRequest-good.xml");
    Response response = logoutRequestService.soapLogoutRequest(requestStream, null);
    assertThat(response.getStatus(), is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    verify(httpSessionInvalidator, times(0)).invalidateSession(anyString(), any());
  }

  @Test
  public void soapLogoutRequestBadSignature() throws Exception {
    HttpSessionInvalidator httpSessionInvalidator = mock(HttpSessionInvalidator.class);
    logoutRequestService.setHttpSessionInvalidator(httpSessionInvalidator);

    LogoutResponse logoutResponse = mock(LogoutResponse.class);
    doReturn(logoutResponse)
        .when(logoutMessage)
        .buildLogoutResponse(anyString(), anyString(), anyString());
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
  public void soapLogoutRequestNotALogout() throws Exception {
    HttpSessionInvalidator httpSessionInvalidator = mock(HttpSessionInvalidator.class);
    logoutRequestService.setHttpSessionInvalidator(httpSessionInvalidator);

    InputStream requestStream =
        LogoutRequestServiceTest.class.getResourceAsStream("/SAMLSoapLogoutRequest-bad.xml");
    Response response = logoutRequestService.soapLogoutRequest(requestStream, null);
    assertThat(response.getStatus(), is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    verify(httpSessionInvalidator, times(0)).invalidateSession(anyString(), any());
  }

  @Test
  public void getPostLogoutRequestNotParsable() throws Exception {
    String encodedSamlRequest = "encodedSamlRequest";
    LogoutRequest logoutRequest = mock(LogoutRequest.class);
    when(logoutRequest.getIssueInstant()).thenReturn(DateTime.now());
    SessionIndex sessionIndex = mock(SessionIndex.class);
    when(sessionIndex.getSessionIndex()).thenReturn(SESSION_INDEX);
    when(logoutRequest.getSessionIndexes()).thenReturn(Collections.singletonList(sessionIndex));
    insertLogoutRequest();
    Response response =
        logoutRequestService.postLogoutRequest(encodedSamlRequest, null, relayState);
    assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
    String msg = UNABLE_TO_PARSE_LOGOUT_REQUEST.replaceAll(" ", "+");
    assertTrue(
        "Expected message containing " + msg, response.getLocation().getQuery().contains(msg));
  }

  @Test
  public void testPostLogoutRequestResponse() throws Exception {
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
    String deflatedSamlRequest = RestSecurity.deflateAndBase64Encode(UNENCODED_SAML_REQUEST);
    doReturn(true).when(simpleSign).validateSignature(anyString(), anyString(), anyString(), any());
    initializeLogutRequestService();
    insertLogoutRequest();
    when(logoutMessage.signSamlGetResponse(any(LogoutRequest.class), any(URI.class), anyString()))
        .thenReturn(new URI(redirectLogoutUrl));
    Response response =
        logoutRequestService.getLogoutRequest(
            deflatedSamlRequest, null, relayState, SIGNATURE_ALGORITHM, SIGNATURE);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertTrue(
        "Expected logout url of " + redirectLogoutUrl,
        response.getEntity().toString().contains(redirectLogoutUrl));
  }

  private void insertLogoutRequest() throws XMLStreamException, WSSecurityException {
    LogoutRequest logoutRequest = mock(LogoutRequest.class);
    SessionIndex sessionIndex = mock(SessionIndex.class);
    doReturn(SESSION_INDEX).when(sessionIndex).getSessionIndex();
    doReturn((Collections.singletonList(sessionIndex))).when(logoutRequest).getSessionIndexes();
    doReturn(DateTime.now()).when(logoutRequest).getIssueInstant();
    doReturn(SAMLVersion.VERSION_20).when(logoutRequest).getVersion();
    doReturn(ID).when(logoutRequest).getID();
    doReturn(logoutRequest)
        .when(logoutMessage)
        .extractSamlLogoutRequest(eq(UNENCODED_SAML_REQUEST));
  }

  @Test
  public void testGetLogoutRequestNotParsable() throws Exception {
    String deflatedSamlRequest = RestSecurity.deflateAndBase64Encode(UNENCODED_SAML_REQUEST);
    when(logoutMessage.extractSamlLogoutRequest(eq(UNENCODED_SAML_REQUEST))).thenReturn(null);
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
    String deflatedSamlRequest = RestSecurity.deflateAndBase64Encode(UNENCODED_SAML_REQUEST);
    LogoutRequest logoutRequest = mock(LogoutRequest.class);
    when(logoutMessage.extractSamlLogoutRequest(eq(UNENCODED_SAML_REQUEST)))
        .thenReturn(logoutRequest);

    LogoutRequestService lrs = new LogoutRequestService(simpleSign, idpMetadata, relayStates);

    lrs.setEncryptionService(encryptionService);
    lrs.setLogOutPageTimeOut(LOGOUT_PAGE_TIMEOUT);
    lrs.setLogoutMessage(logoutMessage);
    lrs.setRequest(request);
    lrs.setSessionFactory(sessionFactory);
    lrs.init();
    doReturn(new URI(redirectLogoutUrl))
        .when(logoutMessage)
        .signSamlGetResponse(any(SAMLObject.class), any(URI.class), anyString());
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
    String deflatedSamlResponse = RestSecurity.deflateAndBase64Encode(UNENCODED_SAML_RESPONSE);
    LogoutResponse logoutResponse = mock(LogoutResponse.class);
    when(logoutResponse.getIssueInstant()).thenReturn(new DateTime());
    when(logoutResponse.getVersion()).thenReturn(SAMLVersion.VERSION_20);
    when(logoutResponse.getID()).thenReturn("id");
    when(logoutMessage.extractSamlLogoutResponse(eq(UNENCODED_SAML_RESPONSE)))
        .thenReturn(logoutResponse);

    doReturn(true)
        .when(simpleSign)
        .validateSignature(anyString(), anyString(), anyString(), anyString());
    Response response =
        logoutRequestService.getLogoutRequest(
            null, deflatedSamlResponse, relayState, SIGNATURE_ALGORITHM, SIGNATURE);
    initializeLogutRequestService();
    assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
    assertTrue(
        "Expected a successful logout message",
        response.getLocation().toString().contains("logged+out+successfully."));
  }

  @Test
  public void testGetLogoutRequestResponseNotParsable() throws Exception {
    insertLogoutRequest();
    String deflatedSamlResponse = RestSecurity.deflateAndBase64Encode(UNENCODED_SAML_RESPONSE);
    when(logoutMessage.extractSamlLogoutResponse(eq(UNENCODED_SAML_RESPONSE))).thenReturn(null);
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
    String deflatedSamlRequest = RestSecurity.deflateAndBase64Encode(UNENCODED_SAML_REQUEST);
    doReturn(true).when(simpleSign).validateSignature(anyString(), anyString(), anyString(), any());
    initializeLogutRequestService();
    LogoutRequest logoutRequest = mock(LogoutRequest.class);

    // No session index
    doReturn(Collections.EMPTY_LIST).when(logoutRequest).getSessionIndexes();

    doReturn(DateTime.now()).when(logoutRequest).getIssueInstant();
    doReturn(SAMLVersion.VERSION_20).when(logoutRequest).getVersion();
    doReturn(ID).when(logoutRequest).getID();
    doReturn(logoutRequest)
        .when(logoutMessage)
        .extractSamlLogoutRequest(eq(UNENCODED_SAML_REQUEST));
    when(logoutMessage.signSamlGetResponse(any(LogoutRequest.class), any(URI.class), anyString()))
        .thenReturn(new URI(redirectLogoutUrl));
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
    String deflatedSamlResponse = RestSecurity.deflateAndBase64Encode(UNENCODED_SAML_RESPONSE);
    LogoutResponse logoutResponse = mock(LogoutResponse.class);
    when(logoutMessage.extractSamlLogoutResponse(eq(UNENCODED_SAML_RESPONSE)))
        .thenReturn(logoutResponse);

    LogoutRequestService lrs = new LogoutRequestService(simpleSign, idpMetadata, relayStates);

    lrs.setEncryptionService(encryptionService);
    lrs.setLogOutPageTimeOut(LOGOUT_PAGE_TIMEOUT);
    lrs.setLogoutMessage(logoutMessage);
    lrs.setRequest(request);
    lrs.setSessionFactory(sessionFactory);
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
