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

import static ddf.security.samlp.SamlProtocol.PAOS_BINDING;
import static ddf.security.samlp.SamlProtocol.POST_BINDING;
import static ddf.security.samlp.SamlProtocol.REDIRECT_BINDING;
import static ddf.security.samlp.SamlProtocol.SOAP_BINDING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.junit.Test;
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;

public class SamlProtocolTest {

  @Test
  public void testCreateResponse() throws WSSecurityException {
    Response response =
        SamlProtocol.createResponse(
            SamlProtocol.createIssuer("myissuer"),
            SamlProtocol.createStatus("mystatus"),
            "myid",
            null);
    assertEquals("myissuer", response.getIssuer().getValue());
    assertEquals("mystatus", response.getStatus().getStatusCode().getValue());
    assertEquals("myid", response.getInResponseTo());
  }

  @Test
  public void testCreateIdpMetadata() {
    EntityDescriptor entityDescriptor =
        SamlProtocol.createIdpMetadata(
            "myid",
            "mysigningcert",
            "myencryptioncert",
            Arrays.asList("mynameid"),
            "redirectlocation",
            "postlocation",
            "soaplocation",
            "logoutlocation");
    assertEquals("myid", entityDescriptor.getEntityID());
    assertEquals(
        "mysigningcert",
        entityDescriptor
            .getIDPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL)
            .getKeyDescriptors()
            .get(0)
            .getKeyInfo()
            .getX509Datas()
            .get(0)
            .getX509Certificates()
            .get(0)
            .getValue());
    assertEquals(
        "myencryptioncert",
        entityDescriptor
            .getIDPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL)
            .getKeyDescriptors()
            .get(1)
            .getKeyInfo()
            .getX509Datas()
            .get(0)
            .getX509Certificates()
            .get(0)
            .getValue());
    assertEquals(
        "mynameid",
        entityDescriptor
            .getIDPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL)
            .getNameIDFormats()
            .get(0)
            .getFormat());
    assertEquals(
        "logoutlocation",
        entityDescriptor
            .getIDPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL)
            .getSingleLogoutServices()
            .get(0)
            .getLocation());
    List<SingleSignOnService> ssoServices =
        entityDescriptor
            .getIDPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL)
            .getSingleSignOnServices();
    assertTrue(
        ssoServices.stream()
            .filter(
                service ->
                    service.getBinding().equals(REDIRECT_BINDING)
                        && service.getLocation().equals("redirectlocation"))
            .findFirst()
            .isPresent());

    assertTrue(
        ssoServices.stream()
            .filter(
                service ->
                    service.getBinding().equals(POST_BINDING)
                        && service.getLocation().equals("postlocation"))
            .findFirst()
            .isPresent());

    assertTrue(
        ssoServices.stream()
            .filter(
                service ->
                    service.getBinding().equals(SOAP_BINDING)
                        && service.getLocation().equals("soaplocation"))
            .findFirst()
            .isPresent());

    assertNotNull(entityDescriptor.getCacheDuration());
  }

  @Test
  public void testCreateSpMetadata() {
    EntityDescriptor entityDescriptor =
        SamlProtocol.createSpMetadata(
            "myid",
            "mysigningcert",
            "myencryptioncert",
            Arrays.asList("mynameid"),
            "logoutlocation",
            "redirectlocation",
            "postlocation",
            "paoslocation");
    assertEquals("myid", entityDescriptor.getEntityID());
    assertEquals(
        "mysigningcert",
        entityDescriptor
            .getSPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL)
            .getKeyDescriptors()
            .get(0)
            .getKeyInfo()
            .getX509Datas()
            .get(0)
            .getX509Certificates()
            .get(0)
            .getValue());
    assertEquals(
        "myencryptioncert",
        entityDescriptor
            .getSPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL)
            .getKeyDescriptors()
            .get(1)
            .getKeyInfo()
            .getX509Datas()
            .get(0)
            .getX509Certificates()
            .get(0)
            .getValue());
    assertEquals(
        "mynameid",
        entityDescriptor
            .getSPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL)
            .getNameIDFormats()
            .get(0)
            .getFormat());
    assertEquals(
        "logoutlocation",
        entityDescriptor
            .getSPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL)
            .getSingleLogoutServices()
            .get(0)
            .getLocation());
    List<AssertionConsumerService> acServices =
        entityDescriptor
            .getSPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL)
            .getAssertionConsumerServices();
    assertTrue(
        acServices.stream()
            .filter(
                service ->
                    service.getBinding().equals(REDIRECT_BINDING)
                        && service.getLocation().equals("redirectlocation"))
            .findFirst()
            .isPresent());

    assertTrue(
        acServices.stream()
            .filter(
                service ->
                    service.getBinding().equals(POST_BINDING)
                        && service.getLocation().equals("postlocation"))
            .findFirst()
            .isPresent());

    assertTrue(
        acServices.stream()
            .filter(
                service ->
                    service.getBinding().equals(PAOS_BINDING)
                        && service.getLocation().equals("paoslocation"))
            .findFirst()
            .isPresent());

    assertNotNull(entityDescriptor.getCacheDuration());
  }

  @Test
  public void testCreateAttributeQueryWithDestination() {
    AttributeQuery attributeQuery =
        SamlProtocol.createAttributeQuery(
            SamlProtocol.createIssuer("myissuer"),
            SamlProtocol.createSubject(SamlProtocol.createNameID("mynameid")),
            "mydestination");
    assertEquals("myissuer", attributeQuery.getIssuer().getValue());
    assertEquals("mynameid", attributeQuery.getSubject().getNameID().getValue());
    assertEquals("mydestination", attributeQuery.getDestination());
  }

  @Test
  public void testCreateAttributeQueryWithoutDestination() {
    AttributeQuery attributeQuery =
        SamlProtocol.createAttributeQuery(
            SamlProtocol.createIssuer("myissuer"),
            SamlProtocol.createSubject(SamlProtocol.createNameID("mynameid")));
    assertEquals("myissuer", attributeQuery.getIssuer().getValue());
    assertEquals("mynameid", attributeQuery.getSubject().getNameID().getValue());
    assertNull(attributeQuery.getDestination());
  }

  @Test
  public void testCreateLogoutRequest() {
    LogoutRequest logoutRequest =
        SamlProtocol.createLogoutRequest(
            SamlProtocol.createIssuer("myissuer"),
            SamlProtocol.createNameID("mynameid"),
            "myid",
            Collections.emptyList());
    assertEquals("myissuer", logoutRequest.getIssuer().getValue());
    assertEquals("mynameid", logoutRequest.getNameID().getValue());
    assertEquals("myid", logoutRequest.getID());
  }

  @Test
  public void testCreateLogoutResponse() {
    LogoutResponse logoutResponse =
        SamlProtocol.createLogoutResponse(
            SamlProtocol.createIssuer("myissuer"),
            SamlProtocol.createStatus("mystatus"),
            "inResponseTo",
            "myid");
    assertEquals("myissuer", logoutResponse.getIssuer().getValue());
    assertEquals("mystatus", logoutResponse.getStatus().getStatusCode().getValue());
    assertEquals("inResponseTo", logoutResponse.getInResponseTo());
    assertEquals("myid", logoutResponse.getID());
  }

  @Test
  public void testCreateLogoutResponseWithoutInResponseTo() {
    LogoutResponse logoutResponse =
        SamlProtocol.createLogoutResponse(
            SamlProtocol.createIssuer("myissuer"), SamlProtocol.createStatus("mystatus"), "myid");
    assertEquals("myissuer", logoutResponse.getIssuer().getValue());
    assertEquals("mystatus", logoutResponse.getStatus().getStatusCode().getValue());
    assertEquals("myid", logoutResponse.getID());
    assertNull(logoutResponse.getInResponseTo());
  }
}
