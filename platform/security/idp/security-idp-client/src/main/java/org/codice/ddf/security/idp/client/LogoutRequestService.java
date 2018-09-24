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

import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.SubjectUtils;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.assertion.impl.SecurityAssertionImpl;
import ddf.security.common.SecurityTokenHolder;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.encryption.EncryptionService;
import ddf.security.http.SessionFactory;
import ddf.security.samlp.LogoutMessage;
import ddf.security.samlp.SamlProtocol;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SimpleSign.SignatureException;
import ddf.security.samlp.ValidationException;
import ddf.security.samlp.impl.HtmlResponseTemplate;
import ddf.security.samlp.impl.RelayStates;
import ddf.security.samlp.impl.SamlValidator;
import ddf.security.service.SecurityServiceException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPPart;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.util.DOM2Writer;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.platform.session.api.HttpSessionInvalidator;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.soap.soap11.Envelope;
import org.opensaml.xmlsec.signature.SignableXMLObject;
import org.opensaml.xmlsec.signature.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Path("logout")
public class LogoutRequestService {

  public static final String IDP_REALM_NAME = "idp";
  public static final String NO_SUPPORT_FOR_POST_OR_REDIRECT_BINDINGS =
      "The identity provider does not support either POST or Redirect bindings.";
  public static final String ROOT_NODE_NAME = "root";
  public static final String UNABLE_TO_CREATE_LOGOUT_REQUEST = "Failed to create logout request";
  public static final String UNABLE_TO_CREATE_LOGOUT_RESPONSE = "Failed to create logout response";
  public static final String UNABLE_TO_DECODE_AND_INFLATE_LOGOUT_REQUEST =
      "Unable to decode and inflate logout request";
  public static final String UNABLE_TO_DECODE_AND_INFLATE_LOGOUT_RESPONSE =
      "Unable to decode and inflate logout response.";
  public static final String UNABLE_TO_DECRYPT_LOGOUT_REQUEST =
      "Failed to decrypt logout request params. Invalid number of params";
  public static final String UNABLE_TO_PARSE_LOGOUT_REQUEST = "Unable to parse logout request";
  public static final String UNABLE_TO_PARSE_LOGOUT_RESPONSE = "Unable to parse logout response";
  public static final String UNABLE_TO_SIGN_LOGOUT_RESPONSE = "Failed to sign logout response";
  public static final String UNABLE_TO_VALIDATE_LOGOUT_REQUEST =
      "Unable to validate logout request";
  public static final String UNABLE_TO_VALIDATE_LOGOUT_RESPONSE =
      "Unable to validate logout response";
  public static final String SECURITY_AUDIT_ROLES = "security.audit.roles";
  private static final Logger LOGGER = LoggerFactory.getLogger(LogoutRequestService.class);
  private static final String SAML_REQUEST = "SAMLRequest";
  private static final String SAML_RESPONSE = "SAMLResponse";
  private static final String RELAY_STATE = "RelayState";
  private static final String SIG_ALG = "SigAlg";
  private static final String SIGNATURE = "Signature";

  static {
    OpenSAMLUtil.initSamlEngine();
  }

  private final RelayStates<String> relayStates;

  private SimpleSign simpleSign;

  private IdpMetadata idpMetadata;

  private HttpSessionInvalidator httpSessionInvalidator;

  private ddf.security.service.SecurityManager securityManager;

  @Context private HttpServletRequest request;

  private LogoutMessage logoutMessage;

  private String submitForm;

  private String redirectPage;

  private EncryptionService encryptionService;

  private SessionFactory sessionFactory;

  private long logOutPageTimeOut = 3600000;

  public LogoutRequestService(
      SimpleSign simpleSign, IdpMetadata idpMetadata, RelayStates<String> relayStates) {
    this.simpleSign = simpleSign;
    this.idpMetadata = idpMetadata;
    this.relayStates = relayStates;
  }

  public void init() {
    try (InputStream submitStream =
            LogoutRequestService.class.getResourceAsStream("/templates/submitForm.handlebars");
        InputStream redirectStream =
            LogoutRequestService.class.getResourceAsStream("/templates/redirect.handlebars")) {
      submitForm = IOUtils.toString(submitStream, StandardCharsets.UTF_8);
      redirectPage = IOUtils.toString(redirectStream, StandardCharsets.UTF_8);
    } catch (IOException | RuntimeException e) {
      LOGGER.debug("Unable to load index page for SP.", e);
    }
  }

