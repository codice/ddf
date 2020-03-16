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
package org.codice.ddf.security.claims.attributequery.common;

import ddf.security.samlp.impl.SamlProtocol;
import ddf.security.samlp.impl.SimpleSign;
import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.platform.util.TransformerProperties;
import org.codice.ddf.platform.util.XMLUtils;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.soap.soap11.Envelope;
import org.opensaml.soap.soap11.impl.EnvelopeMarshaller;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.Signer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class AttributeQueryClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(AttributeQueryClient.class);

  private static final String SAML2_PROTOCOL = "urn:oasis:names:tc:SAML:2.0:protocol";

  private static final String SAML2_SUCCESS = "urn:oasis:names:tc:SAML:2.0:status:Success";

  private static final String SAML2_UNKNOWN_ATTR_PROFILE =
      "urn:oasis:names:tc:SAML:2.0:status:UnknownAttrProfile";

  private static final String SAML2_INVALID_ATTR_NAME_VALUE =
      "urn:oasis:names:tc:SAML:2.0:status:InvalidAttrNameOrValue";

  private static final String SAML2_UNKNOWN_PRINCIPAL =
      "urn:oasis:names:tc:SAML:2.0:status:UnknownPrincipal";

  private static final XMLUtils XML_UTILS = XMLUtils.getInstance();

  private Dispatch<StreamSource> dispatch;

  private SimpleSign simpleSign;

  private String externalAttributeStoreUrl;

  private String issuer;

  private String destination;

  public AttributeQueryClient(
      Dispatch<StreamSource> dispatch,
      SimpleSign simpleSign,
      String externalAttributeStoreUrl,
      String issuer,
      String destination) {
    LOGGER.debug("Initializing AttributeQueryClient.");
    this.dispatch = dispatch;
    this.simpleSign = simpleSign;
    this.externalAttributeStoreUrl = externalAttributeStoreUrl;
    this.issuer = issuer;
    this.destination = destination;
  }

  /**
   * Query the external attribute store using an AttributeQuery request.
   *
   * @return Assertion of the response.
   */
  public Assertion query(String username) {
    return retrieveResponse(signRequest(createRequest(username)));
  }

  private AttributeQuery createRequest(String username) {
    LOGGER.debug("Creating SAML Protocol AttributeQuery for user: {}.", username);

    AttributeQuery attributeQuery =
        SamlProtocol.createAttributeQuery(
            SamlProtocol.createIssuer(issuer),
            SamlProtocol.createSubject(SamlProtocol.createNameID(username)),
            destination);

    LOGGER.debug("SAML Protocol AttributeQuery created for user: {}.", username);

    return attributeQuery;
  }

  /**
   * Signs AttributeQuery request.
   *
   * @param attributeQuery request to be signed.
   * @return Document of the AttributeQuery.
   */
  private Document signRequest(AttributeQuery attributeQuery) throws AttributeQueryException {
    Element soapElement;
    try {
      // Create and set signature for request.
      simpleSign.signSamlObject(attributeQuery);

      // Create soap message for request.
      soapElement = createSoapMessage(attributeQuery);

      // Sign soap message.
      Signer.signObject(attributeQuery.getSignature());
    } catch (SignatureException | ddf.security.samlp.SignatureException e) {
      throw new AttributeQueryException("Error occurred during signing of the request.", e);
    }

    // Print AttributeQuery Request.
    if (LOGGER.isTraceEnabled()) {
      printXML("SAML Protocol AttributeQuery Request:\n{}", soapElement);
    }
    return soapElement.getOwnerDocument();
  }

  /**
   * Creates a SOAP message of the AttributeQuery request.
   *
   * @param attributeQuery is added to the SOAP message
   * @return soapElement is the Element of the SOAP message
   */
  private Element createSoapMessage(AttributeQuery attributeQuery) throws AttributeQueryException {
    LOGGER.debug("Creating SOAP message from the SAML AttributeQuery.");

    Envelope envelope = SamlProtocol.createSoapMessage(attributeQuery);

    LOGGER.debug("SOAP message from the SAML AttributeQuery created.");

    try {
      return new EnvelopeMarshaller().marshall(envelope);
    } catch (MarshallingException e) {
      throw new AttributeQueryException("Cannot marshall SOAP object to an Element.", e);
    }
  }

  /**
   * Retrieves the response and returns its SAML Assertion.
   *
   * @param requestDocument of the request.
   * @return Assertion of the response or null if the response is empty.
   * @throws AttributeQueryException
   */
  private Assertion retrieveResponse(Document requestDocument) throws AttributeQueryException {
    Assertion assertion = null;
    try {
      Document responseDocument = sendRequest(requestDocument);
      if (responseDocument == null) {
        return null;
      }

      // Print Response
      if (LOGGER.isTraceEnabled()) {
        printXML("SAML Response:\n {}", responseDocument);
      }

      // Extract Response from Soap message.
      NodeList elementsByTagNameNS =
          responseDocument.getElementsByTagNameNS(SAML2_PROTOCOL, "Response");
      if (elementsByTagNameNS == null) {
        throw new AttributeQueryException("Unable to find SAML Response.");
      }
      Node responseNode = elementsByTagNameNS.item(0);
      Element responseElement = (Element) responseNode;

      Unmarshaller unmarshaller =
          XMLObjectProviderRegistrySupport.getUnmarshallerFactory()
              .getUnmarshaller(responseElement);

      Response response = (Response) unmarshaller.unmarshall(responseElement);
      LOGGER.debug("Successfully marshalled Element to SAML Response.");
      if (response.getStatus().getStatusCode().getValue().equals(SAML2_SUCCESS)) {
        LOGGER.debug("Successful response, retrieved attributes.");
        // Should only have one assertion.
        assertion = response.getAssertions().get(0);

      } else {
        reportError(response.getStatus());
      }
      return assertion;
    } catch (UnmarshallingException e) {
      throw new AttributeQueryException("Unable to marshall Element to SAML Response.", e);
    }
  }

  /**
   * Sends the request to the external attribute store via a Dispatch client.
   *
   * @param requestDocument of the request.
   * @return Document of the response or null if the response is empty.
   * @throws AttributeQueryException
   */
  protected Document sendRequest(Document requestDocument) {
    TransformerProperties transformerProperties = new TransformerProperties();
    transformerProperties.addOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    String request = XML_UTILS.format(requestDocument, transformerProperties);

    StreamSource streamSource;
    try {
      streamSource = dispatch.invoke(new StreamSource(new StringReader(request)));
    } catch (Exception e) {
      throw new AttributeQueryException(
          String.format("Could not connect to: %s", this.externalAttributeStoreUrl), e);
    }
    String response = XML_UTILS.format(streamSource, transformerProperties);
    if (StringUtils.isBlank(response)) {
      LOGGER.debug("Response is empty.");
      return null;
    }

    DocumentBuilder documentBuilder;
    Document responseDoc;
    try {
      documentBuilder = XML_UTILS.getSecureDocumentBuilder(true);
      responseDoc = documentBuilder.parse(new InputSource(new StringReader(response)));
    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new AttributeQueryException("Unable to parse response string into an XML document.", e);
    }
    return responseDoc;
  }

  /**
   * If the response is bad, report the cause of the error.
   *
   * @param status Status of response object.
   */
  private void reportError(Status status) {
    String statusCodeValue = status.getStatusCode().getValue();
    switch (statusCodeValue) {
      case SAML2_UNKNOWN_ATTR_PROFILE:
        LOGGER.debug(
            "Error in the response: {}. Incorrect version number or incorrect parsing error.",
            statusCodeValue);
        break;
      case SAML2_INVALID_ATTR_NAME_VALUE:
        LOGGER.debug(
            "Error in the response: {}. Request attribute name is unknown.", statusCodeValue);
        break;
      case SAML2_UNKNOWN_PRINCIPAL:
        LOGGER.debug(
            "Error in the response: {}. Unknown principal name, user is not recognized.",
            statusCodeValue);
        break;
      default:
        LOGGER.debug("Error in the response: {}", statusCodeValue);
        break;
    }
    if (status.getStatusMessage() != null && status.getStatusMessage().getMessage() != null) {
      LOGGER.debug(status.getStatusMessage().getMessage());
    }
    // Allow bad response to go through.
  }

  /**
   * Prints the given XML.
   *
   * @param xmlNode Node to transform.
   * @param message Message to display.
   */
  private void printXML(String message, Node xmlNode) {
    TransformerProperties transformerProperties = new TransformerProperties();
    transformerProperties.addOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace(message, XML_UTILS.format(xmlNode, transformerProperties));
    }
  }

  public void setDispatch(Dispatch<StreamSource> dispatch) {
    this.dispatch = dispatch;
  }

  public void setSimpleSign(SimpleSign simpleSign) {
    this.simpleSign = simpleSign;
  }

  public void setExternalAttributeStoreUrl(String externalAttributeStoreUrl) {
    this.externalAttributeStoreUrl = externalAttributeStoreUrl;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public void setDestination(String destination) {
    this.destination = destination;
  }
}
