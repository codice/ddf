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

import org.apache.commons.lang.StringUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.codice.ddf.security.idp.server.Idp;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.SPSSODescriptor;

import ddf.security.samlp.SamlProtocol;
import ddf.security.samlp.SimpleSign;

/**
 * Creates a SAML 2 Web SSO Response.
 */
public interface ResponseCreator {

    /**
     * Returns the JAXRS Response appropriate for the given arguments.
     *
     * @param relayState       - encoded relay state
     * @param authnRequest     - unencoded authnRequest
     * @param samlResponse     - initialized SAML 2 Response object
     * @param cookie           - cookie associated with the login
     * @param responseTemplate - response template to be used when generating the response
     * @return javax.ws.rs.core.Response
     * @throws IOException
     * @throws SimpleSign.SignatureException
     * @throws WSSecurityException
     */
    Response getSamlpResponse(String relayState, AuthnRequest authnRequest,
            org.opensaml.saml2.core.Response samlResponse, NewCookie cookie,
            String responseTemplate)
            throws IOException, SimpleSign.SignatureException, WSSecurityException;

    /**
     * Returns the assertion consumer service binding that is appropriate by parsing the
     * AuthnRequest as well as the metadata for the given SP
     *
     * @param authnRequest
     * @param serviceProviders
     * @return String
     */
    static String getAssertionConsumerServiceBinding(AuthnRequest authnRequest,
            Map<String, EntityDescriptor> serviceProviders) {
        if (StringUtils.isNotBlank(authnRequest.getProtocolBinding())) {
            return authnRequest.getProtocolBinding();
        }
        EntityDescriptor entityDescriptor = serviceProviders.get(authnRequest.getIssuer()
                .getValue());
        SPSSODescriptor spssoDescriptor = entityDescriptor.getSPSSODescriptor(
                SamlProtocol.SUPPORTED_PROTOCOL);
        AssertionConsumerService defaultAssertionConsumerService = spssoDescriptor.getDefaultAssertionConsumerService();
        //see if the default service uses our supported bindings, and then use that
        //as we add more bindings, we'll need to update this
        if (defaultAssertionConsumerService.getBinding()
                .equals(Idp.HTTP_POST_BINDING) || defaultAssertionConsumerService.getBinding()
                .equals(Idp.HTTP_REDIRECT_BINDING)) {
            return defaultAssertionConsumerService.getBinding();
        } else {
            //if default doesn't work, check any others that are defined and use the first one that supports our bindings
            for (AssertionConsumerService assertionConsumerService : spssoDescriptor.getAssertionConsumerServices()) {
                if (assertionConsumerService.getBinding()
                        .equals(Idp.HTTP_POST_BINDING) || assertionConsumerService.getBinding()
                        .equals(Idp.HTTP_REDIRECT_BINDING)) {
                    return assertionConsumerService.getBinding();
                }
            }
        }
        return null;
    }
}