  @GET
  @Path("/request")
  public Response sendLogoutRequest(@QueryParam("EncryptedNameIdTime") String encryptedNameIdTime) {
    String nameIdTime = encryptionService.decrypt(encryptedNameIdTime);
    String[] nameIdTimeArray = StringUtils.split(nameIdTime, "\n");
    if (nameIdTimeArray.length == 2) {
      try {
        String name = nameIdTimeArray[0];
        Long time = Long.parseLong(nameIdTimeArray[1]);
        if (System.currentTimeMillis() - time > logOutPageTimeOut) {
          String msg =
              String.format(
                  "Logout request was older than %sms old so it was rejected. Please refresh page and request again.",
                  logOutPageTimeOut);
          LOGGER.info(msg);
          return buildLogoutResponse(msg);
        }

        // Logout removes the SAML assertion. This statement must be called before the SAML
        // assertion is removed.
        List<String> sessionIndexes =
            getIdpSecurityAssertion()
                .getAuthnStatements()
                .stream()
                .filter(Objects::nonNull)
                .map(AuthnStatement::getSessionIndex)
                .collect(Collectors.toList());

        logout();
        LogoutRequest logoutRequest =
            logoutMessage.buildLogoutRequest(name, getEntityId(), sessionIndexes);

        String relayState = relayStates.encode(name);

        return getLogoutRequest(relayState, logoutRequest);
      } catch (RuntimeException e) {
        LOGGER.info(UNABLE_TO_CREATE_LOGOUT_REQUEST, e);
        return buildLogoutResponse(UNABLE_TO_CREATE_LOGOUT_REQUEST);
      }

    } else {
      LOGGER.info(UNABLE_TO_DECRYPT_LOGOUT_REQUEST);
      return buildLogoutResponse(UNABLE_TO_DECRYPT_LOGOUT_REQUEST);
    }
  }

  private Response getLogoutRequest(String relayState, LogoutRequest logoutRequest) {
    try {
      String binding = idpMetadata.getSingleLogoutBinding();
      if (SamlProtocol.POST_BINDING.equals(binding)) {
        return getSamlpPostLogoutRequest(relayState, logoutRequest);
      } else if (SamlProtocol.REDIRECT_BINDING.equals(binding)) {
        return getSamlpRedirectLogoutRequest(relayState, logoutRequest);
      } else {
        return buildLogoutResponse(NO_SUPPORT_FOR_POST_OR_REDIRECT_BINDINGS);
      }
    } catch (Exception e) {
      LOGGER.debug(UNABLE_TO_CREATE_LOGOUT_REQUEST, e);
      return buildLogoutResponse(UNABLE_TO_CREATE_LOGOUT_REQUEST);
    }
  }

  private Response getSamlpPostLogoutRequest(String relayState, LogoutRequest logoutRequest)
      throws SimpleSign.SignatureException, WSSecurityException {
    LOGGER.debug("Configuring SAML LogoutRequest for POST.");
    String encodedSamlRequest = encodeSaml(logoutRequest);
    String singleLogoutLocation = idpMetadata.getSingleLogoutLocation();
    String submitFormUpdated =
        String.format(
            submitForm, singleLogoutLocation, SAML_REQUEST, encodedSamlRequest, relayState);
    Response.ResponseBuilder ok = Response.ok(submitFormUpdated);
    return ok.build();
  }

  String encodeSaml(LogoutRequest logoutRequest) throws SignatureException, WSSecurityException {
    Document doc = DOMUtils.createDocument();
    doc.appendChild(doc.createElement(ROOT_NODE_NAME));
    LOGGER.debug("Signing SAML POST LogoutRequest.");
    simpleSign.signSamlObject(logoutRequest);
    LOGGER.debug("Converting SAML Request to DOM");
    String assertionResponse = DOM2Writer.nodeToString(OpenSAMLUtil.toDom(logoutRequest, doc));
    return Base64.getEncoder().encodeToString(assertionResponse.getBytes(StandardCharsets.UTF_8));
  }

  private Response getSamlpRedirectLogoutRequest(String relayState, LogoutRequest logoutRequest)
      throws IOException, SimpleSign.SignatureException, WSSecurityException, URISyntaxException {
    LOGGER.debug("Configuring SAML Response for Redirect.");
    Document doc = DOMUtils.createDocument();
    doc.appendChild(doc.createElement(ROOT_NODE_NAME));
    URI location =
        logoutMessage.signSamlGetRequest(
            logoutRequest, new URI(idpMetadata.getSingleLogoutLocation()), relayState);
    String redirectUpdated = String.format(redirectPage, location.toString());
    Response.ResponseBuilder ok = Response.ok(redirectUpdated);
    return ok.build();
  }

