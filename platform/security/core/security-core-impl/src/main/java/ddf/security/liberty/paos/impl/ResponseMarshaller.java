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
package ddf.security.liberty.paos.impl;

import ddf.security.liberty.paos.Response;
import java.util.Optional;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.schema.XSBooleanValue;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.common.AbstractSAMLObjectMarshaller;
import org.opensaml.soap.soap11.ActorBearing;
import org.opensaml.soap.soap11.MustUnderstandBearing;
import org.w3c.dom.Element;

public class ResponseMarshaller extends AbstractSAMLObjectMarshaller {

  /** {@inheritDoc} */
  @Override
  protected void marshallAttributes(XMLObject xmlObject, Element domElement)
      throws MarshallingException {
    ResponseImpl response = (ResponseImpl) xmlObject;

    if (response.getRefToMessageID() != null) {
      domElement.setAttributeNS(
          null, Response.REF_TO_MESSAGE_ID_ATTRIB_NAME, response.getRefToMessageID());
    }
    XMLObjectSupport.marshallAttribute(
        MustUnderstandBearing.SOAP11_MUST_UNDERSTAND_ATTR_NAME,
        Optional.ofNullable(response.isSOAP11MustUnderstandXSBoolean())
            .orElse(XSBooleanValue.valueOf("false"))
            .toString(),
        domElement,
        false);
    if (response.getSOAP11Actor() != null) {
      XMLObjectSupport.marshallAttribute(
          ActorBearing.SOAP11_ACTOR_ATTR_NAME, response.getSOAP11Actor(), domElement, false);
    }
  }
}
