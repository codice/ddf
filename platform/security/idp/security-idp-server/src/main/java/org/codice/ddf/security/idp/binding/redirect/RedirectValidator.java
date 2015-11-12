/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.idp.binding.redirect;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.security.idp.binding.api.Validator;
import org.codice.ddf.security.idp.binding.api.impl.ValidatorImpl;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml2.metadata.SPSSODescriptor;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.samlp.SamlProtocol;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;

public class RedirectValidator extends ValidatorImpl implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedirectValidator.class);

    public RedirectValidator(SystemCrypto systemCrypto,
            Map<String, EntityDescriptor> serviceProviders) {
        super(systemCrypto, serviceProviders);
    }

    @Override
    public void validateAuthnRequest(AuthnRequest authnRequest, String samlRequest,
            String relayState, String signatureAlgorithm, String signature, boolean strictSignature)
            throws SimpleSign.SignatureException, ValidationException {
        LOGGER.debug("Validating AuthnRequest required attributes and signature");
        if (strictSignature) {
            if (!StringUtils.isEmpty(signature) && !StringUtils.isEmpty(signatureAlgorithm)) {
                String signedParts;
                try {
                    signedParts = String.format("SAMLRequest=%s&RelayState=%s&SigAlg=%s",
                            URLEncoder.encode(samlRequest, "UTF-8"), relayState,
                            URLEncoder.encode(signatureAlgorithm, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new SimpleSign.SignatureException(
                            "Unable to construct signed query parts.", e);
                }
                EntityDescriptor entityDescriptor = getServiceProviders()
                        .get(authnRequest.getIssuer().getValue());
                SPSSODescriptor spssoDescriptor = entityDescriptor
                        .getSPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL);
                String encryptionCertificate = null;
                String signingCertificate = null;
                if (spssoDescriptor != null) {
                    for (KeyDescriptor key : spssoDescriptor.getKeyDescriptors()) {
                        String certificate = null;
                        if (key.getKeyInfo().getX509Datas().size() > 0 &&
                                key.getKeyInfo().getX509Datas().get(0).getX509Certificates().size()
                                        > 0) {
                            certificate = key.getKeyInfo().getX509Datas().get(0)
                                    .getX509Certificates().get(0).getValue();
                        }
                        if (StringUtils.isBlank(certificate)) {
                            break;
                        }

                        if (UsageType.UNSPECIFIED.equals(key.getUse())) {
                            encryptionCertificate = certificate;
                            signingCertificate = certificate;
                        }

                        if (UsageType.ENCRYPTION.equals(key.getUse())) {
                            encryptionCertificate = certificate;
                        }

                        if (UsageType.SIGNING.equals(key.getUse())) {
                            signingCertificate = certificate;
                        }
                    }
                    if (signingCertificate == null) {
                        throw new ValidationException(
                                "Unable to find signing certificate in metadata. Please check metadata.");
                    }
                } else {
                    throw new ValidationException(
                            "Unable to find supported protocol in metadata SPSSODescriptors.");
                }
                boolean result = getSimpleSign()
                        .validateSignature(signedParts, signature, signingCertificate);
                if (!result) {
                    throw new ValidationException(
                            "Signature verification failed for redirect binding.");
                }
            } else {
                throw new SimpleSign.SignatureException("No signature present for AuthnRequest.");
            }
        }

        super.validateAuthnRequest(authnRequest, samlRequest, relayState, signatureAlgorithm,
                signature, strictSignature);
    }

    @Override
    public void validateRelayState(String relayState) {
        if (relayState == null) {
            throw new IllegalArgumentException("Missing RelayState on IdP request.");
        }
        try {
            relayState = URLDecoder.decode(relayState, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.warn("Unable to URL decode relay state, it may already be decoded.", e);
        }

        super.validateRelayState(relayState);
    }
}
