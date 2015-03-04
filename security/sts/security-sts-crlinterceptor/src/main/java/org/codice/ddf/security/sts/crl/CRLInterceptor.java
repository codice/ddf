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
package org.codice.ddf.security.sts.crl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.common.audit.SecurityLogger;

/**
 * Interceptor that checks an incoming message against a defined certification
 * revocation list (CRL).
 * 
 */
public class CRLInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CRLInterceptor.class);

    private boolean isEnabled = false;

    private CRL crl;

    /**
     * Creates a new crl interceptor.
     * 
     */
    public CRLInterceptor() {
        super(Phase.PRE_PROTOCOL);
        getAfter().add(SAAJInInterceptor.class.getName());
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        HttpServletRequest request = (HttpServletRequest) message
                .get(AbstractHTTPDestination.HTTP_REQUEST);
        X509Certificate[] certs = (X509Certificate[]) request
                .getAttribute("javax.servlet.request.X509Certificate");
        if (certs != null && isEnabled) {
            if (crl != null) {
                LOGGER.debug("Got {} certificate(s) in the incoming request.", certs.length);
                for (X509Certificate curCert : certs) {
                    if (crl.isRevoked(curCert)) {
                        SecurityLogger.logInfo("Denying access for user " + curCert.getSubjectDN()
                                + " due to certificate being revoked by CRL.");
                        LOGGER.warn(
                                "Denying access for user {} due to certificate being revoked by CRL.",
                                curCert.getSubjectDN());
                        throw new AccessDeniedException(
                                "Cannot complete request, certificate was revoked by CRL.");
                    } else {
                        LOGGER.debug("User {} was not revoked by CRL.", curCert.getSubjectDN());
                    }
                }
            } else {
                String errorMsg = "Denying access to all users as no crl file is available. Either fix the file location or disable CRL checking to allow access to users.";
                SecurityLogger.logInfo(errorMsg);
                LOGGER.warn(errorMsg);
                throw new AccessDeniedException(errorMsg);
            }
        } else {
            LOGGER.debug("Allowing message through. CRL checking has been disabled or there were no certificates sent by the client.");
        }

    }

    /**
     * Sets the location of the CRL.
     * 
     * @param location
     *            Location of the DER-encoded CRL file that should be used to
     *            check certificate revocation.
     */
    public synchronized void setCrlLocation(String location) {
        try {
            crl = createCRL(location);
        } catch (FileNotFoundException fnfe) {
            crl = null;
            LOGGER.warn("Could not find CRL file, cannot validate certificates to a CRL.", fnfe);
        } catch (CertificateException ce) {
            crl = null;
            LOGGER.warn(
                    "Encountered an error while trying to create new certificate factory. Cannot validate certificates to a CRL.",
                    ce);
        } catch (CRLException ce) {
            crl = null;
            LOGGER.warn(
                    "Could not create new CRL from the provided file. File may not be valid CRL DER-encoded file.",
                    ce);
        }
    }

    /**
     * Sets the isEnabled flag for the CRL checker which determines if the
     * interceptor should check the incoming request to the specified CRL.
     * 
     * @param isEnabled
     *            boolean value that either turns on crl checking (true) or
     *            turns off checking (false).
     */
    public void setIsEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    /**
     * Generates a new CRL object from the given location.
     * 
     * @param location
     *            File location of the CRL file.
     * @return new CRL object
     * @throws FileNotFoundException
     *             If no file is located at the location
     * @throws CertificateException
     *             If the Certificate factory cannot be located
     * @throws CRLException
     *             If the input CRL file is invalid and cannot be used to
     *             generate a crl object.
     */
    private CRL createCRL(String location) throws FileNotFoundException, CertificateException,
            CRLException {
        FileInputStream fis = new FileInputStream(new File(location));
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return cf.generateCRL(fis);
    }

}
