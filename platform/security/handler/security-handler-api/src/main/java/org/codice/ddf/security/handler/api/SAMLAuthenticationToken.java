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
package org.codice.ddf.security.handler.api;

import java.security.Principal;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

public class SAMLAuthenticationToken extends BaseAuthenticationToken {
  private static final Logger LOGGER = LoggerFactory.getLogger(SAMLAuthenticationToken.class);

  boolean reference = true;

  /**
   * Constructor that only allows SecurityToken objects to be used as the credentials.
   *
   * @param principal represents the
   * @param token
   */
  public SAMLAuthenticationToken(Principal principal, SecurityToken token) {
    super(principal, token);
    reference = false;
  }

  public SAMLAuthenticationToken(Principal principal, String samlRef) {
    super(principal, samlRef);
    reference = true;
  }

  public boolean isReference() {
    return reference;
  }

  public void replaceReferenece(SecurityToken token) {
    if (reference) {
      credentials = token;
      reference = false;
    } else {
      LOGGER.debug("Current token is not a reference - call to replace is ignored.");
    }
  }

  /**
   * Returns the SAML token as a DOM Element.
   *
   * @return the SAML token as a DOM element or null if it doesn't exist
   */
  public Element getSAMLTokenAsElement() {
    if (reference) {
      LOGGER.debug("Attempting to return a SAML token without converting from a reference.");
      return null;
    }

    SecurityToken token = (SecurityToken) getCredentials();
    if (token != null) {
      return token.getToken();
    }
    return null;
  }

  @Override
  public String getCredentialsAsXMLString() {
    String creds = "";
    Element element = getSAMLTokenAsElement();
    if (element != null) {
      DOMImplementationLS lsImpl =
          (DOMImplementationLS)
              element.getOwnerDocument().getImplementation().getFeature("LS", "3.0");
      if (null != lsImpl) {
        LSSerializer serializer = lsImpl.createLSSerializer();
        serializer
            .getDomConfig()
            .setParameter(
                "xml-declaration",
                false); // by default its true, so set it to false to get String without
        // xml-declaration
        creds = serializer.writeToString(element);
      }
      LOGGER.trace("XML representation of SAML token: {}", creds);
    }
    return creds;
  }
}
