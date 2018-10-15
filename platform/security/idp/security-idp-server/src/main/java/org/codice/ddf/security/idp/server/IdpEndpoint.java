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
package org.codice.ddf.security.idp.server;

import static org.apache.commons.lang.StringUtils.isEmpty;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.MoreExecutors;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.assertion.impl.SecurityAssertionImpl;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.encryption.EncryptionService;
import ddf.security.liberty.paos.Request;
import ddf.security.liberty.paos.impl.RequestBuilder;
import ddf.security.liberty.paos.impl.RequestMarshaller;
import ddf.security.liberty.paos.impl.RequestUnmarshaller;
import ddf.security.liberty.paos.impl.ResponseBuilder;
import ddf.security.liberty.paos.impl.ResponseMarshaller;
import ddf.security.liberty.paos.impl.ResponseUnmarshaller;
import ddf.security.samlp.LogoutMessage;
import ddf.security.samlp.SamlProtocol;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SimpleSign.SignatureException;
import ddf.security.samlp.SystemCrypto;
import ddf.security.samlp.ValidationException;
import ddf.security.samlp.impl.EntityInformation;
import ddf.security.samlp.impl.EntityInformation.ServiceInfo;
import ddf.security.samlp.impl.HtmlResponseTemplate;
import ddf.security.samlp.impl.RelayStates;
import ddf.security.samlp.impl.SPMetadataParser;
import ddf.security.samlp.impl.SamlValidator;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPPart;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.rs.security.saml.sso.SSOConstants;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.http.HttpStatus;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.common.util.DOM2Writer;
import org.boon.Boon;
import org.codehaus.stax2.XMLInputFactory2;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.codice.ddf.security.common.HttpUtils;
import org.codice.ddf.security.common.Security;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.GuestAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.PKIAuthenticationTokenFactory;
import org.codice.ddf.security.handler.api.SAMLAuthenticationToken;
import org.codice.ddf.security.handler.api.SessionHandler;
import org.codice.ddf.security.handler.api.UPAuthenticationToken;
import org.codice.ddf.security.handler.basic.BasicAuthenticationHandler;
import org.codice.ddf.security.handler.pki.PKIHandler;
import org.codice.ddf.security.idp.binding.api.Binding;
import org.codice.ddf.security.idp.binding.api.ResponseCreator;
import org.codice.ddf.security.idp.binding.post.PostBinding;
import org.codice.ddf.security.idp.binding.redirect.RedirectBinding;
import org.codice.ddf.security.idp.binding.soap.SoapBinding;
import org.codice.ddf.security.idp.binding.soap.SoapRequestDecoder;
import org.codice.ddf.security.idp.cache.CookieCache;
import org.codice.ddf.security.idp.plugin.SamlPresignPlugin;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.joda.time.DateTime;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.xmlsec.signature.SignableXMLObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

