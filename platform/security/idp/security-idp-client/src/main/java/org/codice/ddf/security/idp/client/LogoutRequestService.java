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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
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
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.xmlsec.signature.SignableXMLObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

@Path("logout")
public class LogoutRequestService {

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
      submitForm = IOUtils.toString(submitStream);
      redirectPage = IOUtils.toString(redirectStream);
    } catch (Exception e) {
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
        logout();
        LogoutRequest logoutRequest = logoutMessage.buildLogoutRequest(name, getEntityId());

        String relayState = relayStates.encode(name);

        return getLogoutRequest(relayState, logoutRequest);
      } catch (Exception e) {
        String msg = "Failed to create logout request.";
        LOGGER.info(msg, e);
        return buildLogoutResponse(msg);
      }

    } else {
      String msg = "Failed to decrypt logout request params. Invalid number of params.";
      LOGGER.info(msg);
      return buildLogoutResponse(msg);
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
        return buildLogoutResponse(
            "The identity provider does not support either POST or Redirect bindings.");
      }
    } catch (Exception e) {
      String msg = "Failed to create logout request";
      LOGGER.debug(msg, e);
      return buildLogoutResponse(msg);
    }
  }

  private Response getSamlpPostLogoutRequest(String relayState, LogoutRequest logoutRequest)
      throws SimpleSign.SignatureException, WSSecurityException {
    LOGGER.debug("Configuring SAML LogoutRequest for POST.");
    Document doc = DOMUtils.createDocument();
    doc.appendChild(doc.createElement("root"));
    LOGGER.debug("Signing SAML POST LogoutRequest.");
    simpleSign.signSamlObject(logoutRequest);
    LOGGER.debug("Converting SAML Request to DOM");
    String assertionResponse = DOM2Writer.nodeToString(OpenSAMLUtil.toDom(logoutRequest, doc));
    String encodedSamlRequest =
        Base64.getEncoder().encodeToString(assertionResponse.getBytes(StandardCharsets.UTF_8));
    String singleLogoutLocation = idpMetadata.getSingleLogoutLocation();
    String submitFormUpdated =
        String.format(
            submitForm, singleLogoutLocation, SAML_REQUEST, encodedSamlRequest, relayState);
    Response.ResponseBuilder ok = Response.ok(submitFormUpdated);
    return ok.build();
  }

  private Response getSamlpRedirectLogoutRequest(String relayState, LogoutRequest logoutRequest)
      throws IOException, SimpleSign.SignatureException, WSSecurityException, URISyntaxException {
    LOGGER.debug("Configuring SAML Response for Redirect.");
    Document doc = DOMUtils.createDocument();
    doc.appendChild(doc.createElement("root"));
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
          String msg = "Unable to parse logout request.";
          LOGGER.debug(msg);
          return buildLogoutResponse(msg);
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
        String msg = "Failed to sign logout response.";
        LOGGER.info(msg, e);
        return buildLogoutResponse(msg);
      } catch (ValidationException e) {
        String msg = "Unable to validate";
        LOGGER.info(msg, e);
        return buildLogoutResponse(msg);
      } catch (XMLStreamException e) {
        String msg = "Unable to parse logout request.";
        LOGGER.info(msg, e);
        return buildLogoutResponse(msg);
      }
    } else {
      try {
        LogoutResponse logoutResponse =
            logoutMessage.extractSamlLogoutResponse(decodeBase64(encodedSamlResponse));
        if (logoutResponse == null) {
          String msg = "Unable to parse logout response.";
          LOGGER.info(msg);
          return buildLogoutResponse(msg);
        }
        new SamlValidator.Builder(simpleSign)
            .buildAndValidate(
                request.getRequestURL().toString(), SamlProtocol.Binding.HTTP_POST, logoutResponse);
      } catch (ValidationException e) {
        String msg = "Unable to validate";
        LOGGER.info(msg, e);
        return buildLogoutResponse(msg);
      } catch (WSSecurityException | XMLStreamException e) {
        String msg = "Unable to parse logout response.";
        LOGGER.info(msg, e);
        return buildLogoutResponse(msg);
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
          String msg = "Unable to parse logout request.";
          return buildLogoutResponse(msg);
        }
        buildAndValidateSaml(
            deflatedSamlRequest, relayState, signatureAlgorithm, signature, logoutRequest);
        logout();
        String entityId = getEntityId();
        LogoutResponse logoutResponse =
            logoutMessage.buildLogoutResponse(entityId, StatusCode.SUCCESS, logoutRequest.getID());
        return getLogoutResponse(relayState, logoutResponse);
      } catch (IOException e) {
        String msg = "Unable to decode and inflate logout request.";
        LOGGER.info(msg, e);
        return buildLogoutResponse(msg);
      } catch (ValidationException e) {
        String msg = "Unable to validate";
        LOGGER.info(msg, e);
        return buildLogoutResponse(msg);
      } catch (WSSecurityException | XMLStreamException e) {
        String msg = "Unable to parse logout request.";
        LOGGER.info(msg, e);
        return buildLogoutResponse(msg);
      }
    } else {
      try {

        LogoutResponse logoutResponse =
            logoutMessage.extractSamlLogoutResponse(
                RestSecurity.inflateBase64(deflatedSamlResponse));
        if (logoutResponse == null) {
          String msg = "Unable to parse logout response.";
          LOGGER.debug(msg);
          return buildLogoutResponse(msg);
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
        String msg = "Unable to decode and inflate logout response.";
        LOGGER.info(msg, e);
        return buildLogoutResponse(msg);
      } catch (ValidationException e) {
        String msg = "Unable to validate";
        LOGGER.info(msg, e);
        return buildLogoutResponse(msg);
      } catch (WSSecurityException | XMLStreamException e) {
        String msg = "Unable to parse logout response.";
        LOGGER.info(msg, e);
        return buildLogoutResponse(msg);
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
    String hostname = SystemBaseUrl.getHost();
    String port = SystemBaseUrl.getPort();
    String rootContext = SystemBaseUrl.getRootContext();

    return String.format("https://%s:%s%s/saml", hostname, port, rootContext);
  }

  private void logout() {
    HttpSession session = sessionFactory.getOrCreateSession(request);

    SecurityTokenHolder tokenHolder =
        ((SecurityTokenHolder) session.getAttribute(SecurityConstants.SAML_ASSERTION));

    SecurityAssertion securityAssertion =
        new SecurityAssertionImpl(tokenHolder.getSecurityToken("idp"));

    boolean hasSecurityAuditRole =
        Arrays.stream(System.getProperty("security.audit.roles").split(","))
            .filter(role -> securityAssertion.getPrincipals().contains(new RolePrincipal(role)))
            .findFirst()
            .isPresent();

    if (hasSecurityAuditRole) {
      SecurityLogger.audit(
          "Subject with admin privileges has logged out: {}",
          securityAssertion.getPrincipal().getName());
    }

    tokenHolder.remove("idp");
  }

  private Response getLogoutResponse(String relayState, LogoutResponse samlResponse) {
    try {

      String binding = idpMetadata.getSingleLogoutBinding();
      if (SamlProtocol.POST_BINDING.equals(binding)) {
        return getSamlpPostLogoutResponse(relayState, samlResponse);
      } else if (SamlProtocol.REDIRECT_BINDING.equals(binding)) {
        return getSamlpRedirectLogoutResponse(relayState, samlResponse);
      } else {
        return buildLogoutResponse(
            "The identity provider does not support either POST or Redirect bindings.");
      }
    } catch (Exception e) {
      String msg = "Failed to create logout response";
      LOGGER.debug(msg, e);
      return buildLogoutResponse(msg);
    }
  }

  private Response getSamlpPostLogoutResponse(String relayState, LogoutResponse samlResponse)
      throws WSSecurityException, SimpleSign.SignatureException {
    LOGGER.debug("Configuring SAML Response for POST.");
    Document doc = DOMUtils.createDocument();
    doc.appendChild(doc.createElement("root"));
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
    doc.appendChild(doc.createElement("root"));
    URI location =
        logoutMessage.signSamlGetResponse(
            samlResponse, new URI(idpMetadata.getSingleLogoutLocation()), relayState);

    return Response.ok(HtmlResponseTemplate.getRedirectPage(location.toString())).build();
  }

  private String decodeBase64(String encoded) {
    return Base64.getMimeEncoder().encodeToString(encoded.getBytes(StandardCharsets.UTF_8));
  }

  private Response buildLogoutResponse(String message) {
    UriBuilder uriBuilder = UriBuilder.fromUri(SystemBaseUrl.getBaseUrl());
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
