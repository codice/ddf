/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.security.filter.login;

import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.common.util.PropertiesLoader;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.rs.security.saml.DeflateEncoderDecoder;
import org.apache.cxf.rs.security.saml.sso.SAMLProtocolResponseValidator;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.saml.ext.OpenSAMLUtil;
import org.codice.ddf.security.handler.api.BSTAuthenticationToken;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.SAMLAuthenticationToken;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.StatusMessage;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Servlet filter that exchanges all incoming tokens for a SAML assertion via an
 * STS.
 */
public class LoginFilter implements Filter {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(LoginFilter.class);

    private static final String DDF_SECURITY_TOKEN = "ddf.security.securityToken";

    private static final String DDF_AUTHENTICATION_TOKEN = "ddf.security.token";

    private static final String SAML_COOKIE_NAME = "org.codice.websso.saml.token";
    private static final String SAML_COOKIE_REF = "org.codice.websso.saml.ref";

    private SecurityManager securityManager;

    private static SAMLObjectBuilder<Status> statusBuilder;

    private static SAMLObjectBuilder<StatusCode> statusCodeBuilder;

    private static SAMLObjectBuilder<StatusMessage> statusMessageBuilder;

    private static SAMLObjectBuilder<Response> responseBuilder;

    private static SAMLObjectBuilder<Issuer> issuerBuilder;

    private static XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();

    private String signaturePropertiesFile;

    private Crypto signatureCrypto;

    private DocumentBuilder docBuilder;

    private Object lock = new Object();

