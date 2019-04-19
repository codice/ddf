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
package org.codice.ddf.security.validator.guest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import ddf.security.principal.GuestPrincipal;
import java.util.Base64;
import java.util.UUID;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.codice.ddf.security.handler.api.BSTAuthenticationToken;
import org.codice.ddf.security.handler.api.GuestAuthenticationToken;
import org.junit.Before;
import org.junit.Test;

public class GuestValidatorTest {

  private ReceivedToken receivedToken;

  private ReceivedToken receivedBadToken;

  private ReceivedToken receivedTokenIpv6;

  private GuestValidator validator;

  private TokenValidatorParameters parameters;

  private ReceivedToken receivedAnyRealmToken;

  private ReceivedToken receivedTokenIpv6Reachability;

  @Before
  public void setup() {
    validator = new GuestValidator();
    GuestAuthenticationToken guestAuthenticationToken = new GuestAuthenticationToken("127.0.0.1");

    GuestAuthenticationToken guestAuthenticationTokenAnyRealm =
        new GuestAuthenticationToken("127.0.0.1");

    GuestAuthenticationToken guestAuthenticationTokenIpv6 =
        new GuestAuthenticationToken("0:0:0:0:0:0:0:1");

    GuestAuthenticationToken guestAuthenticationTokenIpv6Reachability =
        new GuestAuthenticationToken("0:0:0:0:0:0:0:1%4");

    BinarySecurityTokenType binarySecurityTokenType = new BinarySecurityTokenType();
    binarySecurityTokenType.setValueType(GuestAuthenticationToken.GUEST_TOKEN_VALUE_TYPE);
    binarySecurityTokenType.setEncodingType(BSTAuthenticationToken.BASE64_ENCODING);
    binarySecurityTokenType.setId(GuestAuthenticationToken.BST_GUEST_LN);
    binarySecurityTokenType.setValue(guestAuthenticationToken.getEncodedCredentials());
    JAXBElement<BinarySecurityTokenType> binarySecurityTokenElement =
        new JAXBElement<BinarySecurityTokenType>(
            new QName(
                "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                "BinarySecurityToken"),
            BinarySecurityTokenType.class,
            binarySecurityTokenType);

    BinarySecurityTokenType binarySecurityTokenTypeBadToken = new BinarySecurityTokenType();
    binarySecurityTokenTypeBadToken.setValueType(GuestAuthenticationToken.GUEST_TOKEN_VALUE_TYPE);
    binarySecurityTokenTypeBadToken.setEncodingType(BSTAuthenticationToken.BASE64_ENCODING);
    binarySecurityTokenTypeBadToken.setId(GuestAuthenticationToken.BST_GUEST_LN);
    binarySecurityTokenTypeBadToken.setValue(
        Base64.getEncoder().encodeToString("NotGuest".getBytes()));
    JAXBElement<BinarySecurityTokenType> binarySecurityTokenElementBadToken =
        new JAXBElement<BinarySecurityTokenType>(
            new QName(
                "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                "BinarySecurityToken"),
            BinarySecurityTokenType.class,
            binarySecurityTokenTypeBadToken);

    BinarySecurityTokenType binarySecurityTokenTypeAnyRealm = new BinarySecurityTokenType();
    binarySecurityTokenTypeAnyRealm.setValueType(GuestAuthenticationToken.GUEST_TOKEN_VALUE_TYPE);
    binarySecurityTokenTypeAnyRealm.setEncodingType(BSTAuthenticationToken.BASE64_ENCODING);
    binarySecurityTokenTypeAnyRealm.setId(GuestAuthenticationToken.BST_GUEST_LN);
    binarySecurityTokenTypeAnyRealm.setValue(
        guestAuthenticationTokenAnyRealm.getEncodedCredentials());
    JAXBElement<BinarySecurityTokenType> binarySecurityTokenElementAnyRealm =
        new JAXBElement<BinarySecurityTokenType>(
            new QName(
                "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                "BinarySecurityToken"),
            BinarySecurityTokenType.class,
            binarySecurityTokenTypeAnyRealm);

    BinarySecurityTokenType binarySecurityTokenTypeIpv6 = new BinarySecurityTokenType();
    binarySecurityTokenTypeIpv6.setValueType(GuestAuthenticationToken.GUEST_TOKEN_VALUE_TYPE);
    binarySecurityTokenTypeIpv6.setEncodingType(BSTAuthenticationToken.BASE64_ENCODING);
    binarySecurityTokenTypeIpv6.setId(GuestAuthenticationToken.BST_GUEST_LN);
    binarySecurityTokenTypeIpv6.setValue(guestAuthenticationTokenIpv6.getEncodedCredentials());
    JAXBElement<BinarySecurityTokenType> binarySecurityTokenElementIpv6 =
        new JAXBElement<BinarySecurityTokenType>(
            new QName(
                "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                "BinarySecurityToken"),
            BinarySecurityTokenType.class,
            binarySecurityTokenTypeIpv6);

    BinarySecurityTokenType binarySecurityTokenTypeIpv6Reachability = new BinarySecurityTokenType();
    binarySecurityTokenTypeIpv6Reachability.setValueType(
        GuestAuthenticationToken.GUEST_TOKEN_VALUE_TYPE);
    binarySecurityTokenTypeIpv6Reachability.setEncodingType(BSTAuthenticationToken.BASE64_ENCODING);
    binarySecurityTokenTypeIpv6Reachability.setId(GuestAuthenticationToken.BST_GUEST_LN);
    binarySecurityTokenTypeIpv6Reachability.setValue(
        guestAuthenticationTokenIpv6Reachability.getEncodedCredentials());
    JAXBElement<BinarySecurityTokenType> binarySecurityTokenElementIpv6Reachability =
        new JAXBElement<BinarySecurityTokenType>(
            new QName(
                "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                "BinarySecurityToken"),
            BinarySecurityTokenType.class,
            binarySecurityTokenTypeIpv6Reachability);

    receivedToken = new ReceivedToken(binarySecurityTokenElement);
    receivedAnyRealmToken = new ReceivedToken(binarySecurityTokenElementAnyRealm);
    receivedBadToken = new ReceivedToken(binarySecurityTokenElementBadToken);
    receivedTokenIpv6 = new ReceivedToken(binarySecurityTokenElementIpv6);
    receivedTokenIpv6Reachability = new ReceivedToken(binarySecurityTokenElementIpv6Reachability);
    parameters = new TokenValidatorParameters();
    parameters.setToken(receivedToken);
  }

  @Test
  public void testCanHandleToken() throws JAXBException {
    boolean canHandle = validator.canHandleToken(receivedToken);

    assertTrue(canHandle);
  }

  @Test
  public void testCanHandleAnyRealmToken() throws JAXBException {
    boolean canHandle = validator.canHandleToken(receivedAnyRealmToken);

    assertTrue(canHandle);
  }

  @Test
  public void testCanValidateToken() {
    TokenValidatorResponse response = validator.validateToken(parameters);

    assertEquals(ReceivedToken.STATE.VALID, response.getToken().getState());

    assertThat(response.getToken().getPrincipal(), instanceOf(GuestPrincipal.class));
  }

  @Test
  public void testCanValidateAnyRealmToken() {
    TokenValidatorParameters params = new TokenValidatorParameters();
    params.setToken(receivedAnyRealmToken);
    TokenValidatorResponse response = validator.validateToken(params);

    assertEquals(ReceivedToken.STATE.VALID, response.getToken().getState());
  }

  @Test
  public void testCanValidateIpv6Token() {
    TokenValidatorParameters params = new TokenValidatorParameters();
    params.setToken(receivedTokenIpv6);
    TokenValidatorResponse response = validator.validateToken(params);

    assertEquals(ReceivedToken.STATE.VALID, response.getToken().getState());
  }

  @Test
  public void testCanValidateIpv6ReachabilityToken() {
    TokenValidatorParameters params = new TokenValidatorParameters();
    params.setToken(receivedTokenIpv6Reachability);
    TokenValidatorResponse response = validator.validateToken(params);

    assertEquals(ReceivedToken.STATE.VALID, response.getToken().getState());
  }

  @Test
  public void testCanValidateBadToken() {
    parameters.setToken(receivedBadToken);
    TokenValidatorResponse response = validator.validateToken(parameters);

    assertEquals(ReceivedToken.STATE.INVALID, response.getToken().getState());
  }

  @Test
  public void reusesId() {
    String principal1 = validator.validateToken(parameters).getPrincipal().getName();
    String principal2 = validator.validateToken(parameters).getPrincipal().getName();

    assertEquals(principal1, principal2);
    assertThat(principal1.length(), is("Guest@".length() + UUID.randomUUID().toString().length()));
    assertNotEquals(principal1, parameters.getToken().getPrincipal().getName());
  }
}
