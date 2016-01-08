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
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.w3c.dom.Element;

public interface LogoutMessage {
    String getIdpSingleLogoutLocation(@NotNull IDPSSODescriptor descriptor);

    SignableSAMLObject extractXmlObject(@NotNull String samlLogoutResponse)
            throws WSSecurityException, XMLStreamException;

    LogoutResponse extractSamlLogoutResponse(@NotNull String samlLogoutResponse)
            throws XMLStreamException, WSSecurityException;

    LogoutRequest extractSamlLogoutRequest(@NotNull String samlLogoutRequest)
            throws XMLStreamException, WSSecurityException;

    LogoutRequest buildLogoutRequest(@NotNull String nameIdString,
            @NotNull String issuerOrEntityId);

    LogoutResponse buildLogoutResponse(@NotNull String issuerOrEntityId,
            @NotNull String statusCodeValue);

    LogoutResponse buildLogoutResponse(@NotNull String issuerOrEntityId,
            @NotNull String statusCodeValue, String inResponseTo);

    Element getElementFromSaml(@NotNull XMLObject xmlObject) throws WSSecurityException;

    String sendSamlLogoutRequest(@NotNull LogoutRequest request, @NotNull String targetUri)
            throws IOException, WSSecurityException;

    URI signSamlGetResponse(@NotNull SAMLObject samlObject, @NotNull URI uriNameMeLater,
            String relayState)
            throws WSSecurityException, SimpleSign.SignatureException, IOException;

    URI signSamlGetRequest(@NotNull SAMLObject samlObject, @NotNull URI uriNameMeLater,
            String relayState)
            throws WSSecurityException, SimpleSign.SignatureException, IOException;
}