@Path("/")
public class IdpEndpoint implements Idp, SessionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(IdpEndpoint.class);
  private static final String CERTIFICATES_ATTR = "javax.servlet.request.X509Certificate";
  private static final String IDP_LOGIN = "/idp/login";
  private static final String IDP_LOGOUT = "/idp/logout";
  private static final String AUTHN_REQUEST_MUST_USE_TLS = "Authn Request must use TLS.";
  private static final int THIRTY_MINUTE_EXPIRATION = 30;
  private static final String COULD_NOT_FIND_ENTITY_SERVICE_INFO_MSG =
      "Could not find entity service info for {}";

  /** Input factory */
  private static volatile XMLInputFactory xmlInputFactory;

  static {
    XMLInputFactory xmlInputFactoryTmp = XMLInputFactory2.newInstance();
    xmlInputFactoryTmp.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
    xmlInputFactoryTmp.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
    xmlInputFactoryTmp.setProperty(
        XMLInputFactory.SUPPORT_DTD, Boolean.FALSE); // This disables DTDs entirely for that factory
    xmlInputFactoryTmp.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
    xmlInputFactory = xmlInputFactoryTmp;
  }

  private final ExecutorService asyncLogoutService;
  protected CookieCache cookieCache = new CookieCache();
  private PKIAuthenticationTokenFactory tokenFactory;
  private SecurityManager securityManager;
  private AtomicReference<Map<String, EntityInformation>> serviceProviders =
      new AtomicReference<>();
  private List<String> spMetadata;
  private String indexHtml;
  private Boolean strictSignature = true;
  private Boolean strictRelayState = true;
  private SystemCrypto systemCrypto;
  private LogoutMessage logoutMessage;
  private RelayStates<LogoutState> logoutStates;
  private boolean guestAccess = true;

  private Map<ServiceReference<SamlPresignPlugin>, SamlPresignPlugin> presignPlugins =
      new ConcurrentSkipListMap<>();

  public IdpEndpoint(
      String signaturePropertiesPath,
      String encryptionPropertiesPath,
      EncryptionService encryptionService) {
    systemCrypto =
        new SystemCrypto(encryptionPropertiesPath, signaturePropertiesPath, encryptionService);

    this.asyncLogoutService =
        MoreExecutors.getExitingExecutorService(
            new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                StandardThreadFactoryBuilder.newThreadFactory("asyncLogoutService")));
  }

  public void init() {
    try (InputStream indexStream = IdpEndpoint.class.getResourceAsStream("/html/index.html")) {
      indexHtml = IOUtils.toString(indexStream, StandardCharsets.UTF_8);
    } catch (IOException e) {
      LOGGER.info("Unable to load index page for IDP.", e);
    }

    OpenSAMLUtil.initSamlEngine();
    XMLObjectProviderRegistry xmlObjectProviderRegistry =
        ConfigurationService.get(XMLObjectProviderRegistry.class);
    xmlObjectProviderRegistry.registerObjectProvider(
        Request.DEFAULT_ELEMENT_NAME,
        new RequestBuilder(),
        new RequestMarshaller(),
        new RequestUnmarshaller());
    xmlObjectProviderRegistry.registerObjectProvider(
        ddf.security.liberty.paos.Response.DEFAULT_ELEMENT_NAME,
        new ResponseBuilder(),
        new ResponseMarshaller(),
        new ResponseUnmarshaller());
  }

  @Override
  public Map<String, Set<String>> getActiveSessions() {
    try {
      return Security.getInstance().runWithSubjectOrElevate(this::getActiveSessionsSecure);
    } catch (SecurityServiceException | InvocationTargetException e) {
      SecurityLogger.audit("Failed to run command; insufficient permissions");
    }

    return Collections.emptyMap();
  }

  @VisibleForTesting
  Map<String, Set<String>> getActiveSessionsSecure() {
    return cookieCache.getAllSamlSubjects(securityManager);
  }

  @Override
  public void invalidateSession(String subjectName) {
    try {
      Security.getInstance()
          .runWithSubjectOrElevate(
              (Callable<Void>)
                  () -> {
                    invalidateSessionSecure(subjectName);
                    return null;
                  });
    } catch (SecurityServiceException | InvocationTargetException e) {
      SecurityLogger.audit("Failed to run command; insufficient permissions");
    }
  }

  @VisibleForTesting
  void invalidateSessionSecure(String subjectName) {
    String cacheKey = cookieCache.getCacheKeyBySubjectName(subjectName, securityManager);
    if (cacheKey == null) {
      LOGGER.debug(
          "No cache element found for subject name {}; skipping invalidation.", subjectName);
      return;
    }

    LogoutState logoutState = new LogoutState(getActiveSps(cacheKey));

    logoutState.setNameId(subjectName);
    logoutState.setOriginalRequestId(UUID.randomUUID().toString());
    logoutStates.encode(cacheKey, logoutState);

    cookieCache.removeSamlAssertion(cacheKey);

    asyncLogoutService.submit(() -> asyncBackchannelLogout(cacheKey, logoutState));
  }

  private void asyncBackchannelLogout(String cacheKey, LogoutState logoutState) {
    ExecutorService executorService =
        Executors.newFixedThreadPool(
            4, StandardThreadFactoryBuilder.newThreadFactory("sessionLogout"));
    Map<String, Future<?>> futures = new HashMap<>();
    for (Optional<String> nextTarget = logoutState.getNextTarget();
        nextTarget.isPresent();
        nextTarget = logoutState.getNextTarget()) {
      String entityId = nextTarget.get();

      Future<?> future =
          executorService.submit(() -> sendSoapLogout(cacheKey, logoutState, entityId));

      futures.put(entityId, future);
    }

    if (!MoreExecutors.shutdownAndAwaitTermination(executorService, 5, TimeUnit.MINUTES)
        && LOGGER.isDebugEnabled()) {
      String failedEntityIds =
          futures
              .entrySet()
              .stream()
              .filter(e -> e.getValue().isCancelled())
              .map(Entry::getKey)
              .collect(Collectors.joining());
      LOGGER.debug("Timed out waiting for backchannel logout for entityIds [{}]", failedEntityIds);
    }
  }

  private void sendSoapLogout(String cacheKey, LogoutState logoutState, String entityId) {
    LogoutRequest logoutRequest =
        logoutMessage.buildLogoutRequest(
            logoutState.getNameId(),
            SystemBaseUrl.EXTERNAL.constructUrl(IDP_LOGOUT, true),
            logoutState.getSessionIndexes());

    logoutState.setCurrentRequestId(logoutRequest.getID());
    ServiceInfo entityServiceInfo = getServiceInfo(entityId);
    if (entityServiceInfo == null) {
      return;
    }

    String entityServiceInfoUrl = entityServiceInfo.getUrl();

    // Craft a dummy cookie to hold the cacheKey
    Cookie cookie = new Cookie(COOKIE, cacheKey);
    URI uri = URI.create(entityServiceInfoUrl);
    cookie.setPath(uri.getPath());
    cookie.setDomain(uri.getHost());
    cookie.setHttpOnly(true);
    cookie.setSecure(true);

    try {
      logoutRequest = signLogoutRequest(logoutRequest);
    } catch (SignatureException | XMLStreamException | WSSecurityException e) {
      LOGGER.warn("Unable to sign logout request");
      LOGGER.debug("Unable to sign logout request", e);
      return;
    }

    try {
      String response =
          logoutMessage.sendSamlLogoutRequest(logoutRequest, entityServiceInfoUrl, true, cookie);

      LogoutResponse logoutResponse = parseSoapLogoutResponse(response);
      if (logoutResponse != null) {
        String issuer = logoutResponse.getIssuer().getValue();
        String statusCode = logoutResponse.getStatus().getStatusCode().getValue();
        SecurityLogger.audit(
            "Logout response from entity {} for user {} -- Issuer: {}; StatusCode: {}",
            entityId,
            logoutState.getNameId(),
            issuer,
            statusCode);
      }
    } catch (IOException | WSSecurityException | XMLStreamException | SOAPException e) {
      LOGGER.debug("Error occurred executing idp-initiated single signout");
    }
  }

  private LogoutResponse parseSoapLogoutResponse(String soapResponse)
      throws IOException, XMLStreamException, SOAPException, WSSecurityException {
    try (ByteArrayInputStream bais =
        new ByteArrayInputStream(Base64.getMimeDecoder().decode(soapResponse))) {
      String decodedResponse = IOUtils.toString(bais, StandardCharsets.UTF_8.name());
      SOAPPart soapPart = SamlProtocol.parseSoapMessage(decodedResponse);
      Document document = soapPart.getEnvelope().getBody().extractContentAsDocument();
      return (LogoutResponse) SamlProtocol.getXmlObjectFromNode(document.getFirstChild());
    }
  }

  private LogoutRequest signLogoutRequest(LogoutRequest logoutRequest)
      throws SignatureException, WSSecurityException, XMLStreamException {
    new SimpleSign(systemCrypto).signSamlObject(logoutRequest);
    Element reqElem = logoutMessage.getElementFromSaml(logoutRequest);
    String nodeString = DOM2Writer.nodeToString(reqElem);

    final Document responseDoc =
        StaxUtils.read(new ByteArrayInputStream(nodeString.getBytes(StandardCharsets.UTF_8)));
    return (LogoutRequest) OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
  }

  private ServiceInfo getServiceInfo(String entityId) {
    ServiceInfo entityServiceInfo =
        getServiceProvidersMap().get(entityId).getLogoutService(SamlProtocol.Binding.SOAP);
    if (entityServiceInfo == null) {
      LOGGER.info(COULD_NOT_FIND_ENTITY_SERVICE_INFO_MSG, entityId);
      return null;
    }
    if (entityServiceInfo.getBinding() != SamlProtocol.Binding.SOAP) {
      LOGGER.info("SOAP binding not available for SP [{}]", entityId);
      return null;
    }
    return entityServiceInfo;
  }

  private Map<String, EntityInformation> getServiceProvidersMap() {
    Map<String, EntityInformation> spMap = serviceProviders.get();
    if (spMap == null) {
      spMap = SPMetadataParser.parse(spMetadata, SUPPORTED_BINDINGS);

      boolean updated = serviceProviders.compareAndSet(null, spMap);
      if (!updated) {
        LOGGER.debug("Safe but concurrent update to serviceProviders map; using processed value");
      }
    }

    return Collections.unmodifiableMap(spMap);
  }

  @POST
  @Path("/login")
  @Consumes({"text/xml", "application/soap+xml"})
  public Response doSoapLogin(InputStream body, @Context HttpServletRequest request) {
    if (!request.isSecure()) {
      throw new IllegalArgumentException(AUTHN_REQUEST_MUST_USE_TLS);
    }
    SoapBinding soapBinding =
        new SoapBinding(
            systemCrypto,
            getServiceProvidersMap(),
            getPresignPlugins(),
            spMetadata,
            SUPPORTED_BINDINGS);
    try {
      String bodyStr = IOUtils.toString(body, StandardCharsets.UTF_8);
      AuthnRequest authnRequest = soapBinding.decoder().decodeRequest(bodyStr);
      String relayState = ((SoapRequestDecoder) soapBinding.decoder()).decodeRelayState(bodyStr);
      soapBinding.validator().validateRelayState(relayState, strictRelayState);
      soapBinding
          .validator()
          .validateAuthnRequest(authnRequest, bodyStr, null, null, null, strictSignature);
      boolean hasCookie = hasValidCookie(request, authnRequest.isForceAuthn());
      AuthObj authObj = determineAuthMethod(bodyStr, authnRequest);
      org.opensaml.saml.saml2.core.Response response =
          handleLogin(
              authnRequest,
              authObj.method,
              request,
              authObj,
              soapBinding,
              authnRequest.isPassive(),
              hasCookie,
              authnRequest.getSignature() != null);

      Response samlpResponse =
          soapBinding.creator().getSamlpResponse(relayState, authnRequest, response, null);
      samlpResponse
          .getHeaders()
          .put(
              "SOAPAction",
              Collections.singletonList("http://www.oasis-open.org/committees/security"));
      return samlpResponse;
    } catch (IOException e) {
      LOGGER.debug("Unable to decode SOAP AuthN Request", e);
    } catch (SimpleSign.SignatureException e) {
      LOGGER.debug("Unable to validate signature.", e);
    } catch (ValidationException e) {
      LOGGER.debug("Unable to validate request.", e);
    } catch (SecurityServiceException e) {
      LOGGER.debug("Unable to authenticate user.", e);
    } catch (WSSecurityException | IllegalArgumentException e) {
      LOGGER.debug("Bad request.", e);
    }
    return null;
  }

  @SuppressWarnings({
    "squid:S3776",
    "squid:S135"
  } /* Cognitive complexity, Complex loop with multiple break/continue statements */)
  private AuthObj determineAuthMethod(String bodyStr, AuthnRequest authnRequest) {
    XMLStreamReader xmlStreamReader = null;
    try {
      xmlStreamReader = xmlInputFactory.createXMLStreamReader(new StringReader(bodyStr));
    } catch (XMLStreamException e) {
      LOGGER.debug("Unable to parse SOAP message from client.", e);
    }
    SoapMessage localSoapMessage = new SoapMessage(Soap11.getInstance());
    SAAJInInterceptor.SAAJPreInInterceptor preInInterceptor =
        new SAAJInInterceptor.SAAJPreInInterceptor();
    localSoapMessage.setContent(XMLStreamReader.class, xmlStreamReader);
    preInInterceptor.handleMessage(localSoapMessage);
    SAAJInInterceptor inInterceptor = new SAAJInInterceptor();
    inInterceptor.handleMessage(localSoapMessage);

    SOAPPart soapMessageContent = (SOAPPart) localSoapMessage.getContent(Node.class);
    AuthObj authObj = new AuthObj();
    try {
      Iterator soapHeaderElements =
          soapMessageContent.getEnvelope().getHeader().examineAllHeaderElements();
      while (soapHeaderElements.hasNext()) {
        SOAPHeaderElement soapHeaderElement = (SOAPHeaderElement) soapHeaderElements.next();
        if (soapHeaderElement.getLocalName().equals("Security")) {
          Iterator childElements = soapHeaderElement.getChildElements();
          while (childElements.hasNext()) {
            Object nextElement = childElements.next();
            if (nextElement instanceof SOAPElement) {
              SOAPElement element = (SOAPElement) nextElement;
              if (element.getLocalName().equals("UsernameToken")) {
                Iterator usernameTokenElements = element.getChildElements();
                Object next;
                while (usernameTokenElements.hasNext()) {
                  next = usernameTokenElements.next();
                  if (next instanceof Element) {
                    Element nextEl = (Element) next;
                    if (nextEl.getLocalName().equals("Username")) {
                      authObj.username = nextEl.getTextContent();
                    } else if (nextEl.getLocalName().equals("Password")) {
                      authObj.password = nextEl.getTextContent();
                    }
                  }
                }
                if (authObj.username != null && authObj.password != null) {
                  authObj.method = USER_PASS;
                  break;
                }
              } else if (element.getLocalName().equals("Assertion")
                  && element.getNamespaceURI().equals("urn:oasis:names:tc:SAML:2.0:assertion")) {
                authObj.assertion =
                    new SecurityToken(element.getAttribute("ID"), element, null, null);
                authObj.method = SAML;
                break;
              }
            }
          }
        }
      }
    } catch (SOAPException e) {
      LOGGER.debug("Unable to parse SOAP message.", e);
    }

    RequestedAuthnContext requestedAuthnContext = authnRequest.getRequestedAuthnContext();
    boolean requestingPki = false;
    boolean requestingUp = false;
    if (requestedAuthnContext != null) {
      List<AuthnContextClassRef> authnContextClassRefs =
          requestedAuthnContext.getAuthnContextClassRefs();
      for (AuthnContextClassRef authnContextClassRef : authnContextClassRefs) {
        String authnContextClassRefStr = authnContextClassRef.getAuthnContextClassRef();
        if (SAML2Constants.AUTH_CONTEXT_CLASS_REF_X509.equals(authnContextClassRefStr)
            || SAML2Constants.AUTH_CONTEXT_CLASS_REF_SMARTCARD_PKI.equals(authnContextClassRefStr)
            || SAML2Constants.AUTH_CONTEXT_CLASS_REF_SOFTWARE_PKI.equals(authnContextClassRefStr)
            || SAML2Constants.AUTH_CONTEXT_CLASS_REF_SPKI.equals(authnContextClassRefStr)
            || SAML2Constants.AUTH_CONTEXT_CLASS_REF_TLS_CLIENT.equals(authnContextClassRefStr)) {
          requestingPki = true;
        } else if (SAML2Constants.AUTH_CONTEXT_CLASS_REF_PASSWORD.equals(authnContextClassRefStr)
            || SAML2Constants.AUTH_CONTEXT_CLASS_REF_PASSWORD_PROTECTED_TRANSPORT.equals(
                authnContextClassRefStr)) {
          requestingUp = true;
        }
      }
    } else {
      // The requested auth context isn't required so we don't know what they want... just set both
      // to true
      requestingPki = true;
      requestingUp = true;
    }
    if (requestingUp && authObj.method != null && authObj.method.equals(USER_PASS)) {
      LOGGER.trace("Found UsernameToken and correct AuthnContextClassRef");
      return authObj;
    } else if (requestingPki && authObj.method == null) {
      LOGGER.trace("Found no token, but client requested PKI AuthnContextClassRef");
      authObj.method = PKI;
      return authObj;
    } else if (authObj.method == null) {
      LOGGER.debug(
          "No authentication tokens found for the current request and the client did not request PKI authentication");
    }
    return authObj;
  }

  @POST
  @Path("/login")
  public Response showPostLogin(
      @FormParam(SAML_REQ) String samlRequest,
      @FormParam(RELAY_STATE) String relayState,
      @Context HttpServletRequest request)
      throws WSSecurityException {
    LOGGER.debug("Received POST IdP request.");
    return showLoginPage(
        samlRequest,
        relayState,
        null,
        null,
        request,
        new PostBinding(
            systemCrypto,
            getServiceProvidersMap(),
            getPresignPlugins(),
            spMetadata,
            SUPPORTED_BINDINGS),
        SamlProtocol.POST_BINDING);
  }

  @GET
  @Path("/login")
  public Response showGetLogin(
      @QueryParam(SAML_REQ) String samlRequest,
      @QueryParam(RELAY_STATE) String relayState,
      @QueryParam(SSOConstants.SIG_ALG) String signatureAlgorithm,
      @QueryParam(SSOConstants.SIGNATURE) String signature,
      @Context HttpServletRequest request)
      throws WSSecurityException {
    LOGGER.debug("Received GET IdP request.");
    return showLoginPage(
        samlRequest,
        relayState,
        signatureAlgorithm,
        signature,
        request,
        new RedirectBinding(
            systemCrypto,
            getServiceProvidersMap(),
            getPresignPlugins(),
            spMetadata,
            SUPPORTED_BINDINGS),
        SamlProtocol.REDIRECT_BINDING);
  }

  @SuppressWarnings({
    "squid:S3776",
    "squid:S00107",
    "squid:S1141"
  } /* Cognitive complexity, 8 parameters, nested try */)
  private Response showLoginPage(
      String samlRequest,
      String relayState,
      String signatureAlgorithm,
      String signature,
      HttpServletRequest request,
      Binding binding,
      String originalBinding)
      throws WSSecurityException {
    String responseStr;
    AuthnRequest authnRequest = null;
    try {
      Map<String, Object> responseMap = new HashMap<>();
      binding.validator().validateRelayState(relayState, strictRelayState);
      authnRequest = binding.decoder().decodeRequest(samlRequest);
      authnRequest.getIssueInstant();
      binding
          .validator()
          .validateAuthnRequest(
              authnRequest,
              samlRequest,
              relayState,
              signatureAlgorithm,
              signature,
              strictSignature);
      if (!request.isSecure()) {
        throw new IllegalArgumentException(AUTHN_REQUEST_MUST_USE_TLS);
      }
      X509Certificate[] certs = (X509Certificate[]) request.getAttribute(CERTIFICATES_ATTR);
      boolean hasCerts = (certs != null && certs.length > 0);
      boolean hasCookie = hasValidCookie(request, authnRequest.isForceAuthn());
      if ((authnRequest.isPassive() && hasCerts) || hasCookie) {
        LOGGER.debug("Received Passive & PKI AuthnRequest.");
        org.opensaml.saml.saml2.core.Response samlpResponse;
        try {
          binding = getResponseBinding(authnRequest);

          samlpResponse =
              handleLogin(
                  authnRequest,
                  PKI,
                  request,
                  null,
                  binding,
                  authnRequest.isPassive(),
                  hasCookie,
                  authnRequest.getSignature() != null || signature != null);
          LOGGER.debug("Passive & PKI AuthnRequest logged in successfully.");
        } catch (SecurityServiceException e) {
          LOGGER.debug(e.getMessage(), e);
          return getErrorResponse(relayState, authnRequest, StatusCode.AUTHN_FAILED, binding);
        } catch (WSSecurityException e) {
          LOGGER.debug(e.getMessage(), e);
          return getErrorResponse(relayState, authnRequest, StatusCode.REQUEST_DENIED, binding);
        } catch (ConstraintViolationException e) {
          LOGGER.debug(e.getMessage(), e);
          return getErrorResponse(
              relayState, authnRequest, StatusCode.REQUEST_UNSUPPORTED, binding);
        } catch (IdpException e) {
          LOGGER.debug(e.getMessage(), e);
          return getErrorResponse(
              relayState, authnRequest, StatusCode.UNSUPPORTED_BINDING, binding);
        }
        LOGGER.debug("Returning Passive & PKI SAML Response.");
        NewCookie cookie = null;
        if (hasCookie) {
          cookieCache.addActiveSp(
              getCookie(request).getValue(), authnRequest.getIssuer().getValue());
        } else {
          cookie = createCookie(request, samlpResponse);
          if (cookie != null) {
            cookieCache.addActiveSp(cookie.getValue(), authnRequest.getIssuer().getValue());
          }
        }
        logAddedSp(authnRequest);

        return binding.creator().getSamlpResponse(relayState, authnRequest, samlpResponse, cookie);
      } else {
        LOGGER.debug("Building the JSON map to embed in the index.html page for login.");
        responseMap.put(PKI, hasCerts);
        responseMap.put(GUEST, guestAccess);
        // Using the ORIGINAL request
        // SAML Spec: "The relying party MUST therefore perform the verification step using the
        // original URL-encoded values it received on the query string. It is not sufficient to
        // re-encode the parameters after they have been processed by software because the resulting
        // encoding may not match the signer's encoding".
        responseMap.put(SAML_REQ, samlRequest);
        responseMap.put(RELAY_STATE, relayState);
        String assertionConsumerServiceURL =
            binding.creator().getAssertionConsumerServiceURL(authnRequest);
        responseMap.put(ACS_URL, assertionConsumerServiceURL);
        responseMap.put(SSOConstants.SIG_ALG, signatureAlgorithm);
        responseMap.put(SSOConstants.SIGNATURE, signature);
        responseMap.put(ORIGINAL_BINDING, originalBinding);
      }

      String json = Boon.toJson(responseMap);

      LOGGER.debug("Returning index.html page.");
      responseStr = indexHtml.replace(IDP_STATE_OBJ, json);
      return Response.ok(responseStr).build();
    } catch (IllegalArgumentException e) {
      LOGGER.debug(e.getMessage(), e);
      if (authnRequest != null) {
        try {
          return getErrorResponse(
              relayState, authnRequest, StatusCode.REQUEST_UNSUPPORTED, binding);
        } catch (IOException | SimpleSign.SignatureException e1) {
          LOGGER.debug(e1.getMessage(), e1);
        }
      }
    } catch (UnsupportedOperationException e) {
      LOGGER.debug(e.getMessage(), e);
      if (authnRequest != null) {
        try {
          return getErrorResponse(
              relayState, authnRequest, StatusCode.UNSUPPORTED_BINDING, binding);
        } catch (IOException | SimpleSign.SignatureException e1) {
          LOGGER.debug(e1.getMessage(), e1);
        }
      }
    } catch (SimpleSign.SignatureException e) {
      LOGGER.debug("Unable to validate AuthRequest Signature", e);
    } catch (IOException e) {
      LOGGER.debug("Unable to decode AuthRequest", e);
    } catch (ValidationException e) {
      LOGGER.debug("AuthnRequest schema validation failed.", e);
    }

    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
  }

  void logAddedSp(AuthnRequest authnRequest) {
    LOGGER.debug(
        "request id [{}] added activeSP list: {}",
        authnRequest.getID(),
        authnRequest.getIssuer().getValue());
  }

  private Response getErrorResponse(
      String relayState, AuthnRequest authnRequest, String statusCode, Binding binding)
      throws WSSecurityException, IOException, SimpleSign.SignatureException {
    LOGGER.debug("Creating SAML Response for error condition.");
    org.opensaml.saml.saml2.core.Response samlResponse =
        SamlProtocol.createResponse(
            SamlProtocol.createIssuer(SystemBaseUrl.EXTERNAL.constructUrl(IDP_LOGIN, true)),
            SamlProtocol.createStatus(statusCode),
            authnRequest.getID(),
            null);
    LOGGER.debug("Encoding error SAML Response for post or redirect.");

    if (binding instanceof RedirectBinding) {
      binding =
          new PostBinding(
              systemCrypto,
              getServiceProvidersMap(),
              getPresignPlugins(),
              spMetadata,
              SUPPORTED_BINDINGS);
    }
    return binding.creator().getSamlpResponse(relayState, authnRequest, samlResponse, null);
  }

  @GET
  @Path("/login/sso")
  @SuppressWarnings("squid:S3776" /*complexity*/)
  public Response processLogin(
      @QueryParam(SAML_REQ) String samlRequest,
      @QueryParam(RELAY_STATE) String relayState,
      @QueryParam(AUTH_METHOD) String authMethod,
      @QueryParam(SSOConstants.SIG_ALG) String signatureAlgorithm,
      @QueryParam(SSOConstants.SIGNATURE) String signature,
      @QueryParam(ORIGINAL_BINDING) String originalBinding,
      @Context HttpServletRequest request) {
    LOGGER.debug(
        "Processing login request: [ authMethod {} ], [ sigAlg {} ], [ relayState {} ]",
        authMethod,
        signatureAlgorithm,
        relayState);
    try {
      Binding binding;
      if (!request.isSecure()) {
        throw new IllegalArgumentException(AUTHN_REQUEST_MUST_USE_TLS);
      }
      if (HTTP_POST_BINDING.equals(originalBinding)) {
        binding =
            new PostBinding(
                systemCrypto,
                getServiceProvidersMap(),
                getPresignPlugins(),
                spMetadata,
                SUPPORTED_BINDINGS);
      } else if (HTTP_REDIRECT_BINDING.equals(originalBinding)) {
        binding =
            new RedirectBinding(
                systemCrypto,
                getServiceProvidersMap(),
                getPresignPlugins(),
                spMetadata,
                SUPPORTED_BINDINGS);
      } else {
        throw new IdpException(
            new UnsupportedOperationException("Must use HTTP POST or Redirect bindings."));
      }

      AuthnRequest authnRequest = binding.decoder().decodeRequest(samlRequest);
      binding.validator().validateRelayState(relayState, strictRelayState);
      binding
          .validator()
          .validateAuthnRequest(
              authnRequest,
              samlRequest,
              relayState,
              signatureAlgorithm,
              signature,
              strictSignature);

      binding = getResponseBinding(authnRequest);

      org.opensaml.saml.saml2.core.Response encodedSaml =
          handleLogin(
              authnRequest,
              authMethod,
              request,
              null,
              binding,
              false,
              false,
              authnRequest.getSignature() != null || signature != null);
      LOGGER.debug("Returning SAML Response for relayState: {}", relayState);
      NewCookie newCookie = createCookie(request, encodedSaml);
      Response response =
          binding.creator().getSamlpResponse(relayState, authnRequest, encodedSaml, newCookie);
      if (newCookie != null) {
        cookieCache.addActiveSp(newCookie.getValue(), authnRequest.getIssuer().getValue());
        logAddedSp(authnRequest);
      }

      return response;
    } catch (SecurityServiceException e) {
      LOGGER.info("Unable to retrieve subject for user.", e);
      return Response.status(Response.Status.UNAUTHORIZED).build();
    } catch (WSSecurityException e) {
      LOGGER.info("Unable to encode SAMLP response.", e);
    } catch (SimpleSign.SignatureException e) {
      LOGGER.info("Unable to sign SAML response.", e);
    } catch (IllegalArgumentException | IdpException e) {
      LOGGER.info(e.getMessage(), e);
      return Response.status(Response.Status.BAD_REQUEST).build();
    } catch (ValidationException e) {
      LOGGER.info("AuthnRequest schema validation failed.", e);
      return Response.status(Response.Status.BAD_REQUEST).build();
    } catch (IOException e) {
      LOGGER.info("Unable to create SAML Response.", e);
    }

    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
  }

  @SuppressWarnings({"squid:S3776" /*Cognitive complexity*/, "squid:S00107" /* too many params */})
  protected org.opensaml.saml.saml2.core.Response handleLogin(
      AuthnRequest authnRequest,
      String authMethod,
      HttpServletRequest request,
      AuthObj authObj,
      Binding binding,
      boolean passive,
      boolean hasCookie,
      boolean hasSignature)
      throws SecurityServiceException, WSSecurityException {
    LOGGER.debug("Performing login for user. passive: {}, cookie: {}", passive, hasCookie);
    BaseAuthenticationToken token = null;
    request.setAttribute(ContextPolicy.ACTIVE_REALM, BaseAuthenticationToken.ALL_REALM);
    if (PKI.equals(authMethod)) {
      LOGGER.debug("Logging user in via PKI.");
      PKIHandler pkiHandler = new PKIHandler();
      pkiHandler.setTokenFactory(tokenFactory);
      HandlerResult handlerResult = pkiHandler.getNormalizedToken(request, null, null, false);
      if (handlerResult.getStatus().equals(HandlerResult.Status.COMPLETED)) {
        token = handlerResult.getToken();
      }
    } else if (USER_PASS.equals(authMethod)) {
      LOGGER.debug("Logging user in via BASIC auth.");
      if (authObj != null && authObj.username != null && authObj.password != null) {
        token =
            new UPAuthenticationToken(
                authObj.username, authObj.password, BaseAuthenticationToken.ALL_REALM);
      } else {
        BasicAuthenticationHandler basicAuthenticationHandler = new BasicAuthenticationHandler();
        HandlerResult handlerResult =
            basicAuthenticationHandler.getNormalizedToken(request, null, null, false);
        if (handlerResult.getStatus().equals(HandlerResult.Status.COMPLETED)) {
          token = handlerResult.getToken();
        }
      }
    } else if (SAML.equals(authMethod)) {
      LOGGER.debug("Logging user in via SAML assertion.");
      token =
          new SAMLAuthenticationToken(null, authObj.assertion, BaseAuthenticationToken.ALL_REALM);
    } else if (GUEST.equals(authMethod) && guestAccess) {
      LOGGER.debug("Logging user in as Guest.");
      token =
          new GuestAuthenticationToken(BaseAuthenticationToken.ALL_REALM, request.getRemoteAddr());
    } else {
      throw new IllegalArgumentException("Auth method is not supported.");
    }

    org.w3c.dom.Element samlToken = null;
    String statusCode;
    if (hasCookie) {
      samlToken = getSamlAssertion(request);
      statusCode = StatusCode.SUCCESS;
    } else {
      try {
        statusCode = StatusCode.AUTHN_FAILED;
        Subject subject = securityManager.getSubject(token);
        for (Object principal : subject.getPrincipals().asList()) {
          if (principal instanceof SecurityAssertion) {
            SecurityToken securityToken = ((SecurityAssertion) principal).getSecurityToken();
            samlToken = securityToken.getToken();
          }
        }
        if (samlToken != null) {
          statusCode = StatusCode.SUCCESS;
        }
      } catch (SecurityServiceException e) {
        if (!passive) {
          throw e;
        } else {
          statusCode = StatusCode.AUTHN_FAILED;
        }
      }
    }

    LOGGER.debug("User log in successful.");
    org.opensaml.saml.saml2.core.Response response =
        SamlProtocol.createResponse(
            SamlProtocol.createIssuer(SystemBaseUrl.EXTERNAL.constructUrl(IDP_LOGIN, true)),
            SamlProtocol.createStatus(statusCode),
            authnRequest.getID(),
            samlToken);
    if (hasSignature) {
      response.setDestination(binding.creator().getAssertionConsumerServiceURL(authnRequest));
    }
    return response;
  }

  private Cookie getCookie(HttpServletRequest request) {
    Map<String, Cookie> cookies = HttpUtils.getCookieMap(request);
    return cookies.get(COOKIE);
  }

  private Element getSamlAssertion(HttpServletRequest request) {
    Element samlToken = null;
    Cookie cookie = getCookie(request);
    if (cookie != null) {
      LOGGER.debug("Retrieving cookie {}:{} from cache.", cookie.getValue(), cookie.getName());
      String key = cookie.getValue();
      LOGGER.debug("Retrieving SAML Token from cookie.");
      samlToken = cookieCache.getSamlAssertion(key);
    }
    return samlToken;
  }

  private boolean hasValidCookie(HttpServletRequest request, boolean forceAuthn) {
    Cookie cookie = getCookie(request);
    if (cookie != null) {
      LOGGER.debug("Retrieving cookie {}:{} from cache.", cookie.getValue(), cookie.getName());
      String key = cookie.getValue();
      LOGGER.debug("Retrieving SAML Token from cookie.");
      Element samlToken = cookieCache.getSamlAssertion(key);

      if (samlToken != null) {
        String assertionId = samlToken.getAttribute("ID");
        SecurityToken securityToken = new SecurityToken(assertionId, samlToken, null);
        SecurityAssertionImpl assertion = new SecurityAssertionImpl(securityToken);

        if (forceAuthn || !assertion.isPresentlyValid()) {
          cookieCache.removeSamlAssertion(key);
          return false;
        }
        return true;
      }
    }
    return false;
  }

  private LogoutState getLogoutState(HttpServletRequest request) {
    LogoutState logoutState = null;
    Cookie cookie = getCookie(request);
    if (cookie != null) {
      logoutState = logoutStates.decode(cookie.getValue(), false);
    }
    return logoutState;
  }

  private NewCookie createCookie(
      HttpServletRequest request, org.opensaml.saml.saml2.core.Response response) {
    LOGGER.debug("Creating cookie for user.");
    if (response.getAssertions() != null && !response.getAssertions().isEmpty()) {
      Assertion assertion = response.getAssertions().get(0);
      if (assertion != null) {
        UUID uuid = UUID.randomUUID();

        cookieCache.cacheSamlAssertion(uuid.toString(), assertion.getDOM());
        URL url;
        try {
          url = new URL(request.getRequestURL().toString());
          LOGGER.debug("Returning new cookie for user.");

          String servicesIdpPath = SystemBaseUrl.INTERNAL.getRootContext() + "/idp";

          if (url.getHost().equals(SystemBaseUrl.EXTERNAL.getHost())
              && String.valueOf(url.getPort()).equals(SystemBaseUrl.EXTERNAL.getPort())) {
            servicesIdpPath =
                SystemBaseUrl.EXTERNAL.getRootContext()
                    + SystemBaseUrl.INTERNAL.getRootContext()
                    + "/idp";
          }

          return new NewCookie(
              COOKIE,
              uuid.toString(),
              servicesIdpPath,
              url.getHost(),
              NewCookie.DEFAULT_VERSION,
              null,
              -1,
              null,
              true,
              true);
        } catch (MalformedURLException e) {
          LOGGER.info("Unable to create session cookie. Client will need to log in again.", e);
        }
      }
    }
    return null;
  }

  @GET
  @Path("/login/metadata")
  @Produces("application/xml")
  public Response retrieveMetadata() throws WSSecurityException, CertificateEncodingException {
    List<String> nameIdFormats = new ArrayList<>();
    nameIdFormats.add(SAML2Constants.NAMEID_FORMAT_PERSISTENT);
    nameIdFormats.add(SAML2Constants.NAMEID_FORMAT_UNSPECIFIED);
    nameIdFormats.add(SAML2Constants.NAMEID_FORMAT_X509_SUBJECT_NAME);
    CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
    cryptoType.setAlias(systemCrypto.getSignatureCrypto().getDefaultX509Identifier());
    X509Certificate[] certs = systemCrypto.getSignatureCrypto().getX509Certificates(cryptoType);
    X509Certificate issuerCert = null;
    if (certs != null && certs.length > 0) {
      issuerCert = certs[0];
    }

    cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
    cryptoType.setAlias(systemCrypto.getEncryptionCrypto().getDefaultX509Identifier());
    certs = systemCrypto.getEncryptionCrypto().getX509Certificates(cryptoType);
    X509Certificate encryptionCert = null;
    if (certs != null && certs.length > 0) {
      encryptionCert = certs[0];
    }
    EntityDescriptor entityDescriptor =
        SamlProtocol.createIdpMetadata(
            SystemBaseUrl.EXTERNAL.constructUrl(IDP_LOGIN, true),
            Base64.getEncoder()
                .encodeToString(issuerCert != null ? issuerCert.getEncoded() : new byte[0]),
            Base64.getEncoder()
                .encodeToString(encryptionCert != null ? encryptionCert.getEncoded() : new byte[0]),
            nameIdFormats,
            SystemBaseUrl.EXTERNAL.constructUrl(IDP_LOGIN, true),
            SystemBaseUrl.EXTERNAL.constructUrl(IDP_LOGIN, true),
            SystemBaseUrl.EXTERNAL.constructUrl(IDP_LOGIN, true),
            SystemBaseUrl.EXTERNAL.constructUrl(IDP_LOGOUT, true));
    Document doc = DOMUtils.createDocument();
    doc.appendChild(doc.createElement("root"));
    return Response.ok(DOM2Writer.nodeToString(OpenSAMLUtil.toDom(entityDescriptor, doc, false)))
        .build();
  }

  /**
   * aka HTTP-Redirect
   *
   * @param samlRequest the base64 encoded saml request
   * @param samlResponse the base64 encoded saml response
   * @param relayState the UUID that references the logout state
   * @param signatureAlgorithm this signing algorithm
   * @param signature the signature of the url
   * @param request the http servlet request
   * @return Response redirecting to an service provider
   * @throws WSSecurityException
   * @throws IdpException
   */
  @Override
  @GET
  @Path("/logout")
  public Response processRedirectLogout(
      @QueryParam(SAML_REQ) final String samlRequest,
      @QueryParam(SAML_RESPONSE) final String samlResponse,
      @QueryParam(RELAY_STATE) final String relayState,
      @QueryParam(SSOConstants.SIG_ALG) final String signatureAlgorithm,
      @QueryParam(SSOConstants.SIGNATURE) final String signature,
      @Context final HttpServletRequest request)
      throws WSSecurityException, IdpException {
    LogoutState logoutState = getLogoutState(request);
    Cookie cookie = getCookie(request);

    Binding binding =
        new RedirectBinding(
            systemCrypto,
            getServiceProvidersMap(),
            getPresignPlugins(),
            spMetadata,
            SUPPORTED_BINDINGS);
    binding.validator().validateRelayState(relayState, strictRelayState);

    try {
      if (samlRequest != null) {
        LogoutRequest logoutRequest =
            logoutMessage.extractSamlLogoutRequest(RestSecurity.inflateBase64(samlRequest));
        validateRedirect(
            relayState,
            signatureAlgorithm,
            signature,
            request,
            samlRequest,
            logoutRequest,
            logoutRequest.getIssuer().getValue());
        return handleLogoutRequest(
            cookie, logoutState, logoutRequest, SamlProtocol.Binding.HTTP_REDIRECT, relayState);

      } else if (samlResponse != null) {
        LogoutResponse logoutResponse =
            logoutMessage.extractSamlLogoutResponse(RestSecurity.inflateBase64(samlResponse));
        String requestId = logoutState != null ? logoutState.getCurrentRequestId() : null;
        validateRedirect(
            relayState,
            signatureAlgorithm,
            signature,
            request,
            samlResponse,
            logoutResponse,
            logoutResponse.getIssuer().getValue(),
            requestId);
        return handleLogoutResponse(
            cookie, logoutState, logoutResponse, SamlProtocol.Binding.HTTP_REDIRECT);
      }
    } catch (XMLStreamException e) {
      throw new IdpException("Unable to parse Saml Object.", e);
    } catch (ValidationException e) {
      throw new IdpException("Unable to validate Saml Object", e);
    } catch (IOException e) {
      throw new IdpException("Unable to deflate Saml Object", e);
    }

    throw new IdpException("Could not process logout");
  }

  void validateRedirect(
      String relayState,
      String signatureAlgorithm,
      String signature,
      HttpServletRequest request,
      String samlString,
      SignableXMLObject logoutRequest,
      String issuer)
      throws ValidationException {
    validateRedirect(
        relayState,
        signatureAlgorithm,
        signature,
        request,
        samlString,
        logoutRequest,
        issuer,
        null);
  }

  @SuppressWarnings("squid:S00107" /* 8 parameters */)
  void validateRedirect(
      String relayState,
      String signatureAlgorithm,
      String signature,
      HttpServletRequest request,
      String samlString,
      SignableXMLObject logoutRequest,
      String issuer,
      String requestId)
      throws ValidationException {
    if (strictSignature) {
      if (isEmpty(signature) || isEmpty(signatureAlgorithm) || isEmpty(issuer)) {
        throw new ValidationException("No signature present for AuthnRequest.");
      }
      SamlValidator.Builder validator =
          new SamlValidator.Builder(new SimpleSign(systemCrypto))
              .setRedirectParams(
                  relayState,
                  signature,
                  signatureAlgorithm,
                  samlString,
                  getServiceProvidersMap().get(issuer).getSigningCertificate());

      if (requestId != null) {
        validator.setRequestId(requestId);
      }

      validator.buildAndValidate(
          request.getRequestURL().toString(), SamlProtocol.Binding.HTTP_REDIRECT, logoutRequest);
    }
  }

  @Override
  @POST
  @Path("/logout")
  public Response processPostLogout(
      @FormParam(SAML_REQ) final String samlRequest,
      @FormParam(SAML_RESPONSE) final String samlResponse,
      @FormParam(RELAY_STATE) final String relayState,
      @Context final HttpServletRequest request)
      throws WSSecurityException, IdpException {
    Binding binding =
        new PostBinding(
            systemCrypto,
            getServiceProvidersMap(),
            getPresignPlugins(),
            spMetadata,
            SUPPORTED_BINDINGS);
    binding.validator().validateRelayState(relayState, strictRelayState);
    LogoutState logoutState = getLogoutState(request);
    Cookie cookie = getCookie(request);
    try {
      if (samlRequest != null) {
        LogoutRequest logoutRequest =
            logoutMessage.extractSamlLogoutRequest(RestSecurity.base64Decode(samlRequest));
        validatePost(request, logoutRequest);
        return handleLogoutRequest(
            cookie, logoutState, logoutRequest, SamlProtocol.Binding.HTTP_POST, relayState);
      } else if (samlResponse != null) {
        LogoutResponse logoutResponse =
            logoutMessage.extractSamlLogoutResponse(RestSecurity.base64Decode(samlResponse));
        String requestId = logoutState != null ? logoutState.getCurrentRequestId() : null;
        validatePost(request, logoutResponse, requestId);
        return handleLogoutResponse(
            cookie, logoutState, logoutResponse, SamlProtocol.Binding.HTTP_POST);
      }
    } catch (XMLStreamException e) {
      throw new IdpException("Unable to inflate Saml Object", e);
    } catch (ValidationException e) {
      throw new IdpException("Unable to validate Saml Object", e);
    }

    throw new IdpException("Unable to process logout");
  }

  void validatePost(HttpServletRequest request, SignableSAMLObject samlObject)
      throws ValidationException {
    validatePost(request, samlObject, null);
  }

  void validatePost(HttpServletRequest request, SignableSAMLObject samlObject, String requestId)
      throws ValidationException {
    if (strictSignature) {
      SamlValidator.Builder validator = new SamlValidator.Builder(new SimpleSign(systemCrypto));
      if (requestId != null) {
        validator.setRequestId(requestId);
      }
      validator.buildAndValidate(
          request.getRequestURL().toString(), SamlProtocol.Binding.HTTP_POST, samlObject);
    }
  }

  private Response handleLogoutResponse(
      Cookie cookie,
      LogoutState logoutState,
      LogoutResponse logoutObject,
      SamlProtocol.Binding incomingBinding)
      throws IdpException {
    if (logoutObject != null
        && logoutObject.getStatus() != null
        && logoutObject.getStatus().getStatusCode() != null
        && !StatusCode.SUCCESS.equals(logoutObject.getStatus().getStatusCode().getValue())
        && logoutState != null) {
      logoutState.setPartialLogout(true);
    }
    return continueLogout(logoutState, cookie, incomingBinding);
  }

  Response handleLogoutRequest(
      Cookie cookie,
      LogoutState logoutState,
      LogoutRequest logoutRequest,
      SamlProtocol.Binding incomingBinding,
      String relayState)
      throws IdpException {
    if (logoutState != null) {
      LOGGER.info("Received logout request and already have a logout state (in progress)");
      return Response.ok("Logout already in progress").build();
    }

    if (cookie == null) {
      LOGGER.error("Unable to logout. Cookie not found.");
      return Response.status(HttpStatus.SC_BAD_REQUEST).build();
    }

    LogoutState localLogoutState = new LogoutState(getActiveSps(cookie.getValue()));
    localLogoutState.setOriginalIssuer(logoutRequest.getIssuer().getValue());
    localLogoutState.setNameId(logoutRequest.getNameID().getValue());
    localLogoutState.setOriginalRequestId(logoutRequest.getID());
    localLogoutState.setInitialRelayState(relayState);
    localLogoutState.setSessionIndexObjects(logoutRequest.getSessionIndexes());

    logoutStates.encode(cookie.getValue(), localLogoutState);

    cookieCache.removeSamlAssertion(cookie.getValue());
    return continueLogout(localLogoutState, cookie, incomingBinding);
  }

  private Response continueLogout(
      LogoutState logoutState, Cookie cookie, SamlProtocol.Binding incomingBinding)
      throws IdpException {
    if (logoutState == null) {
      throw new IdpException("Cannot continue a Logout that doesn't exist!");
    }

    try {
      SignableSAMLObject logoutObject;
      String relay = null;
      String entityId = "";
      SamlProtocol.Type samlType;
      EntityInformation.ServiceInfo entityServiceInfo = null;

      Optional<String> nextTarget = logoutState.getNextTarget();
      if (nextTarget.isPresent()) {
        // Another target exists, log them out
        entityId = nextTarget.get();
        if (logoutState.getOriginalIssuer().equals(entityId)) {
          return continueLogout(logoutState, cookie, incomingBinding);
        }
        LogoutRequest logoutRequest =
            logoutMessage.buildLogoutRequest(
                logoutState.getNameId(),
                SystemBaseUrl.EXTERNAL.constructUrl(IDP_LOGIN, true),
                logoutState.getSessionIndexes());

        entityServiceInfo =
            getServiceProvidersMap().get(entityId).getLogoutService(incomingBinding);

        if (entityServiceInfo == null) {
          LOGGER.info(COULD_NOT_FIND_ENTITY_SERVICE_INFO_MSG, entityId);
          return continueLogout(logoutState, cookie, incomingBinding);
        }

        Instant notOnOrAfter = Instant.now().plus(THIRTY_MINUTE_EXPIRATION, ChronoUnit.MINUTES);
        logoutRequest.setNotOnOrAfter(new DateTime(notOnOrAfter.getEpochSecond()));
        logoutRequest.setDestination(entityServiceInfo.getUrl());
        logoutState.setCurrentRequestId(logoutRequest.getID());
        logoutObject = logoutRequest;
        samlType = SamlProtocol.Type.REQUEST;
        relay = null;
      } else {
        // No more targets, respond to original issuer
        entityId = logoutState.getOriginalIssuer();
        logoutObject =
            logoutState.isPartialLogout()
                ? logoutMessage.buildLogoutResponse(
                    SystemBaseUrl.EXTERNAL.constructUrl(IDP_LOGIN, true),
                    StatusCode.SUCCESS,
                    StatusCode.PARTIAL_LOGOUT,
                    logoutState.getOriginalRequestId())
                : logoutMessage.buildLogoutResponse(
                    SystemBaseUrl.EXTERNAL.constructUrl(IDP_LOGIN, true),
                    StatusCode.SUCCESS,
                    logoutState.getOriginalRequestId());

        entityServiceInfo =
            getServiceProvidersMap().get(entityId).getLogoutService(incomingBinding);

        if (entityServiceInfo == null) {
          LOGGER.info(COULD_NOT_FIND_ENTITY_SERVICE_INFO_MSG, entityId);
          return continueLogout(logoutState, cookie, incomingBinding);
        }

        ((LogoutResponse) logoutObject).setDestination(entityServiceInfo.getUrl());
        relay = logoutState.getInitialRelayState();
        logoutStates.decode(cookie.getValue(), true);
        samlType = SamlProtocol.Type.RESPONSE;
      }

      LOGGER.debug(
          "Responding to [{}] with a [{}] and relay state [{}]", entityId, samlType, relay);

      switch (entityServiceInfo.getBinding()) {
        case HTTP_REDIRECT:
          return getSamlRedirectLogoutResponse(
              logoutObject, entityServiceInfo.getUrl(), relay, samlType);
        case HTTP_POST:
          return getSamlPostLogoutResponse(
              logoutObject, entityServiceInfo.getUrl(), relay, samlType);
        default:
          LOGGER.debug("No supported binding available for SP [{}].", entityId);
          logoutState.setPartialLogout(true);
          return continueLogout(logoutState, cookie, incomingBinding);
      }

    } catch (WSSecurityException | SimpleSign.SignatureException | IOException e) {
      LOGGER.debug("Error while processing logout", e);
    }

    throw new IdpException("Server error while processing logout");
  }

  public Set<String> getActiveSps(String cacheId) {
    return cookieCache.getActiveSpSet(cacheId);
  }

  private Response getSamlRedirectLogoutResponse(
      XMLObject samlResponse, String targetUrl, String relayState, SamlProtocol.Type samlType)
      throws IOException, SimpleSign.SignatureException, WSSecurityException {
    LOGGER.debug("Signing SAML response for redirect.");
    Document doc = DOMUtils.createDocument();
    doc.appendChild(doc.createElement("root"));
    String encodedResponse =
        URLEncoder.encode(
            RestSecurity.deflateAndBase64Encode(
                DOM2Writer.nodeToString(OpenSAMLUtil.toDom(samlResponse, doc, false))),
            "UTF-8");
    StringBuilder requestToSign =
        new StringBuilder(samlType.getKey()).append("=").append(encodedResponse);
    if (relayState != null) {
      requestToSign
          .append("&RelayState=")
          .append(URLEncoder.encode(relayState, StandardCharsets.UTF_8.name()));
    }
    UriBuilder uriBuilder = UriBuilder.fromUri(targetUrl);
    uriBuilder.queryParam(samlType.getKey(), encodedResponse);
    if (relayState != null) {
      uriBuilder.queryParam(SSOConstants.RELAY_STATE, relayState);
    }
    new SimpleSign(systemCrypto).signUriString(requestToSign.toString(), uriBuilder);
    LOGGER.debug("Signing successful.");
    return Response.temporaryRedirect(uriBuilder.build()).status(303).build();
  }

  private Response getSamlPostLogoutResponse(
      SignableSAMLObject samlObject,
      String targetUrl,
      String relayState,
      SamlProtocol.Type samlType)
      throws SimpleSign.SignatureException, WSSecurityException {
    Document doc = DOMUtils.createDocument();
    doc.appendChild(doc.createElement("root"));
    LOGGER.debug("Signing SAML POST Response.");
    new SimpleSign(systemCrypto).signSamlObject(samlObject);
    LOGGER.debug("Converting SAML Response to DOM");
    String assertionResponse = DOM2Writer.nodeToString(OpenSAMLUtil.toDom(samlObject, doc));
    String encodedSamlResponse =
        Base64.getEncoder().encodeToString(assertionResponse.getBytes(StandardCharsets.UTF_8));
    return Response.ok(
            HtmlResponseTemplate.getPostPage(targetUrl, samlType, encodedSamlResponse, relayState))
        .build();
  }

  private Binding getResponseBinding(AuthnRequest authnRequest) throws IdpException {
    String assertionConsumerServiceBinding =
        ResponseCreator.getAssertionConsumerServiceBinding(authnRequest, getServiceProvidersMap());

    if (HTTP_POST_BINDING.equals(assertionConsumerServiceBinding)) {
      return new PostBinding(
          systemCrypto,
          getServiceProvidersMap(),
          getPresignPlugins(),
          spMetadata,
          SUPPORTED_BINDINGS);
    } else if (HTTP_REDIRECT_BINDING.equals(assertionConsumerServiceBinding)) {
      throw new IdpException(
          new UnsupportedOperationException(
              "HTTP Redirect binding is not supported for single sign on responses."));
    } else {
      throw new IdpException(new UnsupportedOperationException("Must use HTTP POST binding."));
    }
  }

  public void setSecurityManager(SecurityManager securityManager) {
    this.securityManager = securityManager;
  }

  public void setTokenFactory(PKIAuthenticationTokenFactory tokenFactory) {
    this.tokenFactory = tokenFactory;
  }

  public void setSpMetadata(List<String> spMetadata) {
    this.spMetadata = spMetadata;
    serviceProviders.getAndSet(null);
  }

  public void setStrictSignature(Boolean strictSignature) {
    this.strictSignature = strictSignature;
  }

  public void setStrictRelayState(Boolean strictRelayState) {
    this.strictRelayState = strictRelayState;
  }

  public void setExpirationTime(int expirationTime) {
    this.cookieCache.setExpirationTime(expirationTime);
  }

  public void setLogoutMessage(LogoutMessage logoutMessage) {
    this.logoutMessage = logoutMessage;
  }

  public void setLogoutStates(RelayStates<LogoutState> logoutStates) {
    this.logoutStates = logoutStates;
  }

  public RelayStates<LogoutState> getLogoutStates() {
    return this.logoutStates;
  }

  public void setGuestAccess(boolean guestAccess) {
    this.guestAccess = guestAccess;
  }

  public void bindPresignPlugin(ServiceReference<SamlPresignPlugin> pluginRef) {
    if (pluginRef == null) {
      return;
    }

    final Bundle bundle = FrameworkUtil.getBundle(IdpEndpoint.class);
    if (bundle != null) {
      final SamlPresignPlugin service = bundle.getBundleContext().getService(pluginRef);
      presignPlugins.put(pluginRef, service);
    }
  }

  public void unbindPresignPlugin(ServiceReference<SamlPresignPlugin> pluginRef) {
    if (pluginRef == null) {
      return;
    }

    presignPlugins.remove(pluginRef);
  }

  private Set<SamlPresignPlugin> getPresignPlugins() {
    return new HashSet<>(presignPlugins.values());
  }

  private static class AuthObj {
    String method;

    String username;

    String password;

    SecurityToken assertion;
  }
}
