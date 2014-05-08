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
package org.codice.security.filter.login;

import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.UUID;

/**
 *
 */
public class LoginFilter implements Filter {

    private static final transient Logger LOGGER = LoggerFactory
            .getLogger(LoginFilter.class);

    private static final String SAML_COOKIE_NAME = "org.codice.websso.saml.token";

    private SecurityManager securityManager;

    private static SAMLObjectBuilder<Status> statusBuilder;

    private static SAMLObjectBuilder<StatusCode> statusCodeBuilder;

    private static SAMLObjectBuilder<StatusMessage> statusMessageBuilder;

    private static SAMLObjectBuilder<Response> responseBuilder;

    private static SAMLObjectBuilder<Issuer> issuerBuilder;

    private static XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();

    private String signaturePropertiesFile;

    private Crypto signatureCrypto;

    @Override public void init(FilterConfig filterConfig) throws ServletException {
        LOGGER.info("Starting log in filter.");
    }

    @Override public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        Subject subject = null;

        Object securityToken = httpRequest.getAttribute("ddf.security.securityToken");
        Object token = httpRequest.getAttribute("ddf.security.token");
        if(securityToken != null) {
            try {
                //wrap the token
                AssertionWrapper assertion = new AssertionWrapper(((SecurityToken)securityToken).getToken());

                //get the crypto junk
                Crypto crypto = getSignatureCrypto();
                org.opensaml.saml2.core.Response samlResponse = createSamlResponse(httpRequest.getRequestURI(), assertion.getIssuerString(), createStatus(SAMLProtocolResponseValidator.SAML2_STATUSCODE_SUCCESS, null));
                samlResponse.getAssertions().add(assertion.getSaml2());
                SAMLProtocolResponseValidator validator = new SAMLProtocolResponseValidator();

                //validate the assertion
                validator.validateSamlResponse(samlResponse, crypto, null);

                //if it is all good, then we'll create our subject
                subject = securityManager.getSubject(securityToken);
            } catch (SecurityServiceException e) {
                LOGGER.error("Unable to get subject from SAML request.", e);
                returnNotAuthorized(httpResponse);
            } catch (WSSecurityException e) {
                LOGGER.error("Unable to read/validate security token from http request.", e);
            }
        } else if(token != null) {
            try {
                subject = securityManager.getSubject(token);

                for(Object principal : subject.getPrincipals().asList()){
                    if(principal instanceof SecurityAssertion) {
                        Element samlToken = ((SecurityAssertion) principal).getSecurityToken().getToken();
                        createSamlCookie(httpRequest, httpResponse, encodeSaml(samlToken));
                    }
                }
            } catch (SecurityServiceException e) {
                LOGGER.error("Unable to get subject from auth request.", e);
                returnNotAuthorized(httpResponse);
            } catch (WSSecurityException e) {
                LOGGER.error("Unable to encode SAML cookie.", e);
                returnNotAuthorized(httpResponse);
            }
        }

        if(subject != null) {
            httpRequest.setAttribute("ddf.security.subject", subject);
        } else {
            LOGGER.debug("Could not attach subject to http request.");
            returnNotAuthorized(httpResponse);
        }

    }

    public static Response createSamlResponse(
            String inResponseTo,
            String issuer,
            Status status
    ) {
        if (responseBuilder == null) {
            responseBuilder = (SAMLObjectBuilder<Response>)
                    builderFactory.getBuilder(Response.DEFAULT_ELEMENT_NAME);
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

    public static Issuer createIssuer(
            String issuerValue
    ) {
        if (issuerBuilder == null) {
            issuerBuilder = (SAMLObjectBuilder<Issuer>)
                    builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME);
        }
        Issuer issuer = issuerBuilder.buildObject();
        issuer.setValue(issuerValue);

        return issuer;
    }

    public static Status createStatus(
            String statusCodeValue,
            String statusMessage
    ) {
        if (statusBuilder == null) {
            statusBuilder = (SAMLObjectBuilder<Status>)
                    builderFactory.getBuilder(Status.DEFAULT_ELEMENT_NAME);
        }
        if (statusCodeBuilder == null) {
            statusCodeBuilder = (SAMLObjectBuilder<StatusCode>)
                    builderFactory.getBuilder(StatusCode.DEFAULT_ELEMENT_NAME);
        }
        if (statusMessageBuilder == null) {
            statusMessageBuilder = (SAMLObjectBuilder<StatusMessage>)
                    builderFactory.getBuilder(StatusMessage.DEFAULT_ELEMENT_NAME);
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

    private void returnNotAuthorized(HttpServletResponse response) {
        try {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentLength(0);
            response.flushBuffer();
        } catch (IOException ioe) {
            LOGGER.debug("Failed to send auth response: {}", ioe);
        }

    }

    private void createSamlCookie(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String cookieValue) {
        try {
            Cookie cookie = new Cookie(SAML_COOKIE_NAME, cookieValue);
            URL url = new URL(httpRequest.getRequestURL().toString());
            cookie.setDomain(url.getHost());
            cookie.setPath("/");
            //TODO do we want a max age??
            cookie.setMaxAge(0);

            httpResponse.addCookie(cookie);
        } catch (MalformedURLException e) {
            LOGGER.error("Unable to get URL from request.", e);
            returnNotAuthorized(httpResponse);
        }
    }

    private String encodeSaml(Element token) throws WSSecurityException {
        AssertionWrapper assertion = new AssertionWrapper(token);
        String samlStr = assertion.assertionToString();
        DeflateEncoderDecoder deflateEncoderDecoder = new DeflateEncoderDecoder();
        byte[] deflatedToken = deflateEncoderDecoder.deflateToken(samlStr.getBytes());
        return Base64Utility.encode(deflatedToken);
    }

    protected Crypto getSignatureCrypto() {
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

    @Override public void destroy() {
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
