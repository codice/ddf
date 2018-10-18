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

import ddf.security.common.audit.SecurityLogger;
import ddf.security.http.SessionFactory;
import ddf.security.liberty.paos.Request;
import ddf.security.liberty.paos.Response;
import ddf.security.liberty.paos.impl.RequestBuilder;
import ddf.security.liberty.paos.impl.RequestMarshaller;
import ddf.security.liberty.paos.impl.RequestUnmarshaller;
import ddf.security.liberty.paos.impl.ResponseBuilder;
import ddf.security.liberty.paos.impl.ResponseMarshaller;
import ddf.security.liberty.paos.impl.ResponseUnmarshaller;
import ddf.security.samlp.SamlProtocol;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.impl.RelayStates;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.impl.UriBuilderImpl;
import org.apache.cxf.rs.security.saml.sso.SamlpRequestComponentBuilder;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.common.util.DOM2Writer;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.platform.filter.AuthenticationFailureException;
import org.codice.ddf.platform.filter.FilterChain;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.codice.ddf.security.handler.api.AuthenticationHandler;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.saml.SAMLAssertionHandler;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.joda.time.DateTime;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.IDPEntry;
import org.opensaml.saml.saml2.core.IDPList;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml.saml2.core.impl.AuthnContextClassRefBuilder;
import org.opensaml.saml.saml2.core.impl.IDPEntryBuilder;
import org.opensaml.saml.saml2.core.impl.IDPListBuilder;
import org.opensaml.saml.saml2.core.impl.RequestedAuthnContextBuilder;
import org.opensaml.saml.saml2.ecp.RelayState;
import org.opensaml.saml.saml2.ecp.impl.RelayStateBuilder;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Handler for SAML 2.0 IdP based authentication. Unauthenticated clients will be redirected to the
 * configured IdP for authentication.
 */
