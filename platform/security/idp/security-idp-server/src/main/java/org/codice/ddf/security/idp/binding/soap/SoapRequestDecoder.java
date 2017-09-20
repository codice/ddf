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

import ddf.security.samlp.SamlProtocol;
import java.util.Iterator;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPPart;
import javax.xml.stream.XMLStreamException;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.codice.ddf.security.idp.binding.api.RequestDecoder;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.AuthnRequest;

public class SoapRequestDecoder implements RequestDecoder {

  @Override
  public AuthnRequest decodeRequest(String samlRequest) {
    XMLObject authnXmlObj;
    try {
      SOAPPart soapMessage = SamlProtocol.parseSoapMessage(samlRequest);
      try {
        authnXmlObj =
            SamlProtocol.getXmlObjectFromNode(soapMessage.getEnvelope().getBody().getFirstChild());
      } catch (WSSecurityException | SOAPException ex) {
        throw new IllegalArgumentException("Unable to convert AuthnRequest document to XMLObject.");
      }
      if (!(authnXmlObj instanceof AuthnRequest)) {
        throw new IllegalArgumentException("SAMLRequest object is not AuthnRequest.");
      }
    } catch (XMLStreamException e) {
      throw new IllegalArgumentException("stuff");
    }
    return (AuthnRequest) authnXmlObj;
  }

  public String decodeRelayState(String samlRequest) {
    String relayState = null;
    try {
      SOAPPart soapMessage = SamlProtocol.parseSoapMessage(samlRequest);
      SOAPEnvelope envelope = soapMessage.getEnvelope();
      SOAPHeader header = envelope.getHeader();
      Iterator iterator = header.examineAllHeaderElements();
      while (iterator.hasNext()) {
        SOAPHeaderElement soapHeaderElement = (SOAPHeaderElement) iterator.next();
        if ("RelayState".equals(soapHeaderElement.getLocalName())) {
          relayState = soapHeaderElement.getValue();
          break;
        }
      }
    } catch (XMLStreamException e) {
      throw new IllegalArgumentException("Unable to convert parse SOAP request.");
    } catch (SOAPException e) {
      throw new IllegalArgumentException("Unable to get SOAP envelope.");
    }
    return relayState;
  }
}
