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

import ddf.security.http.SessionFactory;
import ddf.security.samlp.SamlProtocol;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;
import ddf.security.samlp.ValidationException;
import ddf.security.samlp.impl.RelayStates;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
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
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPPart;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.common.util.DOM2Writer;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.platform.filter.AuthenticationException;
import org.codice.ddf.platform.filter.SecurityFilter;
import org.codice.ddf.security.common.HttpUtils;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.codice.ddf.security.filter.websso.WebSSOFilter;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.saml.SAMLAssertionHandler;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

@Path("sso")
public class AssertionConsumerService {

  private static final Logger LOGGER = LoggerFactory.getLogger(IdpHandler.class);

  private static final String SAML_RESPONSE = "SAMLResponse";

  private static final String RELAY_STATE = "RelayState";

  private static final String SIG_ALG = "SigAlg";

  private static final String SIGNATURE = "Signature";

  private static final String UNABLE_TO_LOGIN =
      "Unable to login with provided AuthN response assertion.";

  private final SimpleSign simpleSign;

  private final IdpMetadata idpMetadata;

  private final RelayStates<String> relayStates;

  @Context private HttpServletRequest request;

  private SecurityFilter loginFilter;

  private SystemCrypto systemCrypto;

  private SessionFactory sessionFactory;

  static {
    OpenSAMLUtil.initSamlEngine();
  }

  public AssertionConsumerService(
      SimpleSign simpleSign,
      IdpMetadata metadata,
      SystemCrypto crypto,
      RelayStates<String> relayStates) {
    this.simpleSign = simpleSign;
    idpMetadata = metadata;
    systemCrypto = crypto;
    this.relayStates = relayStates;
  }

  @POST
  @Consumes({"*/*"})
  @Produces(MediaType.APPLICATION_FORM_URLENCODED)
  public Response postSamlResponse(
      @FormParam(SAML_RESPONSE) String encodedSamlResponse,
      @FormParam(RELAY_STATE) String relayState) {

    return processSamlResponse(RestSecurity.base64Decode(encodedSamlResponse), relayState, false);
  }

  @POST
  @Consumes({"text/xml", "application/soap+xml"})
  public Response processSoapResponse(InputStream body, @Context HttpServletRequest request) {
    try {
      SOAPPart soapMessage = SamlProtocol.parseSoapMessage(IOUtils.toString(body));
      String relayState = getRelayState(soapMessage);
      org.opensaml.saml.saml2.core.Response samlpResponse = getSamlpResponse(soapMessage);
      boolean validateResponse = validateResponse(samlpResponse, false);
      if (validateResponse) {
        return processSamlResponse(samlpResponse, relayState, false);
      }
    } catch (XMLStreamException e) {
      LOGGER.debug("Unable to parse SOAP message from response.", e);
    } catch (IOException e) {
      LOGGER.debug("Unable to get SAMLP response.", e);
    } catch (SOAPException e) {
      LOGGER.debug("Unable to get relay state from response.", e);
    }
    return Response.serverError().entity("Invalid AuthN response.").build();
  }

  private String getRelayState(SOAPPart soapRequest) throws SOAPException {
    Iterator responseHeaderElements =
        soapRequest.getEnvelope().getHeader().examineAllHeaderElements();
    while (responseHeaderElements.hasNext()) {
      Object nextElement = responseHeaderElements.next();
      if (nextElement instanceof SOAPElement) {
        SOAPElement soapHeaderElement = (SOAPElement) nextElement;
        if (RELAY_STATE.equals(soapHeaderElement.getLocalName())) {
          return soapHeaderElement.getValue();
        }
      }
    }
    return "";
  }

