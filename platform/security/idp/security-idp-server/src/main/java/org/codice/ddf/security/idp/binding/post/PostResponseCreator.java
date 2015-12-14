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

import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.util.DOM2Writer;
import org.codice.ddf.security.idp.binding.api.ResponseCreator;
import org.codice.ddf.security.idp.binding.api.impl.ResponseCreatorImpl;
import org.codice.ddf.security.idp.server.Idp;
import org.opensaml.saml2.core.AuthnRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;
import ddf.security.samlp.impl.EntityInformation;

public class PostResponseCreator extends ResponseCreatorImpl implements ResponseCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostResponseCreator.class);

    public PostResponseCreator(SystemCrypto systemCrypto,
            Map<String, EntityInformation> serviceProviders) {
        super(systemCrypto, serviceProviders);
    }

    @Override
    public Response getSamlpResponse(String relayState, AuthnRequest authnRequest,
            org.opensaml.saml2.core.Response samlResponse, NewCookie cookie,
            String responseTemplate) throws WSSecurityException, SimpleSign.SignatureException {
        LOGGER.debug("Configuring SAML Response for POST.");
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
        LOGGER.debug("Signing SAML POST Response.");
        getSimpleSign().signSamlObject(samlResponse);
        LOGGER.debug("Converting SAML Response to DOM");
        String assertionResponse = DOM2Writer.nodeToString(OpenSAMLUtil.toDom(samlResponse, doc));
        String encodedSamlResponse = new String(Base64.encodeBase64(assertionResponse.getBytes()));
        String assertionConsumerServiceURL = getAssertionConsumerServiceURL(authnRequest);
        String submitFormUpdated = responseTemplate.replace("{{" + Idp.ACS_URL + "}}",
                assertionConsumerServiceURL);
        submitFormUpdated = submitFormUpdated.replace("{{" + Idp.SAML_TYPE + "}}", "SAMLResponse");
        submitFormUpdated = submitFormUpdated.replace("{{" + Idp.SAML_RESPONSE + "}}",
                encodedSamlResponse);
        submitFormUpdated = submitFormUpdated.replace("{{" + Idp.RELAY_STATE + "}}", relayState);
        Response.ResponseBuilder ok = Response.ok(submitFormUpdated);
        if (cookie != null) {
            ok = ok.cookie(cookie);
        }
        return ok.build();
    }
}