  private String extractSubject(Map<String, Object> sessionAttributes) {
    return Stream.of(sessionAttributes.get(SecurityConstants.SAML_ASSERTION))
        .filter(SecurityTokenHolder.class::isInstance)
        .map(SecurityTokenHolder.class::cast)
        .map(SecurityTokenHolder::getRealmTokenMap)
        .map(Map::values)
        .flatMap(Collection::stream)
        .map(this::extractSubject)
        .filter(Objects::nonNull)
        .map(SubjectUtils::getName)
        .findFirst()
        .orElse(null);
  }

  private Subject extractSubject(SecurityToken securityToken) {
    try {
      return securityManager.getSubject(securityToken);
    } catch (SecurityServiceException e) {
      LOGGER.debug("Error extracting subject from security token", e);
      return null;
    }
  }

  @POST
  @Consumes({"text/xml", "application/soap+xml"})
  public Response soapLogoutRequest(InputStream body, @Context HttpServletRequest request) {
    XMLObject xmlObject;
    try {
      String bodyString = IOUtils.toString(body, StandardCharsets.UTF_8);
      SOAPPart soapMessage = SamlProtocol.parseSoapMessage(bodyString);

      xmlObject =
          SamlProtocol.getXmlObjectFromNode(soapMessage.getEnvelope().getBody().getFirstChild());
      if (!(xmlObject instanceof LogoutRequest)) {
        LOGGER.info(UNABLE_TO_PARSE_LOGOUT_REQUEST);
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(
              "Type of object is {}", xmlObject == null ? "null" : xmlObject.getSchemaType());
        }
        return Response.serverError().build();
      }
    } catch (SOAPException | XMLStreamException | IOException | WSSecurityException e) {
      LOGGER.debug("Error parsing input", e);
      return Response.serverError().build();
    }
    LogoutRequest logoutRequest = (LogoutRequest) xmlObject;
    // Pre-build response with success status
    LogoutResponse logoutResponse =
        logoutMessage.buildLogoutResponse(
            logoutRequest.getIssuer().getValue(), StatusCode.SUCCESS, logoutRequest.getID());

