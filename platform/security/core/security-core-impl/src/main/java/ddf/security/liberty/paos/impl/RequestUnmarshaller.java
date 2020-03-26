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

import ddf.security.liberty.paos.Request;
import javax.annotation.Nonnull;
import javax.xml.namespace.QName;
import net.shibboleth.utilities.java.support.xml.QNameSupport;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.core.xml.schema.XSBooleanValue;
import org.opensaml.saml.common.AbstractSAMLObjectUnmarshaller;
import org.opensaml.soap.soap11.ActorBearing;
import org.opensaml.soap.soap11.MustUnderstandBearing;
import org.w3c.dom.Attr;

public class RequestUnmarshaller extends AbstractSAMLObjectUnmarshaller {

  /** {@inheritDoc} */
  @Override
  protected void processAttribute(@Nonnull XMLObject samlObject, @Nonnull Attr attribute)
      throws UnmarshallingException {
    RequestImpl request = (RequestImpl) samlObject;

    QName attrName = QNameSupport.getNodeQName(attribute);
    if (MustUnderstandBearing.SOAP11_MUST_UNDERSTAND_ATTR_NAME.equals(attrName)) {
      request.setSOAP11MustUnderstand(XSBooleanValue.valueOf(attribute.getValue()));
    } else if (ActorBearing.SOAP11_ACTOR_ATTR_NAME.equals(attrName)) {
      request.setSOAP11Actor(attribute.getValue());
    } else if (Request.RESPONSE_CONSUMER_URL_ATTRIB_NAME.equals(attribute.getLocalName())) {
      request.setResponseConsumerURL(attribute.getValue());
    } else if (Request.SERVICE_ATTRIB_NAME.equals(attribute.getLocalName())) {
      request.setService(attribute.getValue());
    } else if (Request.MESSAGE_ID_ATTRIB_NAME.equals(attribute.getLocalName())) {
      request.setMessageID(attribute.getValue());
    } else {
      super.processAttribute(samlObject, attribute);
    }
  }
}
