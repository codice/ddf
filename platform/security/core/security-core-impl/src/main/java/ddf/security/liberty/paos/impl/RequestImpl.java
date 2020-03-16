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
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSBooleanValue;
import org.opensaml.saml.common.AbstractSAMLObject;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.soap.soap11.ActorBearing;
import org.opensaml.soap.soap11.MustUnderstandBearing;

public class RequestImpl extends AbstractSAMLObject
    implements Request, SAMLObject, MustUnderstandBearing, ActorBearing {

  private String responseConsumerURL;

  private String service;

  private String messageID;

  private String soap11Actor;

  private XSBooleanValue soap11MustUnderstand;

  protected RequestImpl(
      @Nullable String namespaceURI,
      @Nonnull String elementLocalName,
      @Nullable String namespacePrefix) {
    super(namespaceURI, elementLocalName, namespacePrefix);
  }

  @Override
  public String getResponseConsumerURL() {
    return responseConsumerURL;
  }

  @Override
  public void setResponseConsumerURL(String newResponseConsumerURL) {
    this.responseConsumerURL = newResponseConsumerURL;
  }

  @Override
  public String getService() {
    return service;
  }

  @Override
  public void setService(String newService) {
    this.service = prepareForAssignment(service, newService);
  }

  @Override
  public String getMessageID() {
    return messageID;
  }

  @Override
  public void setMessageID(String newMessageID) {
    this.messageID = prepareForAssignment(messageID, newMessageID);
  }

  @Nullable
  @Override
  public List<XMLObject> getOrderedChildren() {
    return null;
  }

  @Nullable
  @Override
  public String getSOAP11Actor() {
    return soap11Actor;
  }

  @Override
  public void setSOAP11Actor(@Nullable String soap11Actor) {
    this.soap11Actor = prepareForAssignment(this.soap11Actor, soap11Actor);
  }

  @Nullable
  @Override
  public Boolean isSOAP11MustUnderstand() {
    if (soap11MustUnderstand != null) {
      return soap11MustUnderstand.getValue();
    }
    return Boolean.FALSE;
  }

  @Nullable
  @Override
  public XSBooleanValue isSOAP11MustUnderstandXSBoolean() {
    return soap11MustUnderstand;
  }

  @Override
  public void setSOAP11MustUnderstand(@Nullable Boolean aBoolean) {
    if (aBoolean != null) {
      soap11MustUnderstand =
          prepareForAssignment(soap11MustUnderstand, new XSBooleanValue(aBoolean, true));
    } else {
      soap11MustUnderstand = prepareForAssignment(soap11MustUnderstand, null);
    }
  }

  @Override
  public void setSOAP11MustUnderstand(@Nullable XSBooleanValue xsBooleanValue) {
    soap11MustUnderstand = prepareForAssignment(soap11MustUnderstand, xsBooleanValue);
  }
}
