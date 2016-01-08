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
package org.codice.ddf.security.idp.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.impl.UriBuilderImpl;
import org.apache.cxf.rs.security.saml.sso.SamlpRequestComponentBuilder;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.common.util.DOM2Writer;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.codice.ddf.security.handler.api.AuthenticationHandler;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.saml.SAMLAssertionHandler;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ddf.security.http.SessionFactory;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.impl.RelayStates;

/**
 * Handler for SAML 2.0 IdP based authentication. Unauthenticated clients will be redirected to the
 * configured IdP for authentication.
 */
public class IdpHandler implements AuthenticationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdpHandler.class);

    /**
     * IdP type to use when configuring context policy.
     */
    public static final String AUTH_TYPE = "IDP";

    public static final String SOURCE = "IdpHandler";

    public static final String UNABLE_TO_ENCODE_SAML_AUTHN_REQUEST =
            "Unable to encode SAML AuthnRequest";

    public static final String UNABLE_TO_SIGN_SAML_AUTHN_REQUEST =
            "Unable to sign SAML Authn Request";

    static {
        OpenSAMLUtil.initSamlEngine();
    }

    private static XMLObjectBuilderFactory builderFactory =
            XMLObjectProviderRegistrySupport.getBuilderFactory();

    @SuppressWarnings("unchecked")
    private static SAMLObjectBuilder<AuthnRequest> authnRequestBuilder =
            (SAMLObjectBuilder<AuthnRequest>) builderFactory.getBuilder(AuthnRequest.DEFAULT_ELEMENT_NAME);

    @SuppressWarnings("unchecked")
    private static SAMLObjectBuilder<Issuer> issuerBuilder =
            (SAMLObjectBuilder<Issuer>) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME);

    private final String postBindingTemplate;

    private final SimpleSign simpleSign;

    private final IdpMetadata idpMetadata;

    private final SystemBaseUrl baseUrl;

    private final RelayStates<String> relayStates;

    private SessionFactory sessionFactory;

    public IdpHandler(SimpleSign simpleSign, IdpMetadata metadata, SystemBaseUrl baseUrl,
            RelayStates<String> relayStates) throws IOException {
        LOGGER.debug("Creating IdP handler.");

        this.simpleSign = simpleSign;
        idpMetadata = metadata;

        this.baseUrl = baseUrl;
        this.relayStates = relayStates;

        try (InputStream postFormStream = IdpHandler.class.getResourceAsStream("/post-binding.html")) {
            postBindingTemplate = IOUtils.toString(postFormStream);
        }
    }

    @Override
    public String getAuthenticationType() {
        return AUTH_TYPE;
    }

    /**
     * Handler implementing SAML 2.0 IdP authentication. Supports HTTP-Redirect and HTTP-POST bindings.
     *
     * @param request  http request to obtain attributes from and to pass into any local filter chains required
     * @param response http response to return http responses or redirects
     * @param chain    original filter chain (should not be called from your handler)
     * @param resolve  flag with true implying that credentials should be obtained, false implying return if no credentials are found.
     * @return result of handling this request - status and optional tokens
     * @throws ServletException
     */
    @Override
    public HandlerResult getNormalizedToken(ServletRequest request, ServletResponse response,
            FilterChain chain, boolean resolve) throws ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(httpRequest) {
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
        HandlerResult samlResult = samlAssertionHandler.getNormalizedToken(wrappedRequest,
                null,
                null,
                false);

        if (samlResult != null && samlResult.getStatus() == HandlerResult.Status.COMPLETED) {
            return samlResult;
        }

        HandlerResult handlerResult = new HandlerResult(HandlerResult.Status.REDIRECTED, null);
        handlerResult.setSource("idp-" + SOURCE);

        String path = httpRequest.getServletPath();
        LOGGER.debug("Doing IdP authentication and authorization for path {}", path);

        // Default to HTTP-Redirect if binding is null
        if (idpMetadata.getSingleSignOnBinding() == null || idpMetadata.getSingleSignOnBinding()
                .endsWith("Redirect")) {
            doHttpRedirectBinding((HttpServletRequest) request, (HttpServletResponse) response);
        } else {
            doHttpPostBinding((HttpServletRequest) request, (HttpServletResponse) response);
        }

        return handlerResult;
    }

    private void doHttpRedirectBinding(HttpServletRequest request, HttpServletResponse response)
            throws ServletException {

        String redirectUrl;
        String idpRequest = null;
        String relayState = createRelayState(request);
        try {
            String queryParams = String.format("SAMLRequest=%s&RelayState=%s", createAuthnRequest(
                    false), URLEncoder.encode(relayState, "UTF-8"));
            idpRequest = idpMetadata.getSingleSignOnLocation() + "?" + queryParams;
            UriBuilder idpUri = new UriBuilderImpl(new URI(idpRequest));

            simpleSign.signUriString(queryParams, idpUri);

            redirectUrl = idpUri.build()
                    .toString();
        } catch (UnsupportedEncodingException e) {
            LOGGER.warn("Unable to encode relay state: " + relayState, e);
            throw new ServletException("Unable to create return location");
        } catch (SimpleSign.SignatureException e) {
            String msg = "Unable to sign request";
            LOGGER.warn(msg, e);
            throw new ServletException(msg);
        } catch (URISyntaxException e) {
            LOGGER.warn("Unable to parse IDP request location: " + idpRequest, e);
            throw new ServletException("Unable to determine IDP location.");
        }

        try {
            response.sendRedirect(redirectUrl);
            response.flushBuffer();
        } catch (IOException e) {
            LOGGER.warn("Unable to redirect AuthnRequest to " + redirectUrl, e);
            throw new ServletException("Unable to redirect to IdP");
        }
    }

    private void doHttpPostBinding(HttpServletRequest request, HttpServletResponse response)
            throws ServletException {
        try {
            response.getWriter()
                    .printf(postBindingTemplate,
                            idpMetadata.getSingleSignOnLocation(),
                            createAuthnRequest(true),
                            createRelayState(request));
            response.setStatus(200);
            response.flushBuffer();
        } catch (IOException e) {
            LOGGER.warn("Unable to post AuthnRequest to IdP", e);
            throw new ServletException("Unable to post to IdP");
        }
    }

    private String createAuthnRequest(boolean isPost) throws ServletException {

        String spIssuerId = String.format("https://%s:%s%s/saml",
                baseUrl.getHost(),
                baseUrl.getHttpsPort(),
                baseUrl.getRootContext());
        String spAssertionConsumerServiceUrl = spIssuerId + "/sso";

        AuthnRequest authnRequest = authnRequestBuilder.buildObject();

        Issuer issuer = issuerBuilder.buildObject();
        issuer.setValue(spIssuerId);
        authnRequest.setIssuer(issuer);

        authnRequest.setAssertionConsumerServiceURL(spAssertionConsumerServiceUrl);

        authnRequest.setID("_" + UUID.randomUUID()
                .toString());
        authnRequest.setVersion(SAMLVersion.VERSION_20);
        authnRequest.setIssueInstant(new DateTime());

        authnRequest.setDestination(idpMetadata.getSingleSignOnLocation());

        authnRequest.setProtocolBinding(idpMetadata.getSingleSignOnBinding());
        authnRequest.setNameIDPolicy(SamlpRequestComponentBuilder.createNameIDPolicy(true,
                SAML2Constants.NAMEID_FORMAT_PERSISTENT,
                spIssuerId));

        return serializeAndSign(isPost, authnRequest);
    }

    private String serializeAndSign(boolean isPost, AuthnRequest authnRequest)
            throws ServletException {
        try {
            if (isPost) {
                simpleSign.signSamlObject(authnRequest);
            }

            Document doc = DOMUtils.createDocument();
            doc.appendChild(doc.createElement("root"));

            Element requestElement = OpenSAMLUtil.toDom(authnRequest, doc);

            String requestMessage = DOM2Writer.nodeToString(requestElement);

            LOGGER.trace(requestMessage);

            if (isPost) {
                return encodePostRequest(requestMessage);
            } else {
                return encodeRedirectRequest(requestMessage);
            }
        } catch (WSSecurityException | IOException e) {
            LOGGER.warn(UNABLE_TO_ENCODE_SAML_AUTHN_REQUEST, e);
            throw new ServletException(UNABLE_TO_ENCODE_SAML_AUTHN_REQUEST);
        } catch (SimpleSign.SignatureException e) {
            LOGGER.warn(UNABLE_TO_SIGN_SAML_AUTHN_REQUEST, e);
            throw new ServletException(UNABLE_TO_SIGN_SAML_AUTHN_REQUEST);
        }
    }

    private String createRelayState(HttpServletRequest request) {
        return relayStates.encode(recreateFullRequestUrl(request));
    }

    private String encodeRedirectRequest(String request) throws WSSecurityException, IOException {
        return URLEncoder.encode(RestSecurity.deflateAndBase64Encode(request), "UTF-8");
    }

    private String encodePostRequest(String request) throws WSSecurityException, IOException {
        return new String(Base64.encodeBase64(request.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }

    private String recreateFullRequestUrl(HttpServletRequest request) {
        StringBuffer requestURL = request.getRequestURL();
        String queryString = request.getQueryString();

        if (queryString == null) {
            return requestURL.toString();
        } else {
            return requestURL.append('?')
                    .append(queryString)
                    .toString();
        }
    }

    @Override
    public HandlerResult handleError(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain chain) throws ServletException {
        String realm = (String) servletRequest.getAttribute(ContextPolicy.ACTIVE_REALM);
        HandlerResult result = new HandlerResult(HandlerResult.Status.NO_ACTION, null);
        result.setSource(realm + "-" + SOURCE);
        LOGGER.debug("In error handler for idp - no action taken.");
        return result;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
}
