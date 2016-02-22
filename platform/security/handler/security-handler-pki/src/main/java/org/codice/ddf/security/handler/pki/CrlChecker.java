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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.cert.CRL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.wss4j.common.crypto.Merlin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.PropertiesLoader;
import ddf.security.common.audit.SecurityLogger;

public class CrlChecker {

    public static final String CRL_PROPERTY_KEY = Merlin.OLD_PREFIX + Merlin.X509_CRL_FILE;
    
    public static final Path ENCRYPTION_PROPERTIES_PATH = Paths.get("etc", "ws-security", "server", "encryption.properties");

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPKIHandler.class);

    private final Path crlPath;

    private CRL crl = null;

    /**
     * Constructor method. Reads encryption.properties and creates CRL.
     */
    public CrlChecker() {
        crlPath = getCrlPathFromPropertiesFile();
        if (crlPath != null) {
            crl = createCrl(crlPath.toString());
        } else {
            String errorMsg = String.format(
                    "CRL property in %s is not set. Certs will not be checked against a CRL",
                    ENCRYPTION_PROPERTIES_PATH.toString());
            SecurityLogger.logTrace(errorMsg);
            LOGGER.warn(errorMsg);
        }
    }

    /**
     * Checks the given certs against the CRL. The CRL is configured in this class's constructor method.
     *
     * @param certs certificates to be checked against the CRL
     * @return true if one of the certs passes the CRL or if CRL revocation is disabled, false if they are all revoked.
     */
    public boolean passesCrlCheck(X509Certificate[] certs) {
        if (!isCrlEnabled()) {
            String errorMsg = "CRL is not set. Skipping CRL check";
            LOGGER.trace(errorMsg);
            SecurityLogger.logTrace(errorMsg);
            return true;
        }
        LOGGER.trace("Checking request certs against CRL.");
        return passesCrl(certs);
    }
    
    /**
     * Determines whether or not the CRL is enabled. If the CRL is uncommented out in encryption.properties,
     * it is enabled; otherwise, it is disabled.
     * 
     * @return Returns true if the CRL is uncommented out, false otherwise
     */
    public boolean isCrlEnabled() {
        return crl != null;
    }
    
    /**
     * Gets the CRL path. If the CRL is disabled, it will be null.
     * 
     * @return the CRL path
     */
    public Path getCrlPath() {
        return crlPath;
    }

    /**
     * Checks if the provided cert is listed in the CRL.
     *
     * @param certs List of certs to be checked against the CRL
     * @return boolean value Whether or not the client presenting the certs should be let through
     */
    private boolean passesCrl(X509Certificate[] certs) {
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
     * Generates a new CRL object from the given location.
     *
     * @param location Path to the CRL file
     * @return A CRL object constructed from the given file path. Null if an error occurred while attempting to read the file.
     */
    private CRL createCrl(String location) {
        try (FileInputStream fis = new FileInputStream(new File(location))) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return cf.generateCRL(fis);
        } catch (IOException e) {
            LOGGER.error("An error occurred while accessing {}", location, e);
            return null;
        } catch (GeneralSecurityException e) {
            LOGGER.error("Encountered an error while generating CRL from file {}", location, e);
            return null;
        }
    }
    
    /**
     * Loads the properties from a given location.
     *
     * @param location location of properties file
     * @return Properties from the file
     */
    private Properties loadProperties(String location) {
        return PropertiesLoader.loadProperties(location);
    }
    
    /**
     * Gets the path to the CRL from the encryption.properties file.
     * 
     * @return path to the CRL if CRL property is found (enabled CRL), null otherwise (disabled CRL).
     */
    Path getCrlPathFromPropertiesFile() {
        Properties encryptionProperties = loadProperties(ENCRYPTION_PROPERTIES_PATH.toString());
        String path = encryptionProperties.getProperty(CRL_PROPERTY_KEY);
        if(StringUtils.isNotBlank(path)) {
            return Paths.get(path);
        } else {
            return null;
        }
    }
}
