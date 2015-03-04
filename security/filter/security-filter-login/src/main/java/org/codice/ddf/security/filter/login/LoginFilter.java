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
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.rs.security.saml.sso.SAMLProtocolResponseValidator;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.ws.security.WSDocInfo;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.saml.ext.OpenSAMLUtil;
import org.apache.ws.security.validate.Credential;
import org.apache.ws.security.validate.SamlAssertionValidator;
import org.apache.ws.security.validate.Validator;
import org.codice.ddf.security.common.PropertiesLoader;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.InvalidSAMLReceivedException;
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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Servlet filter that exchanges all incoming tokens for a SAML assertion via an
 * STS.
 */
public class LoginFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginFilter.class);

    private static final String DDF_AUTHENTICATION_TOKEN = "ddf.security.token";

    private static final String SAML_EXPIRATION = "saml.expiration";

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

    private Validator assertionValidator = new SamlAssertionValidator();

    private final Object lock = new Object();

    /**
     * Default expiration value is 31 minutes
     */
    private int expirationTime = 31;

    public LoginFilter() {
        super();
    }

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
                LOGGER.debug("Now performing request as user {} for {}", subject.getPrincipal(), StringUtils.isNotBlank(httpRequest.getContextPath()) ? httpRequest.getContextPath() : httpRequest.getServletPath());
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

        Object ddfAuthToken = httpRequest.getAttribute(DDF_AUTHENTICATION_TOKEN);

        if (ddfAuthToken instanceof HandlerResult) {
            HandlerResult result = (HandlerResult) ddfAuthToken;
            BaseAuthenticationToken thisToken = result.getToken();

            /*
             * If the user has already authenticated they will have a valid SAML token. Validate
             * that here and create the subject from the token.
             */
            if (thisToken instanceof SAMLAuthenticationToken) {
                subject = handleAuthenticationToken(httpRequest,
                        (SAMLAuthenticationToken) thisToken);
            } else if (thisToken != null) {
                subject = handleAuthenticationToken(httpRequest, thisToken);
            }
        }

        return subject;
    }

    private Subject handleAuthenticationToken(HttpServletRequest httpRequest, SAMLAuthenticationToken token) throws ServletException {
        Subject subject;
        try {
            LOGGER.debug("Validating received SAML assertion.");

            boolean wasReference = false;
            if (token.isReference()) {
                wasReference = true;
                LOGGER.trace("Converting SAML reference to assertion");
                Object sessionToken = httpRequest.getSession(false).getAttribute(SecurityConstants.SAML_ASSERTION);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Http Session assertion - class: {}  loader: {}", sessionToken.getClass().getName(), sessionToken.getClass().getClassLoader());
                    LOGGER.trace("SecurityToken class: {}  loader: {}", SecurityToken.class.getName(), SecurityToken.class.getClassLoader());
                }
                SecurityToken savedToken = (SecurityToken) sessionToken;
                if (savedToken != null) {
                    token.replaceReferenece(savedToken);
                }
                if (token.isReference()) {
                    String msg = "Missing or invalid SAML assertion for provided reference.";
                    LOGGER.error(msg);
                    throw new InvalidSAMLReceivedException(msg);
                }
            }

            SAMLAuthenticationToken newToken = renewSecurityToken(httpRequest.getSession(false), token);

            synchronized (lock) {
                SecurityToken securityToken;
                if (newToken != null) {
                    securityToken = (SecurityToken) newToken.getCredentials();
                } else {
                    securityToken = (SecurityToken) token.getCredentials();
                }
                if(!wasReference) {
                    // wrap the token
                    AssertionWrapper assertion = new AssertionWrapper(securityToken.getToken());

                    // get the crypto junk
                    Crypto crypto = getSignatureCrypto();
                    Response samlResponse = createSamlResponse(
                            httpRequest.getRequestURI(), assertion.getIssuerString(),
                            createStatus(SAMLProtocolResponseValidator.SAML2_STATUSCODE_SUCCESS,
                                    null));

                    if (docBuilder == null) {
                        throw new SecurityServiceException("Unable to validate SAML assertions.");
                    }

                    Document doc = docBuilder.newDocument();
                    Element policyElement = OpenSAMLUtil.toDom(samlResponse, doc);
                    doc.appendChild(policyElement);

                    Credential credential = new Credential();
                    credential.setAssertion(assertion);

                    RequestData requestData = new RequestData();
                    requestData.setSigCrypto(crypto);
                    WSSConfig wssConfig = WSSConfig.getNewInstance();
                    requestData.setWssConfig(wssConfig);

                    if (assertion.isSigned()) {
                        if (assertion.getSaml1() != null) {
                            assertion.getSaml1().getDOM().setIdAttributeNS(null, "AssertionID", true);
                        } else {
                            assertion.getSaml2().getDOM().setIdAttributeNS(null, "ID", true);
                        }

                        // Verify the signature
                        try {
                            assertion.verifySignature(requestData, new WSDocInfo(samlResponse.getDOM().getOwnerDocument()));
                        } catch (WSSecurityException e) {
                            throw new WSSecurityException(WSSecurityException.FAILURE, "invalidSAMLsecurity");
                        }
                    }

                    // Validate the Assertion & verify trust in the signature
                    try {
                        assertionValidator.validate(credential, requestData);
                    } catch (WSSecurityException ex) {
                        throw new WSSecurityException(WSSecurityException.FAILURE, "invalidSAMLsecurity");
                    }
                }

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

    private SAMLAuthenticationToken renewSecurityToken(HttpSession session, SAMLAuthenticationToken savedToken) throws WSSecurityException {
        long timeoutSeconds = -1;
        if (session != null) {
            Long afterMil = (Long) session.getAttribute(SAML_EXPIRATION);
            long beforeMil = System.currentTimeMillis();
            if (afterMil == null) {
                synchronized (lock) {
                    AssertionWrapper assertion = new AssertionWrapper(((SecurityToken) savedToken.getCredentials()).getToken());
                    if (assertion.getSaml2() != null) {
                        DateTime after = assertion.getSaml2().getConditions().getNotOnOrAfter();
                        afterMil = after.getMillis();
                    }
                }
            }

            if (afterMil != null) {
                timeoutSeconds = (afterMil - beforeMil) / 1000;
            }
            if (timeoutSeconds <= 60) {
                synchronized (lock) {
                    try {
                        LOGGER.debug("Attempting to refresh user's SAML assertion.");

                        Subject subject = securityManager.getSubject(savedToken);
                        LOGGER.debug("Refresh of user assertion successful");
                        for (Object principal : subject.getPrincipals()) {
                            if (principal instanceof SecurityAssertion) {
                                SecurityToken token = ((SecurityAssertion) principal).getSecurityToken();
                                SAMLAuthenticationToken samlAuthenticationToken = new SAMLAuthenticationToken(
                                        (java.security.Principal) savedToken.getPrincipal(), token, savedToken.getRealm());
                                if (LOGGER.isTraceEnabled()) {
                                    LOGGER.trace("Setting session token - class: {}  classloader: {}", token.getClass().getName(), token.getClass().getClassLoader());
                                }
                                session.setAttribute(SecurityConstants.SAML_ASSERTION, token);

                                AssertionWrapper assertion = new AssertionWrapper(((SecurityToken) savedToken.getCredentials()).getToken());
                                if (assertion.getSaml2() != null) {
                                    DateTime after = assertion.getSaml2().getConditions().getNotOnOrAfter();
                                    afterMil = after.getMillis();
                                }

                                session.setAttribute(SAML_EXPIRATION, afterMil);
                                LOGGER.debug("Saved new user assertion to session.");

                                return samlAuthenticationToken;
                            }
                        }

                    } catch (SecurityServiceException e) {
                        LOGGER.warn("Unable to refresh user's SAML assertion. User will log out prematurely.", e);
                        session.invalidate();
                    } catch (Exception e) {
                        LOGGER.warn("Unhandled exception occurred.", e);
                        session.invalidate();
                    }
                }
            }
        }
        return null;
    }

    private Subject handleAuthenticationToken(HttpServletRequest httpRequest, BaseAuthenticationToken token) throws ServletException {
        Subject subject;

        HttpSession session = httpRequest.getSession(true);
        //if we already have an assertion inside the session, then use that instead
        if (session.getAttribute(SecurityConstants.SAML_ASSERTION) == null) {

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
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("SAML assertion returned: {}",
                                    XMLUtils.toString(samlToken));
                        }
                        SecurityToken securityToken = ((SecurityAssertion) principal)
                                .getSecurityToken();
                        createSamlCookie(httpRequest, securityToken);
                    }
                }
            } catch (SecurityServiceException e) {
                LOGGER.error("Unable to get subject from auth request.", e);
                throw new ServletException(e);
            }
        } else {
            LOGGER.trace("Creating SAML authentication token with session.");
            SAMLAuthenticationToken samlToken = new SAMLAuthenticationToken(null, session.getId(),
                    token.getRealm());
            return handleAuthenticationToken(httpRequest, samlToken);

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
     * @param securityToken the SecurityToken object representing the SAML assertion
     */
    private void createSamlCookie(HttpServletRequest httpRequest, SecurityToken securityToken) {
        synchronized (lock) {
            HttpSession session = httpRequest.getSession(true);
            if (session.getAttribute(SecurityConstants.SAML_ASSERTION) == null) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Creating token in session - class: {}  classloader: {}", securityToken.getClass().getName(), securityToken.getClass().getClassLoader());
                }
                session.setAttribute(SecurityConstants.SAML_ASSERTION, securityToken);
                AssertionWrapper assertion = null;
                DateTime after = null;
                try {
                    assertion = new AssertionWrapper(securityToken.getToken());
                    after = assertion.getSaml2().getConditions().getNotOnOrAfter();
                    session.setAttribute(SAML_EXPIRATION, after.getMillis());
                } catch (Exception e) {
                    LOGGER.warn("Unable to set expiration date.", e);
                }
            }
            int minutes = getExpirationTime();
            //we just want to set this to some non-zero value if the configuration is messed up
            int seconds = 60;
            if (minutes > 0) {
                seconds = minutes * 60;
            }
            session.setMaxInactiveInterval(seconds);
        }
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

    /**
     * Returns session expiration time in minutes.
     * @return minutes for session expiration
     */
    public int getExpirationTime() {
        return expirationTime;
    }

    /**
     * Sets session expiration time in minutes
     * @param expirationTime - time in minutes
     */
    public void setExpirationTime(int expirationTime) {
        this.expirationTime = expirationTime;
    }
}
