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
package org.codice.ddf.security.idp.binding.api.impl;

import ddf.security.samlp.SamlProtocol.Binding;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SimpleSign.SignatureException;
import ddf.security.samlp.SystemCrypto;
import ddf.security.samlp.impl.EntityInformation;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.stream.XMLStreamException;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.util.DOM2Writer;
import org.codice.ddf.security.idp.binding.api.ResponseCreator;
import org.codice.ddf.security.idp.plugin.SamlPresignPlugin;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class ResponseCreatorImpl implements ResponseCreator {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResponseCreatorImpl.class);

  private final Map<String, EntityInformation> serviceProviders;

  protected final SystemCrypto systemCrypto;

  private SimpleSign simpleSign;

  private final Set<SamlPresignPlugin> presignPlugins;

  private final List<String> spMetadata;

  private final Set<Binding> supportedBindings;

  public ResponseCreatorImpl(
      SystemCrypto systemCrypto,
      Map<String, EntityInformation> serviceProviders,
      Set<SamlPresignPlugin> presignPlugins,
      List<String> spMetadata,
      Set<Binding> supportedBindings) {
    this.systemCrypto = systemCrypto;
    this.serviceProviders = serviceProviders;
    this.simpleSign = new SimpleSign(systemCrypto);
    this.presignPlugins = presignPlugins;
    this.spMetadata = spMetadata;
    this.supportedBindings = supportedBindings;
  }

  public String getAssertionConsumerServiceURL(AuthnRequest authnRequest) {
    LOGGER.debug("Attempting to determine AssertionConsumerServiceURL.");
    EntityInformation.ServiceInfo assertionConsumerService =
        serviceProviders
            .get(authnRequest.getIssuer().getValue())
            .getAssertionConsumerService(
                authnRequest, null, authnRequest.getAssertionConsumerServiceIndex());

    if (assertionConsumerService == null) {
      throw new IllegalArgumentException(
          "No valid AssertionConsumerServiceURL available for given AuthnRequest.");
    }
    return assertionConsumerService.getUrl();
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

  protected Response processPresignPlugins(AuthnRequest authnRequest, Response response) {
    LOGGER.debug("Processing SAML Presign plugins.");
    for (SamlPresignPlugin presignPlugin : presignPlugins) {
      presignPlugin.processPresign(response, authnRequest, spMetadata, supportedBindings);
    }
    return resignAssertions(response);
  }

  private Response resignAssertions(Response response) {
    try {
      for (Assertion assertion : response.getAssertions()) {
        getSimpleSign().resignAssertion(assertion);
      }

      Document doc = DOMUtils.createDocument();
      Element requestElement = OpenSAMLUtil.toDom(response, doc);
      String responseMessage = DOM2Writer.nodeToString(requestElement);
      final Document responseDoc =
          StaxUtils.read(
              new ByteArrayInputStream(responseMessage.getBytes(StandardCharsets.UTF_8)));
      final XMLObject responseXmlObject = OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
      return (Response) responseXmlObject;
    } catch (SignatureException | WSSecurityException | XMLStreamException e) {
      LOGGER.warn("Error resigning: {}", e.getMessage());

      return response;
    }
  }
}