public class IdpHandler implements AuthenticationHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(IdpHandler.class);

  /** IdP type to use when configuring context policy. */
  public static final String AUTH_TYPE = "IDP";

  public static final String SOURCE = "IdpHandler";

  public static final String UNABLE_TO_ENCODE_SAML_AUTHN_REQUEST =
      "Unable to encode SAML AuthnRequest";

  public static final String UNABLE_TO_SIGN_SAML_AUTHN_REQUEST =
      "Unable to sign SAML Authn Request";

  public static final String PAOS = "PAOS";

  public static final String PAOS_MIME = "application/vnd.paos+xml";

  public static final String PAOS_NS = "urn:liberty:paos:2003-08";

  public static final String ECP_NS = "urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp";

  public static final String SAML_REQUEST = "SAMLRequest";

  public static final String PAOS_REQUEST = "PAOSRequest";

  public static final String PAOS_RESPONSE = "PAOSResponse";

  public static final String ECP_REQUEST = "ECPRequest";

  public static final String ECP_RELAY_STATE = "ECPRelayState";

  public static final String HTTP_SCHEMAS_XMLSOAP_ORG_SOAP_ACTOR_NEXT =
      "http://schemas.xmlsoap.org/soap/actor/next";

  public static final String TLS_SERVER_END_POINT = "tls-server-end-point";

  static {
    OpenSAMLUtil.initSamlEngine();
    XMLObjectProviderRegistry xmlObjectProviderRegistry =
        ConfigurationService.get(XMLObjectProviderRegistry.class);
    xmlObjectProviderRegistry.registerObjectProvider(
        Request.DEFAULT_ELEMENT_NAME,
        new RequestBuilder(),
        new RequestMarshaller(),
        new RequestUnmarshaller());
    xmlObjectProviderRegistry.registerObjectProvider(
        Response.DEFAULT_ELEMENT_NAME,
        new ResponseBuilder(),
        new ResponseMarshaller(),
        new ResponseUnmarshaller());
  }

  private static XMLObjectBuilderFactory builderFactory =
      XMLObjectProviderRegistrySupport.getBuilderFactory();

  @SuppressWarnings("unchecked")
  private static SAMLObjectBuilder<AuthnRequest> authnRequestBuilder =
      (SAMLObjectBuilder<AuthnRequest>)
          builderFactory.getBuilder(AuthnRequest.DEFAULT_ELEMENT_NAME);

  @SuppressWarnings("unchecked")
  private static SAMLObjectBuilder<Issuer> issuerBuilder =
      (SAMLObjectBuilder<Issuer>) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME);

  private final String postBindingTemplate;

  private final SimpleSign simpleSign;

  private final IdpMetadata idpMetadata;

  private final RelayStates<String> relayStates;

  public final String soapMessageTemplate;

  public final String soapfaultMessageTemplate;

  private static final String IDP_METADATA_MISSING =
      "IdP metadata is missing. No IDPSSODescriptor present.";

  private boolean userAgentCheck = true;

  private SessionFactory sessionFactory;

  private List<String> authContextClasses;

  public IdpHandler(SimpleSign simpleSign, IdpMetadata metadata, RelayStates<String> relayStates)
      throws IOException {
    LOGGER.debug("Creating IdP handler.");

    this.simpleSign = simpleSign;
    idpMetadata = metadata;

    this.relayStates = relayStates;

    try (InputStream postFormStream = IdpHandler.class.getResourceAsStream("/post-binding.html");
        InputStream soapMessageStream =
            IdpHandler.class.getResourceAsStream("/templates/soap.handlebars");
        InputStream soapfaultMessageStream =
            IdpHandler.class.getResourceAsStream("/templates/soapfault.handlebars")) {
      postBindingTemplate = IOUtils.toString(postFormStream, StandardCharsets.UTF_8);
      soapMessageTemplate = IOUtils.toString(soapMessageStream, StandardCharsets.UTF_8);
      soapfaultMessageTemplate = IOUtils.toString(soapfaultMessageStream, StandardCharsets.UTF_8);
    }
  }

  @Override
  public String getAuthenticationType() {
    return AUTH_TYPE;
  }

  /**
   * Handler implementing SAML 2.0 IdP authentication. Supports HTTP-Redirect and HTTP-POST
   * bindings.
   *
   * @param request http request to obtain attributes from and to pass into any local filter chains
   *     required
   * @param response http response to return http responses or redirects
   * @param chain original filter chain (should not be called from your handler)
   * @param resolve flag with true implying that credentials should be obtained, false implying
   *     return if no credentials are found.
   * @return result of handling this request - status and optional tokens
   * @throws ServletException
   */
  @Override
  public HandlerResult getNormalizedToken(
      ServletRequest request, ServletResponse response, FilterChain chain, boolean resolve)
      throws AuthenticationFailureException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    if (httpRequest.getMethod().equals("HEAD")) {
      ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_OK);
      try {
        response.flushBuffer();
      } catch (IOException e) {
        throw new AuthenticationFailureException(
            "Unable to send response to HEAD message from IdP client.");
      }
      return new HandlerResult(HandlerResult.Status.NO_ACTION, null);
    }
    HttpServletRequestWrapper wrappedRequest =
        new HttpServletRequestWrapper(httpRequest) {
          @Override
          public Object getAttribute(String name) {
            if (ContextPolicy.ACTIVE_REALM.equals(name)) {
              return "idp";
            }
            return super.getAttribute(name);
          }
        };

    SAMLAssertionHandler samlAssertionHandler = new SAMLAssertionHandler();
    samlAssertionHandler.setSessionFactory(sessionFactory);

    LOGGER.trace("Processing SAML assertion with SAML Handler.");
    HandlerResult samlResult =
        samlAssertionHandler.getNormalizedToken(wrappedRequest, null, null, false);

    if (samlResult != null && samlResult.getStatus() == HandlerResult.Status.COMPLETED) {
      return samlResult;
    }

    if (isEcpEnabled(request)) {
      return doPaosRequest(request, response);
    }

    if (userAgentCheck && userAgentIsNotBrowser(httpRequest)) {
      SecurityLogger.audit("Attempting to log client in as a legacy system.");
      // if we get here, it is most likely an older DDF that is federating
      // it isn't going to understand the redirect to the IdP and it doesn't support ECP
      // so we need to fall back to other handlers to allow it to log in using PKI, Basic or Guest

      return new HandlerResult(HandlerResult.Status.NO_ACTION, null);
    }

    HandlerResult handlerResult = new HandlerResult(HandlerResult.Status.REDIRECTED, null);
    handlerResult.setSource("idp-" + SOURCE);

    String path = httpRequest.getServletPath();
    LOGGER.debug("Doing IdP authentication and authorization for path {}", path);

    // Default to HTTP-Redirect if binding is null
    if (idpMetadata.getSingleSignOnBinding() == null
        || idpMetadata.getSingleSignOnBinding().endsWith("Redirect")) {
      doHttpRedirectBinding((HttpServletRequest) request, (HttpServletResponse) response);
    } else {
      doHttpPostBinding((HttpServletRequest) request, (HttpServletResponse) response);
    }

    return handlerResult;
  }

  private boolean userAgentIsNotBrowser(HttpServletRequest httpRequest) {
    String userAgentHeader = httpRequest.getHeader("User-Agent");
    // basically all browsers support the "Mozilla" way of operating, so they all have "Mozilla"
    // in the string. I just added the rest in case that ever changes for existing browsers.
    // New browsers should contain "Mozilla" as well, though.
    return userAgentHeader == null
        || !(userAgentHeader.contains("Mozilla")
            || userAgentHeader.contains("Safari")
            || userAgentHeader.contains("OPR")
            || userAgentHeader.contains("MSIE")
            || userAgentHeader.contains("Edge")
            || userAgentHeader.contains("Chrome"));
  }

  private boolean isEcpEnabled(ServletRequest request) {
    String acceptHeader = ((HttpServletRequest) request).getHeader(HttpHeaders.ACCEPT);
    String paosHeader = ((HttpServletRequest) request).getHeader(PAOS);
    return acceptHeader != null
        && paosHeader != null
        && acceptHeader.contains(PAOS_MIME)
        && paosHeader.contains(PAOS_NS)
        && paosHeader.contains(ECP_NS);
  }

  private HandlerResult doPaosRequest(ServletRequest request, ServletResponse response) {
    HttpServletResponse httpServletResponse = (HttpServletResponse) response;
    HandlerResult handlerResult = new HandlerResult(HandlerResult.Status.REDIRECTED, null);
    handlerResult.setSource("idp-" + SOURCE);
    String paosHeader = ((HttpServletRequest) request).getHeader(PAOS);

    // some of these options aren't currently used, leaving these here as a marker for what
    // isn't implemented
    boolean wantChannelBind =
        paosHeader.contains("urn:oasis:names:tc:SAML:protocol:ext:channel-binding");
    boolean wantHok = paosHeader.contains("urn:oasis:names:tc:SAML:2.0:cm:holder-of-key");
    boolean wantSigned =
        paosHeader.contains(
            "urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp:2.0:WantAuthnRequestsSigned");
    boolean wantDelegation =
        paosHeader.contains("urn:oasis:names:tc:SAML:2.0:conditions:delegation");

    LOGGER.trace(
        "ECP Client requested: channel bind {}, holder of key {}, signatures {}, delegation {}",
        wantChannelBind,
        wantHok,
        wantSigned,
        wantDelegation);
    LOGGER.trace("Configuring SAML Response for POST.");
    Document doc = DOMUtils.createDocument();
    doc.appendChild(doc.createElement("root"));
    LOGGER.trace("Signing SAML POST Response.");
    String authnRequest;
    String paosRequest;
    String ecpRequest;
    String ecpRelayState;
    try {
      IDPSSODescriptor idpssoDescriptor = idpMetadata.getDescriptor();
      if (idpssoDescriptor == null) {
        throw new AuthenticationFailureException(IDP_METADATA_MISSING);
      }
      authnRequest =
          createAndSignAuthnRequest(
              true, wantSigned && idpssoDescriptor.getWantAuthnRequestsSigned());
      paosRequest = createPaosRequest((HttpServletRequest) request);
      ecpRequest = createEcpRequest();
      ecpRelayState = createEcpRelayState((HttpServletRequest) request);
    } catch (WSSecurityException | AuthenticationFailureException e) {
      LOGGER.debug("Unable to create and sign AuthnRequest.", e);
      httpServletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      try {
        httpServletResponse.flushBuffer();
      } catch (IOException e1) {
        LOGGER.debug("Failed to send error response: {}", e1);
      }
      return handlerResult;
    }

    LOGGER.trace("Converting SAML Response to DOM");
    String soapMessage = soapMessageTemplate.replace("{{" + PAOS_REQUEST + "}}", paosRequest);
    soapMessage = soapMessage.replace("{{" + ECP_REQUEST + "}}", ecpRequest);
    soapMessage = soapMessage.replace("{{" + SAML_REQUEST + "}}", authnRequest);
    soapMessage = soapMessage.replace("{{" + ECP_RELAY_STATE + "}}", ecpRelayState);
    soapMessage = soapMessage.replace("{{" + PAOS_RESPONSE + "}}", "");
    try {
      httpServletResponse.setStatus(HttpServletResponse.SC_OK);
      httpServletResponse.setContentType(PAOS_MIME);
      httpServletResponse.getOutputStream().print(soapMessage);
      httpServletResponse.flushBuffer();
    } catch (IOException ioe) {
      LOGGER.debug("Failed to send auth response: {}", ioe);
    }

    return handlerResult;
  }

  private String createPaosRequest(HttpServletRequest request) throws WSSecurityException {
    String spIssuerId = getSpIssuerId();
    String spAssertionConsumerServiceUrl = getSpAssertionConsumerServiceUrl(spIssuerId);
    RequestBuilder requestBuilder = new RequestBuilder();
    Request paosRequest = requestBuilder.buildObject();
    paosRequest.setResponseConsumerURL(spAssertionConsumerServiceUrl);
    paosRequest.setMessageID(createRelayState(request));
    paosRequest.setService(Request.ECP_SERVICE);
    paosRequest.setSOAP11MustUnderstand(true);
    paosRequest.setSOAP11Actor(HTTP_SCHEMAS_XMLSOAP_ORG_SOAP_ACTOR_NEXT);

    return convertXmlObjectToString(paosRequest);
  }

  private String createEcpRequest() throws WSSecurityException {
    org.opensaml.saml.saml2.ecp.impl.RequestBuilder requestBuilder =
        new org.opensaml.saml.saml2.ecp.impl.RequestBuilder();
    org.opensaml.saml.saml2.ecp.Request ecpRequest = requestBuilder.buildObject();
    ecpRequest.setSOAP11MustUnderstand(true);
    ecpRequest.setSOAP11Actor(HTTP_SCHEMAS_XMLSOAP_ORG_SOAP_ACTOR_NEXT);
    Issuer issuer = issuerBuilder.buildObject();
    issuer.setValue(getSpIssuerId());
    ecpRequest.setIssuer(issuer);

    IDPListBuilder idpListBuilder = new IDPListBuilder();
    IDPList idpList = idpListBuilder.buildObject();
    IDPEntryBuilder idpEntryBuilder = new IDPEntryBuilder();
    IDPEntry idpEntry = idpEntryBuilder.buildObject();
    idpEntry.setProviderID(idpMetadata.getEntityId());
    idpEntry.setName(idpMetadata.getSingleSignOnLocation());
    idpEntry.setLoc(idpMetadata.getSingleSignOnLocation());
    idpList.getIDPEntrys().add(idpEntry);
    ecpRequest.setIDPList(idpList);

    return convertXmlObjectToString(ecpRequest);
  }

  private String createEcpRelayState(HttpServletRequest request) throws WSSecurityException {
    RelayStateBuilder relayStateBuilder = new RelayStateBuilder();
    RelayState relayState = relayStateBuilder.buildObject();
    relayState.setSOAP11Actor(HTTP_SCHEMAS_XMLSOAP_ORG_SOAP_ACTOR_NEXT);
    relayState.setSOAP11MustUnderstand(true);
    relayState.setValue(createRelayState(request));

    return convertXmlObjectToString(relayState);
  }

  private String convertXmlObjectToString(XMLObject xmlObject) throws WSSecurityException {
    Document doc = DOMUtils.createDocument();
    doc.appendChild(doc.createElement("root"));

    Element requestElement = OpenSAMLUtil.toDom(xmlObject, doc);

    return DOM2Writer.nodeToString(requestElement);
  }

  private void doHttpRedirectBinding(HttpServletRequest request, HttpServletResponse response)
      throws AuthenticationFailureException {

    String redirectUrl;
    String idpRequest = null;
    String relayState = createRelayState(request);
    try {
      IDPSSODescriptor idpssoDescriptor = idpMetadata.getDescriptor();
      if (idpssoDescriptor == null) {
        throw new AuthenticationFailureException(IDP_METADATA_MISSING);
      }
      StringBuilder queryParams =
          new StringBuilder("SAMLRequest=")
              .append(
                  encodeAuthnRequest(
                      createAndSignAuthnRequest(
                          false, idpssoDescriptor.getWantAuthnRequestsSigned()),
                      false));
      if (relayState != null) {
        queryParams.append("&RelayState=").append(URLEncoder.encode(relayState, "UTF-8"));
      }
      idpRequest = idpMetadata.getSingleSignOnLocation() + "?" + queryParams;
      UriBuilder idpUri = new UriBuilderImpl(new URI(idpRequest));

      simpleSign.signUriString(queryParams.toString(), idpUri);

      redirectUrl = idpUri.build().toString();
    } catch (UnsupportedEncodingException e) {
      LOGGER.info("Unable to encode relay state: {}", relayState, e);
      throw new AuthenticationFailureException("Unable to create return location");
    } catch (SimpleSign.SignatureException e) {
      String msg = "Unable to sign request";
      LOGGER.info(msg, e);
      throw new AuthenticationFailureException(msg);
    } catch (URISyntaxException e) {
      LOGGER.info("Unable to parse IDP request location: {}", idpRequest, e);
      throw new AuthenticationFailureException("Unable to determine IDP location.");
    }

    try {
      response.sendRedirect(redirectUrl);
      response.flushBuffer();
    } catch (IOException e) {
      LOGGER.info("Unable to redirect AuthnRequest to {}", redirectUrl, e);
      throw new AuthenticationFailureException("Unable to redirect to IdP");
    }
  }

  private void doHttpPostBinding(HttpServletRequest request, HttpServletResponse response)
      throws AuthenticationFailureException {
    try {
      IDPSSODescriptor idpssoDescriptor = idpMetadata.getDescriptor();
      if (idpssoDescriptor == null) {
        throw new AuthenticationFailureException(IDP_METADATA_MISSING);
      }
      response
          .getWriter()
          .printf(
              postBindingTemplate,
              idpMetadata.getSingleSignOnLocation(),
              encodeAuthnRequest(
                  createAndSignAuthnRequest(true, idpssoDescriptor.getWantAuthnRequestsSigned()),
                  true),
              createRelayState(request));
      response.setStatus(200);
      response.flushBuffer();
    } catch (IOException e) {
      LOGGER.info("Unable to post AuthnRequest to IdP", e);
      throw new AuthenticationFailureException("Unable to post to IdP");
    }
  }

  private String createAndSignAuthnRequest(boolean isPost, boolean wantSigned)
      throws AuthenticationFailureException {

    String spIssuerId = getSpIssuerId();
    String spAssertionConsumerServiceUrl = getSpAssertionConsumerServiceUrl(spIssuerId);

    AuthnRequest authnRequest = authnRequestBuilder.buildObject();

    Issuer issuer = issuerBuilder.buildObject();
    issuer.setValue(spIssuerId);
    authnRequest.setIssuer(issuer);

    authnRequest.setAssertionConsumerServiceURL(spAssertionConsumerServiceUrl);

    authnRequest.setID("_" + UUID.randomUUID().toString());
    authnRequest.setVersion(SAMLVersion.VERSION_20);
    authnRequest.setIssueInstant(new DateTime());

    authnRequest.setDestination(idpMetadata.getSingleSignOnLocation());

    authnRequest.setProtocolBinding(SamlProtocol.POST_BINDING);
    authnRequest.setNameIDPolicy(
        SamlpRequestComponentBuilder.createNameIDPolicy(
            true, SAML2Constants.NAMEID_FORMAT_PERSISTENT, spIssuerId));

    RequestedAuthnContextBuilder requestedAuthnContextBuilder = new RequestedAuthnContextBuilder();
    RequestedAuthnContext requestedAuthnContext = requestedAuthnContextBuilder.buildObject();
    AuthnContextClassRefBuilder authnContextClassRefBuilder = new AuthnContextClassRefBuilder();

    for (String authContextClass : authContextClasses) {
      if (StringUtils.isNotEmpty(authContextClass)) {
        AuthnContextClassRef authnContextClassRef = authnContextClassRefBuilder.buildObject();
        authnContextClassRef.setAuthnContextClassRef(authContextClass);
        requestedAuthnContext.getAuthnContextClassRefs().add(authnContextClassRef);
      }
    }

    authnRequest.setRequestedAuthnContext(requestedAuthnContext);

    return serializeAndSign(isPost, wantSigned, authnRequest);
  }

  private String getSpAssertionConsumerServiceUrl(String spIssuerId) {
    return spIssuerId + "/sso";
  }

  private String getSpIssuerId() {
    return SystemBaseUrl.EXTERNAL.constructUrl("/saml", true);
  }

  private String serializeAndSign(boolean isPost, boolean wantSigned, AuthnRequest authnRequest)
      throws AuthenticationFailureException {
    try {
      if (isPost && wantSigned) {
        simpleSign.signSamlObject(authnRequest);
      }

      Document doc = DOMUtils.createDocument();
      doc.appendChild(doc.createElement("root"));

      Element requestElement = OpenSAMLUtil.toDom(authnRequest, doc);

      String requestMessage = DOM2Writer.nodeToString(requestElement);

      LOGGER.trace(requestMessage);

      return requestMessage;
    } catch (WSSecurityException e) {
      LOGGER.info(UNABLE_TO_ENCODE_SAML_AUTHN_REQUEST, e);
      throw new AuthenticationFailureException(UNABLE_TO_ENCODE_SAML_AUTHN_REQUEST);
    } catch (SimpleSign.SignatureException e) {
      LOGGER.info(UNABLE_TO_SIGN_SAML_AUTHN_REQUEST, e);
      throw new AuthenticationFailureException(UNABLE_TO_SIGN_SAML_AUTHN_REQUEST);
    }
  }

  private String encodeAuthnRequest(String requestMessage, boolean isPost)
      throws AuthenticationFailureException {
    try {
      if (isPost) {
        return encodePostRequest(requestMessage);
      } else {
        return encodeRedirectRequest(requestMessage);
      }
    } catch (WSSecurityException | IOException e) {
      LOGGER.info(UNABLE_TO_ENCODE_SAML_AUTHN_REQUEST, e);
      throw new AuthenticationFailureException(UNABLE_TO_ENCODE_SAML_AUTHN_REQUEST);
    }
  }

  private String createRelayState(HttpServletRequest request) {
    return relayStates.encode(recreateFullRequestUrl(request));
  }

  private String encodeRedirectRequest(String request) throws WSSecurityException, IOException {
    return URLEncoder.encode(RestSecurity.deflateAndBase64Encode(request), "UTF-8");
  }

  private String encodePostRequest(String request) throws WSSecurityException {
    return Base64.getEncoder().encodeToString(request.getBytes(StandardCharsets.UTF_8));
  }

  private String recreateFullRequestUrl(HttpServletRequest request) {
    StringBuffer requestURL = request.getRequestURL();

    try {
      URL url = new URL(requestURL.toString());
      if (url.getHost().equals(SystemBaseUrl.EXTERNAL.getHost())
          && String.valueOf(url.getPort()).equals(SystemBaseUrl.EXTERNAL.getPort())
          && !url.getPath().startsWith(SystemBaseUrl.EXTERNAL.getRootContext())) {
        requestURL = new StringBuffer(SystemBaseUrl.EXTERNAL.constructUrl(request.getRequestURI()));
      }
    } catch (MalformedURLException e) {
      LOGGER.error("Unable to convert request URL to URL object.");
    }

    String queryString = request.getQueryString();

    if (queryString == null) {
      return requestURL.toString();
    } else {
      return requestURL.append('?').append(queryString).toString();
    }
  }

  @Override
  public HandlerResult handleError(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
      throws AuthenticationFailureException {
    String realm = (String) servletRequest.getAttribute(ContextPolicy.ACTIVE_REALM);
    HandlerResult result = new HandlerResult(HandlerResult.Status.NO_ACTION, null);
    result.setSource(realm + "-" + SOURCE);
    LOGGER.debug("In error handler for idp - no action taken.");
    return result;
  }

  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  public void setUserAgentCheck(boolean userAgentCheck) {
    this.userAgentCheck = userAgentCheck;
  }

  public List<String> getAuthContextClasses() {
    return authContextClasses;
  }

  public void setAuthContextClasses(List<String> authContextClasses) {
    this.authContextClasses = authContextClasses;
  }
}
