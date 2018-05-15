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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
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
import javax.xml.stream.XMLStreamException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.util.DOM2Writer;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.xmlsec.signature.SignableXMLObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

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

  @POST
  @Produces(MediaType.APPLICATION_FORM_URLENCODED)
  public Response postLogoutRequest(
      @FormParam(SAML_REQUEST) String encodedSamlRequest,
      @FormParam(SAML_REQUEST) String encodedSamlResponse,
      @FormParam(RELAY_STATE) String relayState) {

    if (encodedSamlRequest != null) {
      try {
        LogoutRequest logoutRequest =
            logoutMessage.extractSamlLogoutRequest(decodeBase64(encodedSamlRequest));
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
      } catch (WSSecurityException e) {
        LOGGER.info(UNABLE_TO_SIGN_LOGOUT_RESPONSE, e);
        return buildLogoutResponse(UNABLE_TO_SIGN_LOGOUT_RESPONSE);
      } catch (ValidationException e) {
        LOGGER.info(UNABLE_TO_VALIDATE_LOGOUT_REQUEST, e);
        return buildLogoutResponse(UNABLE_TO_VALIDATE_LOGOUT_REQUEST);
      } catch (XMLStreamException e) {
        LOGGER.info(UNABLE_TO_PARSE_LOGOUT_REQUEST, e);
        return buildLogoutResponse(UNABLE_TO_PARSE_LOGOUT_REQUEST);
      }
    } else {
      try {
        LogoutResponse logoutResponse =
            logoutMessage.extractSamlLogoutResponse(decodeBase64(encodedSamlResponse));
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
    String hostname = SystemBaseUrl.EXTERNAL.getHost();
    String port = SystemBaseUrl.EXTERNAL.getPort();
    String rootContext = SystemBaseUrl.EXTERNAL.getRootContext();

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
      } else {
        return buildLogoutResponse(NO_SUPPORT_FOR_POST_OR_REDIRECT_BINDINGS);
      }
    } catch (Exception e) {
      LOGGER.debug(UNABLE_TO_CREATE_LOGOUT_RESPONSE, e);
      return buildLogoutResponse(UNABLE_TO_CREATE_LOGOUT_RESPONSE);
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

  private String decodeBase64(String encoded) {
    return Base64.getMimeEncoder().encodeToString(encoded.getBytes(StandardCharsets.UTF_8));
  }

  private Response buildLogoutResponse(String message) {
    UriBuilder uriBuilder = UriBuilder.fromUri(SystemBaseUrl.EXTERNAL.getBaseUrl());
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

  public void setLogOutPageTimeOut(long logOutPageTimeOut) {
    this.logOutPageTimeOut = logOutPageTimeOut;
  }
}