  private org.opensaml.saml.saml2.core.Response getSamlpResponse(SOAPPart soapRequest)
      throws IOException {
    XMLObject responseXmlObj;
    try {
      responseXmlObj =
          SamlProtocol.getXmlObjectFromNode(soapRequest.getEnvelope().getBody().getFirstChild());
    } catch (WSSecurityException | SOAPException | XMLStreamException ex) {
      throw new IOException("Unable to convert AuthnRequest document to XMLObject.");
    }
    if (!(responseXmlObj instanceof org.opensaml.saml.saml2.core.Response)) {
      throw new IOException("SAMLRequest object is not org.opensaml.saml.saml2.core.Response.");
    }
    return (org.opensaml.saml.saml2.core.Response) responseXmlObj;
  }

  /**
   * The HTTP-Redirect binding should not be used for Single Sign-On responses.
   *
   * <p>SAML Profiles Spec: The identity provider issues a <Response> message to be delivered by the
   * user agent to the service provider. Either the HTTP POST, or HTTP Artifact binding can be used
   * to transfer the message to the service provider through the user agent. The HTTP Redirect
   * binding MUST NOT be used, as the response will typically exceed the URL length permitted by
   * most user agents.
   *
   * <p>Keeping this method to work with non-conformant Identity Providers.
   */
  @GET
  public Response getSamlResponse(
      @QueryParam(SAML_RESPONSE) String deflatedSamlResponse,
      @QueryParam(RELAY_STATE) String relayState,
      @QueryParam(SIG_ALG) String signatureAlgorithm,
      @QueryParam(SIGNATURE) String signature) {

    LOGGER.warn("HTTP-Redirect binding should not be used for Single Sign-On responses");
    if (StringUtils.isBlank(deflatedSamlResponse)) {
      return Response.serverError().entity("SAML is in a bad state.").build();
    }
    if (!validateSignature(deflatedSamlResponse, relayState, signatureAlgorithm, signature)) {
      return Response.serverError().entity("Invalid AuthN response signature.").build();
    }

    try {
      return processSamlResponse(
          RestSecurity.inflateBase64(deflatedSamlResponse), relayState, signature != null);

    } catch (IOException e) {
      String msg = "Unable to decode and inflate AuthN response.";
      LOGGER.info(msg, e);
      return Response.serverError().entity(msg).build();
    }
  }

  private boolean validateSignature(
      String deflatedSamlResponse, String relayState, String signatureAlgorithm, String signature) {
    boolean signaturePasses = false;
    if (signature != null) {
      if (StringUtils.isNotBlank(deflatedSamlResponse)
          && StringUtils.isNotBlank(relayState)
          && StringUtils.isNotBlank(signatureAlgorithm)) {
        try {
          String signedMessage =
              String.format(
                  "%s=%s&%s=%s&%s=%s",
                  SAML_RESPONSE,
                  URLEncoder.encode(deflatedSamlResponse, StandardCharsets.UTF_8.name()),
                  RELAY_STATE,
                  URLEncoder.encode(relayState, StandardCharsets.UTF_8.name()),
                  SIG_ALG,
                  URLEncoder.encode(signatureAlgorithm, StandardCharsets.UTF_8.name()));
          signaturePasses =
              simpleSign.validateSignature(
                  signatureAlgorithm,
                  signedMessage,
                  signature,
                  idpMetadata.getSigningCertificate());
        } catch (SimpleSign.SignatureException | UnsupportedEncodingException e) {
          LOGGER.debug("Failed to validate AuthN response signature.", e);
        }
      }
    } else {
      LOGGER.info(
          "Received unsigned AuthN response.  Could not verify IDP identity or response integrity.");
      signaturePasses = true;
    }

    return signaturePasses;
  }

