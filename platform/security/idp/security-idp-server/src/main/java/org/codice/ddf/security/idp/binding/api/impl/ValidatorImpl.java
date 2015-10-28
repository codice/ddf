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
package org.codice.ddf.security.idp.binding.api.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.codice.ddf.security.idp.binding.api.Validator;
import org.codice.ddf.security.idp.server.Idp;
import org.opensaml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.xml.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;

public abstract class ValidatorImpl implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatorImpl.class);

    protected final SystemCrypto systemCrypto;

    protected SimpleSign simpleSign;

    private Map<String, EntityDescriptor> serviceProviders;

    public ValidatorImpl(SystemCrypto systemCrypto, Map<String, EntityDescriptor> serviceProviders) {
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
            Collection authNContextClasses = CollectionUtils.transformedCollection(
                    authnRequest.getRequestedAuthnContext().getAuthnContextClassRefs(),
                    new AuthnContextClassTransformer());
            if (authnRequest.isPassive() && authnRequest.getRequestedAuthnContext().getComparison()
                    .equals(AuthnContextComparisonTypeEnumeration.EXACT) && !CollectionUtils
                    .containsAny(authNContextClasses,
                            Arrays.asList(SAML2Constants.AUTH_CONTEXT_CLASS_REF_X509,
                                    SAML2Constants.AUTH_CONTEXT_CLASS_REF_SMARTCARD_PKI,
                                    SAML2Constants.AUTH_CONTEXT_CLASS_REF_SOFTWARE_PKI,
                                    SAML2Constants.AUTH_CONTEXT_CLASS_REF_SPKI,
                                    SAML2Constants.AUTH_CONTEXT_CLASS_REF_TLS_CLIENT))) {
                throw new IllegalArgumentException(
                        "Unable to passively log user in when not specifying PKI AuthnContextClassRef");
            }
        }

        if (!(authnRequest.getProtocolBinding().equals(Idp.HTTP_POST_BINDING) || authnRequest
                .getProtocolBinding().equals(Idp.HTTP_REDIRECT_BINDING))) {
            throw new UnsupportedOperationException(
                    "Only HTTP-POST and HTTP-Redirect bindings are supported");
        }
    }

    @Override
    public void validateRelayState(String relayState) {
        LOGGER.debug("Validating RelayState");
        if (relayState == null) {
            throw new IllegalArgumentException("Missing RelayState on IdP request.");
        }
        if (relayState.getBytes().length < 0 || relayState.getBytes().length > 80) {
            LOGGER.warn("RelayState has invalid size: {}", relayState.getBytes().length);
        }
    }

    public static class AuthnContextClassTransformer implements Transformer {

        @Override
        public Object transform(Object o) {
            if (o instanceof AuthnContextClassRef) {
                return ((AuthnContextClassRef) o).getAuthnContextClassRef();
            }
            return o;
        }
    }

    protected SystemCrypto getSystemCrypto() {
        return systemCrypto;
    }

    public SimpleSign getSimpleSign() {
        return simpleSign;
    }

    protected Map<String, EntityDescriptor> getServiceProviders() {
        return serviceProviders;
    }
}
