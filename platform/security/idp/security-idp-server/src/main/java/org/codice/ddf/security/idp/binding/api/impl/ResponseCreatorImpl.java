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

import org.codice.ddf.security.idp.binding.api.ResponseCreator;
import org.opensaml.saml2.core.AuthnRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;
import ddf.security.samlp.impl.EntityInformation;

public abstract class ResponseCreatorImpl implements ResponseCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseCreatorImpl.class);

    private final Map<String, EntityInformation> serviceProviders;

    protected final SystemCrypto systemCrypto;

    protected SimpleSign simpleSign;

    public ResponseCreatorImpl(SystemCrypto systemCrypto,
            Map<String, EntityInformation> serviceProviders) {
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
            assertionConsumerServiceURL = serviceProviders.get(authnRequest.getIssuer()
                    .getValue())
                    .getAssertionConsumerServiceURL();
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

    protected Map<String, EntityInformation> getServiceProviders() {
        return serviceProviders;
    }
}
