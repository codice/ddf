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
package org.codice.ddf.security.handler.pki;

import ddf.security.common.util.PropertiesLoader;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.CredentialException;
import org.apache.ws.security.components.crypto.Merlin;
import org.apache.ws.security.util.Base64;
import org.codice.ddf.security.handler.api.AuthenticationHandler;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.security.cert.X509Certificate;

/**
 * Handler for PKI based authentication. X509 chain will be extracted from the HTTP request and
 * converted to a BinarySecurityToken.
 */
public class PKIHandler implements AuthenticationHandler {

    /**
     * PKI type to use when configuring context policy.
     */
    public static final String AUTH_TYPE = "PKI";

    private static final transient Logger LOGGER = LoggerFactory
            .getLogger(PKIHandler.class);

    private static final JAXBContext btContext = initContext();

    private Merlin merlin;

    private String signaturePropertiesPath;

    private static JAXBContext initContext() {
        try {
            return JAXBContext.newInstance(BinarySecurityTokenType.class);
        } catch (JAXBException e) {
            LOGGER.error("Unable to create BinarySecurityToken JAXB context.", e);
        }
        return null;
    }

    /**
     * Initializes Merlin crypto object.
     */
    public void init() {
        try {
            merlin = new Merlin(PropertiesLoader.loadProperties(signaturePropertiesPath));
        } catch (CredentialException e) {
            LOGGER.error("Unable to read merlin properties file for crypto operations.", e);
        } catch (IOException e) {
            LOGGER.error("Unable to read merlin properties file.", e);
        }
    }

    @Override
    public String getAuthenticationType() {
        return AUTH_TYPE;
    }

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
        HandlerResult handlerResult;
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute(
                "javax.servlet.request.X509Certificate");
        //doesn't matter what the resolve flag is set to, we do the same action
        if (certs != null && certs.length > 0) {
            byte[] certBytes = null;
            try {
                certBytes = getCertBytes(certs);
            } catch (WSSecurityException e) {
                LOGGER.error("Unable to convert PKI certs to byte array.", e);
            }
            if (certBytes != null) {
                String bst = getBinarySecurityToken(Base64.encode(certBytes));
                handlerResult = new HandlerResult(HandlerResult.Status.COMPLETED,
                        certs[0].getSubjectDN(), bst);
                return handlerResult;
            } else {
                handlerResult = new HandlerResult(HandlerResult.Status.NO_ACTION, null, "");
                return handlerResult;
            }
        } else {
            handlerResult = new HandlerResult(HandlerResult.Status.NO_ACTION, null, "");
            return handlerResult;
        }
    }

    @Override
    public HandlerResult handleError(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain chain) throws ServletException {
        HandlerResult result = new HandlerResult();
        LOGGER.debug("In error handler for pki - no action taken.");
        result.setStatus(HandlerResult.Status.NO_ACTION);
        return result;
    }

    /**
     * Returns a byte array representing a certificate chain.
     *
     * @param certs
     * @return byte[]
     * @throws WSSecurityException
     */
    private byte[] getCertBytes(X509Certificate[] certs) throws WSSecurityException {
        byte[] certBytes = null;

        if (merlin != null) {
            certBytes = merlin.getBytesFromCertificates(certs);
        }

        return certBytes;
    }

    /**
     * Creates a binary security token based on the provided credential.
     */
    private synchronized String getBinarySecurityToken(String credential) {
        Writer writer = new StringWriter();

        Marshaller marshaller = null;

        BinarySecurityTokenType binarySecurityTokenType = new BinarySecurityTokenType();
        binarySecurityTokenType.setValueType(WSConstants.X509TOKEN_NS + "#X509PKIPathv1");
        binarySecurityTokenType.setEncodingType(WSConstants.SOAPMESSAGE_NS + "#Base64Binary");
        binarySecurityTokenType.setId(WSConstants.X509_CERT_LN);
        binarySecurityTokenType.setValue(Base64.encode(credential.getBytes()));
        JAXBElement<BinarySecurityTokenType> binarySecurityTokenElement = new JAXBElement<BinarySecurityTokenType>(
                new QName(
                        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                        "BinarySecurityToken"), BinarySecurityTokenType.class,
                binarySecurityTokenType
        );

        if (btContext != null) {
            try {
                marshaller = btContext.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            } catch (JAXBException e) {
                LOGGER.error("Exception while creating UsernameToken marshaller.", e);
            }

            if (marshaller != null) {
                try {
                    marshaller.marshal(binarySecurityTokenElement, writer);
                } catch (JAXBException e) {
                    LOGGER.error("Exception while writing username token.", e);
                }
            }
        }

        String binarySecurityToken = writer.toString();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Binary Security Token: " + binarySecurityToken);
        }

        return binarySecurityToken;
    }

    public String getSignaturePropertiesPath() {
        return signaturePropertiesPath;
    }

    public void setSignaturePropertiesPath(String signaturePropertiesPath) {
        this.signaturePropertiesPath = signaturePropertiesPath;
    }
}