    try {
      if (!validateSignature(logoutRequest)) {
        return getSamlpSoapLogoutResponse(logoutResponse, StatusCode.AUTHN_FAILED, null);
      }

      new SamlValidator.Builder(simpleSign)
          .buildAndValidate(
              this.request.getRequestURL().toString(),
              SamlProtocol.Binding.HTTP_POST,
              logoutRequest);

      httpSessionInvalidator.invalidateSession(
          logoutRequest.getNameID().getValue(), this::extractSubject);

      SecurityLogger.audit(
          "Subject logged out by backchannel request: {}", logoutRequest.getNameID().getValue());

      return getSamlpSoapLogoutResponse(logoutResponse);
    } catch (ValidationException e) {
      LOGGER.info(UNABLE_TO_VALIDATE_LOGOUT_REQUEST, e);
      return getSamlpSoapLogoutResponse(logoutResponse, StatusCode.RESPONDER, e.getMessage());
    }
  }

  private boolean validateSignature(LogoutRequest logoutRequest) {
    Signature signature = logoutRequest.getSignature();
    if (signature == null) {
      LOGGER.debug("Unsigned logoutRequest");
      return false;
    }

    Element dom = logoutRequest.getDOM();
    if (dom == null) {
      LOGGER.debug("Incorrectly formatted logoutRequest");
      return false;
    }

    try {
      simpleSign.validateSignature(signature, dom.getOwnerDocument());
      return true;
    } catch (SignatureException e) {
      LOGGER.debug("Invalid signature on logoutRequest", e);
      return false;
    }
  }

  @POST
  @Produces(MediaType.APPLICATION_FORM_URLENCODED)
  public Response postLogoutRequest(
      @FormParam(SAML_REQUEST) String encodedSamlRequest,
      @FormParam(SAML_RESPONSE) String encodedSamlResponse,
      @FormParam(RELAY_STATE) String relayState) {

    if (encodedSamlRequest != null) {
      try {
        LogoutRequest logoutRequest =
            logoutMessage.extractSamlLogoutRequest(RestSecurity.base64Decode(encodedSamlRequest));
        if (logoutRequest == null) {
          LOGGER.debug(UNABLE_TO_PARSE_LOGOUT_REQUEST);
          return buildLogoutResponse(UNABLE_TO_PARSE_LOGOUT_REQUEST);
        }

        new SamlValidator.Builder(simpleSign)
            .buildAndValidate(
                request.getRequestURL().toString(), SamlProtocol.Binding.HTTP_POST, logoutRequest);
        logout();
        LogoutResponse logoutResponse =
            logoutMessage.buildLogoutResponse(
                logoutRequest.getIssuer().getValue(), StatusCode.SUCCESS, logoutRequest.getID());

        return getLogoutResponse(relayState, logoutResponse);
      } catch (WSSecurityException | XMLStreamException e) {
        LOGGER.info(UNABLE_TO_PARSE_LOGOUT_REQUEST, e);
        return buildLogoutResponse(UNABLE_TO_PARSE_LOGOUT_REQUEST);
      } catch (ValidationException e) {
        LOGGER.info(UNABLE_TO_VALIDATE_LOGOUT_REQUEST, e);
        return buildLogoutResponse(UNABLE_TO_VALIDATE_LOGOUT_REQUEST);
      }
    } else {
      try {
        LogoutResponse logoutResponse =
            logoutMessage.extractSamlLogoutResponse(RestSecurity.base64Decode(encodedSamlResponse));
        if (logoutResponse == null) {
          LOGGER.info(UNABLE_TO_PARSE_LOGOUT_RESPONSE);
          return buildLogoutResponse(UNABLE_TO_PARSE_LOGOUT_RESPONSE);
        }
        new SamlValidator.Builder(simpleSign)
            .buildAndValidate(
                request.getRequestURL().toString(), SamlProtocol.Binding.HTTP_POST, logoutResponse);
      } catch (ValidationException e) {
        LOGGER.info(UNABLE_TO_VALIDATE_LOGOUT_RESPONSE, e);
        return buildLogoutResponse(UNABLE_TO_VALIDATE_LOGOUT_RESPONSE);
      } catch (WSSecurityException | XMLStreamException e) {
        LOGGER.info(UNABLE_TO_PARSE_LOGOUT_RESPONSE, e);
        return buildLogoutResponse(UNABLE_TO_PARSE_LOGOUT_RESPONSE);
      }
      String nameId = "You";
      String decodedValue;
      if (relayState != null && (decodedValue = relayStates.decode(relayState)) != null) {
        nameId = decodedValue;
      }
      return buildLogoutResponse(nameId + " logged out successfully.");
    }
  }

  @GET
  public Response getLogoutRequest(
      @QueryParam(SAML_REQUEST) String deflatedSamlRequest,
      @QueryParam(SAML_RESPONSE) String deflatedSamlResponse,
      @QueryParam(RELAY_STATE) String relayState,
      @QueryParam(SIG_ALG) String signatureAlgorithm,
      @QueryParam(SIGNATURE) String signature) {

    if (deflatedSamlRequest != null) {
      try {
        LogoutRequest logoutRequest =
            logoutMessage.extractSamlLogoutRequest(RestSecurity.inflateBase64(deflatedSamlRequest));
        if (logoutRequest == null) {
          return buildLogoutResponse(UNABLE_TO_PARSE_LOGOUT_REQUEST);
        }
        buildAndValidateSaml(
            deflatedSamlRequest, relayState, signatureAlgorithm, signature, logoutRequest);
        logout();
        String entityId = getEntityId();
        LogoutResponse logoutResponse =
            logoutMessage.buildLogoutResponse(entityId, StatusCode.SUCCESS, logoutRequest.getID());
        return getLogoutResponse(relayState, logoutResponse);
      } catch (IOException e) {
        LOGGER.info(UNABLE_TO_DECODE_AND_INFLATE_LOGOUT_REQUEST, e);
        return buildLogoutResponse(UNABLE_TO_DECODE_AND_INFLATE_LOGOUT_REQUEST);
      } catch (ValidationException e) {
        LOGGER.info(UNABLE_TO_VALIDATE_LOGOUT_REQUEST, e);
        return buildLogoutResponse(UNABLE_TO_VALIDATE_LOGOUT_REQUEST);
      } catch (WSSecurityException | XMLStreamException e) {
        LOGGER.info(UNABLE_TO_PARSE_LOGOUT_REQUEST, e);
        return buildLogoutResponse(UNABLE_TO_PARSE_LOGOUT_REQUEST);
      }
    } else {
      try {

        LogoutResponse logoutResponse =
            logoutMessage.extractSamlLogoutResponse(
                RestSecurity.inflateBase64(deflatedSamlResponse));
        if (logoutResponse == null) {
          LOGGER.debug(UNABLE_TO_PARSE_LOGOUT_RESPONSE);
          return buildLogoutResponse(UNABLE_TO_PARSE_LOGOUT_RESPONSE);
        }
        buildAndValidateSaml(
            deflatedSamlResponse, relayState, signatureAlgorithm, signature, logoutResponse);
        String nameId = "You";
        String decodedValue;
        if (relayState != null && (decodedValue = relayStates.decode(relayState)) != null) {
          nameId = decodedValue;
        }
        return buildLogoutResponse(nameId + " logged out successfully.");
      } catch (IOException e) {
        LOGGER.info(UNABLE_TO_DECODE_AND_INFLATE_LOGOUT_RESPONSE, e);
        return buildLogoutResponse(UNABLE_TO_DECODE_AND_INFLATE_LOGOUT_RESPONSE);
      } catch (ValidationException e) {
        LOGGER.info(UNABLE_TO_VALIDATE_LOGOUT_RESPONSE, e);
        return buildLogoutResponse(UNABLE_TO_VALIDATE_LOGOUT_RESPONSE);
      } catch (WSSecurityException | XMLStreamException e) {
        LOGGER.info(UNABLE_TO_PARSE_LOGOUT_RESPONSE, e);
        return buildLogoutResponse(UNABLE_TO_PARSE_LOGOUT_RESPONSE);
      }
    }
  }

  protected void buildAndValidateSaml(
      String samlRequest,
      String relayState,
      String signatureAlgorithm,
      String signature,
      SignableXMLObject xmlObject)
      throws ValidationException {
    new SamlValidator.Builder(simpleSign)
        .setRedirectParams(
            relayState,
            signature,
            signatureAlgorithm,
            samlRequest,
            idpMetadata.getSigningCertificate())
        .buildAndValidate(
            request.getRequestURL().toString(), SamlProtocol.Binding.HTTP_REDIRECT, xmlObject);
  }

  private String getEntityId() {
    String hostname = SystemBaseUrl.INTERNAL.getHost();
    String port = SystemBaseUrl.INTERNAL.getPort();
    String rootContext = SystemBaseUrl.INTERNAL.getRootContext();

    return String.format("https://%s:%s%s/saml", hostname, port, rootContext);
  }

  private SecurityAssertion getIdpSecurityAssertion() {

    return new SecurityAssertionImpl(getTokenHolder().getSecurityToken(IDP_REALM_NAME));
  }

  private void logout() {
    logSecurityAuditRole();
    getTokenHolder().remove(IDP_REALM_NAME);
  }

  private void logSecurityAuditRole() {
    if (shouldAuditSubject()) {
      SecurityLogger.audit(
          "Subject with admin privileges has logged out: {}",
          getIdpSecurityAssertion().getPrincipal().getName());
    }
  }

  private boolean shouldAuditSubject() {
    return Arrays.stream(System.getProperty(SECURITY_AUDIT_ROLES).split(","))
        .anyMatch(
            role -> getIdpSecurityAssertion().getPrincipals().contains(new RolePrincipal(role)));
  }

  private SecurityTokenHolder getTokenHolder() {
    return (SecurityTokenHolder)
        sessionFactory.getOrCreateSession(request).getAttribute(SecurityConstants.SAML_ASSERTION);
  }

  private Response getLogoutResponse(String relayState, LogoutResponse samlResponse) {
    try {
      String binding = idpMetadata.getSingleLogoutBinding();
      if (SamlProtocol.POST_BINDING.equals(binding)) {
        return getSamlpPostLogoutResponse(relayState, samlResponse);
      } else if (SamlProtocol.REDIRECT_BINDING.equals(binding)) {
        return getSamlpRedirectLogoutResponse(relayState, samlResponse);
      } else if (SamlProtocol.SOAP_BINDING.equals(binding)) {
        return getSamlpSoapLogoutResponse(samlResponse);
      } else {
        return buildLogoutResponse(NO_SUPPORT_FOR_POST_OR_REDIRECT_BINDINGS);
      }
    } catch (Exception e) {
      LOGGER.debug(UNABLE_TO_CREATE_LOGOUT_RESPONSE, e);
      return buildLogoutResponse(UNABLE_TO_CREATE_LOGOUT_RESPONSE);
    }
  }

  private Response getSamlpSoapLogoutResponse(LogoutResponse samlResponse) {
    return getSamlpSoapLogoutResponse(samlResponse, null, null);
  }

  private Response getSamlpSoapLogoutResponse(
      LogoutResponse samlResponse, String statusCode, String statusMessage) {
    if (samlResponse == null) {
      return Response.serverError().build();
    }
    LOGGER.debug("Configuring SAML Response for SOAP.");
    Document doc = DOMUtils.createDocument();
    doc.appendChild(doc.createElement(ROOT_NODE_NAME));
    LOGGER.debug("Setting SAML status on Response for SOAP");
    if (statusCode != null) {
      if (statusMessage != null) {
        samlResponse.setStatus(SamlProtocol.createStatus(statusCode, statusMessage));
      } else {
        samlResponse.setStatus(SamlProtocol.createStatus(statusCode));
      }
    }

    try {
      LOGGER.debug("Signing SAML Response for SOAP.");
      LogoutResponse logoutResponse = simpleSign.forceSignSamlObject(samlResponse);

      Envelope soapMessage = SamlProtocol.createSoapMessage(logoutResponse);

      LOGGER.debug("Converting SAML Response to DOM");
      String assertionResponse = DOM2Writer.nodeToString(OpenSAMLUtil.toDom(soapMessage, doc));
      String encodedSamlResponse =
          Base64.getEncoder().encodeToString(assertionResponse.getBytes(StandardCharsets.UTF_8));

      return Response.ok(encodedSamlResponse).build();
    } catch (SignatureException | WSSecurityException | XMLStreamException e) {
      LOGGER.debug("Failure constructing SOAP LogoutResponse", e);
      return Response.serverError().build();
    }
  }

  private Response getSamlpPostLogoutResponse(String relayState, LogoutResponse samlResponse)
      throws WSSecurityException, SimpleSign.SignatureException {
    LOGGER.debug("Configuring SAML Response for POST.");
    Document doc = DOMUtils.createDocument();
    doc.appendChild(doc.createElement(ROOT_NODE_NAME));
    LOGGER.debug("Signing SAML POST Response.");
    simpleSign.signSamlObject(samlResponse);
    LOGGER.debug("Converting SAML Response to DOM");
    String assertionResponse = DOM2Writer.nodeToString(OpenSAMLUtil.toDom(samlResponse, doc));
    String encodedSamlResponse =
        Base64.getEncoder().encodeToString(assertionResponse.getBytes(StandardCharsets.UTF_8));

    return Response.ok(
            HtmlResponseTemplate.getPostPage(
                idpMetadata.getSingleLogoutLocation(),
                SamlProtocol.Type.RESPONSE,
                encodedSamlResponse,
                relayState))
        .build();
  }

  private Response getSamlpRedirectLogoutResponse(String relayState, LogoutResponse samlResponse)
      throws IOException, SimpleSign.SignatureException, WSSecurityException, URISyntaxException {
    LOGGER.debug("Configuring SAML Response for Redirect.");
    Document doc = DOMUtils.createDocument();
    doc.appendChild(doc.createElement(ROOT_NODE_NAME));
    URI location =
        logoutMessage.signSamlGetResponse(
            samlResponse, new URI(idpMetadata.getSingleLogoutLocation()), relayState);

    return Response.ok(HtmlResponseTemplate.getRedirectPage(location.toString())).build();
  }

  private Response buildLogoutResponse(String message) {
    UriBuilder uriBuilder = UriBuilder.fromUri(SystemBaseUrl.INTERNAL.getBaseUrl());
    uriBuilder.path("logout/logout-response.html");
    uriBuilder.queryParam("msg", message);
    return Response.seeOther(uriBuilder.build()).build();
  }

  public void setRequest(HttpServletRequest request) {
    this.request = request;
  }

  public void setLogoutMessage(LogoutMessage logoutMessage) {
    this.logoutMessage = logoutMessage;
  }

  public void setEncryptionService(EncryptionService encryptionService) {
    this.encryptionService = encryptionService;
  }

  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  public void setHttpSessionInvalidator(HttpSessionInvalidator httpSessionInvalidator) {
    this.httpSessionInvalidator = httpSessionInvalidator;
  }

  public void setSecurityManager(ddf.security.service.SecurityManager securityManager) {
    this.securityManager = securityManager;
  }

  public void setLogOutPageTimeOut(long logOutPageTimeOut) {
    this.logOutPageTimeOut = logOutPageTimeOut;
  }
}
