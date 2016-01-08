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
package org.codice.ddf.security.idp.binding.api;

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.opensaml.saml.saml2.core.AuthnRequest;

import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.impl.EntityInformation;

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
            org.opensaml.saml.saml2.core.Response samlResponse, NewCookie cookie,
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
            Map<String, EntityInformation> serviceProviders) {
        EntityInformation.ServiceInfo assertionConsumerService =
                serviceProviders.get(authnRequest.getIssuer()
                        .getValue())
                        .getAssertionConsumerService(authnRequest, null);
        if (assertionConsumerService == null) {
            return null;
        }
        return assertionConsumerService.getBinding()
                .getUri();
    }
}
