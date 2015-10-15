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

import javax.servlet.http.HttpServletResponse;

import org.apache.wss4j.common.crypto.Merlin;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.PropertiesLoader;
import ddf.security.common.audit.SecurityLogger;

public class CrlChecker {

    public static final String CRL_PROPERTY_KEY = Merlin.OLD_PREFIX + Merlin.X509_CRL_FILE;

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractPKIHandler.class);

    private static String encryptionPropertiesLocation = "etc/ws-security/server/encryption.properties";

    private CRL crl = null;

    /**
     * Constructor method. Reads encryption.properties and sets CRL location
     */
    protected CrlChecker() {
        Properties encryptionProperties = loadProperties(encryptionPropertiesLocation);
        setCrlLocation(encryptionProperties.getProperty(CRL_PROPERTY_KEY));
    }

    public HandlerResult check(HttpServletResponse httpResponse, BaseAuthenticationToken token,
            X509Certificate[] certs, HandlerResult handlerResult) {
        if (crl == null) {
            LOGGER.trace("CRL is not set. Skipping CRL check");
            handlerResult.setToken(token);
            handlerResult.setStatus(HandlerResult.Status.COMPLETED);
            return handlerResult;
        }
        LOGGER.trace("Checking request certs against CRL.");
        return checkAgainstCrl(httpResponse, token, certs, handlerResult);
    }

    /**
     * Checks the certificates against the CRL. If it is in the CRL, send a 401 error and return a HandlerResult with
     * the status of REDIRECTED. Otherwise, set appropriate tokens on the HandlerResult and return with status of COMPLETED
     *
     * @param httpResponse  HttpServletResponse to send 401 error if needed
     * @param token         BaseAuthenticationToken containing the auth data to be attached to the HandlerResult if it passes the CRL
     * @param certs         Certificates extracted from the request to check against the CRL
     * @param handlerResult HandlerResult to modify and return
     * @return returns the modified handler result. REDIRECTED status if it failed the CRL check or COMPLETED if it passed
     */

    HandlerResult checkAgainstCrl(HttpServletResponse httpResponse, BaseAuthenticationToken token,
            X509Certificate[] certs, HandlerResult handlerResult) {
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

    /**
     * Checks if the provided cert is listed in the CRL.
     *
     * @param certs
     * @return boolean value
     */
    public boolean passesCRL(X509Certificate[] certs) {
        if (certs != null) {
            if (crl != null) {
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

    /**
     * Sets the location of the CRL. Enables CRL checking if property is set, disables it otherwise
     *
     * @param location Location of the DER-encoded CRL file that should be used to
     *                 check certificate revocation.
     */
    public void setCrlLocation(String location) {
        if (location == null) {
            LOGGER.warn("CRL property in [{}] is not set. Certs will not be checked against a CRL.",
                    encryptionPropertiesLocation);
            crl = null;
            return;
        }

        crl = null;
        try {
            crl = createCRL(location);
        } catch (FileNotFoundException fnfe) {
            LOGGER.error("Could not find CRL file, cannot validate certificates to a CRL.", fnfe);
            crl = null;
        } catch (CertificateException ce) {
            LOGGER.error(
                    "Encountered an error while trying to create new certificate factory. Cannot validate certificates to a CRL.",
                    ce);
            crl = null;
        } catch (CRLException ce) {
            LOGGER.error(
                    "Could not create new CRL from the provided file. File may not be valid CRL DER-encoded file.",
                    ce);
            crl = null;
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
        try (FileInputStream fis = new FileInputStream(new File(location))) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return cf.generateCRL(fis);
        } catch (IOException e) {
            LOGGER.error("An error occurred while accessing {}", location, e);
            return null;
        }
    }

    /**
     * Abstracted for unit tests
     *
     * @param location location of properties file
     * @return Properties from
     */
    Properties loadProperties(String location) {
        return PropertiesLoader.loadProperties(location);
    }

}
