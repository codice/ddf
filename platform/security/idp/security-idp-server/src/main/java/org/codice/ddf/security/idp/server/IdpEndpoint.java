/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.idp.server;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.xml.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.encryption.EncryptionService;
import ddf.security.samlp.MetadataConfigurationParser;
import ddf.security.samlp.SamlProtocol;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;

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
            boolean hasCookie = exchangeCookieForAssertion(request) != null;
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
                return binding.creator()
                        .getSamlpResponse(relayState, authnRequest, samlpResponse,
                                createCookie(request, samlpResponse), template);
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

            return binding.creator()
                    .getSamlpResponse(relayState, authnRequest, encodedSaml,
                            createCookie(request, encodedSaml), template);
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
            token = new GuestAuthenticationToken(BaseAuthenticationToken.ALL_REALM,
                    request.getRemoteAddr());
        } else {
            throw new IllegalArgumentException("Auth method is not supported.");
        }

        org.w3c.dom.Element samlToken = null;
        String statusCode;
        if (hasCookie) {
            samlToken = exchangeCookieForAssertion(request);
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

    private synchronized Element exchangeCookieForAssertion(HttpServletRequest request) {
        Element samlToken = null;
        Map<String, Cookie> cookies = HttpUtils.getCookieMap(request);
        Cookie cookie = cookies.get(COOKIE);
        if (cookie != null) {
            LOGGER.debug("Retrieving cookie from cache.");
            samlToken = cookieCache.get(cookie.getValue());
        }
        return samlToken;
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

                cookieCache.put(uuid.toString(), assertion.getDOM());
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
}
