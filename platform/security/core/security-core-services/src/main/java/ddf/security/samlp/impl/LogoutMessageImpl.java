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
package ddf.security.samlp.impl;

import ddf.security.samlp.LogoutMessage;
import ddf.security.samlp.LogoutSecurityException;
import ddf.security.samlp.LogoutWrapper;
import ddf.security.samlp.SignatureException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import javax.ws.rs.core.UriBuilder;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang.Validate;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.rs.security.saml.sso.SSOConstants;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.util.DOM2Writer;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.codice.ddf.security.jaxrs.impl.SamlSecurity;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.SingleLogoutService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class LogoutMessageImpl implements LogoutMessage {

  private static final String SOAP_BINDING = "urn:oasis:names:tc:SAML:2.0:bindings:SOAP";

  private static final String SAML_SOAP_ACTION = "http://www.oasis-open.org/committees/security";

  private static final String ISSUER_CANNOT_BE_NULL_MSG = "Issuer cannot be null";

  static {
    OpenSAMLUtil.initSamlEngine();
  }

  private SystemCrypto systemCrypto;
  private UuidGenerator uuidGenerator;

  public LogoutMessageImpl(UuidGenerator generator) {
    uuidGenerator = generator;
  }

  public String getIdpSingleLogoutLocation(IDPSSODescriptor descriptor) {
    return descriptor
        .getSingleLogoutServices()
        .stream()
        .filter(service -> SOAP_BINDING.equals(service.getBinding()))
        .map(SingleLogoutService::getLocation)
        .findFirst()
        .orElse("");
  }

  public LogoutWrapper<SignableSAMLObject> extractXmlObject(String samlLogoutResponse)
      throws LogoutSecurityException, XMLStreamException {
    try {
      Document responseDoc =
          StaxUtils.read(
              new ByteArrayInputStream(samlLogoutResponse.getBytes(StandardCharsets.UTF_8)));
      XMLObject xmlObject = OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
      if (xmlObject instanceof SignableSAMLObject) {
        return new LogoutWrapperImpl<>((SignableSAMLObject) xmlObject);
      }
      return null;
    } catch (WSSecurityException e) {
      throw new LogoutSecurityException(e);
    }
  }

  private LogoutWrapper<LogoutResponse> extractResponse(String samlObject)
      throws LogoutSecurityException, XMLStreamException {
    try {
      Document responseDoc =
          StaxUtils.read(new ByteArrayInputStream(samlObject.getBytes(StandardCharsets.UTF_8)));
      XMLObject responseXmlObject = OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
      if (LogoutResponse.class.isAssignableFrom(responseXmlObject.getClass())) {
        return new LogoutWrapperImpl<>((LogoutResponse) responseXmlObject);
      }
      return null;
    } catch (WSSecurityException e) {
      throw new LogoutSecurityException(e);
    }
  }

  private LogoutWrapper<LogoutRequest> extractRequest(String samlObject)
      throws LogoutSecurityException, XMLStreamException {
    try {
      Document requestDoc =
          StaxUtils.read(new ByteArrayInputStream(samlObject.getBytes(StandardCharsets.UTF_8)));
      XMLObject requestXmlObject = OpenSAMLUtil.fromDom(requestDoc.getDocumentElement());
      if (LogoutRequest.class.isAssignableFrom(requestXmlObject.getClass())) {
        return new LogoutWrapperImpl<>((LogoutRequest) requestXmlObject);
      }
      return null;
    } catch (WSSecurityException e) {
      throw new LogoutSecurityException(e);
    }
  }

  @Override
  public LogoutWrapper<LogoutResponse> extractSamlLogoutResponse(String samlLogoutResponse)
      throws XMLStreamException, LogoutSecurityException {
    return extractResponse(samlLogoutResponse);
  }

  @Override
  public LogoutWrapper<LogoutRequest> extractSamlLogoutRequest(String samlLogoutResponse)
      throws XMLStreamException, LogoutSecurityException {
    return extractRequest(samlLogoutResponse);
  }

  /**
   * Returns a new <code>LogoutRequest</code> with a randomly generated ID, the current time for the
   * <code>IssueInstant</code>, and <code>Version</code> set to <code>"2.0"</code>
   *
   * @param nameIdString NameId of user to log out
   * @param issuerOrEntityId The Issuer of the LogoutRequest
   * @param sessionIndexes The list of session indexes selected for log out
   * @return the built <code>LogoutRequest</code>
   */
  @Override
  public LogoutWrapper<LogoutRequest> buildLogoutRequest(
      String nameIdString, String issuerOrEntityId, List<String> sessionIndexes) {
    return buildLogoutRequest(nameIdString, issuerOrEntityId, generateId(), sessionIndexes);
  }

  public LogoutWrapper<LogoutRequest> buildLogoutRequest(
      String nameIdString, String issuerOrEntityId, String id, List<String> sessionIndexes) {
    if (nameIdString == null) {
      throw new IllegalArgumentException("Name ID cannot be null");
    }
    if (issuerOrEntityId == null) {
      throw new IllegalArgumentException(ISSUER_CANNOT_BE_NULL_MSG);
    }
    if (id == null) {
      throw new IllegalArgumentException("ID cannot be null");
    }
    if (sessionIndexes == null) {
      throw new IllegalArgumentException("Session Index collection can be empty, but not null.");
    }

    return SamlProtocol.createLogoutRequest(
        SamlProtocol.createIssuer(issuerOrEntityId),
        SamlProtocol.createNameID(nameIdString),
        id,
        sessionIndexes);
  }

  /**
   * @apiNote ONLY use this method in error cases when creating a valid response pass the
   *     inResponseTo id
   * @param issuerOrEntityId the issuer or entity id to use when building the response
   * @param statusCodeValue the success, failure or partial logout status code
   * @return LogoutResponse
   */
  @Override
  public LogoutWrapper<LogoutResponse> buildLogoutResponse(
      String issuerOrEntityId, String statusCodeValue) {
    return buildLogoutResponse(issuerOrEntityId, statusCodeValue, null);
  }

  @Override
  public LogoutWrapper<LogoutResponse> buildLogoutResponse(
      String issuerOrEntityId, String statusCodeValue, String inResponseTo) {
    Validate.notNull(issuerOrEntityId, ISSUER_CANNOT_BE_NULL_MSG);
    Validate.notNull(statusCodeValue, "Status Code cannot be null");

    return SamlProtocol.createLogoutResponse(
        SamlProtocol.createIssuer(issuerOrEntityId),
        SamlProtocol.createStatus(statusCodeValue),
        inResponseTo,
        generateId());
  }

  @Override
  public LogoutWrapper<LogoutResponse> buildLogoutResponse(
      String issuerOrEntityId,
      String topLevelStatusCode,
      String secondLevelStatusCode,
      String inResponseTo) {
    Validate.notNull(issuerOrEntityId, ISSUER_CANNOT_BE_NULL_MSG);
    Validate.notNull(topLevelStatusCode, "Top level Status Code cannot be null");
    Validate.notNull(secondLevelStatusCode, "Second level Status Code cannot be null");

    Status status = SamlProtocol.createStatus(topLevelStatusCode);
    StatusCode statusCode = SamlProtocol.createStatusCode(secondLevelStatusCode);
    status.getStatusCode().setStatusCode(statusCode);

    return SamlProtocol.createLogoutResponse(
        SamlProtocol.createIssuer(issuerOrEntityId), status, inResponseTo, generateId());
  }

  @Override
  public Element getElementFromSaml(LogoutWrapper xmlObject) throws LogoutSecurityException {
    try {
      Document doc = DOMUtils.createDocument();
      doc.appendChild(doc.createElement("root"));
      return OpenSAMLUtil.toDom((XMLObject) xmlObject.getMessage(), doc);
    } catch (WSSecurityException e) {
      throw new LogoutSecurityException(e);
    }
  }

  @Override
  public String sendSamlLogoutRequest(
      LogoutWrapper request, String targetUri, boolean isSoap, @Nullable Cookie cookie)
      throws IOException, LogoutSecurityException {
    XMLObject xmlObject =
        isSoap
            ? SamlProtocol.createSoapMessage((SignableSAMLObject) request.getMessage())
            : (XMLObject) request;

    Element requestElement = getElementFromSaml(new LogoutWrapperImpl(xmlObject));
    String requestMessage = DOM2Writer.nodeToString(requestElement);
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      HttpPost post = new HttpPost(targetUri);
      post.addHeader("Cache-Control", "no-cache, no-store");
      post.addHeader("Pragma", "no-cache");
      post.addHeader("SOAPAction", SAML_SOAP_ACTION);

      post.addHeader("Content-Type", "application/soap+xml");

      post.setEntity(new StringEntity(requestMessage, "utf-8"));
      ResponseHandler<String> responseHandler = new BasicResponseHandler();
      BasicHttpContext context = new BasicHttpContext();
      if (cookie != null) {
        BasicClientCookie basicClientCookie =
            new BasicClientCookie(cookie.getName(), cookie.getValue());
        basicClientCookie.setDomain(cookie.getDomain());
        basicClientCookie.setPath(cookie.getPath());

        BasicCookieStore cookieStore = new BasicCookieStore();
        cookieStore.addCookie(basicClientCookie);
        context.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
      }

      return httpClient.execute(post, responseHandler, context);
    }
  }

  @Override
  public URI signSamlGetResponse(LogoutWrapper samlObject, URI target, String relayState)
      throws LogoutSecurityException, SignatureException, IOException {

    return signSamlGet(samlObject, target, relayState, SSOConstants.SAML_RESPONSE);
  }

  @Override
  public URI signSamlGetRequest(LogoutWrapper samlObject, URI target, String relayState)
      throws LogoutSecurityException, SignatureException, IOException {

    return signSamlGet(samlObject, target, relayState, SSOConstants.SAML_REQUEST);
  }

  private URI signSamlGet(
      LogoutWrapper samlObject, URI target, String relayState, String requestType)
      throws LogoutSecurityException, SignatureException, IOException {
    try {
      Document doc = DOMUtils.createDocument();
      doc.appendChild(doc.createElement("root"));
      SamlSecurity samlSecurity = new SamlSecurity();
      String encodedResponse =
          URLEncoder.encode(
              samlSecurity.deflateAndBase64Encode(
                  DOM2Writer.nodeToString(
                      OpenSAMLUtil.toDom((XMLObject) samlObject.getMessage(), doc, false))),
              "UTF-8");
      String requestToSign =
          String.format(
              "%s=%s&%s=%s", requestType, encodedResponse, SSOConstants.RELAY_STATE, relayState);
      UriBuilder uriBuilder = UriBuilder.fromUri(target);
      uriBuilder.queryParam(requestType, encodedResponse);
      uriBuilder.queryParam(SSOConstants.RELAY_STATE, relayState);
      new SimpleSign(systemCrypto).signUriString(requestToSign, uriBuilder);
      return uriBuilder.build();
    } catch (WSSecurityException e) {
      throw new LogoutSecurityException(e);
    }
  }

  public void setSystemCrypto(SystemCrypto systemCrypto) {
    this.systemCrypto = systemCrypto;
  }

  public UuidGenerator getUuidGenerator() {
    return uuidGenerator;
  }

  private String generateId() {
    return "_" + getUuidGenerator().generateUuid();
  }
}
