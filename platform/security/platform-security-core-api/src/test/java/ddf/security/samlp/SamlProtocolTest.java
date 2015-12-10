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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.junit.Test;
import org.opensaml.saml2.core.AttributeQuery;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.metadata.EntityDescriptor;

public class SamlProtocolTest {

    @Test
    public void testCreateResponse() throws WSSecurityException {
        Response response = SamlProtocol.createResponse(SamlProtocol.createIssuer("myissuer"),
                SamlProtocol.createStatus("mystatus"), "myid", null);
        assertEquals("myissuer", response.getIssuer().getValue());
        assertEquals("mystatus", response.getStatus().getStatusCode().getValue());
        assertEquals("myid", response.getInResponseTo());
    }

    @Test
    public void testCreateIdpMetadata() {
        EntityDescriptor entityDescriptor = SamlProtocol
                .createIdpMetadata("myid", "mysigningcert", "myencryptioncert",
                        Arrays.asList("mynameid"), "redirectlocation", "postlocation",
                        "logoutlocation");
        assertEquals("myid", entityDescriptor.getEntityID());
        assertEquals("mysigningcert",
                entityDescriptor.getIDPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL)
                        .getKeyDescriptors().get(0).getKeyInfo().getX509Datas().get(0)
                        .getX509Certificates().get(0).getValue());
        assertEquals("myencryptioncert",
                entityDescriptor.getIDPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL)
                        .getKeyDescriptors().get(1).getKeyInfo().getX509Datas().get(0)
                        .getX509Certificates().get(0).getValue());
        assertEquals("mynameid",
                entityDescriptor.getIDPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL)
                        .getNameIDFormats().get(0).getFormat());
        assertEquals("redirectlocation",
                entityDescriptor.getIDPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL)
                        .getSingleSignOnServices().get(0).getLocation());
        assertEquals("postlocation",
                entityDescriptor.getIDPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL)
                        .getSingleSignOnServices().get(1).getLocation());
        assertEquals("logoutlocation",
                entityDescriptor.getIDPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL)
                        .getSingleLogoutServices().get(0).getLocation());
    }

    @Test
    public void testCreateSpMetadata() {
        EntityDescriptor entityDescriptor = SamlProtocol
                .createSpMetadata("myid", "mysigningcert", "myencryptioncert", "logoutlocation",
                        "redirectlocation", "postlocation");
        assertEquals("myid", entityDescriptor.getEntityID());
        assertEquals("mysigningcert",
                entityDescriptor.getSPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL)
                        .getKeyDescriptors().get(0).getKeyInfo().getX509Datas().get(0)
                        .getX509Certificates().get(0).getValue());
        assertEquals("myencryptioncert",
                entityDescriptor.getSPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL)
                        .getKeyDescriptors().get(1).getKeyInfo().getX509Datas().get(0)
                        .getX509Certificates().get(0).getValue());
        assertEquals("redirectlocation",
                entityDescriptor.getSPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL)
                        .getAssertionConsumerServices().get(0).getLocation());
        assertEquals("postlocation",
                entityDescriptor.getSPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL)
                        .getAssertionConsumerServices().get(1).getLocation());
        assertEquals("logoutlocation",
                entityDescriptor.getSPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL)
                        .getSingleLogoutServices().get(0).getLocation());
    }

    @Test
    public void testCreateAttributeQueryWithDestination() {
        AttributeQuery attributeQuery = SamlProtocol
                .createAttributeQuery(SamlProtocol.createIssuer("myissuer"),
                        SamlProtocol.createSubject(SamlProtocol.createNameID("mynameid")),
                        "mydestination");
        assertEquals("myissuer", attributeQuery.getIssuer().getValue());
        assertEquals("mynameid", attributeQuery.getSubject().getNameID().getValue());
        assertEquals("mydestination", attributeQuery.getDestination());
    }

    @Test
    public void testCreateAttributeQueryWithoutDestination() {
        AttributeQuery attributeQuery = SamlProtocol
                .createAttributeQuery(SamlProtocol.createIssuer("myissuer"),
                        SamlProtocol.createSubject(SamlProtocol.createNameID("mynameid")));
        assertEquals("myissuer", attributeQuery.getIssuer().getValue());
        assertEquals("mynameid", attributeQuery.getSubject().getNameID().getValue());
    }
}
