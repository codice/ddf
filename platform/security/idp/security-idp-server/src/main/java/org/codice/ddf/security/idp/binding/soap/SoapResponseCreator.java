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
package org.codice.ddf.security.idp.binding.soap;

import ddf.security.samlp.SamlProtocol.Binding;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;
import ddf.security.samlp.impl.EntityInformation;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.util.DOM2Writer;
import org.codice.ddf.security.idp.binding.api.ResponseCreator;
import org.codice.ddf.security.idp.binding.api.impl.ResponseCreatorImpl;
import org.codice.ddf.security.idp.plugin.SamlPresignPlugin;
import org.codice.ddf.security.idp.server.Idp;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.ecp.RelayState;
import org.opensaml.saml.saml2.ecp.RequestAuthenticated;
import org.opensaml.saml.saml2.ecp.impl.RelayStateBuilder;
import org.opensaml.saml.saml2.ecp.impl.RequestAuthenticatedBuilder;
import org.opensaml.saml.saml2.ecp.impl.ResponseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class SoapResponseCreator extends ResponseCreatorImpl implements ResponseCreator {

  public static final Logger LOGGER = LoggerFactory.getLogger(SoapResponseCreator.class);

  public static final String HTTP_SCHEMAS_XMLSOAP_ORG_SOAP_ACTOR_NEXT =
      "http://schemas.xmlsoap.org/soap/actor/next";

  public SoapResponseCreator(
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
    LOGGER.trace("Configuring SAML Response for POST.");

    Document doc = DOMUtils.createDocument();
    doc.appendChild(doc.createElement("root"));

    org.opensaml.saml.saml2.core.Response processingResponse =
        processPresignPlugins(authnRequest, samlResponse);

    LOGGER.trace("Signing SAML POST Response.");
    getSimpleSign().signSamlObject(processingResponse);

    LOGGER.trace("Converting SAML Response to DOM");
    String assertionResponse = DOM2Writer.nodeToString(OpenSAMLUtil.toDom(processingResponse, doc));
    String submitFormUpdated =
        responseTemplate.replace("{{" + Idp.SAML_RESPONSE + "}}", assertionResponse);

    String ecpResponse = createEcpResponse(authnRequest);
    String ecpRelayState = relayState != null ? createEcpRelayState(relayState) : "";
    String ecpRequestAuthenticated = createEcpRequestAuthenticated(authnRequest);
    submitFormUpdated = submitFormUpdated.replace("{{" + Idp.ECP_RESPONSE + "}}", ecpResponse);
    submitFormUpdated = submitFormUpdated.replace("{{" + Idp.ECP_RELAY_STATE + "}}", ecpRelayState);
    submitFormUpdated =
        submitFormUpdated.replace(
            "{{" + Idp.ECP_REQUEST_AUTHENTICATED + "}}", ecpRequestAuthenticated);
    Response.ResponseBuilder ok = Response.ok(submitFormUpdated);
    return ok.build();
  }

  private String createEcpResponse(AuthnRequest authnRequest) throws WSSecurityException {
    ResponseBuilder responseBuilder = new ResponseBuilder();
    org.opensaml.saml.saml2.ecp.Response response = responseBuilder.buildObject();
    response.setSOAP11Actor(HTTP_SCHEMAS_XMLSOAP_ORG_SOAP_ACTOR_NEXT);
    response.setSOAP11MustUnderstand(true);

    response.setAssertionConsumerServiceURL(getAssertionConsumerServiceURL(authnRequest));
    return convertXmlObjectToString(response);
  }

  private String createEcpRelayState(String relayStateStr) throws WSSecurityException {
    RelayStateBuilder relayStateBuilder = new RelayStateBuilder();
    RelayState relayState = relayStateBuilder.buildObject();
    relayState.setSOAP11Actor(HTTP_SCHEMAS_XMLSOAP_ORG_SOAP_ACTOR_NEXT);
    relayState.setSOAP11MustUnderstand(true);
    relayState.setValue(relayStateStr);

    return convertXmlObjectToString(relayState);
  }

  private String createEcpRequestAuthenticated(AuthnRequest authnRequest)
      throws WSSecurityException {
    if (authnRequest.getSignature() != null) {
      RequestAuthenticatedBuilder requestAuthenticatedBuilder = new RequestAuthenticatedBuilder();
      RequestAuthenticated requestAuthenticated = requestAuthenticatedBuilder.buildObject();
      requestAuthenticated.setSOAP11Actor(HTTP_SCHEMAS_XMLSOAP_ORG_SOAP_ACTOR_NEXT);
      return convertXmlObjectToString(requestAuthenticated);
    }
    return "";
  }

  private String convertXmlObjectToString(XMLObject response) throws WSSecurityException {
    Document doc = DOMUtils.createDocument();
    doc.appendChild(doc.createElement("root"));
    return DOM2Writer.nodeToString(OpenSAMLUtil.toDom(response, doc));
  }
}
