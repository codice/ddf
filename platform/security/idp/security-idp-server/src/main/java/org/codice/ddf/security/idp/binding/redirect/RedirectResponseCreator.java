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
package org.codice.ddf.security.idp.binding.redirect;

import ddf.security.samlp.SamlProtocol.Binding;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;
import ddf.security.samlp.impl.EntityInformation;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.rs.security.saml.sso.SSOConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.util.DOM2Writer;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.codice.ddf.security.idp.binding.api.ResponseCreator;
import org.codice.ddf.security.idp.binding.api.impl.ResponseCreatorImpl;
import org.codice.ddf.security.idp.plugin.SamlPresignPlugin;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class RedirectResponseCreator extends ResponseCreatorImpl implements ResponseCreator {

  private static final Logger LOGGER = LoggerFactory.getLogger(RedirectResponseCreator.class);

  public RedirectResponseCreator(
      SystemCrypto systemCrypto,
      Map<String, EntityInformation> serviceProviders,
      Set<SamlPresignPlugin> presignPlugins,
      List<String> spMetadata,
      Set<Binding> supportedBindings) {
    super(systemCrypto, serviceProviders, presignPlugins, spMetadata, supportedBindings);
  }

  @Override
  public Response getSamlpResponse(
      String relayState,
      AuthnRequest authnRequest,
      org.opensaml.saml.saml2.core.Response samlResponse,
      NewCookie cookie,
      String responseTemplate)
      throws IOException, SimpleSign.SignatureException, WSSecurityException {
    LOGGER.debug("Configuring SAML Response for Redirect.");

    Document doc = DOMUtils.createDocument();
    doc.appendChild(doc.createElement("root"));

    org.opensaml.saml.saml2.core.Response processingResponse =
        processPresignPlugins(authnRequest, samlResponse);

    URI location = signSamlGetResponse(processingResponse, authnRequest, relayState);
    String redirectUpdated = responseTemplate.replace("{{redirect}}", location.toString());
    Response.ResponseBuilder ok = Response.ok(redirectUpdated);
    if (cookie != null) {
      ok = ok.cookie(cookie);
    }
    return ok.build();
  }

  protected URI signSamlGetResponse(
      org.opensaml.saml.saml2.core.Response samlResponse,
      AuthnRequest authnRequest,
      String relayState)
      throws WSSecurityException, SimpleSign.SignatureException, IOException {
    LOGGER.debug("Signing SAML response for redirect.");
    Document doc = DOMUtils.createDocument();
    doc.appendChild(doc.createElement("root"));
    String encodedResponse =
        URLEncoder.encode(
            RestSecurity.deflateAndBase64Encode(
                DOM2Writer.nodeToString(OpenSAMLUtil.toDom(samlResponse, doc, false))),
            "UTF-8");
    StringBuilder requestToSign = new StringBuilder("SAMLResponse=").append(encodedResponse);
    if (relayState != null) {
      requestToSign
          .append("&RelayState=")
          .append(URLEncoder.encode(relayState, StandardCharsets.UTF_8.name()));
    }
    String assertionConsumerServiceURL = getAssertionConsumerServiceURL(authnRequest);
    UriBuilder uriBuilder = UriBuilder.fromUri(assertionConsumerServiceURL);
    uriBuilder.queryParam(SSOConstants.SAML_RESPONSE, encodedResponse);
    if (relayState != null) {
      uriBuilder.queryParam(SSOConstants.RELAY_STATE, relayState);
    }
    getSimpleSign().signUriString(requestToSign.toString(), uriBuilder);
    LOGGER.debug("Signing successful.");
    return uriBuilder.build();
  }
}
