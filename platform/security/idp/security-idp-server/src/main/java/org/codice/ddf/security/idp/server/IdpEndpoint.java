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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Encoded;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.rs.security.saml.sso.SSOConstants;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.crypto.PasswordEncryptor;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.common.util.DOM2Writer;
import org.boon.Boon;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.codice.ddf.security.handler.api.AnonymousAuthenticationToken;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.PKIAuthenticationTokenFactory;
import org.codice.ddf.security.handler.basic.BasicAuthenticationHandler;
import org.codice.ddf.security.handler.pki.PKIHandler;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.opensaml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml2.metadata.SPSSODescriptor;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import ddf.security.PropertiesLoader;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.encryption.EncryptionService;
import ddf.security.samlp.MetadataConfigurationParser;
import ddf.security.samlp.SamlProtocol;
import ddf.security.samlp.SimpleSign;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;

@Path("/")
public class IdpEndpoint implements Idp {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdpEndpoint.class);

    private static final String IDP_STATE_OBJ = "IDP_STATE_OBJ";

    private static final String PKI = "pki";

    private static final String GUEST = "guest";

    private static final String USER_PASS = "up";

    private static final String SAML_RESPONSE = "SAMLResponse";

    private static final String ACS_URL = "ACSURL";

    private static final String HTTP_POST_BINDING = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST";

    private static final String SAML_SOAP_BINDING = "urn:oasis:names:tc:SAML:2.0:bindings:SOAP";

    private static final String PAOS_BINDING = "urn:oasis:names:tc:SAML:2.0:bindings:PAOS";

    private static final String HTTP_REDIRECT_BINDING = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect";

    private static final String HTTP_ARTIFACT_BINDING = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact";

    private PKIAuthenticationTokenFactory tokenFactory;

    private SecurityManager securityManager;

    private Crypto signatureCrypto;

    private Crypto encryptionCrypto;

    private EncryptionService encryptionService;

    private String signaturePropertiesPath;

    private String encryptionPropertiesPath;

    private Map<String, EntityDescriptor> serviceProviders = new HashMap<>();

    private String indexHtml;

    private String submitForm;

    private String redirectPage;

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
    }

    private void parseSpMetadata(List<String> spMetadata) {
        if (spMetadata != null) {
            try {
                serviceProviders = MetadataConfigurationParser.buildEntityDescriptors(spMetadata);
            } catch (IOException e) {
                LOGGER.error("Unable to parse SP metadata configuration.", e);
            }
        }
    }

    @POST
    public Response showPostLogin(@FormParam(SAML_REQ) String samlRequest,
            @FormParam(RELAY_STATE) String relayState, @Context HttpServletRequest request,
            @Context HttpServletResponse response) throws WSSecurityException {
        LOGGER.debug("Recevied POST IdP request.");
        return showLoginPage(samlRequest, relayState, null, null, request, true);
    }

    @GET
    public Response showGetLogin(@QueryParam(SAML_REQ) String samlRequest,
            @Encoded @QueryParam(RELAY_STATE) String relayState,
            @QueryParam(SSOConstants.SIG_ALG) String signatureAlgorithm,
            @QueryParam(SSOConstants.SIGNATURE) String signature,
            @Context HttpServletRequest request, @Context HttpServletResponse response)
            throws WSSecurityException {
        LOGGER.debug("Recevied GET IdP request.");
        return showLoginPage(samlRequest, relayState, signatureAlgorithm, signature, request, false);
    }

    private Response showLoginPage(String samlRequest, String relayState, String signatureAlgorithm,
            String signature, HttpServletRequest request, boolean isPost)
            throws WSSecurityException {
        String responseStr;
        AuthnRequest authnRequest = null;
        try {
            Map<String, Object> responseMap = new HashMap<>();
            validateRelayState(relayState);
            authnRequest = unwrapAuthnRequest(samlRequest, isPost);
            validateAuthnRequest(authnRequest, samlRequest, relayState, signatureAlgorithm,
                    signature, isPost);
            X509Certificate[] certs = (X509Certificate[]) request
                    .getAttribute("javax.servlet.request.X509Certificate");
            boolean hasCerts = (certs != null && certs.length > 0);
            if (authnRequest.isPassive() && hasCerts) {
                LOGGER.debug("Received Passive & PKI AuthnRequest.");
                org.opensaml.saml2.core.Response samlpResponse;
                try {
                    samlpResponse = handleLogin(authnRequest, PKI, request,
                            authnRequest.isPassive());
                    LOGGER.debug("Passive & PKI AuthnRequest logged in successfully.");
                } catch (SecurityServiceException e) {
                    LOGGER.error("", e);
                    return getErrorResponse(relayState, authnRequest, StatusCode.AUTHN_FAILED_URI);
                } catch (WSSecurityException e) {
                    LOGGER.error("", e);
                    return getErrorResponse(relayState, authnRequest,
                            StatusCode.REQUEST_DENIED_URI);
                } catch (SimpleSign.SignatureException e) {
                    LOGGER.error("", e);
                    return getErrorResponse(relayState, authnRequest,
                            StatusCode.REQUEST_UNSUPPORTED_URI);
                }
                LOGGER.debug("Returning Passive & PKI SAML Response.");
                return getSamlpResponse(relayState, authnRequest, samlpResponse);
            } else {
                LOGGER.debug("Building the JSON map to embed in the index.html page for login.");
                Document doc = DOMUtils.createDocument();
                doc.appendChild(doc.createElement("root"));
                String authn = DOM2Writer
                        .nodeToString(OpenSAMLUtil.toDom(authnRequest, doc, false));
                String encodedAuthn = RestSecurity.deflateAndBase64Encode(authn);
                responseMap.put("pki", hasCerts);
                responseMap.put(SAML_REQ, encodedAuthn);
                responseMap.put(RELAY_STATE, relayState);
                String assertionConsumerServiceURL = getAssertionConsumerServiceURL(authnRequest);
                responseMap.put(ACS_URL, assertionConsumerServiceURL);
                responseMap.put(SSOConstants.SIG_ALG, signatureAlgorithm);
                responseMap.put(SSOConstants.SIGNATURE, signature);
            }

            String json = Boon.toJson(responseMap);

            LOGGER.debug("Returning index.html page.");
            responseStr = indexHtml.replace(IDP_STATE_OBJ, json);
            return Response.ok(responseStr).build();
        } catch (IllegalArgumentException e) {
            LOGGER.error("", e);
            if (authnRequest != null) {
                try {
                    return getErrorResponse(relayState, authnRequest,
                            StatusCode.REQUEST_UNSUPPORTED_URI);
                } catch (IOException | SimpleSign.SignatureException e1) {
                    LOGGER.error("", e1);
                }
            }
        } catch (UnsupportedOperationException e) {
            LOGGER.error("", e);
            if (authnRequest != null) {
                try {
                    return getErrorResponse(relayState, authnRequest,
                            StatusCode.UNSUPPORTED_BINDING_URI);
                } catch (IOException | SimpleSign.SignatureException e1) {
                    LOGGER.error("", e1);
                }
            }
        } catch (SimpleSign.SignatureException e) {
            LOGGER.error("Unable to validate AuthRequest Signature", e);
        } catch (IOException e) {
            LOGGER.error("Unable to decode AuthRequest", e);
        } catch (ValidationException e) {
            LOGGER.error("AuthnRequest schema validation failed.", e);
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    private Response getErrorResponse(String relayState, AuthnRequest authnRequest,
            String statusCode)
            throws WSSecurityException, IOException, SimpleSign.SignatureException {
        LOGGER.debug("Creating SAML Response for error condition.");
        org.opensaml.saml2.core.Response samlResponse = SamlProtocol.createResponse(
                SamlProtocol.createIssuer("https://localhost:8993/services/idp/login"),
                SamlProtocol.createStatus(statusCode), authnRequest.getID(), null);
        LOGGER.debug("Encoding error SAML Response for post or redirect.");
        return getSamlpResponse(relayState, authnRequest, samlResponse);
    }

    private Response getSamlpResponse(@QueryParam(RELAY_STATE) String relayState,
            AuthnRequest authnRequest, org.opensaml.saml2.core.Response samlResponse)
            throws WSSecurityException, IOException, SimpleSign.SignatureException {
        if (authnRequest.getProtocolBinding().equals(HTTP_POST_BINDING)) {
            LOGGER.debug("Configuring SAML Response for POST.");
            Document doc = DOMUtils.createDocument();
            doc.appendChild(doc.createElement("root"));
            LOGGER.debug("Signing SAML POST Response.");
            signSamlPostResponse(samlResponse);
            LOGGER.debug("Converting SAML Response to DOM");
            String assertionResponse = DOM2Writer
                    .nodeToString(OpenSAMLUtil.toDom(samlResponse, doc));
            String encodedSamlResponse = new String(
                    Base64.encodeBase64(assertionResponse.getBytes()));
            String assertionConsumerServiceURL = getAssertionConsumerServiceURL(authnRequest);
            String submitFormUpdated = submitForm
                    .replace("{{" + ACS_URL + "}}", assertionConsumerServiceURL);
            submitFormUpdated = submitFormUpdated
                    .replace("{{" + SAML_RESPONSE + "}}", encodedSamlResponse);
            submitFormUpdated = submitFormUpdated.replace("{{" + RELAY_STATE + "}}", relayState);
            return Response.ok(submitFormUpdated).build();
        } else if (authnRequest.getProtocolBinding().equals(HTTP_REDIRECT_BINDING)) {
            LOGGER.debug("Configuring SAML Response for Redirect.");
            Document doc = DOMUtils.createDocument();
            doc.appendChild(doc.createElement("root"));
            URI location = signSamlGetResponse(samlResponse, authnRequest, relayState);
            String redirectUpdated = redirectPage.replace("{{redirect}}", location.toString());
            return Response.ok(redirectUpdated).build();
        }
        throw new UnsupportedOperationException("Must use HTTP POST or Redirect bindings.");
    }

    protected AuthnRequest unwrapAuthnRequest(String samlRequest, boolean isPost) {
        LOGGER.debug("Creating AuthnRequest object from SAMLRequest string.");
        if (StringUtils.isEmpty(samlRequest)) {
            throw new IllegalArgumentException("Missing SAMLRequest on IdP request.");
        }
        String decodedRequest;
        if (isPost) {
            decodedRequest = new String(Base64.decodeBase64(samlRequest));
        } else {
            try {
                decodedRequest = RestSecurity.inflateBase64(samlRequest);
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to decode SAMLRequest: base64/inflate.");
            }
        }
        ByteArrayInputStream tokenStream = new ByteArrayInputStream(decodedRequest.getBytes());
        Document authnDoc;
        try {
            authnDoc = StaxUtils.read(new InputStreamReader(tokenStream, "UTF-8"));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to read SAMLRequest as XML.");
        }
        XMLObject authnXmlObj;
        try {
            authnXmlObj = OpenSAMLUtil.fromDom(authnDoc.getDocumentElement());
        } catch (WSSecurityException ex) {
            throw new IllegalArgumentException(
                    "Unable to convert AuthnRequest document to XMLObject.");
        }
        if (!(authnXmlObj instanceof AuthnRequest)) {
            throw new IllegalArgumentException("SAMLRequest object is not AuthnRequest.");
        }
        LOGGER.debug("Created AuthnRequest object successfully.");
        return (AuthnRequest) authnXmlObj;
    }

    protected void validateAuthnRequest(AuthnRequest authnRequest, String samlRequest,
            String relayState, String signatureAlgorithm, String signature, boolean isPost)
            throws SimpleSign.SignatureException, ValidationException {
        LOGGER.debug("Validating AuthnRequest required attributes and signature");
        if (isPost) {
            if (authnRequest.getSignature() != null) {
                SimpleSign.validateSignature(authnRequest.getSignature(),
                        authnRequest.getDOM().getOwnerDocument(), getSignatureCrypto());
            } else {
                throw new SimpleSign.SignatureException("No signature present on AuthnRequest.");
            }
        } else {
            if (!StringUtils.isEmpty(signature) && !StringUtils.isEmpty(signatureAlgorithm)) {
                String signedParts;
                try {
                    signedParts = SSOConstants.SAML_REQUEST + "=" + URLEncoder
                            .encode(samlRequest, "UTF-8") + "&" + SSOConstants.RELAY_STATE + "="
                            + relayState + "&" + SSOConstants.SIG_ALG + "=" + URLEncoder
                            .encode(signatureAlgorithm, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new SimpleSign.SignatureException(
                            "Unable to construct signed query parts.", e);
                }
                EntityDescriptor entityDescriptor = serviceProviders
                        .get(authnRequest.getIssuer().getValue());
                SPSSODescriptor spssoDescriptor = entityDescriptor
                        .getSPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL);
                String encryptionCertificate = null;
                String signingCertificate = null;
                if (spssoDescriptor != null) {
                    for (KeyDescriptor key : spssoDescriptor.getKeyDescriptors()) {
                        String certificate = null;
                        if (key.getKeyInfo().getX509Datas().size() > 0 &&
                                key.getKeyInfo().getX509Datas().get(0).getX509Certificates().size()
                                        > 0) {
                            certificate = key.getKeyInfo().getX509Datas().get(0)
                                    .getX509Certificates().get(0).getValue();
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
                boolean result = SimpleSign
                        .validateSignature(signedParts, signature, signingCertificate);
                if (!result) {
                    throw new ValidationException(
                            "Signature verification failed for redirect binding.");
                }
            } else {
                throw new SimpleSign.SignatureException("No signature present for AuthnRequest.");
            }
        }

        if (authnRequest.getAssertionConsumerServiceURL() != null && (
                authnRequest.getSignature() == null && signature == null)) {
            throw new IllegalArgumentException(
                    "Invalid AuthnRequest, defined an AssertionConsumerServiceURL, but contained no identifying signature.");
        }

        if (authnRequest.getRequestedAuthnContext() != null) {
            Collection authNContextClasses = CollectionUtils.transformedCollection(
                    authnRequest.getRequestedAuthnContext().getAuthnContextClassRefs(),
                    new AuthnContextClassTransformer());
            if (authnRequest.isPassive() && authnRequest.getRequestedAuthnContext().getComparison()
                    .equals(AuthnContextComparisonTypeEnumeration.EXACT) && !CollectionUtils
                    .containsAny(authNContextClasses,
                            Arrays.asList(SAML2Constants.AUTH_CONTEXT_CLASS_REF_X509,
                                    SAML2Constants.AUTH_CONTEXT_CLASS_REF_SMARTCARD_PKI,
                                    SAML2Constants.AUTH_CONTEXT_CLASS_REF_SOFTWARE_PKI,
                                    SAML2Constants.AUTH_CONTEXT_CLASS_REF_SPKI,
                                    SAML2Constants.AUTH_CONTEXT_CLASS_REF_TLS_CLIENT))) {
                throw new IllegalArgumentException(
                        "Unable to passively log user in when not specifying PKI AuthnContextClassRef");
            }
        }

        if (!(authnRequest.getProtocolBinding().equals(HTTP_POST_BINDING) || authnRequest
                .getProtocolBinding().equals(HTTP_REDIRECT_BINDING))) {
            throw new UnsupportedOperationException(
                    "Only HTTP-POST and HTTP-Redirect bindings are supported");
        }
    }

    protected void validateRelayState(String relayState) {
        LOGGER.debug("Validating RelayState");
        if (relayState == null) {
            throw new IllegalArgumentException("Missing RelayState on IdP request.");
        }
        if (relayState.getBytes().length < 0 || relayState.getBytes().length > 80) {
            LOGGER.warn("RelayState has invalid size: {}", relayState.getBytes().length);
        }
    }

    @GET
    @Path("/sso")
    public Response processLogin(@QueryParam(SAML_REQ) String samlRequest,
            @QueryParam(RELAY_STATE) String relayState, @QueryParam(AUTH_METHOD) String authMethod,
            @QueryParam(SSOConstants.SIG_ALG) String signatureAlgorithm,
            @QueryParam(SSOConstants.SIGNATURE) String signature,
            @Context HttpServletRequest request) {
        LOGGER.debug("Processing login request: [ authMethod {} ], [ sigAlg {} ], [ relayState {} ]", authMethod, signatureAlgorithm, relayState);
        try {
            validateRelayState(relayState);
            AuthnRequest authnRequest = unwrapAuthnRequest(samlRequest, false);
            validateAuthnRequest(authnRequest, samlRequest, relayState, signatureAlgorithm,
                    signature,
                    (StringUtils.isEmpty(signatureAlgorithm) || StringUtils.isEmpty(signature)));
            org.opensaml.saml2.core.Response encodedSaml = handleLogin(authnRequest, authMethod,
                    request, false);
            LOGGER.debug("Returning SAML Response for relayState: {}" + relayState);
            return getSamlpResponse(relayState, authnRequest, encodedSaml);
        } catch (SecurityServiceException e) {
            LOGGER.warn("Unable to retrieve subject for user.", e);
            return Response.status(Response.Status.UNAUTHORIZED).build();
        } catch (WSSecurityException e) {
            LOGGER.error("Unable to encode SAMLP response.", e);
        } catch (SimpleSign.SignatureException e) {
            LOGGER.error("Unable to sign SAML response.", e);
        } catch (IllegalArgumentException e) {
            LOGGER.error("", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (ValidationException e) {
            LOGGER.error("AuthnRequest schema validation failed.", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (IOException e) {
            LOGGER.error("Unable to create SAML Response.", e);
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    protected org.opensaml.saml2.core.Response handleLogin(AuthnRequest authnRequest,
            String authMethod, HttpServletRequest request, boolean passive)
            throws SecurityServiceException, WSSecurityException, SimpleSign.SignatureException {
        BaseAuthenticationToken token = null;
        request.setAttribute(ContextPolicy.ACTIVE_REALM, BaseAuthenticationToken.ALL_REALM);
        if (PKI.equals(authMethod)) {
            PKIHandler pkiHandler = new PKIHandler();
            pkiHandler.setTokenFactory(tokenFactory);
            try {
                HandlerResult handlerResult = pkiHandler
                        .getNormalizedToken(request, null, null, false);
                if (handlerResult.getStatus().equals(HandlerResult.Status.COMPLETED)) {
                    token = handlerResult.getToken();
                }
            } catch (ServletException e) {
                LOGGER.warn("Encountered an exception while checking for PKI auth info.", e);
            }
        } else if (USER_PASS.equals(authMethod)) {
            BasicAuthenticationHandler basicAuthenticationHandler = new BasicAuthenticationHandler();
            HandlerResult handlerResult = basicAuthenticationHandler
                    .getNormalizedToken(request, null, null, false);
            if (handlerResult.getStatus().equals(HandlerResult.Status.COMPLETED)) {
                token = handlerResult.getToken();
            }
        } else if (GUEST.equals(authMethod)) {
            token = new AnonymousAuthenticationToken(BaseAuthenticationToken.ALL_REALM);
        } else {
            throw new IllegalArgumentException("Auth method is not supported.");
        }

        org.w3c.dom.Element samlToken = null;
        String statusCode;
        try {
            Subject subject = securityManager.getSubject(token);
            for (Object principal : subject.getPrincipals().asList()) {
                if (principal instanceof SecurityAssertion) {
                    SecurityToken securityToken = ((SecurityAssertion) principal)
                            .getSecurityToken();
                    samlToken = securityToken.getToken();
                }
            }
            statusCode = StatusCode.SUCCESS_URI;
        } catch (SecurityServiceException e) {
            if (!passive) {
                throw e;
            } else {
                statusCode = StatusCode.AUTHN_FAILED_URI;
            }
        }

        return SamlProtocol.createResponse(
                SamlProtocol.createIssuer("https://localhost:8993/services/idp/login"),
                SamlProtocol.createStatus(statusCode), authnRequest.getID(), samlToken);
    }

    protected void signSamlPostResponse(org.opensaml.saml2.core.Response samlResponse)
            throws WSSecurityException, SimpleSign.SignatureException {
        SimpleSign.signSamlObject(samlResponse, getSignatureCrypto(),
                getSignatureCrypto().getDefaultX509Identifier(), getSignaturePassword());
    }

    protected URI signSamlGetResponse(org.opensaml.saml2.core.Response samlResponse,
            AuthnRequest authnRequest, String relayState)
            throws WSSecurityException, SimpleSign.SignatureException, IOException {
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
        String encodedResponse = URLEncoder.encode(RestSecurity.deflateAndBase64Encode(
                DOM2Writer.nodeToString(OpenSAMLUtil.toDom(samlResponse, doc, false))), "UTF-8");
        String requestToSign =
                SSOConstants.SAML_RESPONSE + "=" + encodedResponse + "&" + SSOConstants.RELAY_STATE
                        + "=" + relayState;
        String assertionConsumerServiceURL = getAssertionConsumerServiceURL(authnRequest);
        UriBuilder uriBuilder = UriBuilder.fromUri(assertionConsumerServiceURL);
        //        UriBuilder uriBuilder = UriBuilder.fromUri("https://");
        uriBuilder.queryParam(SSOConstants.SAML_RESPONSE, encodedResponse);
        uriBuilder.queryParam(SSOConstants.RELAY_STATE, relayState);
        SimpleSign.signUriString(requestToSign, uriBuilder, getSignatureCrypto(),
                getSignatureCrypto().getDefaultX509Identifier(), getSignaturePassword());
        return uriBuilder.build();
    }

    private String getAssertionConsumerServiceURL(AuthnRequest authnRequest) {
        String assertionConsumerServiceURL = null;
        LOGGER.debug("Attempting to determine AssertionConsumerServiceURL.");
        //if the AuthnRequest specifies a URL, use that
        if (authnRequest.getAssertionConsumerServiceURL() != null) {
            LOGGER.debug("Using AssertionConsumerServiceURL from AuthnRequest: {}", authnRequest.getAssertionConsumerServiceURL());
            assertionConsumerServiceURL = authnRequest.getAssertionConsumerServiceURL();
        } else {
            //check metadata
            EntityDescriptor entityDescriptor = serviceProviders
                    .get(authnRequest.getIssuer().getValue());
            SPSSODescriptor spssoDescriptor = entityDescriptor
                    .getSPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL);
            AssertionConsumerService defaultAssertionConsumerService = spssoDescriptor
                    .getDefaultAssertionConsumerService();
            //see if the default service uses our supported bindings, and then use that
            if (defaultAssertionConsumerService.getBinding().equals(HTTP_POST_BINDING)
                    || defaultAssertionConsumerService.getBinding().equals(HTTP_REDIRECT_BINDING)) {
                LOGGER.debug("Using AssertionConsumerServiceURL from default assertion consumer service: {}", defaultAssertionConsumerService.getLocation());
                assertionConsumerServiceURL = defaultAssertionConsumerService.getLocation();
            } else {
                //if default doesn't work, check any others that are defined and use the first one that supports our bindings
                for (AssertionConsumerService assertionConsumerService : spssoDescriptor
                        .getAssertionConsumerServices()) {
                    if (assertionConsumerService.getBinding().equals(HTTP_POST_BINDING)
                            || assertionConsumerService.getBinding()
                            .equals(HTTP_REDIRECT_BINDING)) {
                        LOGGER.debug("Using AssertionConsumerServiceURL from supported binding: {}", assertionConsumerService.getLocation());
                        assertionConsumerServiceURL = assertionConsumerService.getLocation();
                        break;
                    }
                }
            }

        }
        if (assertionConsumerServiceURL == null) {
            throw new IllegalArgumentException("No valid AssertionConsumerServiceURL available for given AuthnRequest.");
        }
        return assertionConsumerServiceURL;
    }

    protected Crypto getSignatureCrypto() {
        if (signatureCrypto == null && signaturePropertiesPath != null) {
            Properties sigProperties = PropertiesLoader.loadProperties(signaturePropertiesPath);
            if (sigProperties != null) {
                try {
                    PasswordEncryptor passwordEncryptor = new PasswordEncryptor() {
                        @Override
                        public String encrypt(String password) {
                            return encryptionService.encrypt(password);
                        }

                        @Override
                        public String decrypt(String encryptedPassword) {
                            return encryptionService.decrypt(encryptedPassword);
                        }
                    };
                    signatureCrypto = CryptoFactory
                            .getInstance(sigProperties, IdpEndpoint.class.getClassLoader(),
                                    passwordEncryptor);
                } catch (WSSecurityException e) {
                    LOGGER.debug("Error in loading the signature Crypto object: ", e);
                }
            } else {
                LOGGER.debug("Cannot load signature properties using: " + signaturePropertiesPath);
            }
        }
        return signatureCrypto;
    }

    protected Crypto getEncryptionCrypto() {
        if (encryptionCrypto == null && encryptionPropertiesPath != null) {
            Properties sigProperties = PropertiesLoader.loadProperties(encryptionPropertiesPath);
            if (sigProperties != null) {
                try {
                    PasswordEncryptor passwordEncryptor = new PasswordEncryptor() {
                        @Override
                        public String encrypt(String password) {
                            return encryptionService.encrypt(password);
                        }

                        @Override
                        public String decrypt(String encryptedPassword) {
                            return encryptionService.decrypt(encryptedPassword);
                        }
                    };
                    encryptionCrypto = CryptoFactory
                            .getInstance(sigProperties, IdpEndpoint.class.getClassLoader(),
                                    passwordEncryptor);
                } catch (WSSecurityException e) {
                    LOGGER.debug("Error in loading the signature Crypto object: ", e);
                }
            } else {
                LOGGER.debug("Cannot load signature properties using: " + encryptionPropertiesPath);
            }
        }
        return encryptionCrypto;
    }

    protected String getSignaturePassword() {
        if (signaturePropertiesPath != null) {
            Properties sigProperties = PropertiesLoader.loadProperties(signaturePropertiesPath);
            if (sigProperties != null) {
                String password = sigProperties
                        .getProperty(Merlin.PREFIX + Merlin.KEYSTORE_PRIVATE_PASSWORD);
                if (password == null) {
                    password = sigProperties
                            .getProperty(Merlin.OLD_PREFIX + Merlin.KEYSTORE_PRIVATE_PASSWORD);
                }
                if (password != null) {
                    password = password.trim();
                    password = decryptPassword(password, encryptionService);
                }
                return password;
            }
        }
        return null;
    }

    protected String getEncryptionPassword() {
        if (encryptionPropertiesPath != null) {
            Properties encProperties = PropertiesLoader.loadProperties(encryptionPropertiesPath);
            if (encProperties != null) {
                String password = encProperties
                        .getProperty(Merlin.PREFIX + Merlin.KEYSTORE_PRIVATE_PASSWORD);
                if (password == null) {
                    password = encProperties
                            .getProperty(Merlin.OLD_PREFIX + Merlin.KEYSTORE_PRIVATE_PASSWORD);
                }
                if (password != null) {
                    password = password.trim();
                    password = decryptPassword(password, encryptionService);
                }
                return password;
            }
        }
        return null;
    }

    protected String decryptPassword(String password, EncryptionService passwordEncryptor) {
        if (password.startsWith(Merlin.ENCRYPTED_PASSWORD_PREFIX) && password
                .endsWith(Merlin.ENCRYPTED_PASSWORD_SUFFIX)) {
            if (passwordEncryptor == null) {
                LOGGER.debug("No PasswordEncryptor is configured!");
                return password;
            }
            String substring = password
                    .substring(Merlin.ENCRYPTED_PASSWORD_PREFIX.length(), password.length() - 1);
            return passwordEncryptor.decrypt(substring);
        }

        return password;
    }

    private static class AuthnContextClassTransformer implements Transformer {

        @Override
        public Object transform(Object o) {
            if (o instanceof AuthnContextClassRef) {
                return ((AuthnContextClassRef) o).getAuthnContextClassRef();
            }
            return o;
        }
    }

    @GET
    @Path("/metadata")
    @Produces("application/xml")
    public Response retrieveMetadata() throws WSSecurityException, CertificateEncodingException {
        //TODO get this the right way
        String servletContext = System.getProperty("org.codice.ddf.system.rootContext", "/services");
        String hostname = System.getProperty("org.codice.ddf.system.hostname", "localhost");
        String port = System.getProperty("org.codice.ddf.system.httpsPort", "8993");
        List<String> nameIdFormats = new ArrayList<>();
        nameIdFormats.add(SAML2Constants.NAMEID_FORMAT_PERSISTENT);
        nameIdFormats.add(SAML2Constants.NAMEID_FORMAT_UNSPECIFIED);
        nameIdFormats.add(SAML2Constants.NAMEID_FORMAT_X509_SUBJECT_NAME);
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias(getSignatureCrypto().getDefaultX509Identifier());
        X509Certificate[] certs = getSignatureCrypto().getX509Certificates(cryptoType);
        X509Certificate issuerCert = certs[0];

        cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias(getEncryptionCrypto().getDefaultX509Identifier());
        certs = getEncryptionCrypto().getX509Certificates(cryptoType);
        X509Certificate encryptionCert = certs[0];
        EntityDescriptor entityDescriptor = SamlProtocol
                .createIdpMetadata("https://" + hostname + ":" + port + servletContext + "/idp/login",
                        Base64.encodeBase64String(issuerCert.getEncoded()),
                        Base64.encodeBase64String(encryptionCert.getEncoded()), nameIdFormats,
                        "https://" + hostname + ":" + port + servletContext + "/idp/login",
                        "https://" + hostname + ":" + port + servletContext + "/idp/login",
                        "https://" + hostname + ":" + port + "/logout");
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
        return Response
                .ok(DOM2Writer.nodeToString(OpenSAMLUtil.toDom(entityDescriptor, doc, false)))
                .build();
    }

    public void setSecurityManager(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public void setSignaturePropertiesPath(String signaturePropertiesPath) {
        this.signaturePropertiesPath = signaturePropertiesPath;
    }

    public void setEncryptionPropertiesPath(String encryptionPropertiesPath) {
        this.encryptionPropertiesPath = encryptionPropertiesPath;
    }

    public void setTokenFactory(PKIAuthenticationTokenFactory tokenFactory) {
        this.tokenFactory = tokenFactory;
    }

    public void setEncryptionService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    public void setSpMetadata(List<String> spMetadata) {
        parseSpMetadata(spMetadata);
    }
}
