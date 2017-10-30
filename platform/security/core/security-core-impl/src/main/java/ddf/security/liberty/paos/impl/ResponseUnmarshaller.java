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
import javax.xml.namespace.QName;
import net.shibboleth.utilities.java.support.xml.QNameSupport;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.core.xml.schema.XSBooleanValue;
import org.opensaml.saml.common.AbstractSAMLObjectUnmarshaller;
import org.w3c.dom.Attr;

/** Unmarshaller for instances of {@link Response}. */
public class ResponseUnmarshaller extends AbstractSAMLObjectUnmarshaller {

  /** {@inheritDoc} */
  @Override
  protected void processAttribute(XMLObject samlObject, Attr attribute)
      throws UnmarshallingException {
    Response response = (Response) samlObject;

    QName attrName = QNameSupport.getNodeQName(attribute);
    if (Response.SOAP11_MUST_UNDERSTAND_ATTR_NAME.equals(attrName)) {
      response.setSOAP11MustUnderstand(XSBooleanValue.valueOf(attribute.getValue()));
    } else if (Response.SOAP11_ACTOR_ATTR_NAME.equals(attrName)) {
      response.setSOAP11Actor(attribute.getValue());
    } else if (Response.REF_TO_MESSAGE_ID_ATTRIB_NAME.equals(attribute.getLocalName())) {
      response.setRefToMessageID(attribute.getValue());
    } else {
      super.processAttribute(samlObject, attribute);
    }
  }
}
