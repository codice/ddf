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
package org.codice.ddf.security.handler.pki;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Properties;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codice.ddf.security.handler.api.AuthenticationHandler;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.PKIAuthenticationTokenFactory;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.PropertiesLoader;
import ddf.security.common.audit.SecurityLogger;

public abstract class AbstractPKIHandler implements AuthenticationHandler {

    public static final String SOURCE = "PKIHandler";

    public static final String CRL_PROPERTY_KEY = "org.apache.ws.security.crypto.merlin.x509crl.file";

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractPKIHandler.class);

    private static String encryptionPropertiesLocation = "etc/ws-security/server/encryption.properties";

    protected PKIAuthenticationTokenFactory tokenFactory;

    private boolean isEnabled = false;

    private CRL crl;

    @Override
    public abstract String getAuthenticationType();

    /**
     * Handler implementing PKI authentication. Returns the {@link org.codice.ddf.security.handler.api.HandlerResult} containing
     * a BinarySecurityToken if the operation was successful.
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

        String realm = (String) request.getAttribute(ContextPolicy.ACTIVE_REALM);
        HandlerResult handlerResult = new HandlerResult(HandlerResult.Status.NO_ACTION, null);
        handlerResult.setSource(realm + "-" + SOURCE);

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getServletPath();
        LOGGER.debug("Doing PKI authentication and authorization for path {}", path);

        //doesn't matter what the resolve flag is set to, we do the same action
        BaseAuthenticationToken token = extractAuthenticationInfo(realm,
                (X509Certificate[]) httpRequest
                        .getAttribute("javax.servlet.request.X509Certificate"));

        X509Certificate[] certs = (X509Certificate[]) request
                .getAttribute("javax.servlet.request.X509Certificate");

        HttpServletResponse httpResponse = response instanceof HttpServletResponse ?
                (HttpServletResponse) response :
                null;

        // Somehow the httpResponse was null, return no action and try to process with other handlers
        if (httpResponse == null) {
            LOGGER.error("Somehow the httpResponse was null");
            return handlerResult;
        }

        // No auth info was extracted, return NO_ACTION
        if (token == null) {
            return handlerResult;
        }

        Properties encryptionProperties = loadProperties(encryptionPropertiesLocation);
        setCrlLocation(encryptionProperties.getProperty(CRL_PROPERTY_KEY));
        // No CRL was specified, or there was an error reading
        if (crl == null) {
            handlerResult.setToken(token);
            handlerResult.setStatus(HandlerResult.Status.COMPLETED);
            return handlerResult;
        }

        // CRL was specified, check against CRL and return the result
        handlerResult = checkAgainstCRL(httpResponse, token, certs, handlerResult);
        return handlerResult;
    }

    /**
     * Checks the certificates agains the CRL. If it is in the CRL, send a 401 error and return a HandlerResult with
     * the status of REDIRECTED. Otherwise, set appropriate tokens on the HandlerResult and return with status of COMPLETED
     *
     * @param httpResponse  HttpServletResponse to send 401 error if needed
     * @param token         BaseAuthenticationToken containing the auth data to attached to the HandlerResult if it passes the CRL
     * @param certs         Certificates extracted from the request to check against the CRL
     * @param handlerResult HandlerResult to modify and return
     * @return returns the modified handler result. REDIRECTED status if it failed the CRL check or COMPLETED if it passed
     */
    public HandlerResult checkAgainstCRL(HttpServletResponse httpResponse,
            BaseAuthenticationToken token, X509Certificate[] certs, HandlerResult handlerResult) {
        if (passesCRL(certs)) {
            handlerResult.setToken(token);
            handlerResult.setStatus(HandlerResult.Status.COMPLETED);
        } else {
            // cert is present and in the CRL list - set handlerResult to REDIRECTED and return 401
            handlerResult.setStatus(HandlerResult.Status.REDIRECTED);
            try {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.flushBuffer();
            } catch (IOException e) {
                LOGGER.debug("Unable to send 401 response to client.");
            }
        }
        return handlerResult;
    }

    @Override
    public HandlerResult handleError(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain chain) throws ServletException {
        String realm = (String) servletRequest.getAttribute(ContextPolicy.ACTIVE_REALM);
        HandlerResult result = new HandlerResult(HandlerResult.Status.NO_ACTION, null);
        result.setSource(realm + "-" + SOURCE);
        LOGGER.debug("In error handler for pki - no action taken.");
        return result;
    }

    /**
     * Checks if the provided cert is listed in the CRL.
     *
     * @param certs
     * @return boolean value
     */
    public boolean passesCRL(X509Certificate[] certs) {
        if (certs != null) {
            if (crl != null && isEnabled) {
                LOGGER.debug("Got {} certificate(s) in the incoming request", certs.length);
                for (X509Certificate curCert : certs) {
                    if (crl.isRevoked(curCert)) {
                        SecurityLogger.logInfo("Denying access for user" + curCert.getSubjectDN()
                                + " due to certificate being revoked by CRL.");
                        LOGGER.warn(
                                "Denying access for user {} due to certificate being revoked by CRL.",
                                curCert.getSubjectDN());
                        return false;
                    }
                }
            } else {
                String errorMsg = "The CRL check passed because CRL is disabled. Check that your properties and CRL file are correct if revocation is needed.";
                SecurityLogger.logInfo(errorMsg);
                LOGGER.warn(errorMsg);
                return true;
            }
        } else {
            LOGGER.debug(
                    "Allowing message through CRL check. There were no certificates sent by the client.");
            return true;
        }
        return true;
    }

    protected abstract BaseAuthenticationToken extractAuthenticationInfo(String realm,
            X509Certificate[] certs);

    public void setTokenFactory(PKIAuthenticationTokenFactory factory) {
        tokenFactory = factory;
    }

    /**
     * Sets the location of the CRL. Enables CRL checking if property is set, disables it otherwise
     *
     * @param location Location of the DER-encoded CRL file that should be used to
     *                 check certificate revocation.
     */
    public synchronized void setCrlLocation(String location) {
        if (location == null) {
            LOGGER.warn("CRL property in [{}] is not set. Certs will not be checked against a CRL.",
                    encryptionPropertiesLocation);
            setIsEnabled(false);
            return;
        }

        crl = null;
        try {
            crl = createCRL(location);
            setIsEnabled(true);
        } catch (FileNotFoundException fnfe) {
            LOGGER.error("Could not find CRL file, cannot validate certificates to a CRL.", fnfe);
        } catch (CertificateException ce) {
            LOGGER.error(
                    "Encountered an error while trying to create new certificate factory. Cannot validate certificates to a CRL.",
                    ce);
        } catch (CRLException ce) {
            LOGGER.error(
                    "Could not create new CRL from the provided file. File may not be valid CRL DER-encoded file.",
                    ce);
        }
    }

    /**
     * Generates a new CRL object from the given location.
     *
     * @param location File location of the CRL file.
     * @return new CRL object
     * @throws FileNotFoundException If no file is located at the location
     * @throws CertificateException  If the Certificate factory cannot be located
     * @throws CRLException          If the input CRL file is invalid and cannot be used to
     *                               generate a crl object.
     */
    private CRL createCRL(String location)
            throws FileNotFoundException, CertificateException, CRLException {
        FileInputStream fis = new FileInputStream(new File(location));
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return cf.generateCRL(fis);
    }

    /**
     * For unit tests
     *
     * @return boolean isEnabled
     */
    public boolean getIsEnabled() {
        return isEnabled;
    }

    /**
     * Sets the isEnabled flag for the CRL checker which determines if the
     * handler should check the incoming request to the specified CRL.
     *
     * @param isEnabled boolean value that either turns on crl checking (true) or
     *                  turns off checking (false).
     */
    private void setIsEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    /**
     * Abstracted for unit tests
     *
     * @param location location of properties file
     * @return Properties from
     */
    public Properties loadProperties(String location) {
        return PropertiesLoader.loadProperties(location);
    }
}
