/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.idp.server;

import com.google.common.collect.ImmutableSet;
import ddf.security.samlp.SamlProtocol.Binding;
import java.io.InputStream;
import java.security.cert.CertificateEncodingException;
import javax.jws.WebService;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.apache.cxf.rs.security.saml.sso.SSOConstants;
import org.apache.wss4j.common.ext.WSSecurityException;

/** IdP endpoint interface */
@WebService(name = "IdpService", targetNamespace = "")
@Path("/")
@SuppressWarnings("squid:S1214" /* Consts used by implementator */)
public interface Idp {

  String SAML_REQ = "SAMLRequest";

  String RELAY_STATE = "RelayState";

  String AUTH_METHOD = "AuthMethod";

  String ACS_URL = "ACSURL";

  String SAML_RESPONSE = "SAMLResponse";

  String ECP_RESPONSE = "ECPResponse";

  String ECP_RELAY_STATE = "ECPRelayState";

  String ECP_REQUEST_AUTHENTICATED = "ECPRequestAuthenticated";

  String IDP_STATE_OBJ = "IDP_STATE_OBJ";

  String SAML_TYPE = "SAML_TYPE";

  String PKI = "pki";

  String SAML = "saml";

  String GUEST = "guest";

  String USER_PASS = "up";

  String HTTP_POST_BINDING = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST";

  String SAML_SOAP_BINDING = "urn:oasis:names:tc:SAML:2.0:bindings:SOAP";

  String PAOS_BINDING = "urn:oasis:names:tc:SAML:2.0:bindings:PAOS";

  String HTTP_REDIRECT_BINDING = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect";

  String HTTP_ARTIFACT_BINDING = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact";

  String COOKIE = "org.codice.ddf.security.idp.session";

  String ORIGINAL_BINDING = "OriginalBinding";

  ImmutableSet<Binding> SUPPORTED_BINDINGS =
      ImmutableSet.of(Binding.HTTP_POST, Binding.HTTP_REDIRECT, Binding.SOAP);

  @POST
  @Path("/login")
  @Consumes({"text/xml", "application/soap+xml"})
  Response doSoapLogin(InputStream body, @Context HttpServletRequest request)
      throws WSSecurityException;

  /**
   * Returns the IdP login form.
   *
   * @param samlRequest
   * @param relayState
   * @param request
   * @return Response
   * @throws WSSecurityException
   */
  @POST
  @Path("/login")
  Response showPostLogin(
      @FormParam(SAML_REQ) String samlRequest,
      @FormParam(RELAY_STATE) String relayState,
      @Context HttpServletRequest request)
      throws WSSecurityException;

  /**
   * Returns the IdP login form.
   *
   * @param samlRequest
   * @param relayState
   * @param signatureAlgorithm
   * @param signature
   * @param request
   * @return Response
   * @throws WSSecurityException
   */
  @GET
  @Path("/login")
  Response showGetLogin(
      @QueryParam(SAML_REQ) String samlRequest,
      @QueryParam(RELAY_STATE) String relayState,
      @QueryParam(SSOConstants.SIG_ALG) String signatureAlgorithm,
      @QueryParam(SSOConstants.SIGNATURE) String signature,
      @Context HttpServletRequest request)
      throws WSSecurityException;

  /**
   * Processes a login attempt from the IdP login web app.
   *
   * @param samlRequest
   * @param relayState
   * @param authMethod
   * @param signatureAlgorithm
   * @param signature
   * @param request
   * @return Response
   */
  @GET
  @Path("/login/sso")
  Response processLogin(
      @QueryParam(SAML_REQ) String samlRequest,
      @QueryParam(RELAY_STATE) String relayState,
      @QueryParam(AUTH_METHOD) String authMethod,
      @QueryParam(SSOConstants.SIG_ALG) String signatureAlgorithm,
      @QueryParam(SSOConstants.SIGNATURE) String signature,
      @QueryParam(IdpEndpoint.ORIGINAL_BINDING) String originalBinding,
      @Context HttpServletRequest request);

  /**
   * Returns the metadata associated with this IdP.
   *
   * @return Response
   * @throws WSSecurityException
   * @throws CertificateEncodingException
   */
  @GET
  @Path("/login/metadata")
  @Produces("application/xml")
  Response retrieveMetadata() throws WSSecurityException, CertificateEncodingException;

  @GET
  @Path("/logout")
  Response processRedirectLogout(
      @QueryParam(SAML_REQ) String samlRequest,
      @QueryParam(SAML_RESPONSE) String samlResponse,
      @QueryParam(RELAY_STATE) String relayState,
      @QueryParam(SSOConstants.SIG_ALG) String signatureAlgorithm,
      @QueryParam(SSOConstants.SIGNATURE) String signature,
      @Context HttpServletRequest request)
      throws WSSecurityException, IdpException;

  @POST
  @Path("/logout")
  Response processPostLogout(
      @FormParam(SAML_REQ) String samlRequest,
      @FormParam(SAML_RESPONSE) String samlResponse,
      @FormParam(RELAY_STATE) String relayState,
      @Context HttpServletRequest request)
      throws WSSecurityException, IdpException;
}
