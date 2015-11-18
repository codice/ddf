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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import org.apache.commons.lang.StringUtils;
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
import org.codice.ddf.security.handler.api.AnonymousAuthenticationToken;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
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
import org.codice.ddf.security.session.RelayStates;
import org.opensaml.common.SignableSAMLObject;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml2.metadata.SingleLogoutService;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.encryption.EncryptionService;
import ddf.security.samlp.LogoutService;
import ddf.security.samlp.MetadataConfigurationParser;
import ddf.security.samlp.SamlProtocol;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import sun.plugin.dom.exception.InvalidStateException;

@Path("/")
public class IdpEndpoint implements Idp {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdpEndpoint.class);

    private static final String CERTIFICATES_ATTR = "javax.servlet.request.X509Certificate";

    protected CookieCache cookieCache = new CookieCache();

    private SystemBaseUrl systemBaseUrl = new SystemBaseUrl();

    private PKIAuthenticationTokenFactory tokenFactory;

    private SecurityManager securityManager;

    private Map<String, EntityDescriptor> serviceProviders = new HashMap<>();

    private String indexHtml;

    private String submitForm;

    private String redirectPage;

    private Boolean strictSignature = true;

    private SystemCrypto systemCrypto;

    private LogoutService logoutService;

    private RelayStates<LogoutState> logoutStates;

    public IdpEndpoint(String signaturePropertiesPath, String encryptionPropertiesPath,
            EncryptionService encryptionService) {
        systemCrypto = new SystemCrypto(signaturePropertiesPath, encryptionPropertiesPath,
                encryptionService);
    }

    public void init() {
        try {
            indexHtml = IOUtils.toString(IdpEndpoint.class.getResourceAsStream("/html/index.html"));
            submitForm = IOUtils.toString(
                    IdpEndpoint.class.getResourceAsStream("/templates/submitForm.handlebars"));
            redirectPage = IOUtils.toString(
                    IdpEndpoint.class.getResourceAsStream("/templates/redirect.handlebars"));
        } catch (Exception e) {
            LOGGER.error("Unable to load index page for IDP.", e);
        }

        OpenSAMLUtil.initSamlEngine();
    }

    private void parseServiceProviderMetadata(List<String> serviceProviderMetadata) {
        if (serviceProviderMetadata != null) {
            try {
                MetadataConfigurationParser metadataConfigurationParser = new MetadataConfigurationParser(
                        serviceProviderMetadata);
                serviceProviders = metadataConfigurationParser.getEntryDescriptions();
            } catch (IOException e) {
                LOGGER.error("Unable to parse SP metadata configuration.", e);
            }
        }
    }

    @POST
    public Response showPostLogin(@FormParam(SAML_REQ) String samlRequest,
            @FormParam(RELAY_STATE) String relayState, @Context HttpServletRequest request)
            throws WSSecurityException {
        LOGGER.debug("Recevied POST IdP request.");
        return showLoginPage(samlRequest, relayState, null, null, request,
                new PostBinding(systemCrypto, serviceProviders), submitForm);
    }

    @GET
    public Response showGetLogin(@QueryParam(SAML_REQ) String samlRequest,
            @Encoded @QueryParam(RELAY_STATE) String relayState,
            @QueryParam(SSOConstants.SIG_ALG) String signatureAlgorithm,
            @QueryParam(SSOConstants.SIGNATURE) String signature,
            @Context HttpServletRequest request) throws WSSecurityException {
        LOGGER.debug("Recevied GET IdP request.");
        return showLoginPage(samlRequest, relayState, signatureAlgorithm, signature, request,
                new RedirectBinding(systemCrypto, serviceProviders), redirectPage);
    }

    private Response showLoginPage(String samlRequest, String relayState, String signatureAlgorithm,
            String signature, HttpServletRequest request, Binding binding, String template)
            throws WSSecurityException {
        String responseStr;
        AuthnRequest authnRequest = null;
        try {
            Map<String, Object> responseMap = new HashMap<>();
            binding.validator()
                    .validateRelayState(relayState);
            authnRequest = binding.decoder()
                    .decodeRequest(samlRequest);
            binding.validator()
                    .validateAuthnRequest(authnRequest, samlRequest, relayState, signatureAlgorithm,
                            signature, strictSignature);
            X509Certificate[] certs = (X509Certificate[]) request.getAttribute(CERTIFICATES_ATTR);
            boolean hasCerts = (certs != null && certs.length > 0);
            boolean hasCookie = getSamlAssertion(request) != null;
            if ((authnRequest.isPassive() && hasCerts) || hasCookie) {
                LOGGER.debug("Received Passive & PKI AuthnRequest.");
                org.opensaml.saml2.core.Response samlpResponse;
                try {
                    samlpResponse = handleLogin(authnRequest, Idp.PKI, request,
                            authnRequest.isPassive(), hasCookie);
                    LOGGER.debug("Passive & PKI AuthnRequest logged in successfully.");
                } catch (SecurityServiceException e) {
                    LOGGER.error(e.getMessage(), e);
                    return getErrorResponse(relayState, authnRequest, StatusCode.AUTHN_FAILED_URI,
                            binding);
                } catch (WSSecurityException e) {
                    LOGGER.error(e.getMessage(), e);
                    return getErrorResponse(relayState, authnRequest, StatusCode.REQUEST_DENIED_URI,
                            binding);
                } catch (SimpleSign.SignatureException e) {
                    LOGGER.error(e.getMessage(), e);
                    return getErrorResponse(relayState, authnRequest,
                            StatusCode.REQUEST_UNSUPPORTED_URI, binding);
                }
                LOGGER.debug("Returning Passive & PKI SAML Response.");
                NewCookie cookie = null;
                if (hasCookie) {
                    cookieCache.addActiveSp(getCookie(request).getValue(), authnRequest.getIssuer()
                            .getValue());
                } else {
                    cookie = createCookie(request, samlpResponse);
                    if (cookie != null) {
                        cookieCache.addActiveSp(cookie.getValue(), authnRequest.getIssuer()
                                .getValue());
                    }
                }

                return binding.creator()
                        .getSamlpResponse(relayState, authnRequest, samlpResponse, cookie,
                                template);
            } else {
                LOGGER.debug("Building the JSON map to embed in the index.html page for login.");
                Document doc = DOMUtils.createDocument();
                doc.appendChild(doc.createElement("root"));
                String authn = DOM2Writer.nodeToString(
                        OpenSAMLUtil.toDom(authnRequest, doc, false));
                String encodedAuthn = RestSecurity.deflateAndBase64Encode(authn);
                responseMap.put(PKI, hasCerts);
                responseMap.put(SAML_REQ, encodedAuthn);
                responseMap.put(RELAY_STATE, relayState);
                String assertionConsumerServiceURL = ((ResponseCreatorImpl) binding.creator()).getAssertionConsumerServiceURL(
                        authnRequest);
                responseMap.put(ACS_URL, assertionConsumerServiceURL);
                responseMap.put(SSOConstants.SIG_ALG, signatureAlgorithm);
                responseMap.put(SSOConstants.SIGNATURE, signature);
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
                    return getErrorResponse(relayState, authnRequest,
                            StatusCode.REQUEST_UNSUPPORTED_URI, binding);
                } catch (IOException | SimpleSign.SignatureException e1) {
                    LOGGER.error(e1.getMessage(), e1);
                }
            }
        } catch (UnsupportedOperationException e) {
            LOGGER.error(e.getMessage(), e);
            if (authnRequest != null) {
                try {
                    return getErrorResponse(relayState, authnRequest,
                            StatusCode.UNSUPPORTED_BINDING_URI, binding);
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

    private Response getErrorResponse(String relayState, AuthnRequest authnRequest,
            String statusCode, Binding binding)
            throws WSSecurityException, IOException, SimpleSign.SignatureException {
        LOGGER.debug("Creating SAML Response for error condition.");
        org.opensaml.saml2.core.Response samlResponse = SamlProtocol.createResponse(
                SamlProtocol.createIssuer(systemBaseUrl.constructUrl("/idp/login", true)),
                SamlProtocol.createStatus(statusCode), authnRequest.getID(), null);
        LOGGER.debug("Encoding error SAML Response for post or redirect.");
        String template = "";
        String assertionConsumerServiceBinding = ResponseCreator.getAssertionConsumerServiceBinding(
                authnRequest, serviceProviders);
        if (HTTP_POST_BINDING.equals(assertionConsumerServiceBinding)) {
            template = submitForm;
        } else if (HTTP_REDIRECT_BINDING.equals(assertionConsumerServiceBinding)) {
            template = redirectPage;
        }
        return binding.creator()
                .getSamlpResponse(relayState, authnRequest, samlResponse, null, template);
    }

    @GET
    @Path("/sso")
    public Response processLogin(@QueryParam(SAML_REQ) String samlRequest,
            @QueryParam(RELAY_STATE) String relayState, @QueryParam(AUTH_METHOD) String authMethod,
            @QueryParam(SSOConstants.SIG_ALG) String signatureAlgorithm,
            @QueryParam(SSOConstants.SIGNATURE) String signature,
            @Context HttpServletRequest request) {
        LOGGER.debug(
                "Processing login request: [ authMethod {} ], [ sigAlg {} ], [ relayState {} ]",
                authMethod, signatureAlgorithm, relayState);
        try {
            Binding binding;
            String template;
            //the authn request is always encoded as if it came in via redirect when coming from the web app
            Binding redirectBinding = new RedirectBinding(systemCrypto, serviceProviders);
            AuthnRequest authnRequest = redirectBinding.decoder()
                    .decodeRequest(samlRequest);
            String assertionConsumerServiceBinding = ResponseCreator.getAssertionConsumerServiceBinding(
                    authnRequest, serviceProviders);
            if (HTTP_POST_BINDING.equals(assertionConsumerServiceBinding)) {
                binding = new PostBinding(systemCrypto, serviceProviders);
                template = submitForm;
            } else if (HTTP_REDIRECT_BINDING.equals(assertionConsumerServiceBinding)) {
                binding = redirectBinding;
                template = redirectPage;
            } else {
                throw new UnsupportedOperationException("Must use HTTP POST or Redirect bindings.");
            }
            binding.validator()
                    .validateAuthnRequest(authnRequest, samlRequest, relayState, signatureAlgorithm,
                            signature, strictSignature);
            org.opensaml.saml2.core.Response encodedSaml = handleLogin(authnRequest, authMethod,
                    request, false, false);
            LOGGER.debug("Returning SAML Response for relayState: {}" + relayState);
            NewCookie newCookie = createCookie(request, encodedSaml);
            Response response = binding.creator()
                    .getSamlpResponse(relayState, authnRequest, encodedSaml, newCookie, template);
            if (newCookie != null) {
                cookieCache.addActiveSp(newCookie.getValue(), authnRequest.getIssuer()
                        .getValue());
                LOGGER.debug("Adding SP to activeSP list: {}", authnRequest.getIssuer()
                        .getValue());
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
                HandlerResult handlerResult = pkiHandler.getNormalizedToken(request, null, null,
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
            BasicAuthenticationHandler basicAuthenticationHandler = new BasicAuthenticationHandler();
            HandlerResult handlerResult = basicAuthenticationHandler.getNormalizedToken(request,
                    null, null, false);
            if (handlerResult.getStatus()
                    .equals(HandlerResult.Status.COMPLETED)) {
                token = handlerResult.getToken();
            }
        } else if (GUEST.equals(authMethod)) {
            LOGGER.debug("Logging user in as Guest.");
            token = new AnonymousAuthenticationToken(BaseAuthenticationToken.ALL_REALM,
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
                        SecurityToken securityToken = ((SecurityAssertion) principal).getSecurityToken();
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
        return SamlProtocol.createResponse(
                SamlProtocol.createIssuer(systemBaseUrl.constructUrl("/idp/login", true)),
                SamlProtocol.createStatus(statusCode), authnRequest.getID(), samlToken);
    }

    private synchronized Cookie getCookie(HttpServletRequest request) {
        Map<String, Cookie> cookies = HttpUtils.getCookieMap(request);
        return cookies.get(COOKIE);
    }

    private synchronized Element getSamlAssertion(HttpServletRequest request) {
        Element samlToken = null;
        Cookie cookie = getCookie(request);
        if (cookie != null) {
            LOGGER.debug("Retrieving cookie from cache.");
            samlToken = cookieCache.getSamlAssertion(cookie.getValue());
        }
        return samlToken;
    }

    private synchronized LogoutState getLogoutState(HttpServletRequest request) {
        LogoutState logoutState = null;
        Cookie cookie = getCookie(request);
        if (cookie != null) {
            logoutState = logoutStates.decode(cookie.getValue());
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
                    return new NewCookie(COOKIE, uuid.toString(), null, url.getHost(),
                            NewCookie.DEFAULT_VERSION, null, -1, true);
                } catch (MalformedURLException e) {
                    LOGGER.warn(
                            "Unable to create session cookie. Client will need to log in again.",
                            e);
                }
            }
        }
        return null;
    }

    @GET
    @Path("/metadata")
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
        X509Certificate issuerCert = certs[0];

        cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias(systemCrypto.getEncryptionCrypto()
                .getDefaultX509Identifier());
        certs = systemCrypto.getEncryptionCrypto()
                .getX509Certificates(cryptoType);
        X509Certificate encryptionCert = certs[0];
        EntityDescriptor entityDescriptor = SamlProtocol.createIdpMetadata(
                systemBaseUrl.constructUrl("/idp/login", true),
                Base64.encodeBase64String(issuerCert.getEncoded()),
                Base64.encodeBase64String(encryptionCert.getEncoded()), nameIdFormats,
                systemBaseUrl.constructUrl("/idp/login", true),
                systemBaseUrl.constructUrl("/idp/login", true), null);
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
        return Response.ok(
                DOM2Writer.nodeToString(OpenSAMLUtil.toDom(entityDescriptor, doc, false)))
                .build();
    }

    // This should support Asynchronous (Front-Channel) HTTP Redirect and POST bindings
    //     (meaning it comes from the user agent (browser)) as well as Synchronous
    //     (Back-Channel) like a SOAP binding directly from the SP.
    // Need to propagate a LogoutRequest to all other known SP's.

    // Asynchronous
    // -> all must provide RelayState mechanism that the sp may use to associate req with
    //    original request.
    // -> Logout Request must be signed if POST or Redirect binding is used

    // Synchronous
    // Will receive LogoutRequest directly from SP, then propagate to other SP's and respond
    // to initial request when done

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
    public Response processRedirectLogout(@QueryParam(SAML_REQ) String samlRequest,
            @QueryParam(RELAY_STATE) String relayState,
            @QueryParam(SSOConstants.SIG_ALG) String signatureAlgorithm,
            @QueryParam(SSOConstants.SIGNATURE) String signature,
            @Context HttpServletRequest request) throws WSSecurityException, IdpException {
        // TODO (RCZ) 11/2/15 - Implement Server HTTP-Redirect logout

        try {
            LogoutState logoutState = getLogoutState(request);
            Cookie cookie = getCookie(request);
            XMLObject logoutObject = logoutService.extractXmlObject(samlRequest);
            Binding binding = new RedirectBinding(systemCrypto, serviceProviders);
            binding.validator()
                    .validateRelayState(relayState);

            if (logoutObject instanceof LogoutRequest) {
                // LogoutRequest is the initial request coming from the SP that initiated the chain
                LogoutRequest logoutRequest = ((LogoutRequest) logoutObject);
                validateGetLogoutObject(logoutRequest, samlRequest, logoutRequest.getIssuer()
                        .getValue(), relayState, signatureAlgorithm, signature, strictSignature);
                return handleLogoutRequest(cookie, logoutState, (LogoutRequest) logoutObject);

            } else if (logoutObject instanceof LogoutResponse) {
                // LogoutResponse is one of the SP's responding to the logout request
                LogoutResponse logoutResponse = ((LogoutResponse) logoutObject);
                validateGetLogoutObject(logoutResponse, samlRequest, logoutResponse.getIssuer()
                        .getValue(), relayState, signatureAlgorithm, signature, strictSignature);
                return handleLogoutResponse(cookie, logoutState, (LogoutResponse) logoutObject);

            } else { // Unsupported object type
                // TODO (RCZ) 11/11/15 - Log some unsupported exception?
                // Even if their object is bad we might be able to finish rest of logouts
                continueLogout(logoutState);
            }
        } catch (XMLStreamException e) {
            LOGGER.error("Unable to extract Saml object", e);
        } catch (SimpleSign.SignatureException e) {
            // TODO (RCZ) - exception
        } catch (ValidationException e) {
            // TODO (RCZ) - exception
        } finally {
            // TODO (RCZ) - Make sure we don't throw away logout state on error possibly?
        }
        // TODO (RCZ) 11/10/15 - Respond

        return null;
    }

    private Response handleLogoutResponse(Cookie cookie, LogoutState logoutState,
            LogoutResponse logoutObject) throws IdpException {
        // TODO (RCZ) - logic
        return continueLogout(logoutState);
    }

    Response handleLogoutRequest(Cookie cookie, LogoutState logoutState,
            LogoutRequest logoutRequest) throws IdpException {
        // Initial Logout Request
        if (logoutState != null) {
            // this means that they have a logout in progress and resent another logout
            // request. Either that or we have an old LogoutState which could happen if
            // a logout never actually finished
            throw new InvalidStateException("Weird state");
        }

        logoutState = new LogoutState(getActiveSps(cookie.getValue()));
        logoutState.setOriginalIssuer(logoutRequest.getIssuer()
                .getValue());
        logoutState.setNameId(logoutRequest.getNameID()
                .getValue());
        logoutStates.encode(cookie.getValue(), logoutState);
        return continueLogout(logoutState);
    }

    private Response continueLogout(LogoutState logoutState) throws IdpException {
        if (logoutState == null) {
            throw new IdpException("Cannot continue a Logout that doesn't exist!");
        }
        try {
            Optional<SPSSODescriptor> nextTargetOpt = logoutState.getNextTarget();
            if (nextTargetOpt.isPresent()) {
                SPSSODescriptor nextTarget = nextTargetOpt.get();
                // TODO (RCZ) - Is issuerId the metadata endpoint or the logout endpoint?
                LogoutRequest logoutRequest = logoutService.buildLogoutRequest(
                        logoutState.getNameId(), systemBaseUrl.constructUrl("/idp/logout", true));
                if (supportsLogoutBinding(nextTarget, HTTP_REDIRECT_BINDING)) {
                    Optional<SingleLogoutService> singleLogoutService = nextTarget.getSingleLogoutServices()
                            .stream()
                            .filter(sls -> HTTP_REDIRECT_BINDING.equals(sls.getBinding()))
                            .findFirst();
                    if (singleLogoutService.isPresent()) {
                        return getSamlRedirectResponse(logoutRequest, singleLogoutService.get()
                                .getLocation(), "");
                    }
                } else if (supportsLogoutBinding(nextTarget, HTTP_POST_BINDING)) {
                    // TODO (RCZ) - Post binding
                } else {
                    // TODO (RCZ) - No supported binding
                }
            } else {
                // TODO (RCZ) - finished, redirect to originating SP

            }
        } catch (WSSecurityException e) {
            throw new IdpException(e);
        } catch (SimpleSign.SignatureException e) {
            throw new IdpException(e);
        } catch (IOException e) {
            throw new IdpException(e);
        } finally {
            // TODO (RCZ) - if we need to keep keep going don't forget to re-store logout state
        }

        return null;
    }

    public Set<SPSSODescriptor> getActiveSps(String cacheId) {
        return cookieCache.getActiveSpSet(cacheId)
                .stream()
                .map(serviceProviders::get)
                .map(ed -> ed.getSPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL))
                .collect(Collectors.toSet());
    }

    private Response getSamlRedirectResponse(XMLObject samlResponse, String targetUrl,
            String relayState)
            throws IOException, SimpleSign.SignatureException, WSSecurityException {
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
        URI location = signSamlGetResponse(samlResponse, targetUrl, relayState);
        String redirectUpdated = redirectPage.replace("{{redirect}}", location.toString());
        return Response.ok(redirectUpdated)
                .build();
    }

    private URI signSamlGetResponse(XMLObject samlResponse, String targetUrl, String relayState)
            throws IOException, SimpleSign.SignatureException, WSSecurityException {
        LOGGER.debug("Signing SAML response for redirect.");
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
        String encodedResponse = URLEncoder.encode(RestSecurity.deflateAndBase64Encode(
                DOM2Writer.nodeToString(OpenSAMLUtil.toDom(samlResponse, doc, false))), "UTF-8");
        String requestToSign = String.format("SAMLResponse=%s&RelayState=%s", encodedResponse,
                relayState);
        UriBuilder uriBuilder = UriBuilder.fromUri(targetUrl);
        uriBuilder.queryParam(SSOConstants.SAML_RESPONSE, encodedResponse);
        uriBuilder.queryParam(SSOConstants.RELAY_STATE, relayState);
        new SimpleSign(systemCrypto).signUriString(requestToSign, uriBuilder);
        LOGGER.debug("Signing successful.");
        return uriBuilder.build();
    }

    private boolean supportsLogoutBinding(SPSSODescriptor descriptor, String binding) {
        return descriptor.getSingleLogoutServices()
                .stream()
                .anyMatch(sls -> binding.equals(sls.getBinding()));
    }

    public void validateGetLogoutObject(SignableSAMLObject samlObject, String samlRequest,
            String issuer, String relayState, String signatureAlgorithm, String signature,
            boolean strictSignature) throws SimpleSign.SignatureException, ValidationException {
        LOGGER.debug("Validating AuthnRequest required attributes and signature");
        if (strictSignature) {
            if (!StringUtils.isEmpty(signature) && !StringUtils.isEmpty(signatureAlgorithm)) {
                String signedParts;
                try {
                    signedParts = String.format("SAMLRequest=%s&RelayState=%s&SigAlg=%s",
                            URLEncoder.encode(samlRequest, "UTF-8"), relayState,
                            URLEncoder.encode(signatureAlgorithm, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new SimpleSign.SignatureException(
                            "Unable to construct signed query parts.", e);
                }
                EntityDescriptor entityDescriptor = serviceProviders.get(issuer);
                SPSSODescriptor spssoDescriptor = entityDescriptor.getSPSSODescriptor(
                        SamlProtocol.SUPPORTED_PROTOCOL);
                String encryptionCertificate = null;
                String signingCertificate = null;
                if (spssoDescriptor != null) {
                    for (KeyDescriptor key : spssoDescriptor.getKeyDescriptors()) {
                        String certificate = null;
                        if (key.getKeyInfo()
                                .getX509Datas()
                                .size() > 0 && key.getKeyInfo()
                                .getX509Datas()
                                .get(0)
                                .getX509Certificates()
                                .size() > 0) {
                            certificate = key.getKeyInfo()
                                    .getX509Datas()
                                    .get(0)
                                    .getX509Certificates()
                                    .get(0)
                                    .getValue();
                        }
                        if (StringUtils.isBlank(certificate)) {
                            break;
                        }

                        if (UsageType.UNSPECIFIED.equals(key.getUse())) {
                            encryptionCertificate = certificate;
                            signingCertificate = certificate;
                        }

                        if (UsageType.ENCRYPTION.equals(key.getUse())) {
                            encryptionCertificate = certificate;
                        }

                        if (UsageType.SIGNING.equals(key.getUse())) {
                            signingCertificate = certificate;
                        }
                    }
                    if (signingCertificate == null) {
                        throw new ValidationException(
                                "Unable to find signing certificate in metadata. Please check metadata.");
                    }
                } else {
                    throw new ValidationException(
                            "Unable to find supported protocol in metadata SPSSODescriptors.");
                }
                boolean result = new SimpleSign(systemCrypto).validateSignature(signedParts,
                        signature, signingCertificate);
                if (!result) {
                    throw new ValidationException(
                            "Signature verification failed for redirect binding.");
                }
            } else {
                throw new SimpleSign.SignatureException("No signature present for AuthnRequest.");
            }
        }

        if (strictSignature && issuer != null && (samlObject.getSignature() == null
                && signature == null)) {
            throw new IllegalArgumentException(
                    "Invalid LogoutRequest, contained no identifying signature.");
        }
    }

    @Override
    public Response processPostLogout(@FormParam(SAML_REQ) String samlRequest,
            @FormParam(RELAY_STATE) String relayState, @Context HttpServletRequest request)
            throws WSSecurityException {
        // TODO (RCZ) 11/2/15 - Implement Server Post-Binding logout
        return null;
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

    public void setLogoutState(RelayStates<LogoutState> logoutStates) {
        this.logoutStates = logoutStates;
    }
}
