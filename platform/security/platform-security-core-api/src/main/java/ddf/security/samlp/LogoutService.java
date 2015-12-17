/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.security.samlp;

import java.io.IOException;
import java.net.URI;

import javax.validation.constraints.NotNull;
import javax.xml.stream.XMLStreamException;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.SignableSAMLObject;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.xml.XMLObject;
import org.w3c.dom.Element;

public interface LogoutService {
    String getIdpSingleLogoutLocation(IDPSSODescriptor descriptor);

    SignableSAMLObject extractXmlObject(String samlLogoutResponse)
            throws WSSecurityException, XMLStreamException;

    LogoutResponse extractSamlLogoutResponse(String samlLogoutResponse)
            throws XMLStreamException, WSSecurityException;

    LogoutRequest extractSamlLogoutRequest(String samlLogoutRequest)
            throws XMLStreamException, WSSecurityException;

    LogoutRequest buildLogoutRequest(@NotNull String nameIdString, @NotNull String issuerOrEntityId);

    LogoutResponse buildLogoutResponse(String issuerOrEntityId, String statusCodeValue);

    LogoutResponse buildLogoutResponse(String issuerOrEntityId, String statusCodeValue,
            String inResponseTo);

    Element getElementFromSaml(XMLObject xmlObject) throws WSSecurityException;

    String sendSamlLogoutRequest(@NotNull LogoutRequest request, String targetUri)
            throws IOException, WSSecurityException;

    URI signSamlGetResponse(SAMLObject samlObject, URI uriNameMeLater, String relayState)
            throws WSSecurityException, SimpleSign.SignatureException, IOException;

    URI signSamlGetRequest(SAMLObject samlObject, URI uriNameMeLater, String relayState)
            throws WSSecurityException, SimpleSign.SignatureException, IOException;
}