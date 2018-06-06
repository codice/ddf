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
package ddf.security.samlp;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import javax.xml.stream.XMLStreamException;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.w3c.dom.Element;

public interface LogoutMessage {
  String getIdpSingleLogoutLocation(IDPSSODescriptor descriptor);

  SignableSAMLObject extractXmlObject(String samlLogoutResponse)
      throws WSSecurityException, XMLStreamException;

  LogoutResponse extractSamlLogoutResponse(String samlLogoutResponse)
      throws XMLStreamException, WSSecurityException;

  LogoutRequest extractSamlLogoutRequest(String samlLogoutRequest)
      throws XMLStreamException, WSSecurityException;

  LogoutRequest buildLogoutRequest(
      String nameIdString, String issuerOrEntityId, List<String> sessionIndexes);

  LogoutResponse buildLogoutResponse(String issuerOrEntityId, String statusCodeValue);

  LogoutResponse buildLogoutResponse(
      String issuerOrEntityId, String statusCodeValue, String inResponseTo);

  LogoutResponse buildLogoutResponse(
      String issuerOrEntityId,
      String topLevelStatusCode,
      String secondLevelStatusCode,
      String inResponseTo);

  Element getElementFromSaml(XMLObject xmlObject) throws WSSecurityException;

  String sendSamlLogoutRequest(
      LogoutRequest request, String targetUri, boolean isSoap, @Nullable Cookie cookie)
      throws IOException, WSSecurityException;

  URI signSamlGetResponse(SAMLObject samlObject, URI uriNameMeLater, @Nullable String relayState)
      throws WSSecurityException, SimpleSign.SignatureException, IOException;

  URI signSamlGetRequest(SAMLObject samlObject, URI uriNameMeLater, @Nullable String relayState)
      throws WSSecurityException, SimpleSign.SignatureException, IOException;
}
