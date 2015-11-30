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
package org.codice.ddf.security.filter.login;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.HashSet;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;

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

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.rs.security.saml.sso.SAMLProtocolResponseValidator;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.WSSConfig;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.saml.WSSSAMLKeyInfoProcessor;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.SamlAssertionValidator;
import org.apache.wss4j.dom.validate.Validator;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.platform.util.XMLUtils;
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

import ddf.security.PropertiesLoader;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.assertion.impl.SecurityAssertionImpl;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.common.util.SecurityTokenHolder;
import ddf.security.http.SessionFactory;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;

/**
 * Servlet filter that exchanges all incoming tokens for a SAML assertion via an
 * STS.
 */
public class LoginFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginFilter.class);

    private static final String DDF_AUTHENTICATION_TOKEN = "ddf.security.token";

    private static final ThreadLocal<DocumentBuilder> BUILDER = new ThreadLocal<DocumentBuilder>() {
        @Override
        protected DocumentBuilder initialValue() {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                return factory.newDocumentBuilder();
            } catch (ParserConfigurationException ex) {
                // This exception should not happen
                throw new IllegalArgumentException("Unable to create new DocumentBuilder", ex);
            }
        }
    };

    private static SAMLObjectBuilder<Status> statusBuilder;

    private static SAMLObjectBuilder<StatusCode> statusCodeBuilder;

    private static SAMLObjectBuilder<StatusMessage> statusMessageBuilder;

    private static SAMLObjectBuilder<Response> responseBuilder;

    private static SAMLObjectBuilder<Issuer> issuerBuilder;

    private static XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();

    private final Object lock = new Object();

    private final SystemBaseUrl baseUrl;

    private SecurityManager securityManager;

    private String signaturePropertiesFile;

    private Crypto signatureCrypto;

    private Validator assertionValidator = new SamlAssertionValidator();

    private SessionFactory sessionFactory;

    /**
     * Default expiration value is 31 minutes
     */
    private int expirationTime = 31;

    public LoginFilter(SystemBaseUrl baseUrl) {
        super();
        this.baseUrl = baseUrl;
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

    public void setSessionFactory(SessionFactory sessionFactory) {
        synchronized (lock) {
            this.sessionFactory = sessionFactory;
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOGGER.debug("Starting LoginFilter.");
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
            final Subject subject = validateRequest(httpRequest, httpResponse);
            if (subject != null) {
                httpRequest.setAttribute(SecurityConstants.SECURITY_SUBJECT, subject);
                LOGGER.debug("Now performing request as user {} for {}", subject.getPrincipal(),
                        StringUtils.isNotBlank(httpRequest.getContextPath()) ?
                                httpRequest.getContextPath() :
                                httpRequest.getServletPath());
                SecurityLogger.logDebug("Executing request as user: " + subject.getPrincipal());
                subject.execute(new Callable<Object>() {

                    @Override
                    public Object call() throws Exception {
                        PrivilegedExceptionAction<Void> action = new PrivilegedExceptionAction<Void>() {
                            @Override
                            public Void run() throws Exception {
                                chain.doFilter(request, response);
                                return null;
                            }
                        };
                        SecurityAssertion securityAssertion = subject.getPrincipals()
                                .oneByType(SecurityAssertion.class);
                        if (null != securityAssertion) {
                            HashSet emptySet = new HashSet();
                            javax.security.auth.Subject javaSubject = new javax.security.auth.Subject(
                                    true, securityAssertion.getPrincipals(), emptySet, emptySet);
                            javax.security.auth.Subject.doAs(javaSubject, action);
                        } else {
                            LOGGER.debug("Subject had no security assertion.");
                        }
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

    private Subject handleAuthenticationToken(HttpServletRequest httpRequest,
            SAMLAuthenticationToken token) throws ServletException {
        Subject subject;
        try {
            LOGGER.debug("Validating received SAML assertion.");

            boolean wasReference = false;
            if (token.isReference()) {
                wasReference = true;
                LOGGER.trace("Converting SAML reference to assertion");
                Object sessionToken = httpRequest.getSession(false)
                        .getAttribute(SecurityConstants.SAML_ASSERTION);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Http Session assertion - class: {}  loader: {}",
                            sessionToken.getClass().getName(),
                            sessionToken.getClass().getClassLoader());
                    LOGGER.trace("SecurityToken class: {}  loader: {}",
                            SecurityToken.class.getName(), SecurityToken.class.getClassLoader());
                }
                SecurityToken savedToken = null;
                try {
                    savedToken = ((SecurityTokenHolder) sessionToken)
                            .getSecurityToken(token.getRealm());
                } catch (ClassCastException e) {
                    httpRequest.getSession(false).invalidate();
                }
                if (savedToken != null) {
                    token.replaceReferenece(savedToken);
                }
                if (token.isReference()) {
                    String msg = "Missing or invalid SAML assertion for provided reference.";
                    LOGGER.error(msg);
                    throw new InvalidSAMLReceivedException(msg);
                }
            }

            SAMLAuthenticationToken newToken = renewSecurityToken(httpRequest.getSession(false),
                    token);

            synchronized (lock) {
                SecurityToken securityToken;
                if (newToken != null) {
                    securityToken = (SecurityToken) newToken.getCredentials();
                } else {
                    securityToken = (SecurityToken) token.getCredentials();
                }
                if (!wasReference) {
                    // wrap the token
                    SamlAssertionWrapper assertion = new SamlAssertionWrapper(
                            securityToken.getToken());

                    // get the crypto junk
                    Crypto crypto = getSignatureCrypto();
                    Response samlResponse = createSamlResponse(httpRequest.getRequestURI(),
                            assertion.getIssuerString(),
                            createStatus(SAMLProtocolResponseValidator.SAML2_STATUSCODE_SUCCESS,
                                    null));

                    BUILDER.get().reset();
                    Document doc = BUILDER.get().newDocument();
                    Element policyElement = OpenSAMLUtil.toDom(samlResponse, doc);
                    doc.appendChild(policyElement);

                    Credential credential = new Credential();
                    credential.setSamlAssertion(assertion);

                    RequestData requestData = new RequestData();
                    requestData.setSigVerCrypto(crypto);
                    WSSConfig wssConfig = WSSConfig.getNewInstance();
                    requestData.setWssConfig(wssConfig);

                    if (assertion.isSigned()) {
                        if (assertion.getSaml1() != null) {
                            assertion.getSaml1().getDOM()
                                    .setIdAttributeNS(null, "AssertionID", true);
                        } else {
                            assertion.getSaml2().getDOM().setIdAttributeNS(null, "ID", true);
                        }

                        // Verify the signature
                        WSSSAMLKeyInfoProcessor wsssamlKeyInfoProcessor = new WSSSAMLKeyInfoProcessor(
                                requestData,
                                new WSDocInfo(samlResponse.getDOM().getOwnerDocument()));
                        assertion.verifySignature(wsssamlKeyInfoProcessor, crypto);

                        assertion.parseSubject(new WSSSAMLKeyInfoProcessor(requestData,
                                        new WSDocInfo(samlResponse.getDOM().getOwnerDocument())),
                                requestData.getSigVerCrypto(), requestData.getCallbackHandler());
                    }

                    // Validate the Assertion & verify trust in the signature
                    assertionValidator.validate(credential, requestData);
                }

                // if it is all good, then we'll create our subject
                subject = securityManager.getSubject(securityToken);

                addSamlToSession(httpRequest, token.getRealm(), securityToken);
            }
        } catch (SecurityServiceException e) {
            LOGGER.error("Unable to get subject from SAML request.", e);
            throw new ServletException(e);
        } catch (WSSecurityException e) {
            LOGGER.error("Unable to read/validate security token from request.", e);
            throw new ServletException(e);
        }
        return subject;
    }

    private SAMLAuthenticationToken renewSecurityToken(HttpSession session,
            SAMLAuthenticationToken savedToken) throws ServletException, WSSecurityException {
        if (session != null) {
            SecurityAssertion savedAssertion = new SecurityAssertionImpl(
                    ((SecurityToken) savedToken.getCredentials()));

            if (savedAssertion.getIssuer() != null && !savedAssertion.getIssuer()
                    .equals(baseUrl.getHost())) {
                return null;
            }

            if (savedAssertion.getNotOnOrAfter() == null) {
                return null;
            }

            long afterMil = savedAssertion.getNotOnOrAfter().getTime();
            long timeoutSeconds = (afterMil - System.currentTimeMillis()) / 1000;

            if (timeoutSeconds < 0) {
                String msg = "SAML assertion has expired.";
                LOGGER.info(msg);
                throw new InvalidSAMLReceivedException(msg);
            }

            if (timeoutSeconds <= 60) {
                synchronized (lock) {
                    try {
                        LOGGER.debug("Attempting to refresh user's SAML assertion.");

                        Subject subject = securityManager.getSubject(savedToken);
                        LOGGER.debug("Refresh of user assertion successful");
                        for (Object principal : subject.getPrincipals()) {
                            if (principal instanceof SecurityAssertion) {
                                SecurityToken token = ((SecurityAssertion) principal)
                                        .getSecurityToken();
                                SAMLAuthenticationToken samlAuthenticationToken = new SAMLAuthenticationToken(
                                        (java.security.Principal) savedToken.getPrincipal(), token,
                                        savedToken.getRealm());
                                if (LOGGER.isTraceEnabled()) {
                                    LOGGER.trace(
                                            "Setting session token - class: {}  classloader: {}",
                                            token.getClass().getName(),
                                            token.getClass().getClassLoader());
                                }
                                ((SecurityTokenHolder) session
                                        .getAttribute(SecurityConstants.SAML_ASSERTION))
                                        .addSecurityToken(savedToken.getRealm(), token);

                                LOGGER.debug("Saved new user assertion to session.");

                                return samlAuthenticationToken;
                            }
                        }

                    } catch (SecurityServiceException e) {
                        LOGGER.warn(
                                "Unable to refresh user's SAML assertion. User will log out prematurely.",
                                e);
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

    private Subject handleAuthenticationToken(HttpServletRequest httpRequest,
            BaseAuthenticationToken token) throws ServletException {
        Subject subject = null;
        synchronized (lock) {
            HttpSession session = sessionFactory.getOrCreateSession(httpRequest);
            //if we already have an assertion inside the session and it has not expired, then use that instead
            SecurityToken sessionToken = getSecurityToken(session, token.getRealm());

            if (sessionToken == null) {

                /*
                 * The user didn't have a SAML token from a previous authentication, but they do have the
                 * credentials to log in - perform that action here.
                 */
                try {
                    // login with the specified authentication credentials (AuthenticationToken)
                    subject = securityManager.getSubject(token);

                    for (Object principal : subject.getPrincipals().asList()) {
                        if (principal instanceof SecurityAssertion) {
                            if (LOGGER.isTraceEnabled()) {
                                Element samlToken = ((SecurityAssertion) principal)
                                        .getSecurityToken().getToken();

                                LOGGER.trace("SAML Assertion returned: {}",
                                        XMLUtils.prettyFormat(samlToken));
                            }
                            SecurityToken securityToken = ((SecurityAssertion) principal)
                                    .getSecurityToken();
                            addSamlToSession(httpRequest, token.getRealm(), securityToken);
                        }
                    }
                } catch (SecurityServiceException e) {
                    LOGGER.error("Unable to get subject from auth request.", e);
                    throw new ServletException(e);
                }
            } else {
                LOGGER.trace("Creating SAML authentication token with session.");
                SAMLAuthenticationToken samlToken = new SAMLAuthenticationToken(null,
                        session.getId(), token.getRealm());
                return handleAuthenticationToken(httpRequest, samlToken);

            }
        }
        return subject;
    }

    private SecurityToken getSecurityToken(HttpSession session, String realm) {
        if (session.getAttribute(SecurityConstants.SAML_ASSERTION) == null) {
            LOGGER.error(
                    "Security token holder missing from session. New session created improperly.");
            return null;
        }

        SecurityTokenHolder tokenHolder = ((SecurityTokenHolder) session
                .getAttribute(SecurityConstants.SAML_ASSERTION));

        SecurityToken token = tokenHolder.getSecurityToken(realm);

        if (token != null) {
            SecurityAssertionImpl assertion = new SecurityAssertionImpl(token);
            if (assertion.getNotOnOrAfter() != null
                    && assertion.getNotOnOrAfter().getTime() - System.currentTimeMillis() < 0) {
                LOGGER.debug("Session SAML token has expired.  Removing from session.");
                tokenHolder.remove(realm);
                return null;
            }
        }

        return token;
    }

    private void addSecurityToken(HttpSession session, String realm, SecurityToken token) {
        SecurityTokenHolder holder = (SecurityTokenHolder) session
                .getAttribute(SecurityConstants.SAML_ASSERTION);

        holder.addSecurityToken(realm, token);
    }

    /**
     * Adds SAML assertion to HTTP session.
     *
     * @param httpRequest   the http request object for this request
     * @param securityToken the SecurityToken object representing the SAML assertion
     */
    private void addSamlToSession(HttpServletRequest httpRequest, String realm,
            SecurityToken securityToken) {
        if (securityToken == null) {
            LOGGER.debug("Cannot add null security token to session.");
            return;
        }

        synchronized (lock) {
            HttpSession session = sessionFactory.getOrCreateSession(httpRequest);
            SecurityToken sessionToken = getSecurityToken(session, realm);
            if (sessionToken == null) {
                addSecurityToken(session, realm, securityToken);
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
        BUILDER.remove();
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
     *
     * @return minutes for session expiration
     */
    public int getExpirationTime() {
        return expirationTime;
    }

    /**
     * Sets session expiration time in minutes
     *
     * @param expirationTime - time in minutes
     */
    public void setExpirationTime(int expirationTime) {
        this.expirationTime = expirationTime;
    }

}
