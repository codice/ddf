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
import org.w3c.dom.Element;

public interface LogoutMessage {

  LogoutWrapper extractXmlObject(String samlLogoutResponse)
      throws LogoutSecurityException, XMLStreamException;

  LogoutWrapper extractSamlLogoutResponse(String samlLogoutResponse)
      throws XMLStreamException, LogoutSecurityException;

  LogoutWrapper extractSamlLogoutRequest(String samlLogoutRequest)
      throws XMLStreamException, LogoutSecurityException;

  LogoutWrapper buildLogoutRequest(
      String nameIdString, String issuerOrEntityId, List<String> sessionIndexes);

  LogoutWrapper buildLogoutResponse(String issuerOrEntityId, String statusCodeValue);

  LogoutWrapper buildLogoutResponse(
      String issuerOrEntityId, String statusCodeValue, String inResponseTo);

  LogoutWrapper buildLogoutResponse(
      String issuerOrEntityId,
      String topLevelStatusCode,
      String secondLevelStatusCode,
      String inResponseTo);

  Element getElementFromSaml(LogoutWrapper xmlObject) throws LogoutSecurityException;

  String sendSamlLogoutRequest(
      LogoutWrapper request, String targetUri, boolean isSoap, @Nullable Cookie cookie)
      throws IOException, LogoutSecurityException;

  URI signSamlGetResponse(LogoutWrapper samlObject, URI uriNameMeLater, @Nullable String relayState)
      throws LogoutSecurityException, SignatureException, IOException;

  URI signSamlGetRequest(LogoutWrapper samlObject, URI uriNameMeLater, @Nullable String relayState)
      throws LogoutSecurityException, SignatureException, IOException;
}
