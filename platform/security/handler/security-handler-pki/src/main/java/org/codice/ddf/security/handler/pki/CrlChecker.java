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
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.CRL;
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
    public CrlChecker() {
        Properties encryptionProperties = loadProperties(encryptionPropertiesLocation);
        setCrlLocation(encryptionProperties.getProperty(CRL_PROPERTY_KEY));
    }

    public HandlerResult check(HttpServletResponse httpResponse, BaseAuthenticationToken token,
            X509Certificate[] certs, HandlerResult handlerResult) {
        if (crl == null) {
            String errorMsg = "CRL is not set. Skipping CRL check";
            LOGGER.trace(errorMsg);
            SecurityLogger.logInfo(errorMsg);
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

    private HandlerResult checkAgainstCrl(HttpServletResponse httpResponse,
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

    /**
     * Checks if the provided cert is listed in the CRL.
     *
     * @param certs
     * @return boolean value
     */
    private boolean passesCRL(X509Certificate[] certs) {
        if (certs != null) {
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
    protected void setCrlLocation(String location) {
        if (location == null) {
            LOGGER.warn("CRL property in [{}] is not set. Certs will not be checked against a CRL.",
                    encryptionPropertiesLocation);
            crl = null;
        } else {
            crl = createCRL(location);
        }
    }

    /**
     * Generates a new CRL object from the given location.
     *
     * @param location File location of the CRL file.
     * @return new CRL object
     */
    private CRL createCRL(String location) {
        try (FileInputStream fis = new FileInputStream(new File(location))) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return cf.generateCRL(fis);
        } catch (IOException e) {
            LOGGER.error("An error occurred while accessing {}", location, e);
            return null;
        } catch (GeneralSecurityException e) {
            LOGGER.error("Encountered an error while generating crl from file.", e);
            return null;
        }
    }

    /**
     * Abstracted for unit tests to allow test class to set a specific property location
     *
     * @param location location of properties file
     * @return Properties from
     */
    protected Properties loadProperties(String location) {
        return PropertiesLoader.loadProperties(location);
    }

}