  public Response processSamlResponse(
      org.opensaml.saml.saml2.core.Response samlResponse,
      String relayState,
      boolean wasRedirectSigned) {
    if (samlResponse == null) {
      return Response.serverError().entity("Unable to parse AuthN response.").build();
    }

    if (!validateResponse(samlResponse, wasRedirectSigned)) {
      return Response.serverError().entity("AuthN response failed validation.").build();
    }

    String redirectLocation = relayStates.decode(relayState);
    if (StringUtils.isBlank(redirectLocation)) {
      return Response.serverError()
          .entity(
              "AuthN response returned unknown or expired relay state. Please refresh the page or resend the request to continue the login process.")
          .build();
    }

    URI relayUri;
    try {
      relayUri = new URI(redirectLocation);
      // if the host names don't match up then the cookie won't work correctly and the
      // requester will be stuck in an infinite loop of redirects
      if (relayUri.getHost() != null && !relayUri.getHost().equals(request.getServerName())) {
        relayUri =
            new URI(
                relayUri.getScheme(),
                relayUri.getUserInfo(),
                request.getServerName(),
                relayUri.getPort(),
                relayUri.getPath(),
                relayUri.getQuery(),
                relayUri.getFragment());
      }
      // this avoids the user ever being redirected to the built in login page if they've already
      // logged in via the IDP.
      if ((relayUri.getPath().equals("/login") || relayUri.getPath().equals("/login/"))
          && (relayUri.getQuery() != null && relayUri.getQuery().contains("prevurl"))) {
        relayUri =
            new URI(
                relayUri.getScheme(),
                relayUri.getUserInfo(),
                relayUri.getHost(),
                relayUri.getPort(),
                relayUri.getQuery().replace("prevurl=", ""),
                null,
                relayUri.getFragment());
      }
    } catch (URISyntaxException e) {
      LOGGER.info("Unable to parse relay state.", e);
      return Response.serverError().entity("Unable to redirect back to original location.").build();
    }

    if (!login(samlResponse)) {
      return Response.serverError().entity(UNABLE_TO_LOGIN).build();
    }

    LOGGER.trace("Successfully logged in.  Redirecting to {}", relayUri);
    return Response.temporaryRedirect(relayUri).build();
  }

  public Response processSamlResponse(
      String authnResponse, String relayState, boolean wasRedirectSigned) {
    LOGGER.trace(authnResponse);

    org.opensaml.saml.saml2.core.Response samlResponse = extractSamlResponse(authnResponse);
    boolean responseHasSignature = samlResponse != null && samlResponse.getSignature() != null;
    return processSamlResponse(samlResponse, relayState, wasRedirectSigned || responseHasSignature);
  }

  private boolean validateResponse(
      org.opensaml.saml.saml2.core.Response samlResponse, boolean wasRedirectSigned) {
    try {
      AuthnResponseValidator validator = new AuthnResponseValidator(simpleSign, wasRedirectSigned);
      validator.validate(samlResponse);
    } catch (ValidationException e) {
      LOGGER.info("Invalid AuthN response received from {}", samlResponse.getIssuer(), e);
      return false;
    }

    return true;
  }

  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  private boolean login(org.opensaml.saml.saml2.core.Response samlResponse) {
    if (!request.isSecure()) {
      return false;
    }
    Map<String, Cookie> cookieMap = HttpUtils.getCookieMap(request);
    if (cookieMap.containsKey("JSESSIONID")) {
      sessionFactory.getOrCreateSession(request).invalidate();
    }
    String assertionValue = DOM2Writer.nodeToString(samlResponse.getAssertions().get(0).getDOM());

    String encodedAssertion;
    try {
      encodedAssertion = RestSecurity.deflateAndBase64Encode(assertionValue);
    } catch (IOException e) {
      LOGGER.info("Unable to deflate and encode assertion.", e);
      return false;
    }

    final String authHeader = RestSecurity.SAML_HEADER_PREFIX + encodedAssertion;

    HttpServletRequestWrapper wrappedRequest =
        new HttpServletRequestWrapper(request) {
          @Override
          public String getHeader(String name) {
            if (RestSecurity.AUTH_HEADER.equals(name)) {
              return authHeader;
            }
            return super.getHeader(name);
          }

          @Override
          public Object getAttribute(String name) {
            if (ContextPolicy.ACTIVE_REALM.equals(name)) {
              return "idp";
            }
            return super.getAttribute(name);
          }
        };

    SAMLAssertionHandler samlAssertionHandler = new SAMLAssertionHandler();

    LOGGER.trace("Processing SAML assertion with SAML Handler.");
    HandlerResult samlResult =
        samlAssertionHandler.getNormalizedToken(wrappedRequest, null, null, false);

    if (samlResult.getStatus() != HandlerResult.Status.COMPLETED) {
      LOGGER.debug("Failed to handle SAML assertion.");
      return false;
    }

    request.setAttribute(WebSSOFilter.DDF_AUTHENTICATION_TOKEN, samlResult);
    request.removeAttribute(ContextPolicy.NO_AUTH_POLICY);

    try {
      LOGGER.trace("Trying to login with provided SAML assertion.");
      loginFilter.doFilter(wrappedRequest, null, (servletRequest, servletResponse) -> {});
    } catch (IOException | AuthenticationException e) {
      LOGGER.debug("Failed to apply login filter to SAML assertion", e);
      return false;
    }

    return true;
  }

