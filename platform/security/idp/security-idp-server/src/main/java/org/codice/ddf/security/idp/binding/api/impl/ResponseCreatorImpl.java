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

import java.util.Map;

import org.codice.ddf.security.idp.binding.api.ResponseCreator;
import org.codice.ddf.security.idp.server.Idp;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.SPSSODescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.samlp.SamlProtocol;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;

public abstract class ResponseCreatorImpl implements ResponseCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseCreatorImpl.class);

    private final Map<String, EntityDescriptor> serviceProviders;

    protected final SystemCrypto systemCrypto;

    protected SimpleSign simpleSign;

    public ResponseCreatorImpl(SystemCrypto systemCrypto,
            Map<String, EntityDescriptor> serviceProviders) {
        this.systemCrypto = systemCrypto;
        this.serviceProviders = serviceProviders;
        this.simpleSign = new SimpleSign(systemCrypto);
    }

    public String getAssertionConsumerServiceURL(AuthnRequest authnRequest) {
        String assertionConsumerServiceURL = null;
        LOGGER.debug("Attempting to determine AssertionConsumerServiceURL.");
        //if the AuthnRequest specifies a URL, use that
        if (authnRequest.getAssertionConsumerServiceURL() != null) {
            LOGGER.debug("Using AssertionConsumerServiceURL from AuthnRequest: {}",
                    authnRequest.getAssertionConsumerServiceURL());
            assertionConsumerServiceURL = authnRequest.getAssertionConsumerServiceURL();
        } else {
            //check metadata
            EntityDescriptor entityDescriptor = serviceProviders
                    .get(authnRequest.getIssuer().getValue());
            SPSSODescriptor spssoDescriptor = entityDescriptor
                    .getSPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL);
            AssertionConsumerService defaultAssertionConsumerService = spssoDescriptor
                    .getDefaultAssertionConsumerService();
            //see if the default service uses our supported bindings, and then use that
            //as we add more bindings, we'll need to update this
            if (defaultAssertionConsumerService.getBinding().equals(Idp.HTTP_POST_BINDING)
                    || defaultAssertionConsumerService.getBinding()
                    .equals(Idp.HTTP_REDIRECT_BINDING)) {
                LOGGER.debug(
                        "Using AssertionConsumerServiceURL from default assertion consumer service: {}",
                        defaultAssertionConsumerService.getLocation());
                assertionConsumerServiceURL = defaultAssertionConsumerService.getLocation();
            } else {
                //if default doesn't work, check any others that are defined and use the first one that supports our bindings
                for (AssertionConsumerService assertionConsumerService : spssoDescriptor
                        .getAssertionConsumerServices()) {
                    if (assertionConsumerService.getBinding().equals(Idp.HTTP_POST_BINDING)
                            || assertionConsumerService.getBinding()
                            .equals(Idp.HTTP_REDIRECT_BINDING)) {
                        LOGGER.debug("Using AssertionConsumerServiceURL from supported binding: {}",
                                assertionConsumerService.getLocation());
                        assertionConsumerServiceURL = assertionConsumerService.getLocation();
                        break;
                    }
                }
            }

        }
        if (assertionConsumerServiceURL == null) {
            throw new IllegalArgumentException(
                    "No valid AssertionConsumerServiceURL available for given AuthnRequest.");
        }
        return assertionConsumerServiceURL;
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