    private SAMLCache samlCache = new SAMLCache();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOGGER.debug("Starting LoginFilter.");
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
        try {
            docBuilder = docBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            LOGGER.error("Unable to create doc builder.", e);
        }

    }

    /**
     * Validates an attached SAML assertion, or exchanges any other incoming
     * token for a SAML assertion via the STS.
     *
     * @param request
     * @param response
     * @param chain
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response,
      final FilterChain chain) throws IOException, ServletException {
        LOGGER.debug("Performing doFilter() on LoginFilter");
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (request.getAttribute(ContextPolicy.NO_AUTH_POLICY) != null) {
            LOGGER.debug("NO_AUTH_POLICY header was found, skipping login filter.");
            chain.doFilter(request, response);
        } else {
            // perform validation
            Subject subject = validateRequest(httpRequest, httpResponse);
            if (subject != null) {
                httpRequest.setAttribute(SecurityConstants.SECURITY_SUBJECT, subject);
                LOGGER.debug("Now performing request as user {}", subject.getPrincipal());
                SecurityLogger.logDebug("Executing request as user: " + subject.getPrincipal());
                subject.execute(new Callable<Object>() {

                    @Override
                    public Object call() throws Exception {
                        chain.doFilter(request, response);
                        return null;
                    }

                });

            } else {
                LOGGER.debug("Could not attach subject to http request.");
            }
        }

    }

    private Subject validateRequest(final HttpServletRequest httpRequest,
      final HttpServletResponse httpResponse) throws IOException, ServletException {

        Subject subject = null;

        //Object securityToken = httpRequest.getAttribute(DDF_SECURITY_TOKEN);
        HandlerResult result = (HandlerResult) httpRequest.getAttribute(DDF_AUTHENTICATION_TOKEN);
        if (result != null) {
            BaseAuthenticationToken thisToken = result.getToken();

            /*
             * If the user has already authenticated they will have a valid SAML token. Validate
             * that here and create the subject from the token.
             */
            if (thisToken instanceof SAMLAuthenticationToken)
                subject = handleAuthenticationToken(httpRequest, httpResponse, (SAMLAuthenticationToken) thisToken);
            else if (thisToken instanceof BSTAuthenticationToken)
                subject = handleAuthenticationToken(httpRequest, httpResponse, (BSTAuthenticationToken) thisToken);


        }

        return subject;
    }

    private Subject handleAuthenticationToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse, SAMLAuthenticationToken token) throws ServletException {
        Subject subject;
        try {
            LOGGER.debug("Validating received SAML assertion.");

            // if we received a reference to a SAML assertion, replace it with the real assertion
            if (token.isReference()) {
                LOGGER.trace("Converting SAML reference to assertion");
                String realm = (String) httpRequest.getAttribute(ContextPolicy.ACTIVE_REALM);
                SecurityToken savedToken = samlCache.get(realm, (String)token.getCredentials());
                if (savedToken != null) {
                    token.replaceReferenece(savedToken);
                }
                if (token.isReference()) {
                    LOGGER.error("Couldn't find SAML assertion corresponding to provided reference.");
                    throw new ServletException("Unable to exchanged provided SAML reference for cached assertion.");
                }
            }

            // wrap the token
            SecurityToken securityToken = (SecurityToken) token.getCredentials();
            AssertionWrapper assertion = new AssertionWrapper(securityToken.getToken());

            // get the crypto junk
            Crypto crypto = getSignatureCrypto();
            Response samlResponse = createSamlResponse(
              httpRequest.getRequestURI(), assertion.getIssuerString(),
              createStatus(SAMLProtocolResponseValidator.SAML2_STATUSCODE_SUCCESS, null));

            synchronized (lock) {
                if (docBuilder == null) {
                    throw new SecurityServiceException("Unable to validate SAML assertions.");
                }

                Document doc = docBuilder.newDocument();
                Element policyElement = OpenSAMLUtil.toDom(samlResponse, doc);
                doc.appendChild(policyElement);
                Response marshalledResponse = (Response) OpenSAMLUtil
                  .fromDom(policyElement);
                SAMLProtocolResponseValidator validator = new SAMLProtocolResponseValidator();

                // validate the assertion
                validator.validateSamlResponse(marshalledResponse, crypto, null);

                // if it is all good, then we'll create our subject
                subject = securityManager.getSubject(securityToken);
            }
        } catch (SecurityServiceException e) {
            LOGGER.error("Unable to get subject from SAML request.", e);
            throw new ServletException(e);
        } catch (WSSecurityException e) {
            LOGGER.error("Unable to read/validate security token from http request.", e);
            throw new ServletException(e);
        }
        return subject;
    }

    private Subject handleAuthenticationToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse, BSTAuthenticationToken token) throws ServletException {
        Subject subject;

        /*
         * The user didn't have a SAML token from a previous authentication, but they do have the
         * credentials to log in - perform that action here.
         */
        try {
            synchronized (lock) {
                // login with the specified authentication credentials (AuthenticationToken)
                subject = securityManager.getSubject(token);
            }

            for (Object principal : subject.getPrincipals().asList()) {
                if (principal instanceof SecurityAssertion) {
                    Element samlToken = ((SecurityAssertion) principal).getSecurityToken()
                      .getToken();
                    SecurityToken securityToken = ((SecurityAssertion) principal).getSecurityToken();
                    AssertionWrapper assertion = new AssertionWrapper(securityToken.getToken());
                    DateTime before = assertion.getSaml2().getConditions().getNotBefore();
                    DateTime after = assertion.getSaml2().getConditions().getNotOnOrAfter();
                    long timeoutSeconds = -1;
                    if (before != null && after != null) {
                        long beforeMil = before.getMillis();
                        long afterMil = after.getMillis();
                        timeoutSeconds = (afterMil - beforeMil) / 1000;
                    }
                    createSamlCookie(httpRequest, httpResponse, securityToken, timeoutSeconds);
                }
            }
        } catch (SecurityServiceException e) {
            LOGGER.error("Unable to get subject from auth request.", e);
            throw new ServletException(e);
        } catch (WSSecurityException e) {
            LOGGER.error("Unable to encode SAML cookie.", e);
            throw new ServletException(e);
        }
        return subject;
    }

    /**
     * Creates the SAML response that we use for validation against the CXF
     * code.
     *
     * @param inResponseTo
     * @param issuer
     * @param status
     * @return Response
     */
    private static Response createSamlResponse(String inResponseTo, String issuer, Status status) {
        if (responseBuilder == null) {
            responseBuilder = (SAMLObjectBuilder<Response>) builderFactory
              .getBuilder(Response.DEFAULT_ELEMENT_NAME);
        }
        Response response = responseBuilder.buildObject();

        response.setID(UUID.randomUUID().toString());
        response.setIssueInstant(new DateTime());
        response.setInResponseTo(inResponseTo);
        response.setIssuer(createIssuer(issuer));
        response.setStatus(status);
        response.setVersion(SAMLVersion.VERSION_20);

        return response;
    }

    /**
     * Creates the issuer object for the response.
     *
     * @param issuerValue
     * @return Issuer
     */
    private static Issuer createIssuer(String issuerValue) {
        if (issuerBuilder == null) {
            issuerBuilder = (SAMLObjectBuilder<Issuer>) builderFactory
              .getBuilder(Issuer.DEFAULT_ELEMENT_NAME);
        }
        Issuer issuer = issuerBuilder.buildObject();
        issuer.setValue(issuerValue);

        return issuer;
    }

    /**
     * Creates the status object for the response.
     *
     * @param statusCodeValue
     * @param statusMessage
     * @return Status
     */
    private static Status createStatus(String statusCodeValue, String statusMessage) {
        if (statusBuilder == null) {
            statusBuilder = (SAMLObjectBuilder<Status>) builderFactory
              .getBuilder(Status.DEFAULT_ELEMENT_NAME);
        }
        if (statusCodeBuilder == null) {
            statusCodeBuilder = (SAMLObjectBuilder<StatusCode>) builderFactory
              .getBuilder(StatusCode.DEFAULT_ELEMENT_NAME);
        }
        if (statusMessageBuilder == null) {
            statusMessageBuilder = (SAMLObjectBuilder<StatusMessage>) builderFactory
              .getBuilder(StatusMessage.DEFAULT_ELEMENT_NAME);
        }

        Status status = statusBuilder.buildObject();

        StatusCode statusCode = statusCodeBuilder.buildObject();
        statusCode.setValue(statusCodeValue);
        status.setStatusCode(statusCode);

        if (statusMessage != null) {
            StatusMessage statusMessageObject = statusMessageBuilder.buildObject();
            statusMessageObject.setMessage(statusMessage);
            status.setStatusMessage(statusMessageObject);
        }

        return status;
    }

    /**
     * Creates a cookie to be returned to the browser if the token was
     * successfully exchanged for a SAML assertion. Adds it to the response
     * object to be sent back to the caller.
     *
     * @param httpRequest the http request object for this request
     * @param httpResponse the http response object for this request
     * @param securityToken the SecurityToken object representing the SAML assertion
     * @param timeoutSeconds the timeout value to use for the cookie
     */
    private void createSamlCookie(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
        SecurityToken securityToken, long timeoutSeconds) {
        try {
            String realm = (String) httpRequest.getAttribute(ContextPolicy.ACTIVE_REALM);

            // put this SecurityToken into the cache and get the key
            String samlReference = samlCache.put(realm, securityToken);
            LOGGER.debug("Cached SecurityToken and created cookie reference of {}.", samlReference);
            Cookie cookie = new Cookie(SAML_COOKIE_REF, samlReference);
            URL url = new URL(httpRequest.getRequestURL().toString());
            cookie.setDomain(url.getHost());
            cookie.setPath("/");
            cookie.setSecure(true);
            if (timeoutSeconds > Integer.MAX_VALUE || timeoutSeconds < Integer.MIN_VALUE) {
                cookie.setMaxAge(-1);
            } else {
                cookie.setMaxAge((int) timeoutSeconds);
            }

            httpResponse.addCookie(cookie);
        } catch (MalformedURLException e) {
            LOGGER.error("Unable to get URL from request.", e);
        }
    }

    /**
     * Encodes the SAML assertion as a deflated Base64 String so that it can be
     * used as a Cookie.
     *
     * @param token
     * @return String
     * @throws WSSecurityException
     */
    private String encodeSaml(Element token) throws WSSecurityException {
        AssertionWrapper assertion = new AssertionWrapper(token);
        String samlStr = assertion.assertionToString();
        DeflateEncoderDecoder deflateEncoderDecoder = new DeflateEncoderDecoder();
        byte[] deflatedToken = deflateEncoderDecoder.deflateToken(samlStr.getBytes());
        return Base64Utility.encode(deflatedToken);
    }

    /**
     * Returns a Crypto object initialized against the system signature
     * properties.
     *
     * @return Crypto
     */
    private Crypto getSignatureCrypto() {
        if (signatureCrypto == null && signaturePropertiesFile != null) {
            Properties sigProperties = PropertiesLoader.loadProperties(signaturePropertiesFile);
            if (sigProperties == null) {
                LOGGER.trace("Cannot load signature properties using: {}", signaturePropertiesFile);
                return null;
            }
            try {
                signatureCrypto = CryptoFactory.getInstance(sigProperties);
            } catch (WSSecurityException ex) {
                LOGGER.trace("Error in loading the signature Crypto object.", ex);
                return null;
            }
        }
        return signatureCrypto;
    }

    @Override
    public void destroy() {
        LOGGER.info("Destroying log in filter");
    }

    public SecurityManager getSecurityManager() {
        return securityManager;
    }

    public void setSecurityManager(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public void setSignaturePropertiesFile(String signaturePropertiesFile) {
        this.signaturePropertiesFile = signaturePropertiesFile;
    }
}
