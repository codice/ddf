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
package org.codice.ddf.security.idp.binding.post;

import java.util.Map;

import org.codice.ddf.security.idp.binding.api.Validator;
import org.codice.ddf.security.idp.binding.api.impl.ValidatorImpl;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.xml.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;
import ddf.security.samlp.impl.EntityInformation;

public class PostValidator extends ValidatorImpl implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostValidator.class);

    public PostValidator(SystemCrypto systemCrypto,
            Map<String, EntityInformation> serviceProviders) {
        super(systemCrypto, serviceProviders);
    }

    @Override
    public void validateAuthnRequest(AuthnRequest authnRequest, String samlRequest,
            String relayState, String signatureAlgorithm, String signature, boolean strictSignature)
            throws SimpleSign.SignatureException, ValidationException {
        LOGGER.debug("Validating AuthnRequest required attributes and signature");
        if (strictSignature) {
            if (authnRequest.getSignature() != null) {
                getSimpleSign().validateSignature(authnRequest.getSignature(),
                        authnRequest.getDOM()
                                .getOwnerDocument());
            } else {
                throw new SimpleSign.SignatureException("No signature present on AuthnRequest.");
            }
        }
        super.validateAuthnRequest(authnRequest,
                samlRequest,
                relayState,
                signatureAlgorithm,
                signature,
                strictSignature);
    }
}
