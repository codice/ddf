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
package org.codice.ddf.security.idp.server;

import java.security.cert.CertificateEncodingException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.cxf.rs.security.saml.sso.SSOConstants;
import org.apache.wss4j.common.ext.WSSecurityException;

@Path("/")
public interface Idp {

    String SAML_REQ = "SAMLRequest";

    String RELAY_STATE = "RelayState";

    String AUTH_METHOD = "AuthMethod";

    String ACS_URL = "ACSURL";

    String SAML_RESPONSE = "SAMLResponse";

    String IDP_STATE_OBJ = "IDP_STATE_OBJ";

    String PKI = "pki";

    String GUEST = "guest";

    String USER_PASS = "up";

    String HTTP_POST_BINDING = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST";

    String SAML_SOAP_BINDING = "urn:oasis:names:tc:SAML:2.0:bindings:SOAP";

    String PAOS_BINDING = "urn:oasis:names:tc:SAML:2.0:bindings:PAOS";

    String HTTP_REDIRECT_BINDING = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect";

    String HTTP_ARTIFACT_BINDING = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact";

    String COOKIE = "org.codice.ddf.security.idp.session";

    @POST
    Response showPostLogin(@FormParam(SAML_REQ) String samlRequest,
            @FormParam(RELAY_STATE) String relayState, @Context HttpServletRequest request)
            throws WSSecurityException;

    @GET
    Response showGetLogin(@QueryParam(SAML_REQ) String samlRequest,
            @QueryParam(RELAY_STATE) String relayState,
            @QueryParam(SSOConstants.SIG_ALG) String signatureAlgorithm,
            @QueryParam(SSOConstants.SIGNATURE) String signature,
            @Context HttpServletRequest request) throws WSSecurityException;

    @GET
    @Path("/sso")
    Response processLogin(@QueryParam(SAML_REQ) String samlRequest,
            @QueryParam(RELAY_STATE) String relayState, @QueryParam(AUTH_METHOD) String authMethod,
            @QueryParam(SSOConstants.SIG_ALG) String signatureAlgorithm,
            @QueryParam(SSOConstants.SIGNATURE) String signature,
            @Context HttpServletRequest request);

    @GET
    @Path("/metadata")
    Response retrieveMetadata() throws WSSecurityException, CertificateEncodingException;

}
