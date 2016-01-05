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
package org.codice.ddf.security.idp.binding.api.impl;

import java.util.Map;

import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.codice.ddf.security.idp.binding.api.Validator;
import org.codice.ddf.security.idp.server.Idp;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;
import ddf.security.samlp.ValidationException;
import ddf.security.samlp.impl.EntityInformation;

public abstract class ValidatorImpl implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatorImpl.class);

    protected static final ImmutableSet<String> PKI_SAML_CONTEXTS =
            ImmutableSet.of(SAML2Constants.AUTH_CONTEXT_CLASS_REF_X509,
                    SAML2Constants.AUTH_CONTEXT_CLASS_REF_SMARTCARD_PKI,
                    SAML2Constants.AUTH_CONTEXT_CLASS_REF_SOFTWARE_PKI,
                    SAML2Constants.AUTH_CONTEXT_CLASS_REF_SPKI,
                    SAML2Constants.AUTH_CONTEXT_CLASS_REF_TLS_CLIENT);

    protected final SystemCrypto systemCrypto;

    protected SimpleSign simpleSign;

    private Map<String, EntityInformation> serviceProviders;

    public ValidatorImpl(SystemCrypto systemCrypto,
            Map<String, EntityInformation> serviceProviders) {
        this.systemCrypto = systemCrypto;
        this.serviceProviders = serviceProviders;
        this.simpleSign = new SimpleSign(systemCrypto);
    }

    @Override
    public void validateAuthnRequest(AuthnRequest authnRequest, String samlRequest,
            String relayState, String signatureAlgorithm, String signature, boolean strictSignature)
            throws SimpleSign.SignatureException, ValidationException {
        if (strictSignature && authnRequest.getAssertionConsumerServiceURL() != null && (
                authnRequest.getSignature() == null && signature == null)) {
            throw new IllegalArgumentException(
                    "Invalid AuthnRequest, defined an AssertionConsumerServiceURL, but contained no identifying signature.");
        }

        if (authnRequest.getRequestedAuthnContext() != null) {
            if (authnRequest.isPassive() && authnRequest.getRequestedAuthnContext()
                    .getComparison()
                    .equals(AuthnContextComparisonTypeEnumeration.EXACT)
                    && authnRequest.getRequestedAuthnContext()
                    .getAuthnContextClassRefs()
                    .stream()
                    .map(AuthnContextClassRef::getAuthnContextClassRef)
                    .anyMatch(PKI_SAML_CONTEXTS::contains)) {
                throw new IllegalArgumentException(
                        "Unable to passively log user in when not specifying PKI AuthnContextClassRef");
            }
        }

        if (authnRequest.getProtocolBinding() != null && !(authnRequest.getProtocolBinding()
                .equals(Idp.HTTP_POST_BINDING) || authnRequest.getProtocolBinding()
                .equals(Idp.HTTP_REDIRECT_BINDING))) {
            throw new UnsupportedOperationException(
                    "Only HTTP-POST and HTTP-Redirect bindings are supported");
        }
    }

    @Override
    public void validateRelayState(String relayState) {
        LOGGER.debug("Validating RelayState");
        if (relayState == null || relayState.length() < 0 || relayState.length() > 80) {
            LOGGER.warn("RelayState has invalid size: {}",
                    (relayState == null) ? "no RelayState" : relayState.length());
        }
    }

    protected SystemCrypto getSystemCrypto() {
        return systemCrypto;
    }

    public SimpleSign getSimpleSign() {
        return simpleSign;
    }

    protected Map<String, EntityInformation> getServiceProviders() {
        return serviceProviders;
    }
}