  @GET
  @Path("/metadata")
  @Produces("application/xml")
  public Response retrieveMetadata() throws WSSecurityException, CertificateEncodingException {
    List<String> nameIdFormats = new ArrayList<>();
    nameIdFormats.add(SAML2Constants.NAMEID_FORMAT_PERSISTENT);
    nameIdFormats.add(SAML2Constants.NAMEID_FORMAT_UNSPECIFIED);
    nameIdFormats.add(SAML2Constants.NAMEID_FORMAT_X509_SUBJECT_NAME);
    X509Certificate issuerCert =
        findCertificate(systemCrypto.getSignatureAlias(), systemCrypto.getSignatureCrypto());
    X509Certificate encryptionCert =
        findCertificate(systemCrypto.getEncryptionAlias(), systemCrypto.getEncryptionCrypto());

    String entityId = SystemBaseUrl.EXTERNAL.constructUrl("/saml", true);
    String logoutLocation = SystemBaseUrl.EXTERNAL.constructUrl("/saml/logout", true);
    String assertionConsumerServiceLocation =
        SystemBaseUrl.EXTERNAL.constructUrl("/saml/sso", true);

    EntityDescriptor entityDescriptor =
        SamlProtocol.createSpMetadata(
            entityId,
            Base64.getEncoder().encodeToString(issuerCert.getEncoded()),
            Base64.getEncoder().encodeToString(encryptionCert.getEncoded()),
            nameIdFormats,
            logoutLocation,
            assertionConsumerServiceLocation,
            assertionConsumerServiceLocation,
            assertionConsumerServiceLocation);

    Document doc = DOMUtils.createDocument();
    doc.appendChild(doc.createElement("root"));
    return Response.ok(DOM2Writer.nodeToString(OpenSAMLUtil.toDom(entityDescriptor, doc, false)))
        .build();
  }

  private X509Certificate findCertificate(String alias, Crypto crypto) throws WSSecurityException {
    CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
    cryptoType.setAlias(alias);
    X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
    if (certs == null) {
      throw new WSSecurityException(
          WSSecurityException.ErrorCode.SECURITY_ERROR, "Unable to retrieve certificate");
    }
    return certs[0];
  }

  private org.opensaml.saml.saml2.core.Response extractSamlResponse(String samlResponse) {
    org.opensaml.saml.saml2.core.Response response = null;
    try {
      Document responseDoc =
          StaxUtils.read(new ByteArrayInputStream(samlResponse.getBytes(StandardCharsets.UTF_8)));
      XMLObject responseXmlObject = OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());

      if (responseXmlObject instanceof org.opensaml.saml.saml2.core.Response) {
        response = (org.opensaml.saml.saml2.core.Response) responseXmlObject;
      }
    } catch (XMLStreamException | WSSecurityException e) {
      LOGGER.debug("Failed to convert AuthN response string to object.", e);
    }

    return response;
  }

  public SecurityFilter getLoginFilter() {
    return loginFilter;
  }

  public void setLoginFilter(SecurityFilter loginFilter) {
    this.loginFilter = loginFilter;
  }

  public void setRequest(HttpServletRequest request) {
    this.request = request;
  }
}
