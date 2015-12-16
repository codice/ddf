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
package org.codice.ddf.security.idp.server;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Encoded;
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
import javax.xml.stream.XMLStreamException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.rs.security.saml.sso.SSOConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.common.util.DOM2Writer;
import org.boon.Boon;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.security.common.HttpUtils;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.GuestAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.PKIAuthenticationTokenFactory;
import org.codice.ddf.security.handler.basic.BasicAuthenticationHandler;
import org.codice.ddf.security.handler.pki.PKIHandler;
import org.codice.ddf.security.idp.binding.api.Binding;
import org.codice.ddf.security.idp.binding.api.ResponseCreator;
import org.codice.ddf.security.idp.binding.api.impl.ResponseCreatorImpl;
import org.codice.ddf.security.idp.binding.post.PostBinding;
import org.codice.ddf.security.idp.binding.redirect.RedirectBinding;
import org.codice.ddf.security.idp.cache.CookieCache;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.opensaml.common.SignableSAMLObject;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.validation.ValidatingXMLObject;
import org.opensaml.xml.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.encryption.EncryptionService;
import ddf.security.samlp.LogoutService;
import ddf.security.samlp.MetadataConfigurationParser;
import ddf.security.samlp.SamlProtocol;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;
import ddf.security.samlp.impl.EntityInformation;
import ddf.security.samlp.impl.RelayStates;
import ddf.security.samlp.impl.SamlValidator;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;

