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
package org.codice.ddf.security.idp.binding.api;

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.codice.ddf.security.idp.server.Idp;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.SPSSODescriptor;

import ddf.security.samlp.SamlProtocol;
import ddf.security.samlp.SimpleSign;

public interface ResponseCreator {

    Response getSamlpResponse(String relayState, AuthnRequest authnRequest,
            org.opensaml.saml2.core.Response samlResponse, NewCookie cookie,
            String responseTemplate)
            throws IOException, SimpleSign.SignatureException, WSSecurityException;

    static String getAssertionConsumerServiceBinding(AuthnRequest authnRequest,
            Map<String, EntityDescriptor> serviceProviders) {
        EntityDescriptor entityDescriptor = serviceProviders
                .get(authnRequest.getIssuer().getValue());
        SPSSODescriptor spssoDescriptor = entityDescriptor
                .getSPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL);
        AssertionConsumerService defaultAssertionConsumerService = spssoDescriptor
                .getDefaultAssertionConsumerService();
        //see if the default service uses our supported bindings, and then use that
        //as we add more bindings, we'll need to update this
        if (defaultAssertionConsumerService.getBinding().equals(Idp.HTTP_POST_BINDING)
                || defaultAssertionConsumerService.getBinding().equals(Idp.HTTP_REDIRECT_BINDING)) {
            return defaultAssertionConsumerService.getBinding();
        } else {
            //if default doesn't work, check any others that are defined and use the first one that supports our bindings
            for (AssertionConsumerService assertionConsumerService : spssoDescriptor
                    .getAssertionConsumerServices()) {
                if (assertionConsumerService.getBinding().equals(Idp.HTTP_POST_BINDING)
                        || assertionConsumerService.getBinding()
                        .equals(Idp.HTTP_REDIRECT_BINDING)) {
                    return assertionConsumerService.getBinding();
                }
            }
        }
        return null;
    }
}
