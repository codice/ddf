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
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package ddf.security.sts;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.x500.X500Principal;

import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.ReceivedKey;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.token.provider.DefaultSubjectProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.UsernameTokenPrincipal;
import org.apache.wss4j.common.saml.bean.KeyInfoBean;
import org.apache.wss4j.common.saml.bean.SubjectBean;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import ddf.security.SecurityConstants;

/**
 * Custom SubjectProvider that sets the NameIDFormat
 * based on the type of Principal.
 */
public class SAMLSubjectProvider extends DefaultSubjectProvider {

    /**
     * Log4j Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConstants.SECURITY_LOGGER);

    @Override
    /**
     * Get a SubjectBean object.
     */ public SubjectBean getSubject(TokenProviderParameters providerParameters, Document doc,
            byte[] secret) {
        String subjectNameQualifier = "http://cxf.apache.org/sts";
        String subjectNameIDFormat = SAML2Constants.NAMEID_FORMAT_UNSPECIFIED;

        TokenRequirements tokenRequirements = providerParameters.getTokenRequirements();
        KeyRequirements keyRequirements = providerParameters.getKeyRequirements();
        STSPropertiesMBean stsProperties = providerParameters.getStsProperties();

        String tokenType = tokenRequirements.getTokenType();
        String keyType = keyRequirements.getKeyType();
        String confirmationMethod = getSubjectConfirmationMethod(tokenType, keyType);

        Principal principal = null;
        ReceivedToken receivedToken;
        //TokenValidator in IssueOperation has validated the ReceivedToken
        //if validation was successful, the principal was set in ReceivedToken
        if (providerParameters.getTokenRequirements().getOnBehalfOf() != null) {
            receivedToken = providerParameters.getTokenRequirements().getOnBehalfOf();
            if (receivedToken.getState().equals(ReceivedToken.STATE.VALID)) {
                principal = receivedToken.getPrincipal();
            }
        } else if (providerParameters.getTokenRequirements().getActAs() != null) {
            receivedToken = providerParameters.getTokenRequirements().getActAs();
            if (receivedToken.getState().equals(ReceivedToken.STATE.VALID)) {
                principal = receivedToken.getPrincipal();
            }
        } else if (providerParameters.getTokenRequirements().getValidateTarget() != null) {
            receivedToken = providerParameters.getTokenRequirements().getValidateTarget();
            if (receivedToken.getState().equals(ReceivedToken.STATE.VALID)) {
                principal = receivedToken.getPrincipal();
            }
        } else {
            principal = providerParameters.getPrincipal();
        }

        if (principal == null) {
            LOGGER.error("Error in getting principal");
            throw new STSException("Error in getting principal", STSException.REQUEST_FAILED);
        } else {
            // Set NameIDFormat correctly based on type of principal unless it was already set to some value
            if (principal instanceof UsernameTokenPrincipal) {
                subjectNameIDFormat = SAML2Constants.NAMEID_FORMAT_PERSISTENT;
            } else if (principal instanceof X500Principal) {
                subjectNameIDFormat = SAML2Constants.NAMEID_FORMAT_X509_SUBJECT_NAME;
            } else if (principal instanceof KerberosPrincipal) {
                subjectNameIDFormat = SAML2Constants.NAMEID_FORMAT_KERBEROS;
            }
        }

        SubjectBean subjectBean = new SubjectBean(principal.getName(), subjectNameQualifier,
                confirmationMethod);
        LOGGER.debug("Creating new subject with principal name: " + principal.getName());
        if (subjectNameIDFormat != null && subjectNameIDFormat.length() > 0) {
            subjectBean.setSubjectNameIDFormat(subjectNameIDFormat);
        }

        if (STSConstants.SYMMETRIC_KEY_KEYTYPE.equals(keyType)) {
            Crypto crypto = stsProperties.getEncryptionCrypto();

            EncryptionProperties encryptionProperties = providerParameters
                    .getEncryptionProperties();
            String encryptionName = encryptionProperties.getEncryptionName();
            if (encryptionName == null) {
                // Fall back on the STS encryption name
                encryptionName = stsProperties.getEncryptionUsername();
            }
            if (encryptionName == null) {
                LOGGER.error("No encryption Name is configured for Symmetric KeyType");
                throw new STSException("No Encryption Name is configured",
                        STSException.REQUEST_FAILED);
            }

            CryptoType cryptoType = null;

            // Check for using of service endpoint (AppliesTo) as certificate identifier
            if (STSConstants.USE_ENDPOINT_AS_CERT_ALIAS.equals(encryptionName)) {
                if (providerParameters.getAppliesToAddress() == null) {
                    throw new STSException("AppliesTo is not initilaized for encryption name "
                            + STSConstants.USE_ENDPOINT_AS_CERT_ALIAS);
                }
                cryptoType = new CryptoType(CryptoType.TYPE.ENDPOINT);
                cryptoType.setEndpoint(providerParameters.getAppliesToAddress());
            } else {
                cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
                cryptoType.setAlias(encryptionName);
            }

            try {
                X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
                if ((certs == null) || (certs.length == 0)) {
                    throw new STSException(
                            "Encryption certificate is not found for alias: " + encryptionName);
                }
                KeyInfoBean keyInfo = createKeyInfo(certs[0], secret, doc, encryptionProperties,
                        crypto);
                subjectBean.setKeyInfo(keyInfo);
            } catch (WSSecurityException ex) {
                LOGGER.error("", ex);
                throw new STSException(ex.getMessage(), ex);
            }
        } else if (STSConstants.PUBLIC_KEY_KEYTYPE.equals(keyType)) {
            ReceivedKey receivedKey = keyRequirements.getReceivedKey();

            // Validate UseKey trust
            if (stsProperties.isValidateUseKey() && stsProperties.getSignatureCrypto() != null) {
                if (receivedKey.getX509Cert() != null) {
                    try {
                        Collection<Pattern> constraints = Collections.emptyList();
                        stsProperties.getSignatureCrypto()
                                .verifyTrust(new X509Certificate[] {receivedKey.getX509Cert()},
                                        false, constraints);
                    } catch (WSSecurityException e) {
                        LOGGER.error("Error in trust validation of UseKey: ", e);
                        throw new STSException("Error in trust validation of UseKey",
                                STSException.REQUEST_FAILED);
                    }
                }
                if (receivedKey.getPublicKey() != null) {
                    try {
                        stsProperties.getSignatureCrypto().verifyTrust(receivedKey.getPublicKey());
                    } catch (WSSecurityException e) {
                        LOGGER.error("Error in trust validation of UseKey: ", e);
                        throw new STSException("Error in trust validation of UseKey",
                                STSException.REQUEST_FAILED);
                    }
                }
            }

            KeyInfoBean keyInfo = createKeyInfo(receivedKey.getX509Cert(),
                    receivedKey.getPublicKey());
            subjectBean.setKeyInfo(keyInfo);
        }

        return subjectBean;
    }
}