@Path("/")
public class IdpEndpoint implements Idp {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdpEndpoint.class);

    private static final String CERTIFICATES_ATTR = "javax.servlet.request.X509Certificate";

    public static final String SERVICES_IDP_PATH = "/services/idp";

    protected CookieCache cookieCache = new CookieCache();

    private SystemBaseUrl systemBaseUrl = new SystemBaseUrl();

    private PKIAuthenticationTokenFactory tokenFactory;

    private SecurityManager securityManager;

    private Map<String, EntityInformation> serviceProviders = new ConcurrentHashMap<>();

    private String indexHtml;

    private String submitForm;

    private String redirectPage;

    private Boolean strictSignature = true;

    private SystemCrypto systemCrypto;

    private LogoutService logoutService;

    private RelayStates<LogoutState> logoutStates;

    public static final ImmutableSet<UsageType> USAGE_TYPES = ImmutableSet.of(UsageType.UNSPECIFIED,
            UsageType.SIGNING);

    public IdpEndpoint(String signaturePropertiesPath, String encryptionPropertiesPath,
            EncryptionService encryptionService) {
        systemCrypto = new SystemCrypto(encryptionPropertiesPath,
                signaturePropertiesPath,
                encryptionService);
    }

    public void init() {
        try (
                InputStream indexStream = IdpEndpoint.class.getResourceAsStream("/html/index.html");
                InputStream submitFormStream = IdpEndpoint.class.getResourceAsStream(
                        "/templates/submitForm.handlebars");
                InputStream redirectPageStream = IdpEndpoint.class.getResourceAsStream(
                        "/templates/redirect.handlebars")
        ) {
            indexHtml = IOUtils.toString(indexStream);
            submitForm = IOUtils.toString(submitFormStream);
            redirectPage = IOUtils.toString(redirectPageStream);
        } catch (Exception e) {
            LOGGER.error("Unable to load index page for IDP.", e);
        }

        OpenSAMLUtil.initSamlEngine();
    }

    private void parseServiceProviderMetadata(List<String> serviceProviderMetadata) {
        if (serviceProviderMetadata != null) {
            try {
                MetadataConfigurationParser metadataConfigurationParser =
                        new MetadataConfigurationParser(serviceProviderMetadata,
                                ed -> serviceProviders.put(ed.getEntityID(),
                                        new EntityInformation.Builder(ed,
                                                SUPPORTED_BINDINGS).build()));

                serviceProviders.putAll(metadataConfigurationParser.getEntryDescriptions()
                        .entrySet()
                        .stream()
                        .map(e -> Maps.immutableEntry(e.getKey(),
                                new EntityInformation.Builder(e.getValue(),
                                        SUPPORTED_BINDINGS).build()))
                        .filter(e -> nonNull(e.getValue()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

            } catch (IOException e) {
                LOGGER.error("Unable to parse SP metadata configuration.", e);
            }
        }
    }

    @POST
    @Path("/login")
    public Response showPostLogin(@FormParam(SAML_REQ) String samlRequest,
            @FormParam(RELAY_STATE) String relayState, @Context HttpServletRequest request)
            throws WSSecurityException {
        LOGGER.debug("Received POST IdP request.");
        return showLoginPage(samlRequest,
                relayState,
                null,
                null,
                request,
                new PostBinding(systemCrypto, serviceProviders),
                submitForm,
                SamlProtocol.POST_BINDING);
    }

    @GET
    @Path("/login")
    public Response showGetLogin(@QueryParam(SAML_REQ) String samlRequest,
            @Encoded @QueryParam(RELAY_STATE) String relayState,
            @QueryParam(SSOConstants.SIG_ALG) String signatureAlgorithm,
            @QueryParam(SSOConstants.SIGNATURE) String signature,
            @Context HttpServletRequest request) throws WSSecurityException {
        LOGGER.debug("Received GET IdP request.");
        return showLoginPage(samlRequest,
                relayState,
                signatureAlgorithm,
                signature,
                request,
                new RedirectBinding(systemCrypto, serviceProviders),
                redirectPage,
                SamlProtocol.REDIRECT_BINDING);
    }

    private Response showLoginPage(String samlRequest, String relayState, String signatureAlgorithm,
            String signature, HttpServletRequest request, Binding binding, String template,
            String originalBinding) throws WSSecurityException {
        String responseStr;
        AuthnRequest authnRequest = null;
        try {
            Map<String, Object> responseMap = new HashMap<>();
            binding.validator()
                    .validateRelayState(relayState);
            authnRequest = binding.decoder()
                    .decodeRequest(samlRequest);
            authnRequest.getIssueInstant();
            binding.validator()
                    .validateAuthnRequest(authnRequest,
                            samlRequest,
                            relayState,
                            signatureAlgorithm,
                            signature,
                            strictSignature);
            X509Certificate[] certs = (X509Certificate[]) request.getAttribute(CERTIFICATES_ATTR);
            boolean hasCerts = (certs != null && certs.length > 0);
            boolean hasCookie = getSamlAssertion(request, authnRequest.isForceAuthn()) != null;
            if ((authnRequest.isPassive() && hasCerts) || hasCookie) {
                LOGGER.debug("Received Passive & PKI AuthnRequest.");
                org.opensaml.saml2.core.Response samlpResponse;
                try {
                    samlpResponse = handleLogin(authnRequest,
                            Idp.PKI,
                            request,
                            authnRequest.isPassive(),
                            hasCookie);
                    LOGGER.debug("Passive & PKI AuthnRequest logged in successfully.");
                } catch (SecurityServiceException e) {
                    LOGGER.error(e.getMessage(), e);
                    return getErrorResponse(relayState,
                            authnRequest,
                            StatusCode.AUTHN_FAILED_URI,
                            binding);
                } catch (WSSecurityException e) {
                    LOGGER.error(e.getMessage(), e);
                    return getErrorResponse(relayState,
                            authnRequest,
                            StatusCode.REQUEST_DENIED_URI,
                            binding);
                } catch (SimpleSign.SignatureException e) {
                    LOGGER.error(e.getMessage(), e);
                    return getErrorResponse(relayState,
                            authnRequest,
                            StatusCode.REQUEST_UNSUPPORTED_URI,
                            binding);
                }
                LOGGER.debug("Returning Passive & PKI SAML Response.");
                NewCookie cookie = null;
                if (hasCookie) {
                    cookieCache.addActiveSp(getCookie(request).getValue(),
                            authnRequest.getIssuer()
                                    .getValue());
                } else {
                    cookie = createCookie(request, samlpResponse);
                    if (cookie != null) {
                        cookieCache.addActiveSp(cookie.getValue(),
                                authnRequest.getIssuer()
                                        .getValue());
                    }
                }
                logAddedSp(authnRequest);

                return binding.creator()
                        .getSamlpResponse(relayState,
                                authnRequest,
                                samlpResponse,
                                cookie,
                                template);
            } else {
                LOGGER.debug("Building the JSON map to embed in the index.html page for login.");
                Document doc = DOMUtils.createDocument();
                doc.appendChild(doc.createElement("root"));
                String authn = DOM2Writer.nodeToString(OpenSAMLUtil.toDom(authnRequest,
                        doc,
                        false));
                String encodedAuthn = RestSecurity.deflateAndBase64Encode(authn);
                responseMap.put(PKI, hasCerts);
                responseMap.put(SAML_REQ, encodedAuthn);
                responseMap.put(RELAY_STATE, relayState);
                String assertionConsumerServiceURL =
                        ((ResponseCreatorImpl) binding.creator()).getAssertionConsumerServiceURL(
                                authnRequest);
                responseMap.put(ACS_URL, assertionConsumerServiceURL);
                responseMap.put(SSOConstants.SIG_ALG, signatureAlgorithm);
                responseMap.put(SSOConstants.SIGNATURE, signature);
                responseMap.put(ORIGINAL_BINDING, originalBinding);
            }

            String json = Boon.toJson(responseMap);

            LOGGER.debug("Returning index.html page.");
            responseStr = indexHtml.replace(IDP_STATE_OBJ, json);
            return Response.ok(responseStr)
                    .build();
        } catch (IllegalArgumentException e) {
            LOGGER.error(e.getMessage(), e);
            if (authnRequest != null) {
                try {
                    return getErrorResponse(relayState,
                            authnRequest,
                            StatusCode.REQUEST_UNSUPPORTED_URI,
                            binding);
                } catch (IOException | SimpleSign.SignatureException e1) {
                    LOGGER.error(e1.getMessage(), e1);
                }
            }
        } catch (UnsupportedOperationException e) {
            LOGGER.error(e.getMessage(), e);
            if (authnRequest != null) {
                try {
                    return getErrorResponse(relayState,
                            authnRequest,
                            StatusCode.UNSUPPORTED_BINDING_URI,
                            binding);
                } catch (IOException | SimpleSign.SignatureException e1) {
                    LOGGER.error(e1.getMessage(), e1);
                }
            }
        } catch (SimpleSign.SignatureException e) {
            LOGGER.error("Unable to validate AuthRequest Signature", e);
        } catch (IOException e) {
            LOGGER.error("Unable to decode AuthRequest", e);
        } catch (ValidationException e) {
            LOGGER.error("AuthnRequest schema validation failed.", e);
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .build();
    }

    void logAddedSp(AuthnRequest authnRequest) {
        LOGGER.debug("request id [{}] added activeSP list: {}",
                authnRequest.getID(),
                authnRequest.getIssuer()
                        .getValue());
    }

    private Response getErrorResponse(String relayState, AuthnRequest authnRequest,
            String statusCode, Binding binding)
            throws WSSecurityException, IOException, SimpleSign.SignatureException {
        LOGGER.debug("Creating SAML Response for error condition.");
        org.opensaml.saml2.core.Response samlResponse =
                SamlProtocol.createResponse(SamlProtocol.createIssuer(systemBaseUrl.constructUrl(
                        "/idp/login",
                        true)), SamlProtocol.createStatus(statusCode), authnRequest.getID(), null);
        LOGGER.debug("Encoding error SAML Response for post or redirect.");
        String template = "";
        if (binding instanceof PostBinding) {
            template = submitForm;
        } else if (binding instanceof RedirectBinding) {
            template = redirectPage;
        }
        return binding.creator()
                .getSamlpResponse(relayState, authnRequest, samlResponse, null, template);
    }

    @GET
    @Path("/login/sso")
    public Response processLogin(@QueryParam(SAML_REQ) String samlRequest,
            @QueryParam(RELAY_STATE) String relayState, @QueryParam(AUTH_METHOD) String authMethod,
            @QueryParam(SSOConstants.SIG_ALG) String signatureAlgorithm,
            @QueryParam(SSOConstants.SIGNATURE) String signature,
            @QueryParam(ORIGINAL_BINDING) String originalBinding,
            @Context HttpServletRequest request) {
        LOGGER.debug("Processing login request: [ authMethod {} ], [ sigAlg {} ], [ relayState {} ]",
                authMethod,
                signatureAlgorithm,
                relayState);
        try {
            Binding binding;
            String template;
            //the authn request is always encoded as if it came in via redirect when coming from the web app
            Binding redirectBinding = new RedirectBinding(systemCrypto, serviceProviders);
            AuthnRequest authnRequest = redirectBinding.decoder()
                    .decodeRequest(samlRequest);
            String assertionConsumerServiceBinding =
                    ResponseCreator.getAssertionConsumerServiceBinding(authnRequest,
                            serviceProviders);
            if (HTTP_POST_BINDING.equals(originalBinding)) {
                binding = new PostBinding(systemCrypto, serviceProviders);
                template = submitForm;
            } else if (HTTP_REDIRECT_BINDING.equals(originalBinding)) {
                binding = redirectBinding;
                template = redirectPage;
            } else {
                throw new IdpException(new UnsupportedOperationException(
                        "Must use HTTP POST or Redirect bindings."));
            }
            binding.validator()
                    .validateAuthnRequest(authnRequest,
                            samlRequest,
                            relayState,
                            signatureAlgorithm,
                            signature,
                            strictSignature);

            if (HTTP_POST_BINDING.equals(assertionConsumerServiceBinding)) {
                if (!(binding instanceof PostBinding)) {
                    binding = new PostBinding(systemCrypto, serviceProviders);
                }
            } else if (HTTP_REDIRECT_BINDING.equals(assertionConsumerServiceBinding)) {
                if (!(binding instanceof RedirectBinding)) {
                    binding = new RedirectBinding(systemCrypto, serviceProviders);
                }
            }
            org.opensaml.saml2.core.Response encodedSaml = handleLogin(authnRequest,
                    authMethod,
                    request,
                    false,
                    false);
            LOGGER.debug("Returning SAML Response for relayState: {}" + relayState);
            NewCookie newCookie = createCookie(request, encodedSaml);
            Response response = binding.creator()
                    .getSamlpResponse(relayState, authnRequest, encodedSaml, newCookie, template);
            if (newCookie != null) {
                cookieCache.addActiveSp(newCookie.getValue(),
                        authnRequest.getIssuer()
                                .getValue());
                logAddedSp(authnRequest);
            }

            return response;
        } catch (SecurityServiceException e) {
            LOGGER.warn("Unable to retrieve subject for user.", e);
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build();
        } catch (WSSecurityException e) {
            LOGGER.error("Unable to encode SAMLP response.", e);
        } catch (SimpleSign.SignatureException e) {
            LOGGER.error("Unable to sign SAML response.", e);
        } catch (IllegalArgumentException e) {
            LOGGER.error(e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .build();
        } catch (ValidationException e) {
            LOGGER.error("AuthnRequest schema validation failed.", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .build();
        } catch (IOException e) {
            LOGGER.error("Unable to create SAML Response.", e);
        } catch (IdpException e) {
            LOGGER.error("", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .build();
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .build();
    }

    protected synchronized org.opensaml.saml2.core.Response handleLogin(AuthnRequest authnRequest,
            String authMethod, HttpServletRequest request, boolean passive, boolean hasCookie)
            throws SecurityServiceException, WSSecurityException, SimpleSign.SignatureException {
        LOGGER.debug("Performing login for user. passive: {}, cookie: {}", passive, hasCookie);
        BaseAuthenticationToken token = null;
        request.setAttribute(ContextPolicy.ACTIVE_REALM, BaseAuthenticationToken.ALL_REALM);
        if (Idp.PKI.equals(authMethod)) {
            LOGGER.debug("Logging user in via PKI.");
            PKIHandler pkiHandler = new PKIHandler();
            pkiHandler.setTokenFactory(tokenFactory);
            try {
                HandlerResult handlerResult = pkiHandler.getNormalizedToken(request,
                        null,
                        null,
                        false);
                if (handlerResult.getStatus()
                        .equals(HandlerResult.Status.COMPLETED)) {
                    token = handlerResult.getToken();
                }
            } catch (ServletException e) {
                LOGGER.warn("Encountered an exception while checking for PKI auth info.", e);
            }
        } else if (USER_PASS.equals(authMethod)) {
            LOGGER.debug("Logging user in via BASIC auth.");
            BasicAuthenticationHandler basicAuthenticationHandler =
                    new BasicAuthenticationHandler();
            HandlerResult handlerResult = basicAuthenticationHandler.getNormalizedToken(request,
                    null,
                    null,
                    false);
            if (handlerResult.getStatus()
                    .equals(HandlerResult.Status.COMPLETED)) {
                token = handlerResult.getToken();
            }
        } else if (GUEST.equals(authMethod)) {
            LOGGER.debug("Logging user in as Guest.");
            token = new GuestAuthenticationToken(BaseAuthenticationToken.ALL_REALM,
                    request.getRemoteAddr());
        } else {
            throw new IllegalArgumentException("Auth method is not supported.");
        }

        org.w3c.dom.Element samlToken = null;
        String statusCode;
        if (hasCookie) {
            samlToken = getSamlAssertion(request);
            statusCode = StatusCode.SUCCESS_URI;
        } else {
            try {
                statusCode = StatusCode.AUTHN_FAILED_URI;
                Subject subject = securityManager.getSubject(token);
                for (Object principal : subject.getPrincipals()
                        .asList()) {
                    if (principal instanceof SecurityAssertion) {
                        SecurityToken securityToken =
                                ((SecurityAssertion) principal).getSecurityToken();
                        samlToken = securityToken.getToken();
                    }
                }
                if (samlToken != null) {
                    statusCode = StatusCode.SUCCESS_URI;
                }
            } catch (SecurityServiceException e) {
                if (!passive) {
                    throw e;
                } else {
                    statusCode = StatusCode.AUTHN_FAILED_URI;
                }
            }
        }

        LOGGER.debug("User log in successful.");
        return SamlProtocol.createResponse(SamlProtocol.createIssuer(systemBaseUrl.constructUrl(
                "/idp/login",
                true)), SamlProtocol.createStatus(statusCode), authnRequest.getID(), samlToken);
    }

    private synchronized Cookie getCookie(HttpServletRequest request) {
        Map<String, Cookie> cookies = HttpUtils.getCookieMap(request);
        return cookies.get(COOKIE);
    }

    private synchronized Element getSamlAssertion(HttpServletRequest request) {
        return getSamlAssertion(request, false);
    }

    private synchronized Element getSamlAssertion(HttpServletRequest request, boolean forceAuthn) {
        Element samlToken = null;
        Cookie cookie = getCookie(request);
        if (cookie != null) {
            LOGGER.debug("Retrieving cookie from cache.");
            if (forceAuthn) {
                cookieCache.removeSamlAssertion(cookie.getValue());
            } else {
                samlToken = cookieCache.getSamlAssertion(cookie.getValue());
            }
        }
        return samlToken;
    }

    private synchronized LogoutState getLogoutState(HttpServletRequest request) {
        LogoutState logoutState = null;
        Cookie cookie = getCookie(request);
        if (cookie != null) {
            logoutState = logoutStates.decode(cookie.getValue(), false);
        }
        return logoutState;
    }

    private synchronized NewCookie createCookie(HttpServletRequest request,
            org.opensaml.saml2.core.Response response) {
        LOGGER.debug("Creating cookie for user.");
        if (response.getAssertions() != null && response.getAssertions()
                .size() > 0) {
            Assertion assertion = response.getAssertions()
                    .get(0);
            if (assertion != null) {
                UUID uuid = UUID.randomUUID();

                cookieCache.cacheSamlAssertion(uuid.toString(), assertion.getDOM());
                URL url;
                try {
                    url = new URL(request.getRequestURL()
                            .toString());
                    LOGGER.debug("Returning new cookie for user.");

                    return new NewCookie(COOKIE,
                            uuid.toString(),
                            SERVICES_IDP_PATH,
                            url.getHost(),
                            NewCookie.DEFAULT_VERSION,
                            null,
                            -1,
                            true);
                } catch (MalformedURLException e) {
                    LOGGER.warn("Unable to create session cookie. Client will need to log in again.",
                            e);
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
        cryptoType.setAlias(systemCrypto.getSignatureCrypto()
                .getDefaultX509Identifier());
        X509Certificate[] certs = systemCrypto.getSignatureCrypto()
                .getX509Certificates(cryptoType);
        X509Certificate issuerCert = null;
        if (certs != null && certs.length > 0) {
            issuerCert = certs[0];
        }

        cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias(systemCrypto.getEncryptionCrypto()
                .getDefaultX509Identifier());
        certs = systemCrypto.getEncryptionCrypto()
                .getX509Certificates(cryptoType);
        X509Certificate encryptionCert = null;
        if (certs != null && certs.length > 0) {
            encryptionCert = certs[0];
        }
        EntityDescriptor entityDescriptor =
                SamlProtocol.createIdpMetadata(systemBaseUrl.constructUrl("/idp/login", true),
                        Base64.encodeBase64String(
                                issuerCert != null ? issuerCert.getEncoded() : new byte[0]),
                        Base64.encodeBase64String(
                                encryptionCert != null ? encryptionCert.getEncoded() : new byte[0]),
                        nameIdFormats,
                        systemBaseUrl.constructUrl("/idp/login", true),
                        systemBaseUrl.constructUrl("/idp/login", true),
                        systemBaseUrl.constructUrl("/idp/logout", true));
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
        return Response.ok(DOM2Writer.nodeToString(OpenSAMLUtil.toDom(entityDescriptor,
                doc,
                false)))
                .build();
    }

    /**
     * aka HTTP-Redirect
     *
     * @param samlRequest
     * @param relayState
     * @param signatureAlgorithm
     * @param signature
     * @param request
     * @return
     * @throws WSSecurityException
     */
    @Override
    @GET
    @Path("/logout")
    public Response processRedirectLogout(@QueryParam(SAML_REQ) final String samlRequest,
            @QueryParam(SAML_RESPONSE) final String samlResponse,
            @QueryParam(RELAY_STATE) final String relayState,
            @QueryParam(SSOConstants.SIG_ALG) final String signatureAlgorithm,
            @QueryParam(SSOConstants.SIGNATURE) final String signature,
            @Context final HttpServletRequest request) throws WSSecurityException, IdpException {
        // TODO - if ID present in request, Validate InResponseTo field - https://codice.atlassian.net/browse/DDF-1725
        // also don't forget to do POST
        LogoutState logoutState = getLogoutState(request);
        Cookie cookie = getCookie(request);

        try {
            if (samlRequest != null) {
                LogoutRequest logoutRequest =
                        logoutService.extractSamlLogoutRequest(RestSecurity.inflateBase64(
                                samlRequest));
                validateRedirect(relayState,
                        signatureAlgorithm,
                        signature,
                        request,
                        samlRequest,
                        logoutRequest,
                        logoutRequest.getIssuer()
                                .getValue());
                return handleLogoutRequest(cookie,
                        logoutState,
                        logoutRequest,
                        SamlProtocol.Binding.HTTP_REDIRECT,
                        relayState);

            } else if (samlResponse != null) {
                LogoutResponse logoutResponse =
                        logoutService.extractSamlLogoutResponse(RestSecurity.inflateBase64(
                                samlResponse));
                validateRedirect(relayState,
                        signatureAlgorithm,
                        signature,
                        request,
                        samlResponse,
                        logoutResponse,
                        logoutResponse.getIssuer()
                                .getValue());
                return handleLogoutResponse(cookie,
                        logoutState,
                        logoutResponse,
                        SamlProtocol.Binding.HTTP_REDIRECT);
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

    void validateRedirect(String relayState, String signatureAlgorithm, String signature,
            HttpServletRequest request, String samlString, ValidatingXMLObject logoutRequest,
            String issuer) throws ValidationException {
        if (strictSignature) {
            if (isEmpty(signature) || isEmpty(signatureAlgorithm) || isEmpty(issuer)) {
                throw new ValidationException("No signature present for AuthnRequest.");
            }
            String signingCertificate = serviceProviders.get(issuer)
                    .getSigningCertificate();

            new SamlValidator.Builder(new SimpleSign(systemCrypto)).setRedirectParams(relayState,
                    signature,
                    signatureAlgorithm,
                    samlString,
                    serviceProviders.get(issuer)
                            .getSigningCertificate())
                    .buildAndValidate(request.getRequestURL()
                            .toString(), SamlProtocol.Binding.HTTP_REDIRECT, logoutRequest);
        }
    }

    @Override
    @POST
    @Path("/logout")
    public Response processPostLogout(@FormParam(SAML_REQ) final String samlRequest,
            @FormParam(SAML_RESPONSE) final String samlResponse,
            @FormParam(RELAY_STATE) final String relayState,
            @Context final HttpServletRequest request) throws WSSecurityException, IdpException {
        LogoutState logoutState = getLogoutState(request);
        Cookie cookie = getCookie(request);
        try {
            if (samlRequest != null) {
                LogoutRequest logoutRequest =
                        logoutService.extractSamlLogoutRequest(RestSecurity.inflateBase64(
                                samlRequest));
                validatePost(request, logoutRequest);
                return handleLogoutRequest(cookie,
                        logoutState,
                        logoutRequest,
                        SamlProtocol.Binding.HTTP_POST,
                        relayState);
            } else if (samlResponse != null) {
                LogoutResponse logoutResponse =
                        logoutService.extractSamlLogoutResponse(RestSecurity.inflateBase64(
                                samlResponse));
                validatePost(request, logoutResponse);
                return handleLogoutResponse(cookie,
                        logoutState,
                        logoutResponse,
                        SamlProtocol.Binding.HTTP_POST);
            }
        } catch (IOException | XMLStreamException e) {
            throw new IdpException("Unable to inflate Saml Object", e);
        } catch (ValidationException e) {
            throw new IdpException("Unable to validate Saml Object", e);
        }

        throw new IdpException("Unable to process logout");
    }

    void validatePost(HttpServletRequest request, SignableSAMLObject samlObject)
            throws ValidationException {
        if (strictSignature) {
            new SamlValidator.Builder(new SimpleSign(systemCrypto)).buildAndValidate(request.getRequestURL()
                    .toString(), SamlProtocol.Binding.HTTP_POST, samlObject);
        }
    }

    private Response handleLogoutResponse(Cookie cookie, LogoutState logoutState,
            LogoutResponse logoutObject, SamlProtocol.Binding incomingBinding) throws IdpException {
        if (!StatusCode.SUCCESS_URI.equals(logoutObject.getStatus()
                .getStatusCode()
                .getValue())) {
            logoutState.setPartialLogout(true);
        }
        return continueLogout(logoutState, cookie, incomingBinding);
    }

    Response handleLogoutRequest(Cookie cookie, LogoutState logoutState,
            LogoutRequest logoutRequest, SamlProtocol.Binding incomingBinding, String relayState)
            throws IdpException {
        if (logoutState != null) {
            LOGGER.warn("Received logout request and already have a logout state (in progress)");
            return Response.ok("Logout already in progress")
                    .build();
        }

        logoutState = new LogoutState(getActiveSps(cookie.getValue()));
        logoutState.setOriginalIssuer(logoutRequest.getIssuer()
                .getValue());
        logoutState.setNameId(logoutRequest.getNameID()
                .getValue());
        logoutState.setOriginalRequestId(logoutRequest.getID());
        logoutState.setInitialRelayState(relayState);
        logoutStates.encode(cookie.getValue(), logoutState);

        cookieCache.removeSamlAssertion(cookie.getValue());
        return continueLogout(logoutState, cookie, incomingBinding);
    }

    private Response continueLogout(LogoutState logoutState, Cookie cookie,
            SamlProtocol.Binding incomingBinding) throws IdpException {
        if (logoutState == null) {
            throw new IdpException("Cannot continue a Logout that doesn't exist!");
        }

        try {
            SignableSAMLObject logoutObject;
            String relay = "";
            String entityId = "";
            String reqres = "";

            Optional<String> nextTarget = logoutState.getNextTarget();
            if (nextTarget.isPresent()) {
                // Another target exists, log them out
                entityId = nextTarget.get();
                if (logoutState.getOriginalIssuer()
                        .equals(entityId)) {
                    return continueLogout(logoutState, cookie, incomingBinding);
                }
                logoutObject = logoutService.buildLogoutRequest(logoutState.getNameId(),
                        systemBaseUrl.constructUrl("/idp/logout", true));
                reqres = "SAMLRequest";
            } else {
                // No more targets, respond to original issuer
                entityId = logoutState.getOriginalIssuer();
                String status = logoutState.isPartialLogout() ?
                        StatusCode.PARTIAL_LOGOUT_URI :
                        StatusCode.SUCCESS_URI;
                logoutObject = logoutService.buildLogoutResponse(systemBaseUrl.constructUrl(
                        "/idp/logout",
                        true), status, logoutState.getOriginalRequestId());
                relay = logoutState.getInitialRelayState();
                LogoutState decode = logoutStates.decode(cookie.getValue(), true);
                reqres = "SAMLResponse";
            }

            LOGGER.debug("Responding to [{}] with a [{}] and relay state [{}]",
                    entityId,
                    reqres,
                    relay);

            EntityInformation.ServiceInfo entityServiceInfo = serviceProviders.get(entityId)
                    .getLogoutService(incomingBinding);
            if (entityServiceInfo == null) {
                LOGGER.warn("Could not find entity service info for {}");
                return continueLogout(logoutState, cookie, incomingBinding);
            }
            switch (entityServiceInfo.getBinding()) {
            case HTTP_REDIRECT:
                return getSamlRedirectResponse(logoutObject,
                        entityServiceInfo.getUrl(),
                        relay,
                        reqres);
            case HTTP_POST:
                return getSamlPostResponse(logoutObject, entityServiceInfo.getUrl(), relay, reqres);
            default:
                LOGGER.debug("No supported binding available for SP [{}].", entityId);
                logoutState.setPartialLogout(true);
                return continueLogout(logoutState, cookie, incomingBinding);
            }

        } catch (WSSecurityException | SimpleSign.SignatureException | IOException e) {
            LOGGER.error("Error while processing logout", e);
        }

        throw new IdpException("Server error while processing logout");
    }

    public Set<String> getActiveSps(String cacheId) {
        return cookieCache.getActiveSpSet(cacheId);
    }

    private Response getSamlRedirectResponse(XMLObject samlResponse, String targetUrl,
            String relayState, String reqres)
            throws IOException, SimpleSign.SignatureException, WSSecurityException {
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
        URI location = signSamlGetResponse(samlResponse, targetUrl, relayState, reqres);
        String redirectUpdated = redirectPage.replace("{{redirect}}", location.toString());
        return Response.ok(redirectUpdated)
                .build();
    }

    private URI signSamlGetResponse(XMLObject samlResponse, String targetUrl, String relayState,
            String reqres) throws IOException, SimpleSign.SignatureException, WSSecurityException {
        LOGGER.debug("Signing SAML response for redirect.");
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
        String encodedResponse =
                URLEncoder.encode(RestSecurity.deflateAndBase64Encode(DOM2Writer.nodeToString(
                        OpenSAMLUtil.toDom(samlResponse, doc, false))), "UTF-8");
        String requestToSign = String.format("%s=%s&RelayState=%s",
                reqres,
                encodedResponse,
                relayState);
        UriBuilder uriBuilder = UriBuilder.fromUri(targetUrl);
        uriBuilder.queryParam(reqres, encodedResponse);
        uriBuilder.queryParam(SSOConstants.RELAY_STATE, relayState == null ? "" : relayState);
        new SimpleSign(systemCrypto).signUriString(requestToSign, uriBuilder);
        LOGGER.debug("Signing successful.");
        return uriBuilder.build();
    }

    private Response getSamlPostResponse(SignableSAMLObject samlObject, String targetUrl,
            String relayState, String reqres)
            throws SimpleSign.SignatureException, WSSecurityException {
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
        LOGGER.debug("Signing SAML POST Response.");
        new SimpleSign(systemCrypto).signSamlObject(samlObject);
        LOGGER.debug("Converting SAML Response to DOM");
        String assertionResponse = DOM2Writer.nodeToString(OpenSAMLUtil.toDom(samlObject, doc));
        String encodedSamlResponse = new String(Base64.encodeBase64(assertionResponse.getBytes()));
        String submitFormUpdated = submitForm.replace("{{" + Idp.ACS_URL + "}}", targetUrl);
        submitFormUpdated = submitFormUpdated.replace("{{" + Idp.SAML_TYPE + "}}", reqres);
        submitFormUpdated = submitFormUpdated.replace("{{" + Idp.SAML_RESPONSE + "}}",
                encodedSamlResponse);
        submitFormUpdated = submitFormUpdated.replace("{{" + Idp.RELAY_STATE + "}}", relayState);
        return Response.ok(submitFormUpdated)
                .build();
    }

    public void setSecurityManager(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public void setTokenFactory(PKIAuthenticationTokenFactory tokenFactory) {
        this.tokenFactory = tokenFactory;
    }

    public void setSpMetadata(List<String> spMetadata) {
        parseServiceProviderMetadata(spMetadata);
    }

    public void setStrictSignature(Boolean strictSignature) {
        this.strictSignature = strictSignature;
    }

    public void setExpirationTime(int expirationTime) {
        this.cookieCache.setExpirationTime(expirationTime);
    }

    public void setLogoutService(LogoutService logoutService) {
        this.logoutService = logoutService;
    }

    public void setLogoutStates(RelayStates<LogoutState> logoutStates) {
        this.logoutStates = logoutStates;
    }
}
