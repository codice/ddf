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
package org.codice.ddf.security.idp.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.util.DOM2Writer;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.security.common.HttpUtils;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.codice.ddf.security.filter.websso.WebSSOFilter;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.saml.SAMLAssertionHandler;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import ddf.security.http.SessionFactory;
import ddf.security.samlp.SamlProtocol;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;
import ddf.security.samlp.impl.RelayStates;

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

    private final SystemBaseUrl baseUrl;

    private final RelayStates<String> relayStates;

    @Context
    private HttpServletRequest request;

    private Filter loginFilter;

    private SystemCrypto systemCrypto;

    private SessionFactory sessionFactory;

    static {
        OpenSAMLUtil.initSamlEngine();
    }

    public AssertionConsumerService(SimpleSign simpleSign, IdpMetadata metadata,
            SystemCrypto crypto, SystemBaseUrl systemBaseUrl, RelayStates<String> relayStates) {
        this.simpleSign = simpleSign;
        idpMetadata = metadata;
        systemCrypto = crypto;
        baseUrl = systemBaseUrl;
        this.relayStates = relayStates;
    }

    @POST
    @Produces(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postSamlResponse(@FormParam(SAML_RESPONSE) String encodedSamlResponse,
            @FormParam(RELAY_STATE) String relayState) {

        return processSamlResponse(decodeBase64(encodedSamlResponse), relayState);
    }

    @GET
    public Response getSamlResponse(@QueryParam(SAML_RESPONSE) String deflatedSamlResponse,
            @QueryParam(RELAY_STATE) String relayState,
            @QueryParam(SIG_ALG) String signatureAlgorithm,
            @QueryParam(SIGNATURE) String signature) {

        if (validateSignature(deflatedSamlResponse, relayState, signatureAlgorithm, signature)) {
            try {
                return processSamlResponse(RestSecurity.inflateBase64(deflatedSamlResponse),
                        relayState);
            } catch (IOException e) {
                String msg = "Unable to decode and inflate AuthN response.";
                LOGGER.warn(msg, e);
                return Response.serverError()
                        .entity(msg)
                        .build();
            }
        } else {
            return Response.serverError()
                    .entity("Invalid AuthN response signature.")
                    .build();
        }

    }

    private boolean validateSignature(String deflatedSamlResponse, String relayState,
            String signatureAlgorithm, String signature) {
        boolean signaturePasses = false;
        if (signature != null) {
            if (StringUtils.isNotBlank(deflatedSamlResponse) && StringUtils.isNotBlank(relayState)
                    && StringUtils.isNotBlank(signatureAlgorithm)) {
                try {
                    String signedMessage = String.format("%s=%s&%s=%s&%s=%s",
                            SAML_RESPONSE,
                            URLEncoder.encode(deflatedSamlResponse, "UTF-8"),
                            RELAY_STATE,
                            URLEncoder.encode(relayState, "UTF-8"),
                            SIG_ALG,
                            URLEncoder.encode(signatureAlgorithm, "UTF-8"));
                    signaturePasses = simpleSign.validateSignature(signedMessage,
                            signature,
                            idpMetadata.getSigningCertificate());
                } catch (SimpleSign.SignatureException | UnsupportedEncodingException e) {
                    LOGGER.debug("Failed to validate AuthN response signature.", e);
                }
            }
        } else {
            LOGGER.warn(
                    "Received unsigned AuthN response.  Could not verify IDP identity or response integrity.");
            signaturePasses = true;
        }

        return signaturePasses;
    }

    public Response processSamlResponse(String authnResponse, String relayState) {
        LOGGER.trace(authnResponse);

        org.opensaml.saml2.core.Response samlResponse = extractSamlResponse(authnResponse);
        if (samlResponse == null) {
            return Response.serverError()
                    .entity("Unable to parse AuthN response.")
                    .build();
        }

        if (!validateResponse(samlResponse)) {
            return Response.serverError()
                    .entity("AuthN response failed validation.")
                    .build();
        }

        String redirectLocation = relayStates.decode(relayState);
        if (StringUtils.isBlank(redirectLocation)) {
            return Response.serverError()
                    .entity("AuthN response returned unknown or expired relay state.")
                    .build();
        }

        if (!login(samlResponse)) {
            return Response.serverError()
                    .entity(UNABLE_TO_LOGIN)
                    .build();
        }

        URI relayUri;
        try {
            relayUri = new URI(redirectLocation);
        } catch (URISyntaxException e) {
            LOGGER.warn("Unable to parse relay state.", e);
            return Response.serverError()
                    .entity("Unable to redirect back to original location.")
                    .build();
        }

        LOGGER.trace("Successfully logged in.  Redirecting to {}", relayUri.toString());
        return Response.seeOther(relayUri)
                .build();
    }

    private boolean validateResponse(org.opensaml.saml2.core.Response samlResponse) {
        try {
            samlResponse.registerValidator(new AuthnResponseValidator(simpleSign));
            samlResponse.validate(false);
        } catch (ValidationException e) {
            LOGGER.warn("Invalid AuthN response received from " + samlResponse.getIssuer(), e);
            return false;
        }

        return true;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private boolean login(org.opensaml.saml2.core.Response samlResponse) {
        Map<String, Cookie> cookieMap = HttpUtils.getCookieMap(request);
        if (cookieMap.containsKey("JSESSIONID")) {
            sessionFactory.getOrCreateSession(request)
                    .invalidate();
        }
        String assertionValue = DOM2Writer.nodeToString(samlResponse.getAssertions()
                .get(0)
                .getDOM());

        String encodedAssertion;
        try {
            encodedAssertion = RestSecurity.deflateAndBase64Encode(assertionValue);
        } catch (IOException e) {
            LOGGER.warn("Unable to deflate and encode assertion.", e);
            return false;
        }

        final String authHeader = RestSecurity.SAML_HEADER_PREFIX + encodedAssertion;

        HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request) {
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
        HandlerResult samlResult = samlAssertionHandler.getNormalizedToken(wrappedRequest,
                null,
                null,
                false);

        if (samlResult.getStatus() != HandlerResult.Status.COMPLETED) {
            LOGGER.debug("Failed to handle SAML assertion.");
            return false;
        }

        request.setAttribute(WebSSOFilter.DDF_AUTHENTICATION_TOKEN, samlResult);
        request.removeAttribute(ContextPolicy.NO_AUTH_POLICY);

        try {
            LOGGER.trace("Trying to login with provided SAML assertion.");
            loginFilter.doFilter(wrappedRequest, null, (servletRequest, servletResponse) -> {
            });
        } catch (IOException | ServletException e) {
            LOGGER.debug("Failed to apply login filter to SAML assertion", e);
            return false;
        }

        return true;
    }

    @GET
    @Path("/metadata")
    @Produces("application/xml")
    public Response retrieveMetadata() throws WSSecurityException, CertificateEncodingException {
        X509Certificate issuerCert = findCertificate(systemCrypto.getSignatureAlias(),
                systemCrypto.getSignatureCrypto());
        X509Certificate encryptionCert = findCertificate(systemCrypto.getEncryptionAlias(),
                systemCrypto.getEncryptionCrypto());

        String hostname = baseUrl.getHost();
        String port = baseUrl.getPort();
        String rootContext = baseUrl.getRootContext();

        String entityId = String.format("https://%s:%s%s/saml", hostname, port, rootContext);

        String logoutLocation = String.format("https://%s:%s%s/saml/logout",
                hostname,
                port,
                rootContext);
        String assertionConsumerServiceLocation = String.format("https://%s:%s%s/saml/sso",
                hostname,
                port,
                rootContext);

        EntityDescriptor entityDescriptor = SamlProtocol.createSpMetadata(entityId,
                Base64.encodeBase64String(issuerCert.getEncoded()),
                Base64.encodeBase64String(encryptionCert.getEncoded()),
                logoutLocation,
                assertionConsumerServiceLocation,
                assertionConsumerServiceLocation);

        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
        return Response.ok(DOM2Writer.nodeToString(OpenSAMLUtil.toDom(entityDescriptor,
                doc,
                false)))
                .build();
    }

    private X509Certificate findCertificate(String alias, Crypto crypto)
            throws WSSecurityException {
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias(alias);
        X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
        if (certs == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.SECURITY_ERROR,
                    "Unable to retrieve certificate");
        }
        return certs[0];
    }

    private org.opensaml.saml2.core.Response extractSamlResponse(String samlResponse) {
        org.opensaml.saml2.core.Response response = null;
        try {
            Document responseDoc =
                    StaxUtils.read(new ByteArrayInputStream(samlResponse.getBytes()));
            XMLObject responseXmlObject = OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());

            if (responseXmlObject instanceof org.opensaml.saml2.core.Response) {
                response = (org.opensaml.saml2.core.Response) responseXmlObject;
            }
        } catch (XMLStreamException | WSSecurityException e) {
            LOGGER.debug("Failed to convert AuthN response string to object.", e);
        }

        return response;
    }

    private String decodeBase64(String encoded) {
        return new String(Base64.decodeBase64(encoded.getBytes()));
    }

    public Filter getLoginFilter() {
        return loginFilter;
    }

    public void setLoginFilter(Filter loginFilter) {
        this.loginFilter = loginFilter;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }
}
